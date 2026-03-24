package com.adbdeck.feature.devices.ui

import adbdeck.feature.devices.generated.resources.*
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.adbdeck.core.adb.api.device.AdbDevice
import com.adbdeck.core.adb.api.device.DeviceEndpoint
import com.adbdeck.core.adb.api.device.SavedWifiDevice
import com.adbdeck.core.designsystem.AdbCornerRadius
import com.adbdeck.core.designsystem.Dimensions
import com.adbdeck.core.i18n.AdbCommonStringRes
import com.adbdeck.core.ui.*
import com.adbdeck.core.ui.alertdialogs.AdbAlertDialog
import com.adbdeck.core.ui.alertdialogs.AdbAlertDialogAction
import com.adbdeck.core.ui.buttons.AdbButtonSize
import com.adbdeck.core.ui.buttons.AdbButtonType
import com.adbdeck.core.ui.buttons.AdbFilledButton
import com.adbdeck.core.ui.textfields.AdbOutlinedTextField
import com.adbdeck.core.ui.textfields.AdbTextFieldSize
import com.adbdeck.core.ui.textfields.AdbTextFieldType
import com.adbdeck.feature.devices.*
import org.jetbrains.compose.resources.stringResource

/**
 * Основной экран управления ADB-устройствами.
 *
 * @param component Компонент, управляющий состоянием экрана.
 */
@Composable
fun DevicesScreen(component: DevicesComponent) {
    val state by component.state.collectAsState()
    var wifiConnectDialogState by remember { mutableStateOf<WifiHistoryConnectDialogState?>(null) }
    fun requestConnectHistoryDevice(historyItem: SavedWifiDevice) {
        val endpoint = DeviceEndpoint.fromAddress(historyItem.address)
        if (endpoint != null) {
            wifiConnectDialogState = WifiHistoryConnectDialogState(
                device = historyItem,
                host = endpoint.host,
                portText = endpoint.port.toString(),
            )
        } else {
            component.onConnectHistoryDevice(historyItem)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {

        // ── Двухпанельный layout ───────────────────────────────────────────────
        Row(modifier = Modifier.fillMaxSize()) {

            // Левая панель — список устройств
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            ) {
                DevicesToolbar(
                    isRefreshing = state.listState == DeviceListState.Loading,
                    onRefresh    = component::onRefresh,
                )
                HorizontalDivider()

                Box(modifier = Modifier.weight(1f)) {
                    when (val ls = state.listState) {
                        is DeviceListState.Loading ->
                            LoadingView(message = stringResource(Res.string.devices_screen_loading_devices))

                        is DeviceListState.Empty ->
                            if (state.wifiHistory.isEmpty()) {
                                EmptyView(
                                    message = stringResource(Res.string.devices_screen_empty_devices)
                                )
                            } else {
                                DevicesList(
                                    devices   = emptyList(),
                                    wifiHistory = state.wifiHistory,
                                    state     = state,
                                    component = component,
                                    onRequestConnectHistoryDevice = ::requestConnectHistoryDevice,
                                )
                            }

                        is DeviceListState.Error ->
                            ErrorView(message = ls.message, onRetry = component::onRefresh)

                        is DeviceListState.Success ->
                            DevicesList(
                                devices   = ls.devices,
                                wifiHistory = state.wifiHistory,
                                state     = state,
                                component = component,
                                onRequestConnectHistoryDevice = ::requestConnectHistoryDevice,
                            )
                    }
                }
            }

            // Правая панель — детали выбранного устройства
            val detailsDevice = (state.listState as? DeviceListState.Success)
                ?.devices?.find { it.deviceId == state.detailsDeviceId }

            AnimatedVisibility(
                visible = detailsDevice != null,
                enter   = slideInHorizontally { it },
                exit    = slideOutHorizontally { it },
            ) {
                if (detailsDevice != null) {
                    Row {
                        VerticalDivider()
                        DeviceDetailsPanel(
                            device                    = detailsDevice,
                            infoState                 = state.deviceInfos[detailsDevice.deviceId],
                            isSelected                = detailsDevice.deviceId == state.selectedDeviceId,
                            isActionRunning           = state.isActionRunning,
                            onClose                   = component::onCloseDetails,
                            onSelectDevice            = { component.onSelectDevice(detailsDevice) },
                            onRefreshInfo             = { component.onRefreshDeviceInfo(detailsDevice) },
                            onNavigateToLogcat        = {
                                component.onSelectDevice(detailsDevice)
                                component.onNavigateToLogcat()
                            },
                            onNavigateToPackages      = {
                                component.onSelectDevice(detailsDevice)
                                component.onNavigateToPackages()
                            },
                            onNavigateToSystemMonitor = {
                                component.onSelectDevice(detailsDevice)
                                component.onNavigateToSystemMonitor()
                            },
                            onRequestReboot           = { component.onRequestReboot(detailsDevice) },
                            onRequestRebootRecovery   = { component.onRequestRebootRecovery(detailsDevice) },
                            onRequestRebootBootloader = { component.onRequestRebootBootloader(detailsDevice) },
                            onRequestDisconnect       = { component.onRequestDisconnect(detailsDevice) },
                            modifier                  = Modifier
                                .width(300.dp)
                                .fillMaxHeight(),
                        )
                    }
                }
            }
        }

        val connectDialogState = wifiConnectDialogState
        if (connectDialogState != null) {
            val parsedPort = connectDialogState.portText.toIntOrNull()
            val isPortValid = parsedPort != null && parsedPort in 1..65_535
            val showPortError = connectDialogState.portText.isNotBlank() && !isPortValid

            AdbAlertDialog(
                onDismissRequest = { wifiConnectDialogState = null },
                title = stringResource(Res.string.devices_history_connect_dialog_title),
                confirmAction = AdbAlertDialogAction(
                    text = stringResource(Res.string.devices_history_connect_dialog_action_connect),
                    enabled = isPortValid,
                    onClick = {
                        val current = wifiConnectDialogState ?: return@AdbAlertDialogAction
                        val port = current.portText.toIntOrNull() ?: return@AdbAlertDialogAction
                        if (port !in 1..65_535) return@AdbAlertDialogAction

                        component.onConnectHistoryDevice(current.device, port)
                        wifiConnectDialogState = null
                    },
                    type = AdbButtonType.NEUTRAL,
                    size = AdbButtonSize.MEDIUM,
                ),
                dismissAction = AdbAlertDialogAction(
                    text = stringResource(AdbCommonStringRes.actionCancel),
                    onClick = { wifiConnectDialogState = null },
                    type = AdbButtonType.NEUTRAL,
                    size = AdbButtonSize.MEDIUM,
                ),
            ) {
                Text(
                    text = stringResource(
                        Res.string.devices_history_connect_dialog_host_value,
                        connectDialogState.host,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                AdbOutlinedTextField(
                    value = connectDialogState.portText,
                    onValueChange = { next ->
                        wifiConnectDialogState = connectDialogState.copy(
                            portText = next.filter(Char::isDigit).take(5),
                        )
                    },
                    placeholder = stringResource(Res.string.devices_history_connect_dialog_port_placeholder),
                    type = if (showPortError) AdbTextFieldType.DANGER else AdbTextFieldType.NEUTRAL,
                    size = AdbTextFieldSize.MEDIUM,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    supportingText = if (showPortError) {
                        stringResource(Res.string.devices_history_connect_dialog_port_error)
                    } else {
                        null
                    },
                )
            }
        }

        // ── Диалог подтверждения действия ─────────────────────────────────────
        val pending = state.pendingAction
        if (pending != null) {
            AdbAlertDialog(
                onDismissRequest = component::onCancelAction,
                title = localizePendingActionTitle(pending),
                confirmAction = AdbAlertDialogAction(
                    text = stringResource(AdbCommonStringRes.actionConfirm),
                    onClick = component::onConfirmAction,
                    type = AdbButtonType.DANGER,
                    size = AdbButtonSize.MEDIUM,
                    loading = state.isActionRunning,
                ),
                dismissAction = AdbAlertDialogAction(
                    text = stringResource(AdbCommonStringRes.actionCancel),
                    onClick = component::onCancelAction,
                    type = AdbButtonType.NEUTRAL,
                    size = AdbButtonSize.MEDIUM,
                ),
            ) {
                Text(localizePendingActionMessage(pending))
            }
        }

        // ── Баннер обратной связи ──────────────────────────────────────────────
        val feedback = state.actionFeedback
        if (feedback != null) {
            AdbBanner(
                message = localizeFeedbackMessage(feedback),
                type = if (feedback is DeviceActionFeedback.ActionError) {
                    AdbBannerType.ERROR
                } else {
                    AdbBannerType.SUCCESS
                },
                onDismiss = component::onDismissFeedback,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(16.dp),
            )
        }
    }
}

// ── Тулбар ────────────────────────────────────────────────────────────────────

@Composable
private fun DevicesToolbar(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(Dimensions.topBarHeight)
            .padding(horizontal = Dimensions.paddingDefault),
        verticalAlignment   = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        AdbFilledButton(
            onClick = onRefresh,
            enabled = !isRefreshing,
            text = stringResource(AdbCommonStringRes.actionRefresh),
            loading = isRefreshing,
            leadingIcon = if (isRefreshing) null else Icons.Filled.Refresh,
            contentDescription = stringResource(Res.string.devices_toolbar_refresh_list_content_desc),
            size = AdbButtonSize.MEDIUM,
            cornerRadius = AdbCornerRadius.LARGE
        )
    }
}

// ── Список устройств ──────────────────────────────────────────────────────────

@Composable
private fun DevicesList(
    devices: List<AdbDevice>,
    wifiHistory: List<SavedWifiDevice>,
    state: DevicesState,
    component: DevicesComponent,
    onRequestConnectHistoryDevice: (SavedWifiDevice) -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(Dimensions.paddingDefault),
        verticalArrangement = Arrangement.spacedBy(Dimensions.paddingSmall),
    ) {
        if (devices.isNotEmpty()) {
            item(key = "connected_header") {
                Text(
                    text = stringResource(Res.string.devices_connected_section_title),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = Dimensions.paddingXSmall),
                )
            }
        }

        items(devices, key = { it.deviceId }) { device ->
            DeviceCard(
                device                    = device,
                infoState                 = state.deviceInfos[device.deviceId],
                isSelected                = device.deviceId == state.selectedDeviceId,
                isDetailsOpen             = device.deviceId == state.detailsDeviceId,
                onOpenDetails             = { component.onOpenDetails(device) },
            )
        }

        if (wifiHistory.isNotEmpty()) {
            item(key = "history_header") {
                val topPadding = if (devices.isEmpty()) 0.dp else Dimensions.paddingLarge
                Text(
                    text = stringResource(Res.string.devices_history_section_title),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = topPadding, bottom = Dimensions.paddingXSmall),
                )
            }

            items(wifiHistory, key = { it.address }) { historyItem ->
                WifiHistoryCard(
                    device = historyItem,
                    onConnect = { onRequestConnectHistoryDevice(historyItem) },
                    onRemove = { component.onRemoveHistoryDevice(historyItem) },
                )
            }
        }
    }
}

private data class WifiHistoryConnectDialogState(
    val device: SavedWifiDevice,
    val host: String,
    val portText: String,
)

@Composable
private fun localizePendingActionTitle(action: PendingDeviceAction): String =
    when (action.type) {
        PendingDeviceActionType.REBOOT ->
            stringResource(Res.string.devices_dialog_reboot_title)
        PendingDeviceActionType.REBOOT_RECOVERY ->
            stringResource(Res.string.devices_dialog_reboot_recovery_title)
        PendingDeviceActionType.REBOOT_BOOTLOADER ->
            stringResource(Res.string.devices_dialog_reboot_bootloader_title)
        PendingDeviceActionType.DISCONNECT ->
            stringResource(Res.string.devices_dialog_disconnect_title)
    }

@Composable
private fun localizePendingActionMessage(action: PendingDeviceAction): String =
    when (action.type) {
        PendingDeviceActionType.REBOOT ->
            stringResource(Res.string.devices_dialog_reboot_message, action.device.deviceId)
        PendingDeviceActionType.REBOOT_RECOVERY ->
            stringResource(Res.string.devices_dialog_reboot_recovery_message, action.device.deviceId)
        PendingDeviceActionType.REBOOT_BOOTLOADER ->
            stringResource(Res.string.devices_dialog_reboot_bootloader_message, action.device.deviceId)
        PendingDeviceActionType.DISCONNECT ->
            stringResource(Res.string.devices_dialog_disconnect_message, action.device.deviceId)
    }

@Composable
private fun localizeFeedbackMessage(feedback: DeviceActionFeedback): String =
    when (feedback) {
        is DeviceActionFeedback.ActionSuccess -> when (feedback.actionType) {
            PendingDeviceActionType.REBOOT ->
                stringResource(Res.string.devices_feedback_reboot_started)
            PendingDeviceActionType.REBOOT_RECOVERY ->
                stringResource(Res.string.devices_feedback_recovery_started)
            PendingDeviceActionType.REBOOT_BOOTLOADER ->
                stringResource(Res.string.devices_feedback_bootloader_started)
            PendingDeviceActionType.DISCONNECT ->
                stringResource(Res.string.devices_feedback_disconnected)
        }
        is DeviceActionFeedback.ActionError -> {
            val details = feedback.details.orEmpty().trim()
            if (details.isNotEmpty()) {
                stringResource(AdbCommonStringRes.errorWithDetails, details)
            } else {
                stringResource(AdbCommonStringRes.errorUnknown)
            }
        }
    }

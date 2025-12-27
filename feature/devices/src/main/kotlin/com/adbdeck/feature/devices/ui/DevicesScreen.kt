package com.adbdeck.feature.devices.ui

import adbdeck.feature.devices.generated.resources.Res
import adbdeck.feature.devices.generated.resources.*
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.adbdeck.core.adb.api.device.AdbDevice
import com.adbdeck.core.designsystem.Dimensions
import com.adbdeck.core.i18n.AdbCommonStringRes
import com.adbdeck.core.ui.AdbBanner
import com.adbdeck.core.ui.AdbBannerType
import com.adbdeck.core.ui.EmptyView
import com.adbdeck.core.ui.ErrorView
import com.adbdeck.core.ui.LoadingView
import com.adbdeck.core.ui.buttons.AdbButtonSize
import com.adbdeck.core.ui.buttons.AdbButtonType
import com.adbdeck.core.ui.buttons.AdbFilledButton
import com.adbdeck.core.ui.buttons.AdbOutlinedButton
import com.adbdeck.feature.devices.DeviceActionFeedback
import com.adbdeck.feature.devices.DeviceListState
import com.adbdeck.feature.devices.DevicesComponent
import com.adbdeck.feature.devices.DevicesState
import com.adbdeck.feature.devices.PendingDeviceAction
import com.adbdeck.feature.devices.PendingDeviceActionType
import org.jetbrains.compose.resources.stringResource

/**
 * Основной экран управления ADB-устройствами.
 *
 * ## Компоновка
 *
 * ```
 * Row {
 *   Column(weight 1f) {         // Левая панель — список
 *     DevicesToolbar
 *     HorizontalDivider
 *     DevicesList | Loading | Empty | Error
 *   }
 *   AnimatedVisibility {        // Правая панель — детали (slideIn/Out)
 *     VerticalDivider
 *     DeviceDetailsPanel(300.dp)
 *   }
 * }
 * AlertDialog (pendingAction)   // Подтверждение опасного действия
 * AdbBanner                     // Краткосрочное уведомление
 * ```
 *
 * @param component Компонент, управляющий состоянием экрана.
 */
@Composable
fun DevicesScreen(component: DevicesComponent) {
    val state by component.state.collectAsState()

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
                            EmptyView(
                                message = stringResource(Res.string.devices_screen_empty_devices)
                            )

                        is DeviceListState.Error ->
                            ErrorView(message = ls.message, onRetry = component::onRefresh)

                        is DeviceListState.Success ->
                            DevicesList(
                                devices   = ls.devices,
                                state     = state,
                                component = component,
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

        // ── Диалог подтверждения действия ─────────────────────────────────────
        val pending = state.pendingAction
        if (pending != null) {
            AlertDialog(
                onDismissRequest = { if (!state.isActionRunning) component.onCancelAction() },
                title   = { Text(localizePendingActionTitle(pending)) },
                text    = { Text(localizePendingActionMessage(pending)) },
                confirmButton = {
                    if (state.isActionRunning) {
                        CircularProgressIndicator(
                            modifier    = Modifier.size(24.dp),
                            strokeWidth = 2.dp,
                        )
                    } else {
                        AdbFilledButton(
                            onClick = component::onConfirmAction,
                            text = stringResource(AdbCommonStringRes.actionConfirm),
                            type = AdbButtonType.DANGER,
                            size = AdbButtonSize.MEDIUM,
                        )
                    }
                },
                dismissButton = {
                    if (!state.isActionRunning) {
                        AdbOutlinedButton(
                            onClick = component::onCancelAction,
                            text = stringResource(AdbCommonStringRes.actionCancel),
                            type = AdbButtonType.NEUTRAL,
                            size = AdbButtonSize.MEDIUM,
                        )
                    }
                },
            )
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
        Text(
            text  = stringResource(Res.string.devices_toolbar_title),
            style = MaterialTheme.typography.titleLarge,
        )
        AdbOutlinedButton(
            onClick = onRefresh,
            enabled = !isRefreshing,
            loading = isRefreshing,
            leadingIcon = if (isRefreshing) null else Icons.Outlined.Refresh,
            contentDescription = stringResource(Res.string.devices_toolbar_refresh_list_content_desc),
            size = AdbButtonSize.SMALL,
        )
    }
}

// ── Список устройств ──────────────────────────────────────────────────────────

@Composable
private fun DevicesList(
    devices: List<AdbDevice>,
    state: DevicesState,
    component: DevicesComponent,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(Dimensions.paddingDefault),
        verticalArrangement = Arrangement.spacedBy(Dimensions.paddingSmall),
    ) {
        items(devices, key = { it.deviceId }) { device ->
            DeviceCard(
                device                    = device,
                infoState                 = state.deviceInfos[device.deviceId],
                isSelected                = device.deviceId == state.selectedDeviceId,
                isDetailsOpen             = device.deviceId == state.detailsDeviceId,
                onOpenDetails             = { component.onOpenDetails(device) },
            )
        }
    }
}

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

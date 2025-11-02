package com.adbdeck.feature.devices.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Error
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.adbdeck.core.adb.api.AdbDevice
import com.adbdeck.core.designsystem.Dimensions
import com.adbdeck.core.ui.EmptyView
import com.adbdeck.core.ui.ErrorView
import com.adbdeck.core.ui.LoadingView
import com.adbdeck.feature.devices.DeviceActionFeedback
import com.adbdeck.feature.devices.DeviceListState
import com.adbdeck.feature.devices.DevicesComponent
import com.adbdeck.feature.devices.DevicesState

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
 * FeedbackBar (actionFeedback)  // Краткосрочное уведомление
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
                            LoadingView(message = "Получение списка устройств…")

                        is DeviceListState.Empty ->
                            EmptyView(
                                message = "Нет подключенных устройств.\n" +
                                          "Подключите устройство по USB или запустите эмулятор."
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
                title   = { Text(pending.title) },
                text    = { Text(pending.message) },
                confirmButton = {
                    if (state.isActionRunning) {
                        CircularProgressIndicator(
                            modifier    = Modifier.size(24.dp),
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Button(onClick = component::onConfirmAction) {
                            Text("Подтвердить")
                        }
                    }
                },
                dismissButton = {
                    if (!state.isActionRunning) {
                        OutlinedButton(onClick = component::onCancelAction) {
                            Text("Отмена")
                        }
                    }
                },
            )
        }

        // ── Баннер обратной связи ──────────────────────────────────────────────
        val feedback = state.actionFeedback
        if (feedback != null) {
            FeedbackBar(
                feedback  = feedback,
                onDismiss = component::onDismissFeedback,
                modifier  = Modifier.align(Alignment.BottomCenter),
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
            text  = "Устройства",
            style = MaterialTheme.typography.titleLarge,
        )
        IconButton(onClick = onRefresh, enabled = !isRefreshing) {
            if (isRefreshing) {
                CircularProgressIndicator(
                    modifier    = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                )
            } else {
                Icon(
                    imageVector        = Icons.Outlined.Refresh,
                    contentDescription = "Обновить список устройств",
                    modifier           = Modifier.size(Dimensions.iconSizeNav),
                )
            }
        }
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

// ── Баннер обратной связи ─────────────────────────────────────────────────────

/**
 * Всплывающий баннер с результатом последнего действия.
 *
 * Отображается в нижней части экрана; цвет зависит от типа сообщения.
 *
 * @param feedback  Данные уведомления.
 * @param onDismiss Callback закрытия.
 * @param modifier  Modifier для внешнего контейнера.
 */
@Composable
private fun FeedbackBar(
    feedback: DeviceActionFeedback,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val containerColor = if (feedback.isError) {
        MaterialTheme.colorScheme.errorContainer
    } else {
        MaterialTheme.colorScheme.tertiaryContainer
    }
    val contentColor = if (feedback.isError) {
        MaterialTheme.colorScheme.onErrorContainer
    } else {
        MaterialTheme.colorScheme.onTertiaryContainer
    }
    val iconColor = if (feedback.isError) {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.tertiary
    }
    val icon = if (feedback.isError) Icons.Outlined.Error else Icons.Outlined.CheckCircle

    Surface(
        modifier      = modifier
            .fillMaxWidth()
            .padding(16.dp),
        color         = containerColor,
        shape         = MaterialTheme.shapes.medium,
        tonalElevation = 4.dp,
    ) {
        Row(
            modifier            = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment   = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector        = icon,
                contentDescription = null,
                modifier           = Modifier.size(18.dp),
                tint               = iconColor,
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text     = feedback.message,
                modifier = Modifier.weight(1f),
                style    = MaterialTheme.typography.bodyMedium,
                color    = contentColor,
            )
            IconButton(
                onClick  = onDismiss,
                modifier = Modifier.size(28.dp),
            ) {
                Icon(
                    imageVector        = Icons.Outlined.Close,
                    contentDescription = "Закрыть",
                    modifier           = Modifier.size(16.dp),
                    tint               = contentColor,
                )
            }
        }
    }
}

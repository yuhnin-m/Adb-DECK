package com.adbdeck.app.ui

import adbdeck.app.generated.resources.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material.icons.outlined.WifiOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.adbdeck.app.devicemanager.DeviceSelectorComponent
import com.adbdeck.core.adb.api.device.AdbDevice
import com.adbdeck.core.adb.api.device.DeviceEndpoint
import com.adbdeck.core.adb.api.device.DeviceState
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource

/**
 * Виджет выбора устройства для TopBar.
 *
 * Отображает:
 * - Чип с текущим устройством (или "Нет устройства") и кнопкой раскрытия
 * - Индикатор состояния (цветная точка) текущего устройства
 * - Кнопку обновления (или индикатор прогресса во время операции)
 * - Кнопку отключения (только если устройство выбрано)
 * - Кнопку подключения (только если устройств нет)
 * - Dropdown со всеми устройствами, сохраненными endpoint-ами и "Подключить..."
 *
 * @param component Компонент-делегат, управляющий логикой устройств.
 * @param modifier  Модификатор Compose.
 */
@Composable
fun DeviceBar(
    component: DeviceSelectorComponent,
    modifier: Modifier = Modifier,
) {
    val devices by component.devices.collectAsState()
    val selectedDevice by component.selectedDevice.collectAsState()
    val isConnecting by component.isConnecting.collectAsState()
    val error by component.error.collectAsState()
    val savedEndpoints by component.savedEndpoints.collectAsState()

    var showDropdown by remember { mutableStateOf(false) }
    var showConnectDialog by remember { mutableStateOf(false) }

    // Автоматически скрывать ошибку через 5 секунд
    LaunchedEffect(error) {
        if (error != null) {
            delay(5_000)
            component.onClearError()
        }
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        // ── Индикатор ошибки ─────────────────────────────────────
        if (error != null) {
            Icon(
                imageVector = Icons.Outlined.Warning,
                contentDescription = stringResource(Res.string.app_devicebar_error_content_desc, error ?: ""),
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier
                    .size(16.dp)
                    .clickable { component.onClearError() },
            )
            Spacer(Modifier.width(2.dp))
        }

        // ── Чип выбора устройства + Dropdown ────────────────────
        Box {
            DeviceChip(
                device = selectedDevice,
                extraCount = (devices.size - 1).coerceAtLeast(0),
                onClick = { showDropdown = true },
            )
            DropdownMenu(
                expanded = showDropdown,
                onDismissRequest = { showDropdown = false },
            ) {
                DeviceDropdownContent(
                    devices = devices,
                    selectedDevice = selectedDevice,
                    savedEndpoints = savedEndpoints,
                    onSelectDevice = { device ->
                        component.onSelectDevice(device)
                        showDropdown = false
                    },
                    onConnectSaved = { endpoint ->
                        component.onConnectSaved(endpoint)
                        showDropdown = false
                    },
                    onRemoveEndpoint = { endpoint ->
                        component.onRemoveEndpoint(endpoint)
                    },
                    onConnectNew = {
                        showDropdown = false
                        showConnectDialog = true
                    },
                    onSwitchToTcpIp = { serialId ->
                        component.onSwitchToTcpIp(serialId)
                        showDropdown = false
                    },
                )
            }
        }

        // ── Refresh / Progress ───────────────────────────────────
        if (isConnecting) {
            Box(
                modifier = Modifier.size(32.dp),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        } else {
            IconButton(
                onClick = { component.onRefresh() },
                modifier = Modifier.size(32.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Refresh,
                    contentDescription = stringResource(Res.string.app_devicebar_refresh_content_desc),
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // ── Отключить (только если выбрано устройство) ───────────
        if (selectedDevice != null) {
            IconButton(
                onClick = { component.onDisconnect() },
                modifier = Modifier.size(32.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = stringResource(Res.string.app_devicebar_disconnect_content_desc),
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.75f),
                )
            }
        }

        // ── Подключить новое (только если устройств нет) ─────────
        if (devices.isEmpty() && !isConnecting) {
            IconButton(
                onClick = { showConnectDialog = true },
                modifier = Modifier.size(32.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Add,
                    contentDescription = stringResource(Res.string.app_devicebar_connect_wifi_content_desc),
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }

    // ── Диалог подключения по IP ─────────────────────────────────
    if (showConnectDialog) {
        ConnectDialog(
            onDismiss = { showConnectDialog = false },
            onConnect = { host, port ->
                component.onConnect(host, port)
                showConnectDialog = false
            },
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Внутренние composable-компоненты
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Чип-кнопка, показывающая текущее устройство (или "Нет устройства").
 */
@Composable
private fun DeviceChip(
    device: AdbDevice?,
    extraCount: Int,
    onClick: () -> Unit,
) {
    val containerColor = if (device != null) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }

    Row(
        modifier = Modifier
            .height(32.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(containerColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 0.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (device != null) {
            // Цветная точка состояния
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(deviceStateColor(device.state)),
            )
            Text(
                text = device.deviceId.take(26),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                maxLines = 1,
            )
            // Счетчик остальных устройств
            if (extraCount > 0) {
                Text(
                    text = "+$extraCount",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            Icon(
                imageVector = Icons.Outlined.PhoneAndroid,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            )
            Text(
                text = stringResource(Res.string.app_devicebar_no_device),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Icon(
            imageVector = Icons.Outlined.ExpandMore,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * Содержимое выпадающего меню выбора устройства.
 */
@Composable
private fun DeviceDropdownContent(
    devices: List<AdbDevice>,
    selectedDevice: AdbDevice?,
    savedEndpoints: List<DeviceEndpoint>,
    onSelectDevice: (AdbDevice) -> Unit,
    onConnectSaved: (DeviceEndpoint) -> Unit,
    onRemoveEndpoint: (DeviceEndpoint) -> Unit,
    onConnectNew: () -> Unit,
    onSwitchToTcpIp: (String) -> Unit,
) {
    // ── Подключенные устройства ──────────────────────────────────
    if (devices.isNotEmpty()) {
        DropdownSectionLabel(stringResource(Res.string.app_devicebar_section_connected_devices))
        devices.forEach { device ->
            val isSelected = device.deviceId == selectedDevice?.deviceId
            val isUsbDevice = !device.deviceId.contains(":")
            DropdownMenuItem(
                text = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(deviceStateColor(device.state)),
                        )
                        Column {
                            Text(
                                text = device.deviceId,
                                style = MaterialTheme.typography.bodySmall,
                            )
                            if (device.state != DeviceState.DEVICE) {
                                Text(
                                    text = deviceStateName(device.state),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.error,
                                )
                            }
                        }
                    }
                },
                leadingIcon = if (isSelected) ({
                    Icon(
                        imageVector = Icons.Outlined.Check,
                        contentDescription = stringResource(Res.string.app_devicebar_active_device_content_desc),
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }) else null,
                trailingIcon = if (isUsbDevice) ({
                    // Для USB-устройств — кнопка перевода в TCP/IP
                    IconButton(
                        onClick = { onSwitchToTcpIp(device.deviceId) },
                        modifier = Modifier.size(28.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Wifi,
                            contentDescription = stringResource(Res.string.app_devicebar_switch_tcpip_content_desc),
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                        )
                    }
                }) else null,
                onClick = { onSelectDevice(device) },
            )
        }
    }

    // ── Сохраненные endpoint-ы ───────────────────────────────────
    if (savedEndpoints.isNotEmpty()) {
        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
        DropdownSectionLabel(stringResource(Res.string.app_devicebar_section_saved_devices))
        savedEndpoints.forEach { endpoint ->
            val isAlreadyConnected = devices.any { it.deviceId == endpoint.address }
            DropdownMenuItem(
                text = {
                    Text(
                        text = endpoint.label.ifBlank { endpoint.address },
                        style = MaterialTheme.typography.bodySmall,
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = if (isAlreadyConnected) Icons.Outlined.Wifi else Icons.Outlined.WifiOff,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = if (isAlreadyConnected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                trailingIcon = {
                    Row(horizontalArrangement = Arrangement.spacedBy(0.dp)) {
                        // Переподключить
                        if (!isAlreadyConnected) {
                            IconButton(
                                onClick = { onConnectSaved(endpoint) },
                                modifier = Modifier.size(28.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Link,
                                    contentDescription = stringResource(Res.string.app_devicebar_connect_saved_content_desc),
                                    modifier = Modifier.size(14.dp),
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                        // Удалить из сохраненных
                        IconButton(
                            onClick = { onRemoveEndpoint(endpoint) },
                            modifier = Modifier.size(28.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Close,
                                contentDescription = stringResource(Res.string.app_devicebar_remove_saved_content_desc),
                                modifier = Modifier.size(12.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
                onClick = { if (!isAlreadyConnected) onConnectSaved(endpoint) },
            )
        }
    }

    // ── Подключить новое ─────────────────────────────────────────
    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
    DropdownMenuItem(
        text = {
            Text(
                text = stringResource(Res.string.app_devicebar_connect_by_ip_menu),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
            )
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Outlined.Add,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
        },
        onClick = onConnectNew,
    )
}

/** Заголовок секции в Dropdown. */
@Composable
private fun DropdownSectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
    )
}

/**
 * Диалог подключения устройства по IP-адресу.
 *
 * @param onDismiss  Закрыть диалог без действия.
 * @param onConnect  Подключиться к [host]:[port].
 */
@Composable
private fun ConnectDialog(
    onDismiss: () -> Unit,
    onConnect: (host: String, port: Int) -> Unit,
) {
    var host by remember { mutableStateOf("") }
    var portText by remember { mutableStateOf("5555") }
    val portValid = portText.toIntOrNull()?.let { it in 1..65535 } == true
    val canConnect = host.isNotBlank() && portValid

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.app_connect_dialog_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = host,
                    onValueChange = { host = it.trim() },
                    label = { Text(stringResource(Res.string.app_connect_dialog_host_label)) },
                    placeholder = { Text(stringResource(Res.string.app_connect_dialog_host_placeholder)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                )
                OutlinedTextField(
                    value = portText,
                    onValueChange = { portText = it.filter(Char::isDigit).take(5) },
                    label = { Text(stringResource(Res.string.app_connect_dialog_port_label)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = portText.isNotBlank() && !portValid,
                    supportingText = if (portText.isNotBlank() && !portValid) ({
                        Text(stringResource(Res.string.app_connect_dialog_port_range_error))
                    }) else null,
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConnect(host, portText.toIntOrNull() ?: 5555) },
                enabled = canConnect,
            ) {
                Text(stringResource(Res.string.app_connect_dialog_connect_action))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.app_connect_dialog_cancel_action))
            }
        },
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// Вспомогательные функции
// ─────────────────────────────────────────────────────────────────────────────

/** Цвет индикатора для состояния устройства. */
@Composable
private fun deviceStateColor(state: DeviceState): Color = when (state) {
    DeviceState.DEVICE -> Color(0xFF4CAF50)                         // зеленый
    DeviceState.OFFLINE -> MaterialTheme.colorScheme.error          // красный из темы
    DeviceState.UNAUTHORIZED -> Color(0xFFFF9800)                   // оранжевый
    DeviceState.UNKNOWN -> MaterialTheme.colorScheme.outlineVariant // серый
}

/** Читаемое название состояния устройства на русском. */
@Composable
private fun deviceStateName(state: DeviceState): String = when (state) {
    DeviceState.DEVICE -> stringResource(Res.string.app_device_state_connected)
    DeviceState.OFFLINE -> stringResource(Res.string.app_device_state_unavailable)
    DeviceState.UNAUTHORIZED -> stringResource(Res.string.app_device_state_auth_required)
    DeviceState.UNKNOWN -> stringResource(Res.string.app_device_state_unknown)
}

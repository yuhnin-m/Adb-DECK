package com.adbdeck.feature.devices.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.adbdeck.core.adb.api.AdbDevice
import com.adbdeck.core.adb.api.DeviceInfo
import com.adbdeck.core.adb.api.DeviceInfoLoadState
import com.adbdeck.core.adb.api.DeviceState
import com.adbdeck.core.ui.LoadingView

/**
 * Боковая панель деталей устройства.
 *
 * Отображает всю доступную информацию из [DeviceInfo]:
 * - Заголовок (имя устройства, действия, кнопка закрыть)
 * - Секция «Идентификация» (серийник, модель, производитель, brand, product)
 * - Секция «Android» (версия, SDK, патч безопасности, ABI)
 * - Секция «Дисплей» (разрешение, плотность)
 * - Секция «Батарея» (уровень, статус)
 * - Секция «Сборка» (fingerprint)
 * - Кнопки управления (Reboot, Recovery, Bootloader, Disconnect)
 *
 * @param device                  Базовые данные устройства (из `adb devices`).
 * @param infoState               Состояние загрузки расширенной информации.
 * @param onClose                 Закрыть панель.
 * @param onRefreshInfo           Перезагрузить расширенную информацию.
 * @param onRequestReboot         Запросить перезагрузку.
 * @param onRequestRebootRecovery Запросить перезагрузку в Recovery.
 * @param onRequestRebootBootloader Запросить перезагрузку в Bootloader.
 * @param onRequestDisconnect     Запросить отключение (только для Wi-Fi).
 * @param modifier                Modifier для контейнера.
 */
@Composable
fun DeviceDetailsPanel(
    device: AdbDevice,
    infoState: DeviceInfoLoadState?,
    onClose: () -> Unit,
    onRefreshInfo: () -> Unit,
    onRequestReboot: () -> Unit,
    onRequestRebootRecovery: () -> Unit,
    onRequestRebootBootloader: () -> Unit,
    onRequestDisconnect: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isOnline = device.state == DeviceState.DEVICE
    val isWifi   = device.deviceId.contains(':') && !device.deviceId.startsWith("emulator-")

    Column(
        modifier = modifier.background(MaterialTheme.colorScheme.surface),
    ) {
        // ── Заголовок ─────────────────────────────────────────────────────────
        PanelHeader(
            device       = device,
            infoState    = infoState,
            onClose      = onClose,
            onRefresh    = onRefreshInfo,
        )
        HorizontalDivider()

        // ── Содержимое ────────────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Базовые данные (всегда)
            DetailSection("Подключение") {
                InfoRow("Device ID", device.deviceId)
                InfoRow("Состояние", device.state.rawValue)
                InfoRow("Транспорт", when {
                    device.deviceId.startsWith("emulator-") -> "Эмулятор"
                    device.deviceId.contains(':')           -> "Wi-Fi (TCP/IP)"
                    else                                    -> "USB"
                })
                if (device.info.isNotBlank()) InfoRow("Info", device.info)
            }

            // Расширенная информация
            when (infoState) {
                null, is DeviceInfoLoadState.Loading ->
                    LoadingView(modifier = Modifier.fillMaxWidth().height(80.dp))

                is DeviceInfoLoadState.Failed ->
                    Text(
                        "Детали недоступны: ${infoState.message}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )

                is DeviceInfoLoadState.Loaded -> {
                    val info = infoState.info
                    DeviceInfoDetails(info = info)
                }
            }

            // Кнопки управления
            if (isOnline) {
                HorizontalDivider()
                DeviceControlButtons(
                    isWifi                    = isWifi,
                    onRequestReboot           = onRequestReboot,
                    onRequestRebootRecovery   = onRequestRebootRecovery,
                    onRequestRebootBootloader = onRequestRebootBootloader,
                    onRequestDisconnect       = onRequestDisconnect,
                )
            }
        }
    }
}

// ── Заголовок панели ──────────────────────────────────────────────────────────

@Composable
private fun PanelHeader(
    device: AdbDevice,
    infoState: DeviceInfoLoadState?,
    onClose: () -> Unit,
    onRefresh: () -> Unit,
) {
    val displayName = (infoState as? DeviceInfoLoadState.Loaded)
        ?.info?.displayName
        ?: device.info.ifBlank { device.deviceId }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = transportIcon(detectTransportType(device.deviceId)),
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text     = displayName,
            style    = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onRefresh, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Outlined.Refresh, "Обновить", modifier = Modifier.size(16.dp))
        }
        IconButton(onClick = onClose, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Outlined.Close, "Закрыть", modifier = Modifier.size(16.dp))
        }
    }
}

// ── Детали DeviceInfo ─────────────────────────────────────────────────────────

@Composable
private fun DeviceInfoDetails(info: DeviceInfo) {
    // Идентификация
    DetailSection("Устройство") {
        if (info.model.isNotEmpty())        InfoRow("Модель",        info.model)
        if (info.manufacturer.isNotEmpty()) InfoRow("Производитель", info.manufacturer)
        if (info.brand.isNotEmpty())        InfoRow("Бренд",         info.brand)
        if (info.productName.isNotEmpty())  InfoRow("Продукт",       info.productName)
    }

    // Android
    if (info.androidVersion.isNotEmpty() || info.sdkVersion > 0 || info.securityPatch.isNotEmpty()) {
        DetailSection("Android") {
            if (info.androidVersion.isNotEmpty()) InfoRow("Версия",        info.androidVersion)
            if (info.sdkVersion > 0)              InfoRow("SDK API Level", info.sdkVersion.toString())
            if (info.securityPatch.isNotEmpty())  InfoRow("Патч безопасности", info.securityPatch)
            if (info.cpuAbiList.isNotEmpty())     InfoRow("CPU ABI",       info.cpuAbiList)
        }
    }

    // Дисплей
    if (info.screenResolution.isNotEmpty() || info.screenDensity > 0) {
        DetailSection("Дисплей") {
            if (info.screenResolution.isNotEmpty()) InfoRow("Разрешение", info.screenResolution)
            if (info.screenDensity > 0)             InfoRow("Плотность",  "${info.screenDensity} dpi")
        }
    }

    // Батарея
    if (info.batteryLevel >= 0) {
        DetailSection("Батарея") {
            InfoRow("Уровень",   "${info.batteryLevel}%")
            InfoRow("Зарядка",  if (info.batteryCharging) "Да ⚡" else "Нет")
        }
    }

    // Сборка
    if (info.buildFingerprint.isNotEmpty()) {
        DetailSection("Сборка") {
            Text(
                text     = info.buildFingerprint,
                style    = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize   = 10.sp,
                ),
                color    = MaterialTheme.colorScheme.onSurface,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

// ── Кнопки управления ─────────────────────────────────────────────────────────

@Composable
private fun DeviceControlButtons(
    isWifi: Boolean,
    onRequestReboot: () -> Unit,
    onRequestRebootRecovery: () -> Unit,
    onRequestRebootBootloader: () -> Unit,
    onRequestDisconnect: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            "Управление",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
        )
        // Перезагрузка
        OutlinedButton(
            onClick = onRequestReboot,
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
        ) {
            Icon(Icons.Outlined.RestartAlt, null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text("Перезагрузить")
        }
        // Recovery
        OutlinedButton(
            onClick = onRequestRebootRecovery,
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
        ) {
            Icon(Icons.Outlined.Build, null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text("Recovery Mode")
        }
        // Bootloader
        OutlinedButton(
            onClick = onRequestRebootBootloader,
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
        ) {
            Icon(Icons.Outlined.FlashOn, null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text("Bootloader / Fastboot")
        }
        // Disconnect (только Wi-Fi)
        if (isWifi) {
            OutlinedButton(
                onClick = onRequestDisconnect,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error,
                ),
                border = ButtonDefaults.outlinedButtonBorder.copy(
                    brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.error)
                ),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
            ) {
                Icon(Icons.Outlined.WifiOff, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Отключить Wi-Fi")
            }
        }
    }
}

// ── Вспомогательные composable ────────────────────────────────────────────────

@Composable
private fun DetailSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text  = title.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
        )
        Surface(
            shape          = RoundedCornerShape(8.dp),
            tonalElevation = 1.dp,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                content()
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text     = label,
            style    = MaterialTheme.typography.bodySmall,
            color    = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(100.dp),
        )
        Text(
            text     = value,
            style    = MaterialTheme.typography.bodySmall.copy(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Medium,
            ),
            color    = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

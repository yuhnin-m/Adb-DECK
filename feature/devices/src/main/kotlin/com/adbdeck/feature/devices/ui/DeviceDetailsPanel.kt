package com.adbdeck.feature.devices.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import com.adbdeck.core.adb.api.device.AdbDevice
import com.adbdeck.core.adb.api.device.DeviceInfo
import com.adbdeck.core.adb.api.device.DeviceInfoLoadState
import com.adbdeck.core.adb.api.device.DeviceState
import com.adbdeck.core.ui.LoadingView
import com.adbdeck.core.ui.buttons.AdbButtonSize
import com.adbdeck.core.ui.buttons.AdbButtonType
import com.adbdeck.core.ui.buttons.AdbOutlinedButton
import com.adbdeck.core.ui.buttons.AdbPlainButton
import com.adbdeck.core.ui.sectioncards.AdbSectionCard

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
 * - Секция «Навигация и контекст» (сделать активным, переходы по экранам)
 * - Секция «Управление» (обычная перезагрузка)
 * - Секция «Опасные действия» (Recovery, Bootloader, Disconnect)
 *
 * @param device                  Базовые данные устройства (из `adb devices`).
 * @param infoState               Состояние загрузки расширенной информации.
 * @param isSelected              `true`, если устройство уже выбрано активным.
 * @param isActionRunning         `true`, если выполняется подтверждённое действие.
 * @param onClose                 Закрыть панель.
 * @param onSelectDevice          Сделать устройство активным.
 * @param onRefreshInfo           Перезагрузить расширенную информацию.
 * @param onNavigateToLogcat      Перейти на экран Logcat.
 * @param onNavigateToPackages    Перейти на экран Packages.
 * @param onNavigateToSystemMonitor Перейти на экран System Monitor.
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
    isSelected: Boolean,
    isActionRunning: Boolean,
    onClose: () -> Unit,
    onSelectDevice: () -> Unit,
    onRefreshInfo: () -> Unit,
    onNavigateToLogcat: () -> Unit,
    onNavigateToPackages: () -> Unit,
    onNavigateToSystemMonitor: () -> Unit,
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
            AdbSectionCard(
                title = "Подключение",
                titleUppercase = true,
            ) {
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
                DeviceNavigationButtons(
                    isActionRunning = isActionRunning,
                    onSelectDevice = onSelectDevice,
                    onNavigateToLogcat = onNavigateToLogcat,
                    onNavigateToPackages = onNavigateToPackages,
                    onNavigateToSystemMonitor = onNavigateToSystemMonitor,
                    isSelected = isSelected,
                )
                Spacer(Modifier.height(8.dp))
                DeviceControlButtons(
                    isWifi                    = isWifi,
                    isActionRunning           = isActionRunning,
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
        AdbPlainButton(
            onClick = onRefresh,
            leadingIcon = Icons.Outlined.Refresh,
            contentDescription = "Обновить",
            size = AdbButtonSize.SMALL,
        )
        AdbPlainButton(
            onClick = onClose,
            leadingIcon = Icons.Outlined.Close,
            contentDescription = "Закрыть",
            size = AdbButtonSize.SMALL,
        )
    }
}

// ── Детали DeviceInfo ─────────────────────────────────────────────────────────

@Composable
private fun DeviceInfoDetails(info: DeviceInfo) {
    // Идентификация
    AdbSectionCard(
        title = "Устройство",
        titleUppercase = true,
    ) {
        if (info.model.isNotEmpty())        InfoRow("Модель",        info.model)
        if (info.manufacturer.isNotEmpty()) InfoRow("Производитель", info.manufacturer)
        if (info.brand.isNotEmpty())        InfoRow("Бренд",         info.brand)
        if (info.productName.isNotEmpty())  InfoRow("Продукт",       info.productName)
    }

    // Android
    if (info.androidVersion.isNotEmpty() || info.sdkVersion > 0 || info.securityPatch.isNotEmpty()) {
        AdbSectionCard(
            title = "Android",
            titleUppercase = true,
        ) {
            if (info.androidVersion.isNotEmpty()) InfoRow("Версия",        info.androidVersion)
            if (info.sdkVersion > 0)              InfoRow("SDK API Level", info.sdkVersion.toString())
            if (info.securityPatch.isNotEmpty())  InfoRow("Патч безопасности", info.securityPatch)
            if (info.cpuAbiList.isNotEmpty())     InfoRow("CPU ABI",       info.cpuAbiList)
        }
    }

    // Дисплей
    if (info.screenResolution.isNotEmpty() || info.screenDensity > 0) {
        AdbSectionCard(
            title = "Дисплей",
            titleUppercase = true,
        ) {
            if (info.screenResolution.isNotEmpty()) InfoRow("Разрешение", info.screenResolution)
            if (info.screenDensity > 0)             InfoRow("Плотность",  "${info.screenDensity} dpi")
        }
    }

    // Батарея
    if (info.batteryLevel >= 0) {
        AdbSectionCard(
            title = "Батарея",
            titleUppercase = true,
        ) {
            InfoRow("Уровень",   "${info.batteryLevel}%")
            InfoRow("Зарядка",  if (info.batteryCharging) "Да ⚡" else "Нет")
        }
    }

    // Сборка
    if (info.buildFingerprint.isNotEmpty()) {
        AdbSectionCard(
            title = "Сборка",
            titleUppercase = true,
        ) {
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
    isActionRunning: Boolean,
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

        // Обычное действие
        AdbOutlinedButton(
            onClick = onRequestReboot,
            enabled = !isActionRunning,
            text = "Перезагрузить",
            leadingIcon = Icons.Outlined.RestartAlt,
            size = AdbButtonSize.SMALL,
            fullWidth = true,
        )

        // Destructive-зона
        DangerZone(
            isWifi = isWifi,
            isActionRunning = isActionRunning,
            onRequestRebootRecovery = onRequestRebootRecovery,
            onRequestRebootBootloader = onRequestRebootBootloader,
            onRequestDisconnect = onRequestDisconnect,
        )
    }
}

/**
 * Блок рискованных действий.
 *
 * Выделен отдельной рамкой/фоном, чтобы визуально отделить операции,
 * которые могут прервать рабочую сессию или перевести устройство в спецрежим.
 */
@Composable
private fun DangerZone(
    isWifi: Boolean,
    isActionRunning: Boolean,
    onRequestRebootRecovery: () -> Unit,
    onRequestRebootBootloader: () -> Unit,
    onRequestDisconnect: () -> Unit,
) {
    val dangerShape = MaterialTheme.shapes.small

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.error.copy(alpha = 0.4f),
                shape = dangerShape,
            ),
        shape = dangerShape,
        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f),
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.WarningAmber,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.error,
                )
                Text(
                    text = "Опасные действия",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            AdbOutlinedButton(
                onClick = onRequestRebootRecovery,
                enabled = !isActionRunning,
                text = "Recovery Mode",
                type = AdbButtonType.DANGER,
                leadingIcon = Icons.Outlined.Build,
                size = AdbButtonSize.SMALL,
                fullWidth = true,
            )

            AdbOutlinedButton(
                onClick = onRequestRebootBootloader,
                enabled = !isActionRunning,
                text = "Bootloader / Fastboot",
                type = AdbButtonType.DANGER,
                leadingIcon = Icons.Outlined.FlashOn,
                size = AdbButtonSize.SMALL,
                fullWidth = true,
            )

            if (isWifi) {
                AdbOutlinedButton(
                    onClick = onRequestDisconnect,
                    enabled = !isActionRunning,
                    text = "Отключить Wi-Fi",
                    type = AdbButtonType.DANGER,
                    leadingIcon = Icons.Outlined.WifiOff,
                    size = AdbButtonSize.SMALL,
                    fullWidth = true,
                )
            }
        }
    }
}

// ── Навигация и выбор активного устройства ───────────────────────────────────

/**
 * Секция действий верхнего уровня:
 * - сделать устройство активным;
 * - перейти на связанные экраны (Logcat / Packages / System Monitor).
 */
@Composable
private fun DeviceNavigationButtons(
    isSelected: Boolean,
    isActionRunning: Boolean,
    onSelectDevice: () -> Unit,
    onNavigateToLogcat: () -> Unit,
    onNavigateToPackages: () -> Unit,
    onNavigateToSystemMonitor: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            "Навигация и контекст",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
        )

        AdbOutlinedButton(
            onClick = onSelectDevice,
            enabled = !isSelected && !isActionRunning,
            text = if (isSelected) "Уже активное" else "Сделать активным",
            leadingIcon = Icons.Outlined.CheckCircle,
            size = AdbButtonSize.SMALL,
            fullWidth = true,
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            AdbOutlinedButton(
                onClick = onNavigateToLogcat,
                enabled = !isActionRunning,
                modifier = Modifier.weight(1f),
                text = "Logcat",
                leadingIcon = Icons.Outlined.Terminal,
                size = AdbButtonSize.SMALL,
                fullWidth = true,
            )
            AdbOutlinedButton(
                onClick = onNavigateToPackages,
                enabled = !isActionRunning,
                modifier = Modifier.weight(1f),
                text = "Packages",
                leadingIcon = Icons.Outlined.Apps,
                size = AdbButtonSize.SMALL,
                fullWidth = true,
            )
        }

        AdbOutlinedButton(
            onClick = onNavigateToSystemMonitor,
            enabled = !isActionRunning,
            text = "System Monitor",
            leadingIcon = Icons.Outlined.Monitor,
            size = AdbButtonSize.SMALL,
            fullWidth = true,
        )
    }
}

// ── Вспомогательные composable ────────────────────────────────────────────────

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

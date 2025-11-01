package com.adbdeck.feature.devices.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.adbdeck.core.adb.api.AdbDevice
import com.adbdeck.core.adb.api.DeviceInfo
import com.adbdeck.core.adb.api.DeviceInfoLoadState
import com.adbdeck.core.adb.api.DeviceState
import com.adbdeck.core.adb.api.DeviceTransportType
import com.adbdeck.core.designsystem.AdbDeckAmber
import com.adbdeck.core.designsystem.AdbDeckGreen
import com.adbdeck.core.designsystem.AdbDeckRed

/**
 * Карточка одного ADB-устройства в списке.
 *
 * Показывает:
 * - Иконку типа транспорта (USB / Wi-Fi / Эмулятор)
 * - Название устройства и deviceId
 * - Бейджи состояния и типа подключения
 * - Расширенную информацию (модель, Android, батарея) если загружена
 * - Кнопки быстрых действий + overflow-меню
 *
 * Подсвечивается рамкой при [isSelected] == `true`.
 *
 * @param device          Устройство ADB.
 * @param infoState       Состояние загрузки расширенной информации.
 * @param isSelected      `true` если устройство является активным (выбранным).
 * @param isDetailsOpen   `true` если панель деталей этого устройства открыта.
 * @param onSelect        Установить как активное.
 * @param onOpenDetails   Открыть панель деталей.
 * @param onRefreshInfo   Перезагрузить расширенную информацию.
 * @param onNavigateLogcat    Перейти в Logcat.
 * @param onNavigatePackages  Перейти в Packages.
 * @param onNavigateMonitor   Перейти в System Monitor.
 * @param onRequestReboot         Запросить обычную перезагрузку (открывает диалог).
 * @param onRequestRebootRecovery Запросить перезагрузку в Recovery.
 * @param onRequestRebootBootloader Запросить перезагрузку в Bootloader.
 * @param onRequestDisconnect     Запросить отключение Wi-Fi-устройства.
 */
@Composable
fun DeviceCard(
    device: AdbDevice,
    infoState: DeviceInfoLoadState?,
    isSelected: Boolean,
    isDetailsOpen: Boolean,
    onSelect: () -> Unit,
    onOpenDetails: () -> Unit,
    onRefreshInfo: () -> Unit,
    onNavigateLogcat: () -> Unit,
    onNavigatePackages: () -> Unit,
    onNavigateMonitor: () -> Unit,
    onRequestReboot: () -> Unit,
    onRequestRebootRecovery: () -> Unit,
    onRequestRebootBootloader: () -> Unit,
    onRequestDisconnect: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val transport = detectTransportType(device.deviceId)
    val isOnline  = device.state == DeviceState.DEVICE
    val isWifi    = transport == DeviceTransportType.WIFI

    val borderColor = when {
        isSelected     -> MaterialTheme.colorScheme.primary
        isDetailsOpen  -> MaterialTheme.colorScheme.secondary
        else           -> MaterialTheme.colorScheme.outlineVariant
    }
    val borderWidth = if (isSelected || isDetailsOpen) 2.dp else 1.dp

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .border(borderWidth, borderColor, RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        tonalElevation = if (isSelected) 3.dp else 1.dp,
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // ── Верхняя строка: иконка + имя + бейджи ────────────────────────
            Row(verticalAlignment = Alignment.Top) {
                // Иконка транспорта
                Icon(
                    imageVector = transportIcon(transport),
                    contentDescription = null,
                    modifier = Modifier.size(32.dp).padding(top = 2.dp),
                    tint = if (isOnline) MaterialTheme.colorScheme.primary
                           else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    // Имя устройства
                    val displayName = (infoState as? DeviceInfoLoadState.Loaded)
                        ?.info?.displayName
                        ?: device.info.ifBlank { device.deviceId }

                    Text(
                        text     = displayName,
                        style    = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    // deviceId в моноширинном
                    Text(
                        text  = device.deviceId,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize   = 11.sp,
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(4.dp))
                    // Бейджи в строку
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        DeviceStateBadge(device.state)
                        DeviceTransportBadge(transport)
                        if (isSelected) ActiveBadge()
                    }
                }

                Spacer(Modifier.width(8.dp))

                // Кнопка деталей
                IconButton(
                    onClick = onOpenDetails,
                    modifier = Modifier.size(32.dp),
                ) {
                    Icon(
                        imageVector = if (isDetailsOpen) Icons.Filled.Info
                                      else Icons.Outlined.Info,
                        contentDescription = "Детали",
                        tint = if (isDetailsOpen) MaterialTheme.colorScheme.primary
                               else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }

            // ── Дополнительная инфо (если загружена) ─────────────────────────
            val loadedInfo = (infoState as? DeviceInfoLoadState.Loaded)?.info
            if (loadedInfo != null && isOnline) {
                Spacer(Modifier.height(8.dp))
                HorizontalDivider(thickness = 0.5.dp)
                Spacer(Modifier.height(8.dp))
                DeviceInfoSummary(info = loadedInfo)
            } else if (infoState is DeviceInfoLoadState.Loading) {
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth().height(2.dp))
            }

            // ── Кнопки быстрых действий ───────────────────────────────────────
            Spacer(Modifier.height(10.dp))
            HorizontalDivider(thickness = 0.5.dp)
            Spacer(Modifier.height(6.dp))
            DeviceActionRow(
                device                    = device,
                isOnline                  = isOnline,
                isWifi                    = isWifi,
                isSelected                = isSelected,
                onSelect                  = onSelect,
                onNavigateLogcat          = onNavigateLogcat,
                onNavigatePackages        = onNavigatePackages,
                onNavigateMonitor         = onNavigateMonitor,
                onRequestReboot           = onRequestReboot,
                onRequestRebootRecovery   = onRequestRebootRecovery,
                onRequestRebootBootloader = onRequestRebootBootloader,
                onRequestDisconnect       = onRequestDisconnect,
            )
        }
    }
}

// ── Краткая информация (в теле карточки) ─────────────────────────────────────

@Composable
private fun DeviceInfoSummary(info: DeviceInfo) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (info.androidVersion.isNotEmpty() || info.sdkVersion > 0) {
            InfoChip(
                label = "Android",
                value = buildString {
                    append(info.androidVersion.ifEmpty { "?" })
                    if (info.sdkVersion > 0) append(" (API ${info.sdkVersion})")
                },
            )
        }
        if (info.manufacturer.isNotEmpty()) {
            InfoChip(label = "Mfr", value = info.manufacturer)
        }
        if (info.batteryLevel >= 0) {
            InfoChip(
                label = "Battery",
                value = "${info.batteryLevel}%" + if (info.batteryCharging) " ⚡" else "",
            )
        }
    }
}

@Composable
private fun InfoChip(label: String, value: String) {
    Column {
        Text(
            text  = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text  = value,
            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

// ── Строка действий ───────────────────────────────────────────────────────────

@Composable
private fun DeviceActionRow(
    device: AdbDevice,
    isOnline: Boolean,
    isWifi: Boolean,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onNavigateLogcat: () -> Unit,
    onNavigatePackages: () -> Unit,
    onNavigateMonitor: () -> Unit,
    onRequestReboot: () -> Unit,
    onRequestRebootRecovery: () -> Unit,
    onRequestRebootBootloader: () -> Unit,
    onRequestDisconnect: () -> Unit,
) {
    var showOverflow by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        // ── Кнопка Set Active ────────────────────────────────────────
        if (isOnline) {
            if (isSelected) {
                SmallActionButton(
                    icon  = Icons.Outlined.CheckCircle,
                    label = "Active",
                    tint  = AdbDeckGreen,
                    onClick = {},
                )
            } else {
                SmallActionButton(
                    icon    = Icons.Outlined.RadioButtonUnchecked,
                    label   = "Set Active",
                    onClick = onSelect,
                )
            }
        }

        Spacer(Modifier.weight(1f))

        // ── Быстрые навигационные кнопки ────────────────────────────
        if (isOnline) {
            QuickIconButton(
                icon  = Icons.Outlined.Terminal,
                label = "Logcat",
                onClick = onNavigateLogcat,
            )
            QuickIconButton(
                icon  = Icons.Outlined.Apps,
                label = "Packages",
                onClick = onNavigatePackages,
            )
            QuickIconButton(
                icon  = Icons.Outlined.Monitor,
                label = "System",
                onClick = onNavigateMonitor,
            )
        }

        // ── Overflow-меню ────────────────────────────────────────────
        Box {
            IconButton(
                onClick  = { showOverflow = true },
                modifier = Modifier.size(30.dp),
            ) {
                Icon(
                    Icons.Outlined.MoreVert,
                    contentDescription = "Ещё действия",
                    modifier = Modifier.size(18.dp),
                )
            }
            DropdownMenu(
                expanded        = showOverflow,
                onDismissRequest = { showOverflow = false },
            ) {
                if (isOnline) {
                    DropdownMenuItem(
                        text        = { Text("Перезагрузить") },
                        leadingIcon = { Icon(Icons.Outlined.RestartAlt, null) },
                        onClick     = { showOverflow = false; onRequestReboot() },
                    )
                    DropdownMenuItem(
                        text        = { Text("Recovery Mode") },
                        leadingIcon = { Icon(Icons.Outlined.Build, null) },
                        onClick     = { showOverflow = false; onRequestRebootRecovery() },
                    )
                    DropdownMenuItem(
                        text        = { Text("Bootloader / Fastboot") },
                        leadingIcon = { Icon(Icons.Outlined.FlashOn, null) },
                        onClick     = { showOverflow = false; onRequestRebootBootloader() },
                    )
                }
                if (isWifi) {
                    HorizontalDivider()
                    DropdownMenuItem(
                        text        = { Text("Отключить Wi-Fi", color = MaterialTheme.colorScheme.error) },
                        leadingIcon = {
                            Icon(Icons.Outlined.WifiOff, null, tint = MaterialTheme.colorScheme.error)
                        },
                        onClick = { showOverflow = false; onRequestDisconnect() },
                    )
                }
            }
        }
    }
}

@Composable
private fun SmallActionButton(
    icon: ImageVector,
    label: String,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit,
) {
    TextButton(
        onClick = onClick,
        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
        modifier = Modifier.height(28.dp),
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(14.dp), tint = tint)
        Spacer(Modifier.width(3.dp))
        Text(label, fontSize = 11.sp, color = tint)
    }
}

@Composable
private fun QuickIconButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    IconButton(onClick = onClick, modifier = Modifier.size(30.dp)) {
        Icon(
            imageVector        = icon,
            contentDescription = label,
            modifier           = Modifier.size(16.dp),
            tint               = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// ── Бейджи ────────────────────────────────────────────────────────────────────

@Composable
fun DeviceStateBadge(state: DeviceState) {
    val (color, label) = when (state) {
        DeviceState.DEVICE       -> Pair(AdbDeckGreen, "device")
        DeviceState.OFFLINE      -> Pair(AdbDeckRed, "offline")
        DeviceState.UNAUTHORIZED -> Pair(AdbDeckAmber, "unauthorized")
        DeviceState.UNKNOWN      -> Pair(MaterialTheme.colorScheme.outline, state.rawValue)
    }
    ColorBadge(label, color)
}

@Composable
private fun DeviceTransportBadge(transport: DeviceTransportType) {
    val (color, label) = when (transport) {
        DeviceTransportType.USB      -> Pair(MaterialTheme.colorScheme.secondary, "USB")
        DeviceTransportType.WIFI     -> Pair(MaterialTheme.colorScheme.tertiary,  "Wi-Fi")
        DeviceTransportType.EMULATOR -> Pair(MaterialTheme.colorScheme.primary,   "Emulator")
        DeviceTransportType.UNKNOWN  -> return
    }
    ColorBadge(label, color)
}

@Composable
private fun ActiveBadge() {
    ColorBadge("ACTIVE", AdbDeckGreen, bold = true)
}

@Composable
private fun ColorBadge(text: String, color: Color, bold: Boolean = false) {
    Surface(
        color = color.copy(alpha = 0.15f),
        shape = RoundedCornerShape(4.dp),
    ) {
        Text(
            text     = text,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
            style    = MaterialTheme.typography.labelSmall.copy(
                color      = color,
                fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
            ),
        )
    }
}

// ── Утилиты ───────────────────────────────────────────────────────────────────

/** Определяет тип транспорта по deviceId. */
fun detectTransportType(deviceId: String): DeviceTransportType = when {
    deviceId.startsWith("emulator-") -> DeviceTransportType.EMULATOR
    deviceId.contains(':')           -> DeviceTransportType.WIFI
    else                             -> DeviceTransportType.USB
}

/** Иконка для типа транспорта. */
fun transportIcon(transport: DeviceTransportType): ImageVector = when (transport) {
    DeviceTransportType.USB      -> Icons.Outlined.PhoneAndroid
    DeviceTransportType.WIFI     -> Icons.Outlined.PhoneAndroid
    DeviceTransportType.EMULATOR -> Icons.Outlined.Computer
    DeviceTransportType.UNKNOWN  -> Icons.Outlined.DevicesOther
}

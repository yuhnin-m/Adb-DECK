package com.adbdeck.feature.devices.ui

import adbdeck.feature.devices.generated.resources.Res
import adbdeck.feature.devices.generated.resources.*
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.adbdeck.core.adb.api.device.AdbDevice
import com.adbdeck.core.adb.api.device.DeviceInfo
import com.adbdeck.core.adb.api.device.DeviceInfoLoadState
import com.adbdeck.core.adb.api.device.DeviceState
import com.adbdeck.core.adb.api.device.DeviceTransportType
import com.adbdeck.core.designsystem.AdbDeckAmber
import com.adbdeck.core.designsystem.AdbDeckGreen
import com.adbdeck.core.designsystem.AdbDeckRed
import org.jetbrains.compose.resources.stringResource

/**
 * Карточка одного ADB-устройства в списке.
 *
 * Показывает:
 * - Иконку типа транспорта (USB / Wi-Fi / Эмулятор)
 * - Название устройства и deviceId
 * - Бейджи состояния и типа подключения
 * - Расширенную информацию (модель, Android, батарея) если загружена
 * - Кнопку открытия child-панели деталей
 *
 * Подсвечивается рамкой при [isSelected] == `true`.
 *
 * @param device          Устройство ADB.
 * @param infoState       Состояние загрузки расширенной информации.
 * @param isSelected      `true` если устройство является активным (выбранным).
 * @param isDetailsOpen   `true` если панель деталей этого устройства открыта.
 * @param onOpenDetails   Открыть панель деталей.
 */
@Composable
fun DeviceCard(
    device: AdbDevice,
    infoState: DeviceInfoLoadState?,
    isSelected: Boolean,
    isDetailsOpen: Boolean,
    onOpenDetails: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val transport = detectTransportType(device.deviceId)
    val isOnline  = device.state == DeviceState.DEVICE
    val cardShape = MaterialTheme.shapes.medium

    val borderColor = when {
        isSelected     -> MaterialTheme.colorScheme.primary
        isDetailsOpen  -> MaterialTheme.colorScheme.secondary
        else           -> MaterialTheme.colorScheme.outlineVariant
    }
    val borderWidth = if (isSelected || isDetailsOpen) 2.dp else 1.dp

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onOpenDetails)
            .border(borderWidth, borderColor, cardShape),
        shape = cardShape,
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

                if (isDetailsOpen) {
                    Spacer(Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Outlined.Visibility,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.primary,
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
            }
            if (infoState is DeviceInfoLoadState.Loading) {
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth().height(2.dp))
            }
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
            val androidVersion = info.androidVersion.ifEmpty {
                stringResource(Res.string.devices_card_unknown_placeholder)
            }
            val androidValue = if (info.sdkVersion > 0) {
                stringResource(
                    Res.string.devices_card_android_api_format,
                    androidVersion,
                    info.sdkVersion,
                )
            } else {
                androidVersion
            }
            InfoChip(
                label = stringResource(Res.string.devices_card_chip_android),
                value = androidValue,
            )
        }
        if (info.manufacturer.isNotEmpty()) {
            InfoChip(
                label = stringResource(Res.string.devices_card_chip_manufacturer_short),
                value = info.manufacturer,
            )
        }
        if (info.batteryLevel >= 0) {
            val batteryValue = stringResource(
                Res.string.devices_card_battery_percent_format,
                info.batteryLevel,
            ) + if (info.batteryCharging) " ⚡" else ""
            InfoChip(
                label = stringResource(Res.string.devices_card_chip_battery),
                value = batteryValue,
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

// ── Бейджи ────────────────────────────────────────────────────────────────────

@Composable
fun DeviceStateBadge(state: DeviceState) {
    val (color, label) = when (state) {
        DeviceState.DEVICE       -> Pair(AdbDeckGreen, stringResource(Res.string.devices_card_state_device))
        DeviceState.OFFLINE      -> Pair(AdbDeckRed, stringResource(Res.string.devices_card_state_offline))
        DeviceState.UNAUTHORIZED -> Pair(AdbDeckAmber, stringResource(Res.string.devices_card_state_unauthorized))
        DeviceState.UNKNOWN      -> Pair(MaterialTheme.colorScheme.outline, state.rawValue)
    }
    ColorBadge(label, color)
}

@Composable
private fun DeviceTransportBadge(transport: DeviceTransportType) {
    val (color, label) = when (transport) {
        DeviceTransportType.USB      -> Pair(
            MaterialTheme.colorScheme.secondary,
            stringResource(Res.string.devices_card_transport_usb),
        )
        DeviceTransportType.WIFI     -> Pair(
            MaterialTheme.colorScheme.tertiary,
            stringResource(Res.string.devices_card_transport_wifi),
        )
        DeviceTransportType.EMULATOR -> Pair(
            MaterialTheme.colorScheme.primary,
            stringResource(Res.string.devices_card_transport_emulator),
        )
        DeviceTransportType.UNKNOWN  -> return
    }
    ColorBadge(label, color)
}

@Composable
private fun ActiveBadge() {
    ColorBadge(stringResource(Res.string.devices_card_badge_active), AdbDeckGreen, bold = true)
}

@Composable
private fun ColorBadge(text: String, color: Color, bold: Boolean = false) {
    Surface(
        color = color.copy(alpha = 0.15f),
        shape = MaterialTheme.shapes.extraSmall,
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

package com.adbdeck.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.adbdeck.app.navigation.Screen
import com.adbdeck.core.designsystem.AdbCornerRadius
import com.adbdeck.core.designsystem.Dimensions

/**
 * Боковая навигационная панель (Sidebar) главного окна
 *
 * Содержит:
 * - Логотип и название приложения сверху
 * - Пункты навигации между разделами
 * - Кнопку переключения темы снизу
 *
 * @param activeScreen Текущий активный экран (для подсветки пункта)
 * @param onNavigate Callback навигации — вызывается при нажатии на пункт меню
 * @param isDarkTheme Текущий режим темы (для иконки переключателя)
 * @param onToggleTheme Callback переключения светлой / темной темы
 * @param historyCount Количество записей в логе ADB (истории команд)
 * @param isHistoryOpen Флаг, что панель лога ADB сейчас открыта
 * @param onToggleHistory Callback открытия/закрытия панели лога ADB
 * @param devicesCount Количество видимых ADB-устройств
 * @param isLogcatRunning Флаг активного захвата logcat
 * @param hasUnsavedSettings Флаг несохраненных настроек
 * @param isProcessMonitoring Флаг активного мониторинга процессов (badge «MON» в System Monitor)
 */
@Composable
fun Sidebar(
    activeScreen: Screen,
    onNavigate: (Screen) -> Unit,
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit,
    historyCount: Int,
    isHistoryOpen: Boolean,
    onToggleHistory: () -> Unit,
    devicesCount: Int,
    isLogcatRunning: Boolean,
    hasUnsavedSettings: Boolean,
    isProcessMonitoring: Boolean = false,
) {
    val navScrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .width(Dimensions.sidebarWidth)
            .fillMaxHeight()
            .background(MaterialTheme.colorScheme.surface),
    ) {
        // ── Шапка ───────────────────────────────────────────────
        SidebarHeader()

        HorizontalDivider()
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = Dimensions.paddingSmall, end = Dimensions.paddingSmall)
                    .verticalScroll(navScrollState),
            ) {
                // ── Пункты навигации ─────────────────────────────────────
                SidebarNavItem(
                    icon = Icons.Outlined.Dashboard,
                    label = "Dashboard",
                    isActive = activeScreen is Screen.Dashboard,
                    onClick = { onNavigate(Screen.Dashboard) },
                )
                SidebarNavItem(
                    icon = Icons.Outlined.DevicesOther,
                    label = "Devices",
                    isActive = activeScreen is Screen.Devices,
                    badgeText = devicesCount.takeIf { it > 0 }?.toString(),
                    onClick = { onNavigate(Screen.Devices) },
                )
                SidebarNavItem(
                    icon = Icons.Outlined.Terminal,
                    label = "Logcat",
                    isActive = activeScreen is Screen.Logcat,
                    badgeText = if (isLogcatRunning) "LIVE" else null,
                    badgeKind = SidebarBadgeKind.Positive,
                    onClick = { onNavigate(Screen.Logcat()) },
                )
                SidebarNavItem(
                    icon = Icons.Outlined.Apps,
                    label = "Packages",
                    isActive = activeScreen is Screen.Packages,
                    onClick = { onNavigate(Screen.Packages()) },
                )
                SidebarNavItem(
                    icon = Icons.Outlined.FolderOpen,
                    label = "Files",
                    isActive = activeScreen is Screen.FileExplorer,
                    onClick = { onNavigate(Screen.FileExplorer()) },
                )
                SidebarNavItem(
                    icon = Icons.Outlined.CameraAlt,
                    label = "Screen Tools",
                    isActive = activeScreen is Screen.ScreenTools,
                    onClick = { onNavigate(Screen.ScreenTools) },
                )
                SidebarNavItem(
                    icon = Icons.Outlined.Cast,
                    label = "Scrcpy",
                    isActive = activeScreen is Screen.Scrcpy,
                    onClick = { onNavigate(Screen.Scrcpy) },
                )
                SidebarNavItem(
                    icon = Icons.Outlined.SystemUpdateAlt,
                    label = "APK Install",
                    isActive = activeScreen is Screen.ApkInstall,
                    onClick = { onNavigate(Screen.ApkInstall) },
                )
                SidebarNavItem(
                    icon = Icons.Outlined.Link,
                    label = "Deep Links",
                    isActive = activeScreen is Screen.DeepLinks,
                    onClick = { onNavigate(Screen.DeepLinks()) },
                )
                SidebarNavItem(
                    icon = Icons.Outlined.Notifications,
                    label = "Notifications",
                    isActive = activeScreen is Screen.Notifications,
                    onClick = { onNavigate(Screen.Notifications) },
                )
                SidebarNavItem(
                    icon = Icons.Outlined.Info,
                    label = "Device Info",
                    isActive = activeScreen is Screen.DeviceInfo,
                    onClick = { onNavigate(Screen.DeviceInfo) },
                )
                SidebarNavItem(
                    icon = Icons.Outlined.Tune,
                    label = "Quick Toggles",
                    isActive = activeScreen is Screen.QuickToggles,
                    onClick = { onNavigate(Screen.QuickToggles) },
                )
                SidebarNavItem(
                    icon = Icons.Outlined.Contacts,
                    label = "Contacts",
                    isActive = activeScreen is Screen.Contacts,
                    onClick = { onNavigate(Screen.Contacts) },
                )
                SidebarNavItem(
                    icon = Icons.Outlined.Monitor,
                    label = "Processes",
                    isActive = activeScreen is Screen.SystemMonitor,
                    badgeText = if (isProcessMonitoring) "MON" else null,
                    badgeKind = SidebarBadgeKind.Positive,
                    onClick = { onNavigate(Screen.SystemMonitor) },
                )
                SidebarNavItem(
                    icon = Icons.Outlined.Storage,
                    label = "File System",
                    isActive = activeScreen is Screen.FileSystem,
                    onClick = { onNavigate(Screen.FileSystem) },
                )
                SidebarNavItem(
                    icon = Icons.Outlined.Settings,
                    label = "Settings",
                    isActive = activeScreen is Screen.Settings,
                    badgeText = if (hasUnsavedSettings) "!" else null,
                    badgeKind = SidebarBadgeKind.Warning,
                    onClick = { onNavigate(Screen.Settings) },
                )
                Spacer(Modifier.height(Dimensions.paddingSmall))
            }

            VerticalScrollbar(
                adapter = rememberScrollbarAdapter(navScrollState),
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .padding(vertical = Dimensions.paddingXSmall),
            )
        }
        HorizontalDivider()

        // ── Быстрые переключатели ────────────────────────────────
        SidebarQuickToggles(
            isDarkTheme = isDarkTheme,
            onToggleTheme = onToggleTheme,
            historyCount = historyCount,
            isHistoryOpen = isHistoryOpen,
            onToggleHistory = onToggleHistory,
        )
    }

    // Вертикальный разделитель между Sidebar и контентом
    Box(
        modifier = Modifier
            .width(1.dp)
            .fillMaxHeight()
            .background(MaterialTheme.colorScheme.outlineVariant),
    )
}

/**
 * Шапка Sidebar с названием приложения.
 */
@Composable
private fun SidebarHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(Dimensions.topBarHeight)
            .padding(horizontal = Dimensions.paddingDefault),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Outlined.Terminal,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(Dimensions.iconSizeNav),
        )
        Spacer(Modifier.width(Dimensions.paddingSmall))
        Text(
            text = "ADB Deck",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

/**
 * Один пункт навигации в боковой панели.
 *
 * @param icon     Иконка пункта.
 * @param label    Текстовая метка.
 * @param isActive Если `true` — пункт подсвечивается как активный.
 * @param onClick  Обработчик нажатия.
 */
@Composable
private fun SidebarNavItem(
    icon: ImageVector,
    label: String,
    isActive: Boolean,
    badgeText: String? = null,
    badgeKind: SidebarBadgeKind = SidebarBadgeKind.Neutral,
    onClick: () -> Unit,
) {
    val backgroundColor = if (isActive) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
    } else {
        androidx.compose.ui.graphics.Color.Transparent
    }

    val contentColor = if (isActive) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(Dimensions.navItemHeight)
            .padding(horizontal = Dimensions.paddingSmall, vertical = Dimensions.paddingXSmall)
            .clip(RoundedCornerShape(Dimensions.buttonCornerRadius))
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(horizontal = Dimensions.paddingSmall),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(Dimensions.iconSizeNav),
        )
        Spacer(Modifier.width(Dimensions.paddingMedium))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = contentColor,
            modifier = Modifier.weight(1f),
        )

        if (!badgeText.isNullOrBlank()) {
            SidebarBadge(
                text = badgeText,
                kind = badgeKind,
                isActive = isActive,
            )
        }
    }
}

private enum class SidebarBadgeKind {
    Neutral,
    Positive,
    Warning,
}

@Composable
private fun SidebarBadge(
    text: String,
    kind: SidebarBadgeKind,
    isActive: Boolean,
) {
    val (containerColor, contentColor) = when (kind) {
        SidebarBadgeKind.Neutral -> Pair(
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant,
        )
        SidebarBadgeKind.Positive -> Pair(
            MaterialTheme.colorScheme.tertiaryContainer,
            MaterialTheme.colorScheme.onTertiaryContainer,
        )
        SidebarBadgeKind.Warning -> Pair(
            MaterialTheme.colorScheme.errorContainer,
            MaterialTheme.colorScheme.onErrorContainer,
        )
    }

    val finalContainer = if (isActive) containerColor.copy(alpha = 0.85f) else containerColor

    Surface(
        shape = RoundedCornerShape(999.dp),
        color = finalContainer,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = if (kind == SidebarBadgeKind.Neutral && isActive) MaterialTheme.colorScheme.primary else contentColor,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
        )
    }
}

@Composable
private fun SidebarQuickToggles(
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit,
    historyCount: Int,
    isHistoryOpen: Boolean,
    onToggleHistory: () -> Unit,
) {
    val compactToggleHeight = Dimensions.navItemHeight - Dimensions.paddingSmall

    Surface(
        modifier = Modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(AdbCornerRadius.MEDIUM.value),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f),
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimensions.paddingXSmall),
            horizontalArrangement = Arrangement.spacedBy(Dimensions.paddingXSmall),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SidebarQuickToggleChip(
                modifier = Modifier.size(compactToggleHeight),
                icon = if (isDarkTheme) Icons.Outlined.DarkMode else Icons.Outlined.LightMode,
                iconDescription = if (isDarkTheme) "Dark theme enabled" else "Light theme enabled",
                label = null,
                trailing = null,
                containerColor = if (isDarkTheme) {
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.56f)
                } else {
                    MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
                },
                contentColor = MaterialTheme.colorScheme.onSurface,
                onClick = onToggleTheme,
            )

            SidebarQuickToggleChip(
                modifier = Modifier.weight(1f),
                icon = Icons.Outlined.Terminal,
                iconDescription = if (isHistoryOpen) "Close ADB log panel" else "Open ADB log panel",
                label = "ADB Log",
                trailing = historyCount.takeIf { it > 0 }?.toString(),
                containerColor = if (isHistoryOpen) {
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.56f)
                } else {
                    MaterialTheme.colorScheme.surfaceContainerHighest
                },
                contentColor = if (isHistoryOpen) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                height = compactToggleHeight,
                onClick = onToggleHistory,
            )
        }
    }
}

/**
 * Компактный toggle для нижнего блока Sidebar.
 * Используется как единый визуальный паттерн для темы и языка.
 */
@Composable
private fun SidebarQuickToggleChip(
    icon: ImageVector,
    iconDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    trailing: String? = null,
    height: androidx.compose.ui.unit.Dp = Dimensions.navItemHeight - Dimensions.paddingSmall,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerHighest,
    contentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    Row(
        modifier = modifier
            .height(height)
            .clip(RoundedCornerShape(Dimensions.buttonCornerRadius))
            .background(containerColor)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.18f),
                shape = RoundedCornerShape(Dimensions.buttonCornerRadius),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = Dimensions.paddingSmall),
        horizontalArrangement = if (label == null && trailing == null) {
            Arrangement.Center
        } else {
            Arrangement.Start
        },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = iconDescription,
            modifier = Modifier.size(Dimensions.iconSizeSmall),
            tint = contentColor,
        )
        if (!label.isNullOrBlank()) {
            Spacer(Modifier.width(Dimensions.paddingSmall))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = contentColor,
            )
        }
        if (!trailing.isNullOrBlank()) {
            Spacer(Modifier.weight(1f))
            Text(
                text = trailing,
                style = MaterialTheme.typography.labelSmall,
                color = contentColor,
            )
        }
    }
}

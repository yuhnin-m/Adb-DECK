package com.adbdeck.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.adbdeck.app.navigation.Screen
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
    devicesCount: Int,
    isLogcatRunning: Boolean,
    hasUnsavedSettings: Boolean,
    isProcessMonitoring: Boolean = false,
) {
    Column(
        modifier = Modifier
            .width(Dimensions.sidebarWidth)
            .fillMaxHeight()
            .background(MaterialTheme.colorScheme.surface),
    ) {
        // ── Шапка ───────────────────────────────────────────────
        SidebarHeader()

        HorizontalDivider()
        Spacer(Modifier.height(Dimensions.paddingSmall))

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
            onClick = { onNavigate(Screen.Logcat) },
        )
        SidebarNavItem(
            icon = Icons.Outlined.Apps,
            label = "Packages",
            isActive = activeScreen is Screen.Packages,
            onClick = { onNavigate(Screen.Packages) },
        )
        SidebarNavItem(
            icon = Icons.Outlined.FolderOpen,
            label = "Files",
            isActive = activeScreen is Screen.FileExplorer,
            onClick = { onNavigate(Screen.FileExplorer) },
        )
        SidebarNavItem(
            icon = Icons.Outlined.Monitor,
            label = "System",
            isActive = activeScreen is Screen.SystemMonitor,
            badgeText = if (isProcessMonitoring) "MON" else null,
            badgeKind = SidebarBadgeKind.Positive,
            onClick = { onNavigate(Screen.SystemMonitor) },
        )
        SidebarNavItem(
            icon = Icons.Outlined.Settings,
            label = "Settings",
            isActive = activeScreen is Screen.Settings,
            badgeText = if (hasUnsavedSettings) "!" else null,
            badgeKind = SidebarBadgeKind.Warning,
            onClick = { onNavigate(Screen.Settings) },
        )

        Spacer(Modifier.weight(1f))
        HorizontalDivider()

        // ── Переключатель темы ───────────────────────────────────
        ThemeToggle(isDarkTheme = isDarkTheme, onToggle = onToggleTheme)
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

/**
 * Кнопка переключения светлой / темной темы в нижней части Sidebar
 *
 * @param isDarkTheme Текущий режим темы
 * @param onToggle    Callback переключения
 */
@Composable
private fun ThemeToggle(isDarkTheme: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(Dimensions.navItemHeight)
            .padding(horizontal = Dimensions.paddingDefault),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = if (isDarkTheme) "Темная тема" else "Светлая тема",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onToggle) {
            Icon(
                imageVector = if (isDarkTheme) Icons.Outlined.LightMode else Icons.Outlined.DarkMode,
                contentDescription = if (isDarkTheme) "Переключить на светлую тему" else "Переключить на темную тему",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(Dimensions.iconSizeNav),
            )
        }
    }
}

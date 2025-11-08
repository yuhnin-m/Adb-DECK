package com.adbdeck.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.adbdeck.app.devicemanager.DeviceSelectorComponent
import com.adbdeck.app.navigation.RootComponent
import com.adbdeck.app.navigation.Screen
import com.adbdeck.feature.dashboard.ui.DashboardScreen
import com.adbdeck.feature.devices.ui.DevicesScreen
import com.adbdeck.feature.fileexplorer.ui.FileExplorerScreen
import com.adbdeck.feature.logcat.ui.LogcatScreen
import com.adbdeck.feature.packages.ui.PackagesScreen
import com.adbdeck.feature.settings.ui.SettingsScreen
import com.adbdeck.feature.systemmonitor.ui.SystemMonitorScreen
import com.arkivanov.decompose.extensions.compose.subscribeAsState

/**
 * Корневой composable главного окна ADB Deck.
 *
 * Собирает три слоя desktop layout:
 * - [Sidebar] слева — навигация между разделами
 * - Основной контент справа:
 *   - [TopBar] с заголовком раздела и [DeviceBar] (выбор устройства)
 *   - ContentArea — активный экран
 *   - [StatusBar]
 *
 * @param rootComponent          Корневой навигационный компонент.
 * @param isDarkTheme            Текущий режим темы (для Sidebar).
 * @param onToggleTheme          Callback переключения темы.
 * @param deviceSelectorComponent Компонент выбора устройства для TopBar.
 */
@Composable
fun AppContent(
    rootComponent: RootComponent,
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit,
    deviceSelectorComponent: DeviceSelectorComponent,
) {
    // subscribeAsState() возвращает Compose State<ChildStack>
    val childStack by rootComponent.childStack.subscribeAsState()
    val activeChild = childStack.active.instance
    val devices by deviceSelectorComponent.devices.collectAsState()

    val logcatComponent = childStack.items
        .asSequence()
        .map { it.instance }
        .filterIsInstance<RootComponent.Child.Logcat>()
        .map { it.component }
        .firstOrNull()
    val isLogcatRunning = logcatComponent?.let {
        val logcatState by it.state.collectAsState()
        logcatState.isRunning
    } ?: false

    val settingsComponent = childStack.items
        .asSequence()
        .map { it.instance }
        .filterIsInstance<RootComponent.Child.Settings>()
        .map { it.component }
        .firstOrNull()
    val hasUnsavedSettings = settingsComponent?.let {
        val settingsState by it.state.collectAsState()
        !settingsState.isSaved
    } ?: false

    // Получаем флаг мониторинга процессов из SystemMonitor-компонента (для badge в Sidebar)
    val systemMonitorComponent = childStack.items
        .asSequence()
        .map { it.instance }
        .filterIsInstance<RootComponent.Child.SystemMonitor>()
        .map { it.component }
        .firstOrNull()
    val isProcessMonitoring = systemMonitorComponent?.let {
        val monitoringState by it.isProcessMonitoring.collectAsState()
        monitoringState
    } ?: false

    // Определяем активный экран по типу дочернего компонента
    val activeScreen: Screen = when (activeChild) {
        is RootComponent.Child.Dashboard -> Screen.Dashboard
        is RootComponent.Child.Devices -> Screen.Devices
        is RootComponent.Child.Logcat -> Screen.Logcat
        is RootComponent.Child.Settings -> Screen.Settings
        is RootComponent.Child.Packages -> Screen.Packages
        is RootComponent.Child.SystemMonitor -> Screen.SystemMonitor
        is RootComponent.Child.FileExplorer -> Screen.FileExplorer
        else -> Screen.Dashboard
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            // ── Sidebar ─────────────────────────────────────────
            Sidebar(
                activeScreen = activeScreen,
                onNavigate = rootComponent::navigate,
                isDarkTheme = isDarkTheme,
                onToggleTheme = onToggleTheme,
                devicesCount = devices.size,
                isLogcatRunning = isLogcatRunning,
                hasUnsavedSettings = hasUnsavedSettings,
                isProcessMonitoring = isProcessMonitoring,
            )

            // ── Основная область ──────────────────────────────────
            Column(modifier = Modifier.fillMaxSize()) {
                // TopBar с DeviceBar в trailing-слоте
                TopBar(
                    title = activeScreen.title(),
                    trailingContent = {
                        DeviceBar(component = deviceSelectorComponent)
                    },
                )

                Box(modifier = Modifier.weight(1f)) {
                    // Рендерим активный экран напрямую по типу активного Child
                    when (val instance = activeChild) {
                        is RootComponent.Child.Dashboard -> DashboardScreen(instance.component)
                        is RootComponent.Child.Devices -> DevicesScreen(instance.component)
                        is RootComponent.Child.Logcat -> LogcatScreen(instance.component)
                        is RootComponent.Child.Settings -> SettingsScreen(instance.component)
                        is RootComponent.Child.Packages -> PackagesScreen(instance.component)
                        is RootComponent.Child.SystemMonitor -> SystemMonitorScreen(instance.component)
                        is RootComponent.Child.FileExplorer -> FileExplorerScreen(instance.component)
                    }
                }

                StatusBar()
            }
        }
    }
}

/**
 * Возвращает заголовок TopBar для текущего экрана.
 */
private fun Screen.title(): String = when (this) {
    is Screen.Dashboard -> "Dashboard"
    is Screen.Devices -> "Devices"
    is Screen.Logcat -> "Logcat"
    is Screen.Settings -> "Settings"
    is Screen.Packages -> "Packages"
    is Screen.SystemMonitor -> "System Monitor"
    is Screen.FileExplorer -> "File Explorer"
}

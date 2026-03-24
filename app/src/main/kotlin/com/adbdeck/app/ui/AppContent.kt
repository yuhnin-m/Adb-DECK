package com.adbdeck.app.ui

import adbdeck.app.generated.resources.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.adbdeck.app.devicemanager.DeviceSelectorComponent
import com.adbdeck.app.navigation.RootComponent
import com.adbdeck.app.navigation.Screen
import com.adbdeck.core.designsystem.AdbTheme
import com.adbdeck.core.designsystem.Dimensions
import com.adbdeck.core.process.ProcessHistoryStore
import com.adbdeck.feature.update.AppUpdateUiState
import com.adbdeck.feature.update.ui.AppUpdateOverlay
import com.adbdeck.feature.contacts.ui.ContactsScreen
import com.adbdeck.feature.dashboard.ui.DashboardScreen
import com.adbdeck.feature.deviceinfo.ui.DeviceInfoScreen
import com.adbdeck.feature.devices.ui.DevicesScreen
import com.adbdeck.feature.apkinstall.ui.ApkInstallScreen
import com.adbdeck.feature.deeplinks.ui.DeepLinksScreen
import com.adbdeck.feature.notifications.ui.NotificationsScreen
import com.adbdeck.feature.filesystem.ui.FileSystemScreen
import com.adbdeck.feature.fileexplorer.ui.FileExplorerScreen
import com.adbdeck.feature.logcat.ui.LogcatScreen
import com.adbdeck.feature.packages.ui.PackagesScreen
import com.adbdeck.feature.quicktoggles.ui.QuickTogglesScreen
import com.adbdeck.feature.screentools.ui.ScreenToolsScreen
import com.adbdeck.feature.scrcpy.ui.ScrcpyScreen
import com.adbdeck.feature.settings.ui.SettingsScreen
import com.adbdeck.feature.systemmonitor.ui.SystemMonitorScreen
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import org.jetbrains.compose.resources.stringResource

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
 * @param processHistoryStore    In-memory история команд ProcessRunner.
 */
@Composable
fun AppContent(
    rootComponent: RootComponent,
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit,
    deviceSelectorComponent: DeviceSelectorComponent,
    processHistoryStore: ProcessHistoryStore,
    appUpdateState: AppUpdateUiState = AppUpdateUiState.Hidden,
    onInstallUpdateNow: (() -> Unit)? = null,
    onCancelUpdate: (() -> Unit)? = null,
    onDismissUpdate: (() -> Unit)? = null,
) {
    // subscribeAsState() возвращает Compose State<ChildStack>
    val childStack by rootComponent.childStack.subscribeAsState()
    val activeChild = childStack.active.instance
    val devices by deviceSelectorComponent.devices.collectAsState()
    val processHistoryEntries by processHistoryStore.entries.collectAsState()
    var isHistoryPanelOpen by remember { mutableStateOf(false) }

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
        settingsState.hasPendingChanges
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
        is RootComponent.Child.Logcat -> Screen.Logcat()
        is RootComponent.Child.Settings -> Screen.Settings
        is RootComponent.Child.Packages -> Screen.Packages()
        is RootComponent.Child.SystemMonitor -> Screen.SystemMonitor
        is RootComponent.Child.FileSystem -> Screen.FileSystem
        is RootComponent.Child.FileExplorer -> Screen.FileExplorer()
        is RootComponent.Child.Contacts -> Screen.Contacts
        is RootComponent.Child.ScreenTools -> Screen.ScreenTools
        is RootComponent.Child.ApkInstall -> Screen.ApkInstall
        is RootComponent.Child.DeepLinks -> Screen.DeepLinks()
        is RootComponent.Child.Notifications -> Screen.Notifications
        is RootComponent.Child.DeviceInfo -> Screen.DeviceInfo
        is RootComponent.Child.QuickToggles -> Screen.QuickToggles
        is RootComponent.Child.Scrcpy -> Screen.Scrcpy
        else -> Screen.Dashboard
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = AdbTheme.colorScheme.surface,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Row(modifier = Modifier.fillMaxSize()) {
                // ── Sidebar ─────────────────────────────────────────
                Sidebar(
                    activeScreen = activeScreen,
                    onNavigate = rootComponent::navigate,
                    isDarkTheme = isDarkTheme,
                    onToggleTheme = onToggleTheme,
                    historyCount = processHistoryEntries.size,
                    isHistoryOpen = isHistoryPanelOpen,
                    onToggleHistory = { isHistoryPanelOpen = !isHistoryPanelOpen },
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

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                    ) {
                        // Рендерим активный экран напрямую по типу активного Child
                        when (activeChild) {
                            is RootComponent.Child.Dashboard -> DashboardScreen(activeChild.component)
                            is RootComponent.Child.Devices -> DevicesScreen(activeChild.component)
                            is RootComponent.Child.Logcat -> LogcatScreen(activeChild.component)
                            is RootComponent.Child.Settings -> SettingsScreen(activeChild.component)
                            is RootComponent.Child.Packages -> PackagesScreen(activeChild.component)
                            is RootComponent.Child.SystemMonitor -> SystemMonitorScreen(activeChild.component)
                            is RootComponent.Child.FileSystem -> FileSystemScreen(activeChild.component)
                            is RootComponent.Child.FileExplorer -> FileExplorerScreen(activeChild.component)
                            is RootComponent.Child.Contacts -> ContactsScreen(activeChild.component)
                            is RootComponent.Child.ScreenTools -> ScreenToolsScreen(activeChild.component)
                            is RootComponent.Child.ApkInstall -> ApkInstallScreen(activeChild.component)
                            is RootComponent.Child.DeepLinks -> DeepLinksScreen(activeChild.component)
                            is RootComponent.Child.Notifications -> NotificationsScreen(activeChild.component)
                            is RootComponent.Child.DeviceInfo -> DeviceInfoScreen(activeChild.component)
                            is RootComponent.Child.QuickToggles -> QuickTogglesScreen(activeChild.component)
                            is RootComponent.Child.Scrcpy -> ScrcpyScreen(activeChild.component)
                        }
                    }

                    if (isHistoryPanelOpen) {
                        ProcessHistoryPanel(
                            entries = processHistoryEntries,
                            onClose = { isHistoryPanelOpen = false },
                            onClear = processHistoryStore::clear,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(Dimensions.sidebarWidth + Dimensions.paddingSmall),
                        )
                    }

                    StatusBar()
                }
            }

            AppUpdateOverlay(
                state = appUpdateState,
                onInstallNow = onInstallUpdateNow,
                onCancel = onCancelUpdate,
                onDismiss = onDismissUpdate,
            )
        }
    }
}

/**
 * Возвращает заголовок TopBar для текущего экрана.
 */
@Composable
private fun Screen.title(): String = when (this) {
    is Screen.Dashboard -> stringResource(Res.string.app_topbar_title_dashboard)
    is Screen.Devices -> stringResource(Res.string.app_topbar_title_devices)
    is Screen.Logcat -> stringResource(Res.string.app_topbar_title_logcat)
    is Screen.Settings -> stringResource(Res.string.app_topbar_title_settings)
    is Screen.Packages -> stringResource(Res.string.app_topbar_title_packages)
    is Screen.SystemMonitor -> stringResource(Res.string.app_topbar_title_system_monitor)
    is Screen.FileSystem -> stringResource(Res.string.app_topbar_title_file_system)
    is Screen.FileExplorer -> stringResource(Res.string.app_topbar_title_file_explorer)
    is Screen.Contacts -> stringResource(Res.string.app_topbar_title_contacts)
    is Screen.ScreenTools -> stringResource(Res.string.app_topbar_title_screen_tools)
    is Screen.ApkInstall -> stringResource(Res.string.app_topbar_title_apk_install)
    is Screen.DeepLinks -> stringResource(Res.string.app_topbar_title_deep_links)
    is Screen.Notifications -> stringResource(Res.string.app_topbar_title_notifications)
    is Screen.DeviceInfo -> stringResource(Res.string.app_topbar_title_device_info)
    is Screen.QuickToggles -> stringResource(Res.string.app_topbar_title_quick_toggles)
    is Screen.Scrcpy -> stringResource(Res.string.app_topbar_title_scrcpy)
}

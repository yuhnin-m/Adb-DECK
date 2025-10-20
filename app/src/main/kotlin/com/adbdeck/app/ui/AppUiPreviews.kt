package com.adbdeck.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.adbdeck.app.devicemanager.DeviceSelectorComponent
import com.adbdeck.app.navigation.RootComponent
import com.adbdeck.app.navigation.Screen
import com.adbdeck.core.adb.api.AdbDevice
import com.adbdeck.core.adb.api.DeviceEndpoint
import com.adbdeck.core.adb.api.DeviceState
import com.adbdeck.core.adb.api.LogcatEntry
import com.adbdeck.core.adb.api.LogcatLevel
import com.adbdeck.core.designsystem.AdbDeckTheme
import com.adbdeck.core.designsystem.Dimensions
import com.adbdeck.core.settings.AppTheme
import com.adbdeck.feature.dashboard.DashboardComponent
import com.adbdeck.feature.dashboard.DashboardState
import com.adbdeck.feature.devices.DevicesComponent
import com.adbdeck.feature.devices.DevicesState
import com.adbdeck.feature.logcat.LogcatComponent
import com.adbdeck.feature.logcat.LogcatDisplayMode
import com.adbdeck.feature.logcat.LogcatState
import com.adbdeck.feature.settings.SettingsComponent
import com.adbdeck.feature.settings.SettingsUiState
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.jetbrains.compose.ui.tooling.preview.Preview

private val previewDevices = listOf(
    AdbDevice("emulator-5554", DeviceState.DEVICE, "Pixel_8_API_34"),
    AdbDevice("192.168.0.15:5555", DeviceState.OFFLINE, "Wi-Fi"),
    AdbDevice("R58N123ABC", DeviceState.UNAUTHORIZED, "Samsung"),
)

private val previewLogEntries = listOf(
    LogcatEntry(
        id = 1L,
        raw = "03-04 12:31:40.111  2048  4096 I ActivityManager: Start proc com.example.app",
        date = "03-04",
        time = "12:31:40",
        millis = "111",
        pid = "2048",
        tid = "4096",
        level = LogcatLevel.INFO,
        tag = "ActivityManager",
        message = "Start proc com.example.app",
    ),
    LogcatEntry(
        id = 2L,
        raw = "03-04 12:31:41.222  2048  4096 W ExampleTag: Slow call detected",
        date = "03-04",
        time = "12:31:41",
        millis = "222",
        pid = "2048",
        tid = "4096",
        level = LogcatLevel.WARNING,
        tag = "ExampleTag",
        message = "Slow call detected",
    ),
    LogcatEntry(
        id = 3L,
        raw = "03-04 12:31:42.333  2048  4096 E CrashReporter: NullPointerException",
        date = "03-04",
        time = "12:31:42",
        millis = "333",
        pid = "2048",
        tid = "4096",
        level = LogcatLevel.ERROR,
        tag = "CrashReporter",
        message = "NullPointerException",
    ),
)

private class PreviewDashboardComponent : DashboardComponent {
    override val state: StateFlow<DashboardState> = MutableStateFlow(
        DashboardState(
            adbStatusText = "✓ adb доступен: Android Debug Bridge version 1.0.41",
            deviceCount = previewDevices.size,
        )
    )

    override fun onOpenDevices() = Unit
    override fun onOpenLogcat() = Unit
    override fun onOpenSettings() = Unit
    override fun onRefreshDevices() = Unit
    override fun onCheckAdb() = Unit
}

private class PreviewDevicesComponent : DevicesComponent {
    override val state: StateFlow<DevicesState> =
        MutableStateFlow(DevicesState.Success(previewDevices))

    override fun onRefresh() = Unit
}

private class PreviewLogcatComponent : LogcatComponent {
    override val state: StateFlow<LogcatState> = MutableStateFlow(
        LogcatState(
            isRunning = true,
            activeDeviceId = previewDevices.first().deviceId,
            entries = previewLogEntries,
            filteredEntries = previewLogEntries,
            totalLineCount = previewLogEntries.size,
            displayMode = LogcatDisplayMode.COMPACT,
            showTime = true,
            showMillis = true,
            coloredLevels = true,
            autoScroll = true,
        )
    )

    override fun onStart() = Unit
    override fun onStop() = Unit
    override fun onClear() = Unit
    override fun onSearchChanged(query: String) = Unit
    override fun onTagFilterChanged(tag: String) = Unit
    override fun onPackageFilterChanged(pkg: String) = Unit
    override fun onLevelFilterChanged(level: LogcatLevel?) = Unit
    override fun onDisplayModeChanged(mode: LogcatDisplayMode) = Unit
    override fun onToggleShowDate() = Unit
    override fun onToggleShowTime() = Unit
    override fun onToggleShowMillis() = Unit
    override fun onToggleColoredLevels() = Unit
    override fun onAutoScrollChanged(value: Boolean) = Unit
}

private class PreviewSettingsComponent : SettingsComponent {
    override val state: StateFlow<SettingsUiState> = MutableStateFlow(
        SettingsUiState(
            adbPath = "/opt/homebrew/bin/adb",
            adbCheckResult = "✓ Доступен: Android Debug Bridge version 1.0.41",
            isSaved = true,
            currentTheme = AppTheme.DARK,
            logcatCompactMode = true,
            logcatShowDate = false,
            logcatShowTime = true,
            logcatShowMillis = true,
            logcatColoredLevels = true,
            logcatMaxBufferedLines = 5000,
            logcatAutoScroll = true,
        )
    )

    override fun onAdbPathChanged(path: String) = Unit
    override fun onSave() = Unit
    override fun onCheckAdb() = Unit
    override fun onThemeChanged(theme: AppTheme) = Unit
    override fun onLogcatCompactModeChanged(value: Boolean) = Unit
    override fun onLogcatShowDateChanged(value: Boolean) = Unit
    override fun onLogcatShowTimeChanged(value: Boolean) = Unit
    override fun onLogcatShowMillisChanged(value: Boolean) = Unit
    override fun onLogcatColoredLevelsChanged(value: Boolean) = Unit
    override fun onLogcatMaxBufferedLinesChanged(lines: Int) = Unit
    override fun onLogcatAutoScrollChanged(value: Boolean) = Unit
}

private class PreviewRootComponent(
    initialScreen: Screen = Screen.Dashboard,
) : RootComponent {
    private val dashboard = PreviewDashboardComponent()
    private val devices = PreviewDevicesComponent()
    private val logcat = PreviewLogcatComponent()
    private val settings = PreviewSettingsComponent()

    private val stack = MutableValue(createStack(initialScreen))

    override val childStack: Value<ChildStack<*, RootComponent.Child>> = stack

    override fun navigate(screen: Screen) {
        stack.value = createStack(screen)
    }

    private fun createStack(screen: Screen): ChildStack<Screen, RootComponent.Child> =
        ChildStack(screen, childFor(screen))

    private fun childFor(screen: Screen): RootComponent.Child = when (screen) {
        Screen.Dashboard -> RootComponent.Child.Dashboard(dashboard)
        Screen.Devices -> RootComponent.Child.Devices(devices)
        Screen.Logcat -> RootComponent.Child.Logcat(logcat)
        Screen.Settings -> RootComponent.Child.Settings(settings)
    }
}

private class PreviewDeviceSelectorComponent : DeviceSelectorComponent {
    override val devices: StateFlow<List<AdbDevice>> = MutableStateFlow(previewDevices)
    override val selectedDevice: StateFlow<AdbDevice?> = MutableStateFlow(previewDevices.first())
    override val isConnecting: StateFlow<Boolean> = MutableStateFlow(false)
    override val error: StateFlow<String?> = MutableStateFlow(null)
    override val savedEndpoints: StateFlow<List<DeviceEndpoint>> = MutableStateFlow(
        listOf(
            DeviceEndpoint("192.168.0.15", 5555, "Office Pixel"),
            DeviceEndpoint("10.0.0.21", 5555, "QA Device"),
        )
    )

    override fun onRefresh() = Unit
    override fun onConnect(host: String, port: Int) = Unit
    override fun onDisconnect() = Unit
    override fun onSelectDevice(device: AdbDevice) = Unit
    override fun onConnectSaved(endpoint: DeviceEndpoint) = Unit
    override fun onSwitchToTcpIp(serialId: String, port: Int) = Unit
    override fun onRemoveEndpoint(endpoint: DeviceEndpoint) = Unit
    override fun onClearError() = Unit
}

@Composable
private fun AppComponentPreviewContainer(
    isDarkTheme: Boolean,
    content: @Composable () -> Unit,
) {
    AdbDeckTheme(isDarkTheme = isDarkTheme) {
        Surface(modifier = Modifier.fillMaxSize()) {
            content()
        }
    }
}

@Composable
private fun AppContentPreviewBody(isDarkTheme: Boolean) {
    val root = remember { PreviewRootComponent(Screen.Dashboard) }
    val selector = remember { PreviewDeviceSelectorComponent() }
    AppComponentPreviewContainer(isDarkTheme = isDarkTheme) {
        AppContent(
            rootComponent = root,
            isDarkTheme = isDarkTheme,
            onToggleTheme = {},
            deviceSelectorComponent = selector,
        )
    }
}

@Preview
@Composable
private fun AppContentLightPreview() {
    AppContentPreviewBody(isDarkTheme = false)
}

@Preview
@Composable
private fun AppContentDarkPreview() {
    AppContentPreviewBody(isDarkTheme = true)
}

@Composable
private fun SidebarPreviewBody(isDarkTheme: Boolean) {
    AppComponentPreviewContainer(isDarkTheme = isDarkTheme) {
        Sidebar(
            activeScreen = Screen.Devices,
            onNavigate = {},
            isDarkTheme = isDarkTheme,
            onToggleTheme = {},
            devicesCount = 3,
            isLogcatRunning = true,
            hasUnsavedSettings = true,
        )
    }
}

@Preview
@Composable
private fun SidebarLightPreview() {
    SidebarPreviewBody(isDarkTheme = false)
}

@Preview
@Composable
private fun SidebarDarkPreview() {
    SidebarPreviewBody(isDarkTheme = true)
}

@Composable
private fun TopBarPreviewBody(isDarkTheme: Boolean) {
    val selector = remember { PreviewDeviceSelectorComponent() }
    AppComponentPreviewContainer(isDarkTheme = isDarkTheme) {
        TopBar(
            title = "Devices",
            trailingContent = { DeviceBar(component = selector) },
        )
    }
}

@Preview
@Composable
private fun TopBarLightPreview() {
    TopBarPreviewBody(isDarkTheme = false)
}

@Preview
@Composable
private fun TopBarDarkPreview() {
    TopBarPreviewBody(isDarkTheme = true)
}

@Composable
private fun StatusBarPreviewBody(isDarkTheme: Boolean) {
    AppComponentPreviewContainer(isDarkTheme = isDarkTheme) {
        StatusBar(statusText = "Подключено устройств: 3")
    }
}

@Preview
@Composable
private fun StatusBarLightPreview() {
    StatusBarPreviewBody(isDarkTheme = false)
}

@Preview
@Composable
private fun StatusBarDarkPreview() {
    StatusBarPreviewBody(isDarkTheme = true)
}

@Composable
private fun DeviceBarPreviewBody(isDarkTheme: Boolean) {
    val selector = remember { PreviewDeviceSelectorComponent() }
    AppComponentPreviewContainer(isDarkTheme = isDarkTheme) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(Dimensions.paddingLarge),
            verticalArrangement = Arrangement.Top,
        ) {
            DeviceBar(component = selector)
        }
    }
}

@Preview
@Composable
private fun DeviceBarLightPreview() {
    DeviceBarPreviewBody(isDarkTheme = false)
}

@Preview
@Composable
private fun DeviceBarDarkPreview() {
    DeviceBarPreviewBody(isDarkTheme = true)
}

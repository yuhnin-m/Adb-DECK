package com.adbdeck.feature.dashboard.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.adbdeck.core.designsystem.AdbDeckTheme
import com.adbdeck.feature.dashboard.DashboardAdbCheckState
import com.adbdeck.feature.dashboard.DashboardAdbServerState
import com.adbdeck.feature.dashboard.DashboardAdbServerUiState
import com.adbdeck.feature.dashboard.DashboardComponent
import com.adbdeck.feature.dashboard.DashboardState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.jetbrains.compose.ui.tooling.preview.Preview

private class DashboardPreviewComponent : DashboardComponent {
    override val state: StateFlow<DashboardState> = MutableStateFlow(
        DashboardState(
            adbCheckState = DashboardAdbCheckState.Available("Android Debug Bridge version 1.0.41"),
            deviceCount = 3,
            isRefreshingDevices = false,
            adbServer = DashboardAdbServerUiState(
                adbPath = "/opt/homebrew/bin/adb",
                isAdbFound = true,
                adbVersion = "Android Debug Bridge version 1.0.41",
                serverState = DashboardAdbServerState.RUNNING,
                serverMessage = "Ready",
            ),
        )
    )

    override fun onOpenDevices() = Unit
    override fun onOpenDeviceInfo() = Unit
    override fun onOpenQuickToggles() = Unit
    override fun onOpenLogcat() = Unit
    override fun onOpenPackages() = Unit
    override fun onOpenApkInstall() = Unit
    override fun onOpenDeepLinks() = Unit
    override fun onOpenNotifications() = Unit
    override fun onOpenScreenTools() = Unit
    override fun onOpenScrcpy() = Unit
    override fun onOpenFileExplorer() = Unit
    override fun onOpenFileSystem() = Unit
    override fun onOpenContacts() = Unit
    override fun onOpenSystemMonitor() = Unit
    override fun onOpenSettings() = Unit
    override fun onOpenAdbShell() = Unit
    override fun onOpenRootAdbShell() = Unit
    override fun onRefreshDevices() = Unit
    override fun onCheckAdb() = Unit
    override fun onRefreshAdbServerStatus() = Unit
    override fun onStartAdbServer() = Unit
    override fun onStopAdbServer() = Unit
    override fun onRestartAdbServer() = Unit
    override fun onDismissAdbCheck() = Unit
    override fun onDismissRefreshError() = Unit
    override fun onDismissAdbServerError() = Unit
    override fun onDismissTerminalLaunchError() = Unit
    override fun onDismissAppUpdateBanner() = Unit
}

@Composable
private fun DashboardPreviewBody(isDarkTheme: Boolean) {
    val component = DashboardPreviewComponent()
    AdbDeckTheme(isDarkTheme = isDarkTheme) {
        Surface(modifier = Modifier.fillMaxSize()) {
            DashboardScreen(component = component)
        }
    }
}

@Preview
@Composable
private fun DashboardLightPreview() {
    DashboardPreviewBody(isDarkTheme = false)
}

@Preview
@Composable
private fun DashboardDarkPreview() {
    DashboardPreviewBody(isDarkTheme = true)
}

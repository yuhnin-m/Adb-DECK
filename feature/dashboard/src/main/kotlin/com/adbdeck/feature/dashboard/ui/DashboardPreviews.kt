package com.adbdeck.feature.dashboard.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.adbdeck.core.designsystem.AdbDeckTheme
import com.adbdeck.feature.dashboard.DashboardAdbCheckState
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
        )
    )

    override fun onOpenDevices() = Unit
    override fun onOpenLogcat() = Unit
    override fun onOpenSettings() = Unit
    override fun onRefreshDevices() = Unit
    override fun onCheckAdb() = Unit
    override fun onDismissAdbCheck() = Unit
    override fun onDismissRefreshError() = Unit
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

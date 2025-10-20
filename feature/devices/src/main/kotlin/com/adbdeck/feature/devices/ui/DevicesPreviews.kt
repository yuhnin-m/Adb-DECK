package com.adbdeck.feature.devices.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.adbdeck.core.adb.api.AdbDevice
import com.adbdeck.core.adb.api.DeviceState
import com.adbdeck.core.designsystem.AdbDeckTheme
import com.adbdeck.feature.devices.DevicesComponent
import com.adbdeck.feature.devices.DevicesState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.jetbrains.compose.ui.tooling.preview.Preview

private class DevicesPreviewComponent : DevicesComponent {
    override val state: StateFlow<DevicesState> = MutableStateFlow(
        DevicesState.Success(
            listOf(
                AdbDevice("emulator-5554", DeviceState.DEVICE, "Pixel_8_API_34"),
                AdbDevice("R58N123ABC", DeviceState.UNAUTHORIZED, "Samsung S24"),
                AdbDevice("192.168.0.15:5555", DeviceState.OFFLINE, "Wi-Fi"),
            )
        )
    )

    override fun onRefresh() = Unit
}

@Composable
private fun DevicesPreviewBody(isDarkTheme: Boolean) {
    val component = DevicesPreviewComponent()
    AdbDeckTheme(isDarkTheme = isDarkTheme) {
        Surface(modifier = Modifier.fillMaxSize()) {
            DevicesScreen(component = component)
        }
    }
}

@Preview
@Composable
private fun DevicesLightPreview() {
    DevicesPreviewBody(isDarkTheme = false)
}

@Preview
@Composable
private fun DevicesDarkPreview() {
    DevicesPreviewBody(isDarkTheme = true)
}

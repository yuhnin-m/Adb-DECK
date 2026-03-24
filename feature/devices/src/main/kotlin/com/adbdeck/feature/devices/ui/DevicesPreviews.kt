package com.adbdeck.feature.devices.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.adbdeck.core.adb.api.device.AdbDevice
import com.adbdeck.core.adb.api.device.DeviceInfoLoadState
import com.adbdeck.core.adb.api.device.DeviceState
import com.adbdeck.core.adb.api.device.SavedWifiDevice
import com.adbdeck.core.designsystem.AdbDeckTheme
import com.adbdeck.feature.devices.DeviceListState
import com.adbdeck.feature.devices.DevicesComponent
import com.adbdeck.feature.devices.DevicesState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.jetbrains.compose.ui.tooling.preview.Preview

private val previewDevices = listOf(
    AdbDevice("emulator-5554",      DeviceState.DEVICE,       "Pixel_8_API_34"),
    AdbDevice("R58N123ABC",         DeviceState.UNAUTHORIZED, "Samsung S24"),
    AdbDevice("192.168.0.15:5555",  DeviceState.OFFLINE,      "Wi-Fi"),
)

private val previewWifiHistory = listOf(
    SavedWifiDevice(
        address = "192.168.0.15:5555",
        deviceId = "192.168.0.15:5555",
        displayName = "Google Pixel 8",
        lastSeenAt = 1_710_000_000_000L,
    ),
    SavedWifiDevice(
        address = "10.0.0.21:5555",
        deviceId = "10.0.0.21:5555",
        displayName = "Samsung S24",
        lastSeenAt = 1_709_999_900_000L,
    ),
)

/**
 * Preview-stub реализации [DevicesComponent].
 *
 * Предоставляет статичное состояние для визуального превью в IDE.
 */
private class DevicesPreviewComponent : DevicesComponent {
    override val state: StateFlow<DevicesState> = MutableStateFlow(
        DevicesState(
            listState        = DeviceListState.Success(previewDevices),
            selectedDeviceId = "emulator-5554",
            deviceInfos      = mapOf(
                "emulator-5554" to DeviceInfoLoadState.Loading,
            ),
            wifiHistory = previewWifiHistory,
        )
    )

    override fun onRefresh() = Unit
    override fun onConnectHistoryDevice(device: SavedWifiDevice, portOverride: Int?) = Unit
    override fun onRemoveHistoryDevice(device: SavedWifiDevice) = Unit
    override fun onSelectDevice(device: AdbDevice) = Unit
    override fun onOpenDetails(device: AdbDevice) = Unit
    override fun onCloseDetails() = Unit
    override fun onRefreshDeviceInfo(device: AdbDevice) = Unit
    override fun onNavigateToLogcat() = Unit
    override fun onNavigateToPackages() = Unit
    override fun onNavigateToSystemMonitor() = Unit
    override fun onRequestReboot(device: AdbDevice) = Unit
    override fun onRequestRebootRecovery(device: AdbDevice) = Unit
    override fun onRequestRebootBootloader(device: AdbDevice) = Unit
    override fun onRequestDisconnect(device: AdbDevice) = Unit
    override fun onConfirmAction() = Unit
    override fun onCancelAction() = Unit
    override fun onDismissFeedback() = Unit
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

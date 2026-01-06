package com.adbdeck.feature.deviceinfo.ui

import androidx.compose.runtime.Composable
import com.adbdeck.core.designsystem.AdbDeckTheme
import com.adbdeck.feature.deviceinfo.DeviceInfoComponent
import com.adbdeck.feature.deviceinfo.DeviceInfoRow
import com.adbdeck.feature.deviceinfo.DeviceInfoSection
import com.adbdeck.feature.deviceinfo.DeviceInfoSectionKind
import com.adbdeck.feature.deviceinfo.DeviceInfoSectionLoadState
import com.adbdeck.feature.deviceinfo.DeviceInfoState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.jetbrains.compose.ui.tooling.preview.Preview

/**
 * Стаб [DeviceInfoComponent] для превью и тестовых сценариев.
 */
private class PreviewDeviceInfoComponent(
    initialState: DeviceInfoState,
) : DeviceInfoComponent {

    override val state: StateFlow<DeviceInfoState> = MutableStateFlow(initialState)

    override fun onRefresh() = Unit

    override fun onExportJson(path: String) = Unit

    override fun onDismissFeedback() = Unit
}

private val previewOverviewRows = listOf(
    DeviceInfoRow(id = "overview:0", key = "Device ID", value = "emulator-5554"),
    DeviceInfoRow(id = "overview:1", key = "Model", value = "Pixel 8"),
    DeviceInfoRow(id = "overview:2", key = "Android", value = "14 (SDK 34)"),
)

private val previewSections = listOf(
    DeviceInfoSection(
        kind = DeviceInfoSectionKind.OVERVIEW,
        state = DeviceInfoSectionLoadState.Success(previewOverviewRows),
    ),
    DeviceInfoSection(
        kind = DeviceInfoSectionKind.BUILD,
        state = DeviceInfoSectionLoadState.Success(
            listOf(
                DeviceInfoRow(id = "build:0", key = "Build ID", value = "AP2A.240905.003"),
                DeviceInfoRow(id = "build:1", key = "Build type", value = "userdebug"),
            )
        ),
    ),
    DeviceInfoSection(
        kind = DeviceInfoSectionKind.DISPLAY,
        state = DeviceInfoSectionLoadState.Loading,
    ),
    DeviceInfoSection(
        kind = DeviceInfoSectionKind.CPU_RAM,
        state = DeviceInfoSectionLoadState.Error("Команда meminfo временно недоступна"),
    ),
    DeviceInfoSection(
        kind = DeviceInfoSectionKind.BATTERY,
        state = DeviceInfoSectionLoadState.Success(
            listOf(
                DeviceInfoRow(id = "battery:0", key = "Battery level", value = "86%"),
                DeviceInfoRow(id = "battery:1", key = "Battery status", value = "Charging"),
            )
        ),
    ),
    DeviceInfoSection(
        kind = DeviceInfoSectionKind.NETWORK,
        state = DeviceInfoSectionLoadState.Success(
            listOf(
                DeviceInfoRow(id = "network:0", key = "IP addresses", value = "192.168.0.24/24"),
            )
        ),
    ),
    DeviceInfoSection(
        kind = DeviceInfoSectionKind.CELLULAR,
        state = DeviceInfoSectionLoadState.Success(
            listOf(
                DeviceInfoRow(id = "cellular:0", key = "Operator", value = "MegaFon / MFON"),
                DeviceInfoRow(id = "cellular:1", key = "Voice reg state", value = "IN_SERVICE"),
            )
        ),
    ),
    DeviceInfoSection(
        kind = DeviceInfoSectionKind.MODEM,
        state = DeviceInfoSectionLoadState.Success(
            listOf(
                DeviceInfoRow(id = "modem:0", key = "Baseband", value = "MOLY.LR12A.R3.MP.V110"),
            )
        ),
    ),
    DeviceInfoSection(
        kind = DeviceInfoSectionKind.IMS_RCS,
        state = DeviceInfoSectionLoadState.Success(
            listOf(
                DeviceInfoRow(id = "ims:0", key = "MMTEL state", value = "READY"),
                DeviceInfoRow(id = "ims:1", key = "RCS state", value = "UNAVAILABLE"),
            )
        ),
    ),
    DeviceInfoSection(
        kind = DeviceInfoSectionKind.STORAGE,
        state = DeviceInfoSectionLoadState.Success(
            listOf(
                DeviceInfoRow(id = "storage:0", key = "/data", value = "18G / 64G (29%)"),
            )
        ),
    ),
    DeviceInfoSection(
        kind = DeviceInfoSectionKind.SECURITY,
        state = DeviceInfoSectionLoadState.Success(
            listOf(
                DeviceInfoRow(id = "security:0", key = "SELinux", value = "Enforcing"),
            )
        ),
    ),
    DeviceInfoSection(
        kind = DeviceInfoSectionKind.SYSTEM,
        state = DeviceInfoSectionLoadState.Success(
            listOf(
                DeviceInfoRow(id = "system:0", key = "Current focus", value = "mCurrentFocus=Window{...}"),
            )
        ),
    ),
)

@Preview
@Composable
private fun DeviceInfoLightPreview() {
    AdbDeckTheme(isDarkTheme = false) {
        DeviceInfoScreen(
            component = PreviewDeviceInfoComponent(
                initialState = DeviceInfoState(
                    activeDeviceId = "emulator-5554",
                    sections = previewSections,
                )
            )
        )
    }
}

@Preview
@Composable
private fun DeviceInfoDarkPreview() {
    AdbDeckTheme(isDarkTheme = true) {
        DeviceInfoScreen(
            component = PreviewDeviceInfoComponent(
                initialState = DeviceInfoState(
                    activeDeviceId = "emulator-5554",
                    sections = previewSections,
                )
            )
        )
    }
}

@Preview
@Composable
private fun DeviceInfoNoDevicePreview() {
    AdbDeckTheme(isDarkTheme = false) {
        DeviceInfoScreen(
            component = PreviewDeviceInfoComponent(
                initialState = DeviceInfoState(activeDeviceId = null),
            )
        )
    }
}

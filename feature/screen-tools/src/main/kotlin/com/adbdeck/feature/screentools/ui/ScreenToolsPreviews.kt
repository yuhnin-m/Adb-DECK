package com.adbdeck.feature.screentools.ui

import adbdeck.feature.screen_tools.generated.resources.Res
import adbdeck.feature.screen_tools.generated.resources.screen_tools_choose_active_device
import adbdeck.feature.screen_tools.generated.resources.screen_tools_device_active
import adbdeck.feature.screen_tools.generated.resources.screen_tools_device_not_selected
import adbdeck.feature.screen_tools.generated.resources.screen_tools_status_recording_active
import adbdeck.feature.screen_tools.generated.resources.screen_tools_status_screenshot_creating
import adbdeck.feature.screen_tools.generated.resources.screen_tools_status_screenshot_saved
import androidx.compose.runtime.Composable
import com.adbdeck.core.designsystem.AdbDeckTheme
import com.adbdeck.feature.screentools.RecordingPhase
import com.adbdeck.feature.screentools.ScreenToolsComponent
import com.adbdeck.feature.screentools.ScreenToolsState
import com.adbdeck.feature.screentools.ScreenToolsStatus
import com.adbdeck.feature.screentools.ScreenToolsTab
import com.adbdeck.feature.screentools.ScreenshotQualityPreset
import com.adbdeck.feature.screentools.ScreenshotSectionState
import com.adbdeck.feature.screentools.ScreenrecordQualityPreset
import com.adbdeck.feature.screentools.ScreenrecordSectionState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

/**
 * Preview-реализация компонента Screen Tools.
 */
private class PreviewScreenToolsComponent(
    override val state: StateFlow<ScreenToolsState>,
) : ScreenToolsComponent {
    override fun onSelectTab(tab: ScreenToolsTab) = Unit
    override fun onScreenshotOutputDirectoryChanged(path: String) = Unit
    override fun onPickScreenshotOutputDirectory() = Unit
    override fun onScreenshotQualityChanged(quality: ScreenshotQualityPreset) = Unit
    override fun onScreenrecordOutputDirectoryChanged(path: String) = Unit
    override fun onPickScreenrecordOutputDirectory() = Unit
    override fun onScreenrecordQualityChanged(quality: ScreenrecordQualityPreset) = Unit
    override fun onTakeScreenshot() = Unit
    override fun onCopyLastScreenshotToClipboard() = Unit
    override fun onOpenLastScreenshotFile() = Unit
    override fun onOpenScreenshotFolder() = Unit
    override fun onStartRecording() = Unit
    override fun onStopRecording() = Unit
    override fun onOpenLastVideoFile() = Unit
    override fun onOpenVideoFolder() = Unit
    override fun onDismissFeedback() = Unit
}

@Preview
@Composable
private fun ScreenToolsPreviewLightScreenshot() {
    AdbDeckTheme(isDarkTheme = false) {
        ScreenToolsScreen(
            component = PreviewScreenToolsComponent(
                state = MutableStateFlow(
                    ScreenToolsState(
                        selectedTab = ScreenToolsTab.SCREENSHOT,
                        activeDeviceId = "emulator-5554",
                        deviceMessage = stringResource(
                            Res.string.screen_tools_device_active,
                            "emulator-5554",
                        ),
                        screenshot = ScreenshotSectionState(
                            outputDirectory = "/Users/demo/Pictures/ADBDeck",
                            status = ScreenToolsStatus(
                                stringResource(
                                    Res.string.screen_tools_status_screenshot_saved,
                                    "screenshot_2026-03-04_10-12-55.png",
                                )
                            ),
                            lastFilePath = "/Users/demo/Pictures/ADBDeck/screenshot_2026-03-04_10-12-55.png",
                        ),
                        screenrecord = ScreenrecordSectionState(
                            outputDirectory = "/Users/demo/Videos/ADBDeck",
                        ),
                    )
                )
            )
        )
    }
}

@Preview
@Composable
private fun ScreenToolsPreviewDarkScreenshot() {
    AdbDeckTheme(isDarkTheme = true) {
        ScreenToolsScreen(
            component = PreviewScreenToolsComponent(
                state = MutableStateFlow(
                    ScreenToolsState(
                        selectedTab = ScreenToolsTab.SCREENSHOT,
                        activeDeviceId = "emulator-5554",
                        deviceMessage = stringResource(
                            Res.string.screen_tools_device_active,
                            "emulator-5554",
                        ),
                        screenshot = ScreenshotSectionState(
                            outputDirectory = "/Users/demo/Pictures/ADBDeck",
                            isCapturing = true,
                            status = ScreenToolsStatus(
                                stringResource(Res.string.screen_tools_status_screenshot_creating)
                            ),
                        ),
                        screenrecord = ScreenrecordSectionState(
                            outputDirectory = "/Users/demo/Videos/ADBDeck",
                        ),
                    )
                )
            )
        )
    }
}

@Preview
@Composable
private fun ScreenToolsPreviewLightScreenrecord() {
    AdbDeckTheme(isDarkTheme = false) {
        ScreenToolsScreen(
            component = PreviewScreenToolsComponent(
                state = MutableStateFlow(
                    ScreenToolsState(
                        selectedTab = ScreenToolsTab.SCREENRECORD,
                        activeDeviceId = "R58N123ABC",
                        deviceMessage = stringResource(
                            Res.string.screen_tools_device_active,
                            "R58N123ABC",
                        ),
                        screenshot = ScreenshotSectionState(
                            outputDirectory = "/Users/demo/Pictures/ADBDeck",
                        ),
                        screenrecord = ScreenrecordSectionState(
                            outputDirectory = "/Users/demo/Videos/ADBDeck",
                            phase = RecordingPhase.RECORDING,
                            elapsedSeconds = 125,
                            status = ScreenToolsStatus(
                                stringResource(Res.string.screen_tools_status_recording_active)
                            ),
                        ),
                    )
                )
            )
        )
    }
}

@Preview
@Composable
private fun ScreenToolsPreviewDarkNoDevice() {
    AdbDeckTheme(isDarkTheme = true) {
        ScreenToolsScreen(
            component = PreviewScreenToolsComponent(
                state = MutableStateFlow(
                    ScreenToolsState(
                        selectedTab = ScreenToolsTab.SCREENRECORD,
                        activeDeviceId = null,
                        deviceMessage = stringResource(Res.string.screen_tools_device_not_selected),
                        screenshot = ScreenshotSectionState(
                            outputDirectory = "/Users/demo/Pictures/ADBDeck",
                            status = ScreenToolsStatus(
                                stringResource(Res.string.screen_tools_choose_active_device),
                                isError = true,
                            ),
                        ),
                        screenrecord = ScreenrecordSectionState(
                            outputDirectory = "/Users/demo/Videos/ADBDeck",
                            status = ScreenToolsStatus(
                                stringResource(Res.string.screen_tools_choose_active_device),
                                isError = true,
                            ),
                        ),
                    )
                )
            )
        )
    }
}

package com.adbdeck.feature.screentools.ui

import adbdeck.feature.screen_tools.generated.resources.Res
import adbdeck.feature.screen_tools.generated.resources.screen_tools_tab_screenrecord
import adbdeck.feature.screen_tools.generated.resources.screen_tools_tab_screenshot
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.adbdeck.core.designsystem.Dimensions
import com.adbdeck.core.ui.AdbBanner
import com.adbdeck.core.ui.AdbBannerDismissStyle
import com.adbdeck.core.ui.AdbBannerType
import com.adbdeck.core.ui.segmentedbuttons.AdbSegmentedButtonSize
import com.adbdeck.core.ui.segmentedbuttons.AdbSegmentedOption
import com.adbdeck.core.ui.segmentedbuttons.AdbSingleSegmentedButtons
import com.adbdeck.feature.screentools.RecordingPhase
import com.adbdeck.feature.screentools.ScreenToolsComponent
import com.adbdeck.feature.screentools.ScreenToolsTab
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.stringResource

/**
 * Корневой экран feature Screen Tools.
 */
@Composable
fun ScreenToolsScreen(component: ScreenToolsComponent) {
    val state by component.state.collectAsState()

    // Прогреваем JavaFX заранее, чтобы не ловить фриз при первом открытии вкладки записи.
    LaunchedEffect(Unit) {
        withContext(Dispatchers.Default) {
            JavaFxBootstrap.prewarm()
        }
    }

    val screenshotTabLabel = stringResource(Res.string.screen_tools_tab_screenshot)
    val screenrecordTabLabel = stringResource(Res.string.screen_tools_tab_screenrecord)
    val isScreenrecordBusy by remember(state.screenrecord.phase, state.screenrecord.activeSession) {
        derivedStateOf {
            state.screenrecord.activeSession != null ||
                state.screenrecord.phase != RecordingPhase.IDLE
        }
    }
    val tabOptions = remember(screenshotTabLabel, screenrecordTabLabel, isScreenrecordBusy) {
        listOf(
            AdbSegmentedOption(
                value = ScreenToolsTab.SCREENSHOT,
                label = screenshotTabLabel,
                enabled = !isScreenrecordBusy,
            ),
            AdbSegmentedOption(
                value = ScreenToolsTab.SCREENRECORD,
                label = screenrecordTabLabel,
            ),
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = Dimensions.paddingMedium,
                    vertical = Dimensions.paddingSmall,
                ),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AdbSingleSegmentedButtons(
                options = tabOptions,
                selectedValue = state.selectedTab,
                onValueSelected = component::onSelectTab,
                size = AdbSegmentedButtonSize.MEDIUM,
            )
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) {
            when (state.selectedTab) {
                ScreenToolsTab.SCREENSHOT -> ScreenshotTabContent(
                    state = state.screenshot,
                    isDeviceReady = state.isDeviceReady,
                    isScreenrecordBusy = isScreenrecordBusy,
                    onPickOutputDirectory = component::onPickScreenshotOutputDirectory,
                    onQualityChanged = component::onScreenshotQualityChanged,
                    onTakeScreenshot = component::onTakeScreenshot,
                    onCopyToClipboard = component::onCopyLastScreenshotToClipboard,
                    onOpenFile = component::onOpenLastScreenshotFile,
                    onOpenFolder = component::onOpenScreenshotFolder,
                )

                ScreenToolsTab.SCREENRECORD -> ScreenrecordTabContent(
                    state = state.screenrecord,
                    isDeviceReady = state.isDeviceReady,
                    onPickOutputDirectory = component::onPickScreenrecordOutputDirectory,
                    onQualityChanged = component::onScreenrecordQualityChanged,
                    onStartRecording = component::onStartRecording,
                    onStopRecording = component::onStopRecording,
                    onOpenFile = component::onOpenLastVideoFile,
                    onOpenFolder = component::onOpenVideoFolder,
                )
            }
        }

        state.feedback?.let { feedback ->
            HorizontalDivider()
            AdbBanner(
                message = feedback.message,
                type = if (feedback.isError) AdbBannerType.ERROR else AdbBannerType.SUCCESS,
                onDismiss = component::onDismissFeedback,
                dismissStyle = AdbBannerDismissStyle.TEXT,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = Dimensions.paddingMedium,
                        vertical = Dimensions.paddingSmall,
                    ),
            )
        }
    }
}

package com.adbdeck.feature.screentools.ui

import adbdeck.feature.screen_tools.generated.resources.Res
import adbdeck.feature.screen_tools.generated.resources.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.MovieCreation
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.adbdeck.core.designsystem.AdbTheme
import com.adbdeck.core.designsystem.Dimensions
import com.adbdeck.core.ui.buttons.AdbButtonSize
import com.adbdeck.core.ui.buttons.AdbButtonType
import com.adbdeck.core.ui.buttons.AdbFilledButton
import com.adbdeck.core.ui.buttons.AdbOutlinedButton
import com.adbdeck.feature.screentools.ScreenrecordQualityPreset
import com.adbdeck.feature.screentools.ScreenrecordSectionState
import org.jetbrains.compose.resources.stringResource

/**
 * Контент вкладки screenrecord.
 */
@Composable
internal fun ScreenrecordTabContent(
    state: ScreenrecordSectionState,
    isDeviceReady: Boolean,
    onPickOutputDirectory: () -> Unit,
    onQualityChanged: (ScreenrecordQualityPreset) -> Unit,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit,
    onOpenFile: () -> Unit,
    onOpenFolder: () -> Unit,
) {
    val canOpenVideo = rememberHostFileExists(state.lastFilePath)
    val qualityOptions = rememberScreenrecordQualityOptions()

    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(Dimensions.paddingMedium),
    ) {
        Column(
            modifier = Modifier
                .width(ScreenToolsControlsPanelWidth)
                .fillMaxHeight()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(Dimensions.paddingMedium),
        ) {
            OutputDirectorySection(
                title = stringResource(Res.string.screen_tools_output_screenrecord_title),
                directory = state.outputDirectory,
                pathLabel = stringResource(Res.string.screen_tools_field_path_label),
                chooseLabel = stringResource(Res.string.screen_tools_action_choose_directory),
                openFolderLabel = stringResource(Res.string.screen_tools_action_open_folder),
                onChooseDirectory = onPickOutputDirectory,
                onOpenFolder = onOpenFolder,
            )

            QualitySection(
                title = stringResource(Res.string.screen_tools_quality_screenrecord_title),
                selectedValue = state.quality,
                options = qualityOptions,
                onSelect = onQualityChanged,
            )

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(Dimensions.paddingSmall),
            ) {
                AdbFilledButton(
                    onClick = onStartRecording,
                    enabled = isDeviceReady && !state.isRecording && !state.isStarting && !state.isStopping,
                    text = stringResource(Res.string.screen_tools_action_start_recording),
                    leadingIcon = Icons.Outlined.MovieCreation,
                    modifier = Modifier.fillMaxWidth(),
                    fullWidth = true,
                    size = AdbButtonSize.MEDIUM,
                    type = AdbButtonType.NEUTRAL,
                )

                AdbOutlinedButton(
                    onClick = onStopRecording,
                    enabled = state.isRecording && !state.isStopping,
                    text = stringResource(Res.string.screen_tools_action_stop_recording),
                    leadingIcon = Icons.Outlined.Stop,
                    modifier = Modifier.fillMaxWidth(),
                    fullWidth = true,
                    size = AdbButtonSize.MEDIUM,
                    type = AdbButtonType.DANGER,
                )

                AdbOutlinedButton(
                    onClick = onOpenFile,
                    enabled = canOpenVideo,
                    text = stringResource(Res.string.screen_tools_action_open_file),
                    leadingIcon = Icons.Outlined.PlayArrow,
                    modifier = Modifier.fillMaxWidth(),
                    fullWidth = true,
                    size = AdbButtonSize.MEDIUM,
                    type = AdbButtonType.NEUTRAL,
                )

                AdbOutlinedButton(
                    onClick = onOpenFolder,
                    text = stringResource(Res.string.screen_tools_action_open_folder),
                    leadingIcon = Icons.Outlined.FolderOpen,
                    modifier = Modifier.fillMaxWidth(),
                    fullWidth = true,
                    size = AdbButtonSize.MEDIUM,
                    type = AdbButtonType.NEUTRAL,
                )
            }

            if (state.isRecording || state.isStopping) {
                Text(
                    text = stringResource(
                        Res.string.screen_tools_label_recording_time,
                        formatElapsed(state.elapsedSeconds),
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = AdbTheme.colorScheme.primary,
                )
            }

            StatusSection(
                title = stringResource(Res.string.screen_tools_status_screenrecord_title),
                status = state.status,
                fallbackMessage = stringResource(Res.string.screen_tools_status_screenrecord_idle),
                isRunning = state.isStarting || state.isRecording || state.isStopping,
            )

            state.lastFilePath?.let { path ->
                LastFilePathLabel(
                    pathText = stringResource(Res.string.screen_tools_label_last_file, path),
                )
            }
        }

        Spacer(Modifier.width(Dimensions.paddingMedium))

        EmbeddedVideoPlayer(
            videoPath = state.lastFilePath,
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            showPlaybackControls = true,
            showStatus = true,
        )
    }
}

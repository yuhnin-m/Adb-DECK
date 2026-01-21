package com.adbdeck.feature.screentools.ui

import adbdeck.feature.screen_tools.generated.resources.Res
import adbdeck.feature.screen_tools.generated.resources.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import com.adbdeck.core.designsystem.AdbTheme
import com.adbdeck.core.designsystem.Dimensions
import com.adbdeck.core.ui.EmptyView
import com.adbdeck.core.ui.buttons.AdbButtonSize
import com.adbdeck.core.ui.buttons.AdbButtonType
import com.adbdeck.core.ui.buttons.AdbFilledButton
import com.adbdeck.core.ui.buttons.AdbOutlinedButton
import com.adbdeck.feature.screentools.ScreenshotQualityPreset
import com.adbdeck.feature.screentools.ScreenshotSectionState
import org.jetbrains.compose.resources.stringResource

/**
 * Контент вкладки screenshot.
 */
@Composable
internal fun ScreenshotTabContent(
    state: ScreenshotSectionState,
    isDeviceReady: Boolean,
    isScreenrecordBusy: Boolean,
    onPickOutputDirectory: () -> Unit,
    onQualityChanged: (ScreenshotQualityPreset) -> Unit,
    onTakeScreenshot: () -> Unit,
    onCopyToClipboard: () -> Unit,
    onOpenFile: () -> Unit,
    onOpenFolder: () -> Unit,
) {
    val previewBitmap = rememberScreenshotBitmap(state.lastFilePath)
    val canUseResult = rememberHostFileExists(state.lastFilePath)
    val qualityOptions = rememberScreenshotQualityOptions()

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
                title = stringResource(Res.string.screen_tools_output_screenshot_title),
                directory = state.outputDirectory,
                pathLabel = stringResource(Res.string.screen_tools_field_path_label),
                chooseLabel = stringResource(Res.string.screen_tools_action_choose_directory),
                openFolderLabel = stringResource(Res.string.screen_tools_action_open_folder),
                onChooseDirectory = onPickOutputDirectory,
                onOpenFolder = onOpenFolder,
            )

            QualitySection(
                title = stringResource(Res.string.screen_tools_quality_screenshot_title),
                selectedValue = state.quality,
                options = qualityOptions,
                onSelect = onQualityChanged,
            )

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(Dimensions.paddingSmall),
            ) {
                AdbFilledButton(
                    onClick = onTakeScreenshot,
                    enabled = isDeviceReady && !state.isCapturing && !isScreenrecordBusy,
                    text = stringResource(Res.string.screen_tools_action_take_screenshot),
                    leadingIcon = Icons.Outlined.CameraAlt,
                    modifier = Modifier.fillMaxWidth(),
                    fullWidth = true,
                    size = AdbButtonSize.MEDIUM,
                    type = AdbButtonType.NEUTRAL,
                )

                AdbOutlinedButton(
                    onClick = onCopyToClipboard,
                    enabled = canUseResult,
                    text = stringResource(Res.string.screen_tools_action_copy_clipboard),
                    leadingIcon = Icons.Outlined.ContentCopy,
                    modifier = Modifier.fillMaxWidth(),
                    fullWidth = true,
                    size = AdbButtonSize.MEDIUM,
                    type = AdbButtonType.NEUTRAL,
                )

                AdbOutlinedButton(
                    onClick = onOpenFile,
                    enabled = canUseResult,
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

            StatusSection(
                title = stringResource(Res.string.screen_tools_status_screenshot_title),
                status = state.status,
                fallbackMessage = stringResource(Res.string.screen_tools_status_screenshot_ready),
                isRunning = state.isCapturing,
            )

            state.lastFilePath?.let { path ->
                LastFilePathLabel(
                    pathText = stringResource(Res.string.screen_tools_label_last_file, path),
                )
            }
        }

        Spacer(Modifier.width(Dimensions.paddingMedium))

        Surface(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            color = AdbTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
            shape = MaterialTheme.shapes.small,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(Dimensions.paddingSmall),
                contentAlignment = Alignment.Center,
            ) {
                if (previewBitmap != null) {
                    Image(
                        bitmap = previewBitmap,
                        contentDescription = stringResource(Res.string.screen_tools_preview_last_screenshot_cd),
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit,
                    )
                } else {
                    EmptyView(
                        message = stringResource(Res.string.screen_tools_preview_empty_message),
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
    }
}

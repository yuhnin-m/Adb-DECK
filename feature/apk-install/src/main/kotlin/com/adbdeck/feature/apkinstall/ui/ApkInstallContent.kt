package com.adbdeck.feature.apkinstall.ui

import adbdeck.feature.apk_install.generated.resources.Res
import adbdeck.feature.apk_install.generated.resources.apk_install_action_choose_apk
import adbdeck.feature.apk_install.generated.resources.apk_install_action_clear_log
import adbdeck.feature.apk_install.generated.resources.apk_install_action_copy_result
import adbdeck.feature.apk_install.generated.resources.apk_install_action_install
import adbdeck.feature.apk_install.generated.resources.apk_install_drop_disabled
import adbdeck.feature.apk_install.generated.resources.apk_install_drop_enabled
import adbdeck.feature.apk_install.generated.resources.apk_install_empty_log
import adbdeck.feature.apk_install.generated.resources.apk_install_field_apk_path_placeholder
import adbdeck.feature.apk_install.generated.resources.apk_install_label_last_installed
import adbdeck.feature.apk_install.generated.resources.apk_install_section_drop_title
import adbdeck.feature.apk_install.generated.resources.apk_install_section_file_title
import adbdeck.feature.apk_install.generated.resources.apk_install_section_log_title
import adbdeck.feature.apk_install.generated.resources.apk_install_section_status_title
import adbdeck.feature.apk_install.generated.resources.apk_install_toggle_allow_test_only
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.adbdeck.core.designsystem.AdbTheme
import com.adbdeck.core.designsystem.Dimensions
import com.adbdeck.core.ui.EmptyView
import com.adbdeck.core.ui.buttons.AdbButtonSize
import com.adbdeck.core.ui.buttons.AdbButtonType
import com.adbdeck.core.ui.buttons.AdbFilledButton
import com.adbdeck.core.ui.buttons.AdbOutlinedButton
import com.adbdeck.core.ui.buttons.AdbPlainButton
import com.adbdeck.core.ui.sectioncards.AdbSectionCard
import com.adbdeck.core.ui.textfields.AdbOutlinedTextField
import com.adbdeck.core.ui.textfields.AdbTextFieldSize
import com.adbdeck.feature.apkinstall.ApkInstallState
import org.jetbrains.compose.resources.stringResource

private val ApkInstallSidePanelWidth = 420.dp

/**
 * Контент экрана APK Install (без верхнего/нижнего баннера).
 */
@Composable
internal fun ApkInstallContent(
    state: ApkInstallState,
    onApkPathChanged: (String) -> Unit,
    onPickApkFile: () -> Unit,
    onApkPathDropped: (String) -> Unit,
    onAllowTestOnlyChanged: (Boolean) -> Unit,
    onInstallApk: () -> Unit,
    onCopyStatusResult: () -> Unit,
    onClearLog: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val canEditPath = !state.isInstalling
    val canInstall = state.isDeviceReady && !state.isInstalling && state.apkPath.isNotBlank()
    val canClearLog = state.logLines.isNotEmpty() && !state.isInstalling

    Row(
        modifier = modifier
            .fillMaxSize()
            .padding(Dimensions.paddingMedium),
    ) {
        Column(
            modifier = Modifier
                .width(ApkInstallSidePanelWidth)
                .fillMaxHeight()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(Dimensions.paddingMedium),
        ) {
            AdbSectionCard(
                title = stringResource(Res.string.apk_install_section_file_title),
                titleTextStyle = MaterialTheme.typography.titleMedium,
            ) {
                AdbOutlinedTextField(
                    value = state.apkPath,
                    onValueChange = onApkPathChanged,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = stringResource(Res.string.apk_install_field_apk_path_placeholder),
                    enabled = canEditPath,
                    singleLine = true,
                    size = AdbTextFieldSize.MEDIUM,
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(Res.string.apk_install_toggle_allow_test_only),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
                        Switch(
                            checked = state.allowTestOnlyInstall,
                            onCheckedChange = onAllowTestOnlyChanged,
                            enabled = canEditPath,
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Dimensions.paddingSmall),
                ) {
                    AdbOutlinedButton(
                        onClick = onPickApkFile,
                        enabled = canEditPath,
                        text = stringResource(Res.string.apk_install_action_choose_apk),
                        modifier = Modifier.weight(1f),
                        fullWidth = true,
                        size = AdbButtonSize.MEDIUM,
                        type = AdbButtonType.NEUTRAL,
                    )
                    AdbFilledButton(
                        onClick = onInstallApk,
                        enabled = canInstall,
                        loading = state.isInstalling,
                        text = stringResource(Res.string.apk_install_action_install),
                        modifier = Modifier.weight(1f),
                        fullWidth = true,
                        size = AdbButtonSize.MEDIUM,
                        type = AdbButtonType.SUCCESS,
                    )
                }
            }

            AdbSectionCard(
                title = stringResource(Res.string.apk_install_section_drop_title),
                titleTextStyle = MaterialTheme.typography.titleMedium,
            ) {
                ApkInstallDropZone(
                    enabled = state.isDeviceReady && !state.isInstalling,
                    enabledText = stringResource(Res.string.apk_install_drop_enabled),
                    disabledText = stringResource(Res.string.apk_install_drop_disabled),
                    onApkDropped = onApkPathDropped,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            ApkInstallStatusCard(
                state = state,
                onCopyStatusResult = onCopyStatusResult,
                modifier = Modifier.fillMaxWidth(),
            )

            state.lastInstalledApkPath?.let { path ->
                val baseStyle = MaterialTheme.typography.bodySmall
                val monoStyle = remember(baseStyle) {
                    baseStyle.copy(fontFamily = FontFamily.Monospace)
                }
                Text(
                    text = stringResource(Res.string.apk_install_label_last_installed, path),
                    style = monoStyle,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        Spacer(Modifier.width(Dimensions.paddingMedium))

        AdbSectionCard(
            title = stringResource(Res.string.apk_install_section_log_title),
            titleTextStyle = MaterialTheme.typography.titleMedium,
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            headerTrailing = {
                AdbPlainButton(
                    onClick = onClearLog,
                    enabled = canClearLog,
                    text = stringResource(Res.string.apk_install_action_clear_log),
                    size = AdbButtonSize.MEDIUM,
                    type = AdbButtonType.NEUTRAL,
                )
            },
        ) {
            if (state.logLines.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 220.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    EmptyView(
                        message = stringResource(Res.string.apk_install_empty_log),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            } else {
                val baseStyle = MaterialTheme.typography.bodySmall
                val monoStyle = remember(baseStyle) {
                    baseStyle.copy(fontFamily = FontFamily.Monospace)
                }
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = true),
                    verticalArrangement = Arrangement.spacedBy(Dimensions.paddingXSmall),
                ) {
                    itemsIndexed(
                        items = state.logLines,
                        key = { index, line -> "${index}_${line.hashCode()}" },
                    ) { _, line ->
                        Text(
                            text = line,
                            style = monoStyle,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ApkInstallStatusCard(
    state: ApkInstallState,
    onCopyStatusResult: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val status = state.status
    val statusTextColor = if (status.isError) {
        AdbTheme.colorScheme.error
    } else {
        AdbTheme.colorScheme.onSurface
    }

    AdbSectionCard(
        title = stringResource(Res.string.apk_install_section_status_title),
        titleTextStyle = MaterialTheme.typography.titleMedium,
        modifier = modifier,
        headerTrailing = {
            AdbPlainButton(
                onClick = onCopyStatusResult,
                enabled = status.message.isNotBlank(),
                text = stringResource(Res.string.apk_install_action_copy_result),
                size = AdbButtonSize.MEDIUM,
                type = AdbButtonType.NEUTRAL,
            )
        },
    ) {
        Text(
            text = status.message,
            style = MaterialTheme.typography.bodyMedium,
            color = statusTextColor,
        )

        when {
            status.progress != null -> {
                LinearProgressIndicator(
                    progress = { status.progress.coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            state.isInstalling -> {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

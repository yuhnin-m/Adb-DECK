package com.adbdeck.feature.settings.ui

import adbdeck.feature.settings.generated.resources.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.adbdeck.core.designsystem.AdbCornerRadius
import com.adbdeck.core.designsystem.Dimensions
import com.adbdeck.core.settings.AppLanguage
import com.adbdeck.core.settings.AppTheme
import com.adbdeck.core.ui.AdbBanner
import com.adbdeck.core.ui.AdbBannerType
import com.adbdeck.core.ui.buttons.AdbButtonSize
import com.adbdeck.core.ui.buttons.AdbFilledButton
import com.adbdeck.core.ui.buttons.AdbOutlinedButton
import com.adbdeck.core.ui.filedialogs.showOpenFileDialog
import com.adbdeck.core.ui.sectioncards.AdbSectionCard
import com.adbdeck.core.ui.segmentedbuttons.AdbSegmentedButtonSize
import com.adbdeck.core.ui.segmentedbuttons.AdbSegmentedOption
import com.adbdeck.core.ui.segmentedbuttons.AdbSingleSegmentedButtons
import com.adbdeck.core.ui.settings.AdbIntStepper
import com.adbdeck.core.ui.settings.AdbSettingRow
import com.adbdeck.core.ui.textfields.AdbOutlinedTextField
import com.adbdeck.core.ui.textfields.AdbTextFieldSize
import com.adbdeck.feature.settings.SettingsComponent
import com.adbdeck.feature.settings.SettingsUiState
import com.adbdeck.feature.settings.ToolCheckState
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import org.jetbrains.compose.resources.stringResource

/**
 * Экран настроек приложения.
 */
@Composable
fun SettingsScreen(component: SettingsComponent) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(Dimensions.paddingLarge),
            verticalArrangement = Arrangement.spacedBy(Dimensions.paddingMedium),
        ) {
            Text(
                text = stringResource(Res.string.settings_title),
                style = MaterialTheme.typography.headlineMedium,
            )

            SaveFeedbackHost(component = component)
            ToolsSectionHost(component = component)
            ThemeSectionHost(component = component)
            LogcatSectionHost(component = component)
            SaveActionHost(component = component)
        }
    }
}

private data class ToolsSectionUiState(
    val adbPath: String,
    val bundletoolPath: String,
    val scrcpyPath: String,
    val adbCheckState: ToolCheckState,
    val bundletoolCheckState: ToolCheckState,
    val scrcpyCheckState: ToolCheckState,
)

private data class ThemeSectionUiState(
    val currentTheme: AppTheme,
    val currentLanguage: AppLanguage,
)

private data class LogcatSectionUiState(
    val logcatCompactMode: Boolean,
    val logcatShowDate: Boolean,
    val logcatShowTime: Boolean,
    val logcatShowMillis: Boolean,
    val logcatColoredLevels: Boolean,
    val logcatMaxBufferedLines: Int,
    val logcatAutoScroll: Boolean,
    val logcatFontFamily: String,
    val logcatFontSizeSp: Int,
)

private data class SaveActionUiState(
    val isSaving: Boolean,
    val hasPendingChanges: Boolean,
)

@Composable
private fun SaveFeedbackHost(component: SettingsComponent) {
    val initialFeedback = remember(component) { component.state.value.saveFeedback }
    val feedback by remember(component) {
        component.state
            .map { it.saveFeedback }
            .distinctUntilChanged()
    }.collectAsState(initial = initialFeedback)

    feedback?.let { banner ->
        AdbBanner(
            message = banner.message,
            type = if (banner.isError) AdbBannerType.ERROR else AdbBannerType.SUCCESS,
            onDismiss = component::onDismissFeedback,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun ToolsSectionHost(component: SettingsComponent) {
    val initialUi = remember(component) {
        val initial = component.state.value
        ToolsSectionUiState(
            adbPath = initial.adbPath,
            bundletoolPath = initial.bundletoolPath,
            scrcpyPath = initial.scrcpyPath,
            adbCheckState = initial.adbCheckState,
            bundletoolCheckState = initial.bundletoolCheckState,
            scrcpyCheckState = initial.scrcpyCheckState,
        )
    }
    val uiState by remember(component) {
        component.state
            .map {
                ToolsSectionUiState(
                    adbPath = it.adbPath,
                    bundletoolPath = it.bundletoolPath,
                    scrcpyPath = it.scrcpyPath,
                    adbCheckState = it.adbCheckState,
                    bundletoolCheckState = it.bundletoolCheckState,
                    scrcpyCheckState = it.scrcpyCheckState,
                )
            }
            .distinctUntilChanged()
    }.collectAsState(initial = initialUi)

    ToolsSection(
        state = uiState,
        onAdbPathChanged = component::onAdbPathChanged,
        onCheckAdb = component::onCheckAdb,
        onBundletoolPathChanged = component::onBundletoolPathChanged,
        onCheckBundletool = component::onCheckBundletool,
        onScrcpyPathChanged = component::onScrcpyPathChanged,
        onCheckScrcpy = component::onCheckScrcpy,
    )
}

@Composable
private fun ThemeSectionHost(component: SettingsComponent) {
    val initialUi = remember(component) {
        ThemeSectionUiState(
            currentTheme = component.state.value.currentTheme,
            currentLanguage = component.state.value.currentLanguage,
        )
    }
    val uiState by remember(component) {
        component.state
            .map {
                ThemeSectionUiState(
                    currentTheme = it.currentTheme,
                    currentLanguage = it.currentLanguage,
                )
            }
            .distinctUntilChanged()
    }.collectAsState(initial = initialUi)

    ThemeSection(
        state = uiState,
        onThemeChanged = component::onThemeChanged,
        onLanguageChanged = component::onLanguageChanged,
    )
}

@Composable
private fun LogcatSectionHost(component: SettingsComponent) {
    val initialUi = remember(component) {
        component.state.value.toLogcatSectionUiState()
    }
    val uiState by remember(component) {
        component.state
            .map { it.toLogcatSectionUiState() }
            .distinctUntilChanged()
    }.collectAsState(initial = initialUi)

    LogcatSection(
        state = uiState,
        onCompactModeChanged = component::onLogcatCompactModeChanged,
        onShowDateChanged = component::onLogcatShowDateChanged,
        onShowTimeChanged = component::onLogcatShowTimeChanged,
        onShowMillisChanged = component::onLogcatShowMillisChanged,
        onColoredLevelsChanged = component::onLogcatColoredLevelsChanged,
        onAutoScrollChanged = component::onLogcatAutoScrollChanged,
        onMaxBufferedLinesChanged = component::onLogcatMaxBufferedLinesChanged,
        onFontFamilyChanged = component::onLogcatFontFamilyChanged,
        onFontSizeChanged = component::onLogcatFontSizeChanged,
    )
}

@Composable
private fun SaveActionHost(component: SettingsComponent) {
    val initialUi = remember(component) {
        SaveActionUiState(
            isSaving = component.state.value.isSaving,
            hasPendingChanges = component.state.value.hasPendingChanges,
        )
    }
    val uiState by remember(component) {
        component.state
            .map {
                SaveActionUiState(
                    isSaving = it.isSaving,
                    hasPendingChanges = it.hasPendingChanges,
                )
            }
            .distinctUntilChanged()
    }.collectAsState(initial = initialUi)

    SaveActionSection(
        state = uiState,
        onSave = component::onSave,
    )
}

@Composable
private fun ToolsSection(
    state: ToolsSectionUiState,
    onAdbPathChanged: (String) -> Unit,
    onCheckAdb: () -> Unit,
    onBundletoolPathChanged: (String) -> Unit,
    onCheckBundletool: () -> Unit,
    onScrcpyPathChanged: (String) -> Unit,
    onCheckScrcpy: () -> Unit,
) {
    val sectionTitle = stringResource(Res.string.settings_section_tools)
    val adbLabel = stringResource(Res.string.settings_field_adb_label)
    val adbPlaceholder = stringResource(Res.string.settings_field_adb_placeholder)
    val adbHelp = stringResource(Res.string.settings_field_adb_help)
    val checkAdbLabel = stringResource(Res.string.settings_action_check_adb)

    val bundletoolLabel = stringResource(Res.string.settings_field_bundletool_label)
    val bundletoolPlaceholder = stringResource(Res.string.settings_field_bundletool_placeholder)
    val bundletoolHelp = stringResource(Res.string.settings_field_bundletool_help)
    val checkBundletoolLabel = stringResource(Res.string.settings_action_check_bundletool)

    val scrcpyLabel = stringResource(Res.string.settings_field_scrcpy_label)
    val scrcpyPlaceholder = stringResource(Res.string.settings_field_scrcpy_placeholder)
    val scrcpyHelp = stringResource(Res.string.settings_field_scrcpy_help)
    val checkScrcpyLabel = stringResource(Res.string.settings_action_check_scrcpy)
    val choosePathLabel = stringResource(Res.string.settings_action_choose_path)

    val downloadBundletoolLabel = stringResource(Res.string.settings_bundletool_download)
    val bundletoolMacosExample = stringResource(Res.string.settings_bundletool_examples_macos)
    val bundletoolLinuxExample = stringResource(Res.string.settings_bundletool_examples_linux)
    val bundletoolWindowsExample = stringResource(Res.string.settings_bundletool_examples_windows)

    val downloadScrcpyLabel = stringResource(Res.string.settings_scrcpy_download)
    val scrcpyMacosExample = stringResource(Res.string.settings_scrcpy_examples_macos)
    val scrcpyLinuxExample = stringResource(Res.string.settings_scrcpy_examples_linux)
    val scrcpyWindowsExample = stringResource(Res.string.settings_scrcpy_examples_windows)

    val checkingLabel = stringResource(Res.string.settings_action_checking)

    AdbSectionCard(
        title = sectionTitle,
        titleUppercase = true,
    ) {
        Text(
            text = adbLabel,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        AdbOutlinedTextField(
            value = state.adbPath,
            onValueChange = onAdbPathChanged,
            placeholder = adbPlaceholder,
            supportingText = adbHelp,
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = AdbCornerRadius.MEDIUM,
            size = AdbTextFieldSize.MEDIUM,
            trailingIcon = if (state.adbPath.isNotBlank()) Icons.Outlined.Clear else null,
            onTrailingIconClick = if (state.adbPath.isNotBlank()) {
                { onAdbPathChanged("") }
            } else {
                null
            },
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Dimensions.paddingSmall),
        ) {
            AdbOutlinedButton(
                onClick = {
                    showOpenFileDialog(adbLabel)?.let(onAdbPathChanged)
                },
                text = choosePathLabel,
                leadingIcon = Icons.Outlined.FolderOpen,
                size = AdbButtonSize.MEDIUM,
                cornerRadius = AdbCornerRadius.MEDIUM,
            )
            AdbOutlinedButton(
                onClick = onCheckAdb,
                text = if (state.adbCheckState is ToolCheckState.Checking) checkingLabel else checkAdbLabel,
                loading = state.adbCheckState is ToolCheckState.Checking,
                enabled = state.adbCheckState !is ToolCheckState.Checking,
                size = AdbButtonSize.MEDIUM,
                cornerRadius = AdbCornerRadius.MEDIUM,
            )
        }
        ToolCheckStateView(state = state.adbCheckState)

        Spacer(modifier = Modifier.height(Dimensions.paddingSmall))

        Text(
            text = bundletoolLabel,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        AdbOutlinedTextField(
            value = state.bundletoolPath,
            onValueChange = onBundletoolPathChanged,
            placeholder = bundletoolPlaceholder,
            supportingText = bundletoolHelp,
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = AdbCornerRadius.MEDIUM,
            size = AdbTextFieldSize.MEDIUM,
            trailingIcon = if (state.bundletoolPath.isNotBlank()) Icons.Outlined.Clear else null,
            onTrailingIconClick = if (state.bundletoolPath.isNotBlank()) {
                { onBundletoolPathChanged("") }
            } else {
                null
            },
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Dimensions.paddingSmall),
        ) {
            AdbOutlinedButton(
                onClick = {
                    showOpenFileDialog(bundletoolLabel)?.let(onBundletoolPathChanged)
                },
                text = choosePathLabel,
                leadingIcon = Icons.Outlined.FolderOpen,
                size = AdbButtonSize.MEDIUM,
                cornerRadius = AdbCornerRadius.MEDIUM,
            )
            AdbOutlinedButton(
                onClick = onCheckBundletool,
                text = if (state.bundletoolCheckState is ToolCheckState.Checking) checkingLabel else checkBundletoolLabel,
                loading = state.bundletoolCheckState is ToolCheckState.Checking,
                enabled = state.bundletoolCheckState !is ToolCheckState.Checking,
                size = AdbButtonSize.MEDIUM,
                cornerRadius = AdbCornerRadius.MEDIUM,
            )
        }
        ToolCheckStateView(state = state.bundletoolCheckState)

        Text(
            text = downloadBundletoolLabel,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = bundletoolMacosExample,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = bundletoolLinuxExample,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = bundletoolWindowsExample,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(Dimensions.paddingSmall))

        Text(
            text = scrcpyLabel,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        AdbOutlinedTextField(
            value = state.scrcpyPath,
            onValueChange = onScrcpyPathChanged,
            placeholder = scrcpyPlaceholder,
            supportingText = scrcpyHelp,
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = AdbCornerRadius.MEDIUM,
            size = AdbTextFieldSize.MEDIUM,
            trailingIcon = if (state.scrcpyPath.isNotBlank()) Icons.Outlined.Clear else null,
            onTrailingIconClick = if (state.scrcpyPath.isNotBlank()) {
                { onScrcpyPathChanged("") }
            } else {
                null
            },
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Dimensions.paddingSmall),
        ) {
            AdbOutlinedButton(
                onClick = {
                    showOpenFileDialog(scrcpyLabel)?.let(onScrcpyPathChanged)
                },
                text = choosePathLabel,
                leadingIcon = Icons.Outlined.FolderOpen,
                size = AdbButtonSize.MEDIUM,
                cornerRadius = AdbCornerRadius.MEDIUM,
            )
            AdbOutlinedButton(
                onClick = onCheckScrcpy,
                text = if (state.scrcpyCheckState is ToolCheckState.Checking) checkingLabel else checkScrcpyLabel,
                loading = state.scrcpyCheckState is ToolCheckState.Checking,
                enabled = state.scrcpyCheckState !is ToolCheckState.Checking,
                size = AdbButtonSize.MEDIUM,
                cornerRadius = AdbCornerRadius.MEDIUM,
            )
        }
        ToolCheckStateView(state = state.scrcpyCheckState)

        Text(
            text = downloadScrcpyLabel,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = scrcpyMacosExample,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = scrcpyLinuxExample,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = scrcpyWindowsExample,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ThemeSection(
    state: ThemeSectionUiState,
    onThemeChanged: (AppTheme) -> Unit,
    onLanguageChanged: (AppLanguage) -> Unit,
) {
    val sectionTitle = stringResource(Res.string.settings_section_theme)
    val lightLabel = stringResource(Res.string.settings_theme_light)
    val darkLabel = stringResource(Res.string.settings_theme_dark)
    val systemLabel = stringResource(Res.string.settings_theme_system)
    val languageTitle = stringResource(Res.string.settings_language_title)
    val languageSystemLabel = stringResource(Res.string.settings_language_system)
    val languageEnglishLabel = stringResource(Res.string.settings_language_english)
    val languageRussianLabel = stringResource(Res.string.settings_language_russian)

    val options = remember(lightLabel, darkLabel, systemLabel) {
        listOf(
            AdbSegmentedOption(value = AppTheme.LIGHT, label = lightLabel),
            AdbSegmentedOption(value = AppTheme.DARK, label = darkLabel),
            AdbSegmentedOption(value = AppTheme.SYSTEM, label = systemLabel),
        )
    }
    val languageOptions = remember(
        languageSystemLabel,
        languageEnglishLabel,
        languageRussianLabel,
    ) {
        listOf(
            AdbSegmentedOption(value = AppLanguage.SYSTEM, label = languageSystemLabel),
            AdbSegmentedOption(value = AppLanguage.ENGLISH, label = languageEnglishLabel),
            AdbSegmentedOption(value = AppLanguage.RUSSIAN, label = languageRussianLabel),
        )
    }

    AdbSectionCard(
        title = sectionTitle,
        titleUppercase = true,
    ) {
        AdbSingleSegmentedButtons(
            options = options,
            selectedValue = state.currentTheme,
            onValueSelected = onThemeChanged,
            size = AdbSegmentedButtonSize.MEDIUM,
            cornerRadius = AdbCornerRadius.MEDIUM,
        )

        Spacer(modifier = Modifier.height(Dimensions.paddingSmall))

        Text(
            text = languageTitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        AdbSingleSegmentedButtons(
            options = languageOptions,
            selectedValue = state.currentLanguage,
            onValueSelected = onLanguageChanged,
            size = AdbSegmentedButtonSize.MEDIUM,
            cornerRadius = AdbCornerRadius.MEDIUM,
        )
    }
}

@Composable
private fun LogcatSection(
    state: LogcatSectionUiState,
    onCompactModeChanged: (Boolean) -> Unit,
    onShowDateChanged: (Boolean) -> Unit,
    onShowTimeChanged: (Boolean) -> Unit,
    onShowMillisChanged: (Boolean) -> Unit,
    onColoredLevelsChanged: (Boolean) -> Unit,
    onAutoScrollChanged: (Boolean) -> Unit,
    onMaxBufferedLinesChanged: (Int) -> Unit,
    onFontFamilyChanged: (String) -> Unit,
    onFontSizeChanged: (Int) -> Unit,
) {
    val sectionTitle = stringResource(Res.string.settings_section_logcat)
    val sectionHint = stringResource(Res.string.settings_section_logcat_hint)
    val compactModeLabel = stringResource(Res.string.settings_logcat_compact_mode)
    val showDateLabel = stringResource(Res.string.settings_logcat_show_date)
    val showTimeLabel = stringResource(Res.string.settings_logcat_show_time)
    val showMillisLabel = stringResource(Res.string.settings_logcat_show_millis)
    val coloredLevelsLabel = stringResource(Res.string.settings_logcat_colored_levels)
    val autoScrollLabel = stringResource(Res.string.settings_logcat_auto_scroll)
    val bufferTitle = stringResource(Res.string.settings_logcat_buffer_title)
    val bufferHelp = stringResource(Res.string.settings_logcat_buffer_help)
    val fontFamilyTitle = stringResource(Res.string.settings_logcat_font_family_title)
    val fontSizeTitle = stringResource(Res.string.settings_logcat_font_size_title)
    val decreaseLabel = stringResource(Res.string.settings_stepper_decrease)
    val increaseLabel = stringResource(Res.string.settings_stepper_increase)
    val fontSizeValue = stringResource(Res.string.settings_logcat_font_size_value, state.logcatFontSizeSp)

    val monospaceLabel = stringResource(Res.string.settings_logcat_font_family_monospace)
    val sansSerifLabel = stringResource(Res.string.settings_logcat_font_family_sans_serif)
    val serifLabel = stringResource(Res.string.settings_logcat_font_family_serif)
    val systemLabel = stringResource(Res.string.settings_logcat_font_family_system)
    val fontOptions = remember(monospaceLabel, sansSerifLabel, serifLabel, systemLabel) {
        listOf(
            AdbSegmentedOption(value = "MONOSPACE", label = monospaceLabel),
            AdbSegmentedOption(value = "SANS_SERIF", label = sansSerifLabel),
            AdbSegmentedOption(value = "SERIF", label = serifLabel),
            AdbSegmentedOption(value = "DEFAULT", label = systemLabel),
        )
    }

    AdbSectionCard(
        title = sectionTitle,
        titleUppercase = true,
    ) {
        Text(
            text = sectionHint,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        AdbSettingRow(title = compactModeLabel) {
            Switch(
                checked = state.logcatCompactMode,
                onCheckedChange = onCompactModeChanged,
            )
        }
        AdbSettingRow(title = showDateLabel) {
            Switch(
                checked = state.logcatShowDate,
                onCheckedChange = onShowDateChanged,
            )
        }
        AdbSettingRow(title = showTimeLabel) {
            Switch(
                checked = state.logcatShowTime,
                onCheckedChange = onShowTimeChanged,
            )
        }
        AdbSettingRow(title = showMillisLabel) {
            Switch(
                checked = state.logcatShowMillis,
                onCheckedChange = onShowMillisChanged,
            )
        }
        AdbSettingRow(title = coloredLevelsLabel) {
            Switch(
                checked = state.logcatColoredLevels,
                onCheckedChange = onColoredLevelsChanged,
            )
        }
        AdbSettingRow(title = autoScrollLabel) {
            Switch(
                checked = state.logcatAutoScroll,
                onCheckedChange = onAutoScrollChanged,
            )
        }

        AdbSettingRow(
            title = bufferTitle,
            description = bufferHelp,
        ) {
            AdbIntStepper(
                value = state.logcatMaxBufferedLines,
                onValueChange = onMaxBufferedLinesChanged,
                minValue = 100,
                maxValue = 100_000,
                valueLabel = state.logcatMaxBufferedLines.toString(),
                decreaseContentDescription = decreaseLabel,
                increaseContentDescription = increaseLabel,
            )
        }

        Text(
            text = fontFamilyTitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        AdbSingleSegmentedButtons(
            options = fontOptions,
            selectedValue = state.logcatFontFamily,
            onValueSelected = onFontFamilyChanged,
            size = AdbSegmentedButtonSize.XSMALL,
            cornerRadius = AdbCornerRadius.MEDIUM,
        )

        AdbSettingRow(title = fontSizeTitle) {
            AdbIntStepper(
                value = state.logcatFontSizeSp,
                onValueChange = onFontSizeChanged,
                minValue = 8,
                maxValue = 24,
                valueLabel = fontSizeValue,
                decreaseContentDescription = decreaseLabel,
                increaseContentDescription = increaseLabel,
            )
        }
    }
}

@Composable
private fun SaveActionSection(
    state: SaveActionUiState,
    onSave: () -> Unit,
) {
    val saveLabel = stringResource(Res.string.settings_action_save)
    val savingLabel = stringResource(Res.string.settings_action_saving)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
    ) {
        AdbFilledButton(
            onClick = onSave,
            text = if (state.isSaving) savingLabel else saveLabel,
            loading = state.isSaving,
            enabled = state.hasPendingChanges && !state.isSaving,
            size = AdbButtonSize.MEDIUM,
            cornerRadius = AdbCornerRadius.MEDIUM,
        )
    }
}

@Composable
private fun ToolCheckStateView(state: ToolCheckState) {
    when (state) {
        is ToolCheckState.Success -> AdbBanner(
            message = state.message,
            type = AdbBannerType.SUCCESS,
            modifier = Modifier.fillMaxWidth(),
        )

        is ToolCheckState.Error -> AdbBanner(
            message = state.message,
            type = AdbBannerType.ERROR,
            modifier = Modifier.fillMaxWidth(),
        )

        ToolCheckState.Checking,
        ToolCheckState.Idle,
        -> Unit
    }
}

private fun SettingsUiState.toLogcatSectionUiState(): LogcatSectionUiState = LogcatSectionUiState(
    logcatCompactMode = logcatCompactMode,
    logcatShowDate = logcatShowDate,
    logcatShowTime = logcatShowTime,
    logcatShowMillis = logcatShowMillis,
    logcatColoredLevels = logcatColoredLevels,
    logcatMaxBufferedLines = logcatMaxBufferedLines,
    logcatAutoScroll = logcatAutoScroll,
    logcatFontFamily = logcatFontFamily,
    logcatFontSizeSp = logcatFontSizeSp,
)

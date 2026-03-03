package com.adbdeck.feature.settings.ui

import adbdeck.feature.settings.generated.resources.Res
import adbdeck.feature.settings.generated.resources.settings_adb_download
import adbdeck.feature.settings.generated.resources.settings_adb_examples_linux
import adbdeck.feature.settings.generated.resources.settings_adb_examples_macos
import adbdeck.feature.settings.generated.resources.settings_adb_examples_windows
import adbdeck.feature.settings.generated.resources.settings_action_check
import adbdeck.feature.settings.generated.resources.settings_action_checking
import adbdeck.feature.settings.generated.resources.settings_action_choose_path
import adbdeck.feature.settings.generated.resources.settings_action_save
import adbdeck.feature.settings.generated.resources.settings_action_saving
import adbdeck.feature.settings.generated.resources.settings_bundletool_download
import adbdeck.feature.settings.generated.resources.settings_bundletool_examples_linux
import adbdeck.feature.settings.generated.resources.settings_bundletool_examples_macos
import adbdeck.feature.settings.generated.resources.settings_bundletool_examples_windows
import adbdeck.feature.settings.generated.resources.settings_field_adb_help
import adbdeck.feature.settings.generated.resources.settings_field_adb_placeholder
import adbdeck.feature.settings.generated.resources.settings_field_bundletool_help
import adbdeck.feature.settings.generated.resources.settings_field_bundletool_placeholder
import adbdeck.feature.settings.generated.resources.settings_field_scrcpy_help
import adbdeck.feature.settings.generated.resources.settings_field_scrcpy_placeholder
import adbdeck.feature.settings.generated.resources.settings_language_description
import adbdeck.feature.settings.generated.resources.settings_language_english
import adbdeck.feature.settings.generated.resources.settings_language_russian
import adbdeck.feature.settings.generated.resources.settings_language_system
import adbdeck.feature.settings.generated.resources.settings_language_title_bilingual
import adbdeck.feature.settings.generated.resources.settings_scrcpy_download
import adbdeck.feature.settings.generated.resources.settings_scrcpy_examples_linux
import adbdeck.feature.settings.generated.resources.settings_scrcpy_examples_macos
import adbdeck.feature.settings.generated.resources.settings_scrcpy_examples_windows
import adbdeck.feature.settings.generated.resources.settings_section_appearance
import adbdeck.feature.settings.generated.resources.settings_section_external_tools
import adbdeck.feature.settings.generated.resources.settings_section_theme
import adbdeck.feature.settings.generated.resources.settings_theme_dark
import adbdeck.feature.settings.generated.resources.settings_theme_description
import adbdeck.feature.settings.generated.resources.settings_theme_light
import adbdeck.feature.settings.generated.resources.settings_theme_system
import adbdeck.feature.settings.generated.resources.settings_tool_adb_title
import adbdeck.feature.settings.generated.resources.settings_tool_bundletool_title
import adbdeck.feature.settings.generated.resources.settings_tool_scrcpy_title
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.adbdeck.core.designsystem.AdbTheme
import com.adbdeck.core.designsystem.Dimensions
import com.adbdeck.core.settings.AppLanguage
import com.adbdeck.core.settings.AppTheme
import com.adbdeck.core.ui.AdbBanner
import com.adbdeck.core.ui.AdbBannerType
import com.adbdeck.core.ui.buttons.AdbButtonSize
import com.adbdeck.core.ui.buttons.AdbFilledButton
import com.adbdeck.core.ui.buttons.AdbOutlinedButton
import com.adbdeck.core.ui.filedialogs.HostFileSelectionMode
import com.adbdeck.core.ui.filedialogs.OpenFileDialogConfig
import com.adbdeck.core.ui.filedialogs.showOpenFileDialog
import com.adbdeck.core.ui.sectioncards.AdbSectionCard
import com.adbdeck.core.ui.segmentedbuttons.AdbSegmentedButtonSize
import com.adbdeck.core.ui.segmentedbuttons.AdbSegmentedOption
import com.adbdeck.core.ui.segmentedbuttons.AdbSingleSegmentedButtons
import com.adbdeck.core.ui.textfields.AdbDropdownOption
import com.adbdeck.core.ui.textfields.AdbOutlinedDropdownTextField
import com.adbdeck.core.ui.textfields.AdbOutlinedTextField
import com.adbdeck.core.ui.textfields.AdbTextFieldSize
import com.adbdeck.feature.settings.SettingsComponent
import com.adbdeck.feature.settings.ToolCheckState
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import org.jetbrains.compose.resources.stringResource

// ── State snapshots ───────────────────────────────────────────────────────────

private data class AppearanceSectionUiState(
    val currentTheme: AppTheme,
    val currentLanguage: AppLanguage,
)

private data class ToolsSectionUiState(
    val adbPath: String,
    val bundletoolPath: String,
    val scrcpyPath: String,
    val adbCheckState: ToolCheckState,
    val bundletoolCheckState: ToolCheckState,
    val scrcpyCheckState: ToolCheckState,
)

private data class SaveActionUiState(
    val isSaving: Boolean,
    val hasPendingChanges: Boolean,
)

// ── Root screen ───────────────────────────────────────────────────────────────

@Composable
fun SettingsScreen(component: SettingsComponent) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(Dimensions.paddingLarge),
                verticalArrangement = Arrangement.spacedBy(Dimensions.paddingDefault),
            ) {
                AppearanceSectionHost(component = component)
                ToolsSectionHost(component = component)
                SaveActionHost(component = component)
            }

            SaveFeedbackHost(
                component = component,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(Dimensions.paddingDefault),
            )
        }
    }
}

// ── Save feedback ─────────────────────────────────────────────────────────────

@Composable
private fun SaveFeedbackHost(
    component: SettingsComponent,
    modifier: Modifier = Modifier,
) {
    val initialFeedback = remember(component) { component.state.value.saveFeedback }
    val feedback by remember(component) {
        component.state.map { it.saveFeedback }.distinctUntilChanged()
    }.collectAsState(initial = initialFeedback)

    feedback?.let { banner ->
        AdbBanner(
            message = banner.message,
            type = if (banner.isError) AdbBannerType.ERROR else AdbBannerType.SUCCESS,
            onDismiss = component::onDismissFeedback,
            modifier = modifier,
        )
    }
}

// ── Appearance section ────────────────────────────────────────────────────────

@Composable
private fun AppearanceSectionHost(component: SettingsComponent) {
    val initial = remember(component) {
        AppearanceSectionUiState(
            currentTheme = component.state.value.currentTheme,
            currentLanguage = component.state.value.currentLanguage,
        )
    }
    val uiState by remember(component) {
        component.state
            .map { AppearanceSectionUiState(currentTheme = it.currentTheme, currentLanguage = it.currentLanguage) }
            .distinctUntilChanged()
    }.collectAsState(initial = initial)

    AppearanceSection(
        state = uiState,
        onThemeChanged = component::onThemeChanged,
        onLanguageChanged = component::onLanguageChanged,
    )
}

@Composable
private fun AppearanceSection(
    state: AppearanceSectionUiState,
    onThemeChanged: (AppTheme) -> Unit,
    onLanguageChanged: (AppLanguage) -> Unit,
) {
    val sectionTitle = stringResource(Res.string.settings_section_appearance)

    val languageTitle = stringResource(Res.string.settings_language_title_bilingual)
    val languageDesc = stringResource(Res.string.settings_language_description)
    val languageSystem = stringResource(Res.string.settings_language_system)
    val languageEn = stringResource(Res.string.settings_language_english)
    val languageRu = stringResource(Res.string.settings_language_russian)

    val themeTitle = stringResource(Res.string.settings_section_theme)
    val themeDesc = stringResource(Res.string.settings_theme_description)
    val themeSystem = stringResource(Res.string.settings_theme_system)
    val themeLight = stringResource(Res.string.settings_theme_light)
    val themeDark = stringResource(Res.string.settings_theme_dark)

    val languageOptions = remember(languageSystem, languageEn, languageRu) {
        listOf(
            AdbDropdownOption(value = AppLanguage.SYSTEM, label = languageSystem),
            AdbDropdownOption(value = AppLanguage.ENGLISH, label = languageEn),
            AdbDropdownOption(value = AppLanguage.RUSSIAN, label = languageRu),
        )
    }

    val themeOptions = remember(themeSystem, themeLight, themeDark) {
        listOf(
            AdbSegmentedOption(value = AppTheme.SYSTEM, label = themeSystem),
            AdbSegmentedOption(value = AppTheme.LIGHT, label = themeLight),
            AdbSegmentedOption(value = AppTheme.DARK, label = themeDark),
        )
    }

    AdbSectionCard(
        title = sectionTitle,
        titleUppercase = true,
        titleColor = MaterialTheme.colorScheme.onSurfaceVariant,
        titleTextStyle = MaterialTheme.typography.labelSmall,
        shape = MaterialTheme.shapes.medium,
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
        contentPadding = PaddingValues(0.dp),
        contentSpacing = 0.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        SectionSettingRow(
            title = languageTitle,
            description = languageDesc,
            modifier = Modifier.padding(horizontal = Dimensions.paddingDefault, vertical = Dimensions.paddingMedium),
            control = {
                AdbOutlinedDropdownTextField(
                    options = languageOptions,
                    selectedValue = state.currentLanguage,
                    onValueSelected = onLanguageChanged,
                    modifier = Modifier.width(160.dp),
                    size = AdbTextFieldSize.MEDIUM,
                )
            },
        )

        HorizontalDivider()

        SectionSettingRow(
            title = themeTitle,
            description = themeDesc,
            modifier = Modifier.padding(horizontal = Dimensions.paddingDefault, vertical = Dimensions.paddingMedium),
            control = {
                AdbSingleSegmentedButtons(
                    options = themeOptions,
                    selectedValue = state.currentTheme,
                    onValueSelected = onThemeChanged,
                    size = AdbSegmentedButtonSize.MEDIUM,
                )
            },
        )
    }
}

// ── Tools section ─────────────────────────────────────────────────────────────

@Composable
private fun ToolsSectionHost(component: SettingsComponent) {
    val initial = remember(component) {
        val s = component.state.value
        ToolsSectionUiState(
            adbPath = s.adbPath,
            bundletoolPath = s.bundletoolPath,
            scrcpyPath = s.scrcpyPath,
            adbCheckState = s.adbCheckState,
            bundletoolCheckState = s.bundletoolCheckState,
            scrcpyCheckState = s.scrcpyCheckState,
        )
    }
    val uiState by remember(component) {
        component.state.map {
            ToolsSectionUiState(
                adbPath = it.adbPath,
                bundletoolPath = it.bundletoolPath,
                scrcpyPath = it.scrcpyPath,
                adbCheckState = it.adbCheckState,
                bundletoolCheckState = it.bundletoolCheckState,
                scrcpyCheckState = it.scrcpyCheckState,
            )
        }.distinctUntilChanged()
    }.collectAsState(initial = initial)

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
private fun ToolsSection(
    state: ToolsSectionUiState,
    onAdbPathChanged: (String) -> Unit,
    onCheckAdb: () -> Unit,
    onBundletoolPathChanged: (String) -> Unit,
    onCheckBundletool: () -> Unit,
    onScrcpyPathChanged: (String) -> Unit,
    onCheckScrcpy: () -> Unit,
) {
    val sectionTitle = stringResource(Res.string.settings_section_external_tools)
    val checkLabel = stringResource(Res.string.settings_action_check)
    val checkingLabel = stringResource(Res.string.settings_action_checking)
    val choosePathLabel = stringResource(Res.string.settings_action_choose_path)

    val adbTitle = stringResource(Res.string.settings_tool_adb_title)
    val adbDesc = stringResource(Res.string.settings_field_adb_help)
    val adbPlaceholder = stringResource(Res.string.settings_field_adb_placeholder)
    val adbDownload = stringResource(Res.string.settings_adb_download)
    val adbMacos = stringResource(Res.string.settings_adb_examples_macos)
    val adbLinux = stringResource(Res.string.settings_adb_examples_linux)
    val adbWindows = stringResource(Res.string.settings_adb_examples_windows)

    val bundletoolTitle = stringResource(Res.string.settings_tool_bundletool_title)
    val bundletoolDesc = stringResource(Res.string.settings_field_bundletool_help)
    val bundletoolPlaceholder = stringResource(Res.string.settings_field_bundletool_placeholder)
    val bundletoolDownload = stringResource(Res.string.settings_bundletool_download)
    val bundletoolMacos = stringResource(Res.string.settings_bundletool_examples_macos)
    val bundletoolLinux = stringResource(Res.string.settings_bundletool_examples_linux)
    val bundletoolWindows = stringResource(Res.string.settings_bundletool_examples_windows)

    val scrcpyTitle = stringResource(Res.string.settings_tool_scrcpy_title)
    val scrcpyDesc = stringResource(Res.string.settings_field_scrcpy_help)
    val scrcpyPlaceholder = stringResource(Res.string.settings_field_scrcpy_placeholder)
    val scrcpyDownload = stringResource(Res.string.settings_scrcpy_download)
    val scrcpyMacos = stringResource(Res.string.settings_scrcpy_examples_macos)
    val scrcpyLinux = stringResource(Res.string.settings_scrcpy_examples_linux)
    val scrcpyWindows = stringResource(Res.string.settings_scrcpy_examples_windows)

    AdbSectionCard(
        title = sectionTitle,
        titleUppercase = true,
        titleColor = MaterialTheme.colorScheme.onSurfaceVariant,
        titleTextStyle = MaterialTheme.typography.labelSmall,
        shape = MaterialTheme.shapes.medium,
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
        contentPadding = PaddingValues(0.dp),
        contentSpacing = 0.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        ToolSettingBlock(
            title = adbTitle,
            description = adbDesc,
            path = state.adbPath,
            placeholder = adbPlaceholder,
            checkLabel = checkLabel,
            checkingLabel = checkingLabel,
            choosePathLabel = choosePathLabel,
            checkState = state.adbCheckState,
            helpLines = listOf(adbDownload, adbMacos, adbLinux, adbWindows),
            onPathChanged = onAdbPathChanged,
            onBrowse = {
                chooseToolExecutablePath(title = adbTitle, currentPath = state.adbPath)
                    ?.let(onAdbPathChanged)
            },
            onCheck = onCheckAdb,
        )

        HorizontalDivider()

        ToolSettingBlock(
            title = bundletoolTitle,
            description = bundletoolDesc,
            path = state.bundletoolPath,
            placeholder = bundletoolPlaceholder,
            checkLabel = checkLabel,
            checkingLabel = checkingLabel,
            choosePathLabel = choosePathLabel,
            checkState = state.bundletoolCheckState,
            helpLines = listOf(bundletoolDownload, bundletoolMacos, bundletoolLinux, bundletoolWindows),
            onPathChanged = onBundletoolPathChanged,
            onBrowse = {
                chooseToolExecutablePath(title = bundletoolTitle, currentPath = state.bundletoolPath)
                    ?.let(onBundletoolPathChanged)
            },
            onCheck = onCheckBundletool,
        )

        HorizontalDivider()

        ToolSettingBlock(
            title = scrcpyTitle,
            description = scrcpyDesc,
            path = state.scrcpyPath,
            placeholder = scrcpyPlaceholder,
            checkLabel = checkLabel,
            checkingLabel = checkingLabel,
            choosePathLabel = choosePathLabel,
            checkState = state.scrcpyCheckState,
            helpLines = listOf(scrcpyDownload, scrcpyMacos, scrcpyLinux, scrcpyWindows),
            onPathChanged = onScrcpyPathChanged,
            onBrowse = {
                chooseToolExecutablePath(title = scrcpyTitle, currentPath = state.scrcpyPath)
                    ?.let(onScrcpyPathChanged)
            },
            onCheck = onCheckScrcpy,
        )
    }
}

@Composable
private fun ToolSettingBlock(
    title: String,
    description: String,
    path: String,
    placeholder: String,
    checkLabel: String,
    checkingLabel: String,
    choosePathLabel: String,
    checkState: ToolCheckState,
    helpLines: List<String>,
    onPathChanged: (String) -> Unit,
    onBrowse: () -> Unit,
    onCheck: () -> Unit,
) {
    var showHelp by remember { mutableStateOf(false) }
    val isChecking = checkState is ToolCheckState.Checking

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(Dimensions.paddingDefault),
        verticalArrangement = Arrangement.spacedBy(Dimensions.paddingSmall),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Dimensions.paddingXSmall),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AdbOutlinedTextField(
                value = path,
                onValueChange = onPathChanged,
                placeholder = placeholder,
                modifier = Modifier.weight(1f),
                size = AdbTextFieldSize.MEDIUM,
                trailingIcon = if (path.isNotBlank()) Icons.Outlined.Clear else null,
                onTrailingIconClick = if (path.isNotBlank()) {
                    { onPathChanged("") }
                } else {
                    null
                },
            )

            AdbOutlinedButton(
                onClick = onBrowse,
                text = choosePathLabel,
                leadingIcon = Icons.Outlined.FolderOpen,
                size = AdbButtonSize.MEDIUM,
            )

            AdbFilledButton(
                onClick = onCheck,
                text = if (isChecking) checkingLabel else checkLabel,
                loading = isChecking,
                enabled = !isChecking,
                size = AdbButtonSize.MEDIUM,
            )

            if (helpLines.isNotEmpty()) {
                Box {
                    IconButton(onClick = { showHelp = true }) {
                        Icon(
                            imageVector = Icons.Outlined.Info,
                            contentDescription = null,
                            modifier = Modifier.size(Dimensions.iconSizeCard),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    DropdownMenu(
                        expanded = showHelp,
                        onDismissRequest = { showHelp = false },
                    ) {
                        helpLines.forEach { line ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = line,
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                },
                                onClick = { showHelp = false },
                            )
                        }
                    }
                }
            }
        }

        ToolCheckStateRow(state = checkState)
    }
}

@Composable
private fun ToolCheckStateRow(state: ToolCheckState) {
    val successColor = AdbTheme.semanticColors.success

    when (state) {
        is ToolCheckState.Success -> Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimensions.paddingXSmall),
        ) {
            Icon(
                imageVector = Icons.Outlined.CheckCircle,
                contentDescription = null,
                modifier = Modifier.size(Dimensions.iconSizeSmall),
                tint = successColor,
            )
            Text(
                text = state.message,
                style = MaterialTheme.typography.bodySmall,
                color = successColor,
            )
        }

        is ToolCheckState.Error -> Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimensions.paddingXSmall),
        ) {
            Icon(
                imageVector = Icons.Outlined.ErrorOutline,
                contentDescription = null,
                modifier = Modifier.size(Dimensions.iconSizeSmall),
                tint = MaterialTheme.colorScheme.error,
            )
            Text(
                text = state.message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }

        is ToolCheckState.Checking -> Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimensions.paddingXSmall),
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(Dimensions.iconSizeSmall),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = stringResource(Res.string.settings_action_checking),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        ToolCheckState.Idle -> Unit
    }
}

// ── Save action ───────────────────────────────────────────────────────────────

@Composable
private fun SaveActionHost(component: SettingsComponent) {
    val initial = remember(component) {
        SaveActionUiState(
            isSaving = component.state.value.isSaving,
            hasPendingChanges = component.state.value.hasPendingChanges,
        )
    }
    val uiState by remember(component) {
        component.state
            .map { SaveActionUiState(isSaving = it.isSaving, hasPendingChanges = it.hasPendingChanges) }
            .distinctUntilChanged()
    }.collectAsState(initial = initial)

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
    ) {
        AdbFilledButton(
            onClick = component::onSave,
            text = if (uiState.isSaving) {
                stringResource(Res.string.settings_action_saving)
            } else {
                stringResource(Res.string.settings_action_save)
            },
            loading = uiState.isSaving,
            enabled = uiState.hasPendingChanges && !uiState.isSaving,
            size = AdbButtonSize.MEDIUM,
        )
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

/**
 * Строка настройки внешнего вида: заголовок (жирный) + описание слева, контрол справа.
 */
@Composable
private fun SectionSettingRow(
    title: String,
    modifier: Modifier = Modifier,
    description: String? = null,
    control: @Composable () -> Unit,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimensions.paddingSmall),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (!description.isNullOrBlank()) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        control()
    }
}

private fun chooseToolExecutablePath(title: String, currentPath: String): String? =
    showOpenFileDialog(
        OpenFileDialogConfig(
            title = title,
            initialPath = currentPath,
            selectionMode = HostFileSelectionMode.FILES_ONLY,
            isAcceptAllFileFilterUsed = true,
        ),
    )

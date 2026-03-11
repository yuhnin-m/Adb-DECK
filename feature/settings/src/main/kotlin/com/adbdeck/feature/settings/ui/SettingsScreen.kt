package com.adbdeck.feature.settings.ui

import adbdeck.feature.settings.generated.resources.Res
import adbdeck.feature.settings.generated.resources.settings_action_adb_install_guide
import adbdeck.feature.settings.generated.resources.settings_action_check
import adbdeck.feature.settings.generated.resources.settings_action_checking
import adbdeck.feature.settings.generated.resources.settings_action_choose_path
import adbdeck.feature.settings.generated.resources.settings_action_auto_detect_adb
import adbdeck.feature.settings.generated.resources.settings_action_bundletool_install_guide
import adbdeck.feature.settings.generated.resources.settings_action_scrcpy_install_guide
import adbdeck.feature.settings.generated.resources.settings_action_save
import adbdeck.feature.settings.generated.resources.settings_action_saving
import adbdeck.feature.settings.generated.resources.settings_field_adb_help
import adbdeck.feature.settings.generated.resources.settings_field_adb_placeholder
import adbdeck.feature.settings.generated.resources.settings_field_bundletool_help
import adbdeck.feature.settings.generated.resources.settings_field_bundletool_placeholder
import adbdeck.feature.settings.generated.resources.settings_field_scrcpy_help
import adbdeck.feature.settings.generated.resources.settings_field_scrcpy_placeholder
import adbdeck.feature.settings.generated.resources.settings_help_load_failed
import adbdeck.feature.settings.generated.resources.settings_help_loading
import adbdeck.feature.settings.generated.resources.settings_language_description
import adbdeck.feature.settings.generated.resources.settings_language_english
import adbdeck.feature.settings.generated.resources.settings_language_russian
import adbdeck.feature.settings.generated.resources.settings_language_system
import adbdeck.feature.settings.generated.resources.settings_language_title_bilingual
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
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.awt.SwingPanel
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
import java.awt.Desktop
import java.util.Locale
import javax.swing.JEditorPane
import javax.swing.JScrollPane
import javax.swing.ScrollPaneConstants
import javax.swing.event.HyperlinkEvent
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.stringResource

// ── State snapshots ───────────────────────────────────────────────────────────

private data class AppearanceSectionUiState(
    val currentTheme: AppTheme,
    val currentLanguage: AppLanguage,
)

private data class ToolsSectionUiState(
    val adbPath: String,
    val isAdbAutoDetecting: Boolean,
    val adbAutoDetectCandidates: List<String>,
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

private enum class HelpTool {
    ADB,
    BUNDLETOOL,
    SCRCPY,
}

// ── Root screen ───────────────────────────────────────────────────────────────

@Composable
fun SettingsScreen(component: SettingsComponent) {
    var openedHelpTool by remember { mutableStateOf<HelpTool?>(null) }
    val initialLanguage = remember(component) { component.state.value.currentLanguage }
    val currentLanguage by remember(component) {
        component.state.map { it.currentLanguage }.distinctUntilChanged()
    }.collectAsState(initial = initialLanguage)

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Row(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState())
                        .padding(Dimensions.paddingLarge),
                    verticalArrangement = Arrangement.spacedBy(Dimensions.paddingDefault),
                ) {
                    AppearanceSectionHost(component = component)
                    ToolsSectionHost(
                        component = component,
                        onOpenGuide = { openedHelpTool = it },
                    )
                    SaveActionHost(component = component)
                }

                openedHelpTool?.let { tool ->
                    HelpSidebarPanel(
                        tool = tool,
                        currentLanguage = currentLanguage,
                        onClose = { openedHelpTool = null },
                        modifier = Modifier
                            .width(520.dp)
                            .fillMaxHeight(),
                    )
                }
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
    val appearanceContainerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.28f)
    val appearanceBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)

    AdbSectionCard(
        title = sectionTitle,
        titleUppercase = true,
        titleColor = MaterialTheme.colorScheme.primary,
        titleTextStyle = MaterialTheme.typography.labelSmall,
        shape = MaterialTheme.shapes.medium,
        containerColor = appearanceContainerColor,
        border = BorderStroke(1.dp, appearanceBorderColor),
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
private fun ToolsSectionHost(
    component: SettingsComponent,
    onOpenGuide: (HelpTool) -> Unit,
) {
    val initial = remember(component) {
        val s = component.state.value
        ToolsSectionUiState(
            adbPath = s.adbPath,
            isAdbAutoDetecting = s.isAdbAutoDetecting,
            adbAutoDetectCandidates = s.adbAutoDetectCandidates,
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
                isAdbAutoDetecting = it.isAdbAutoDetecting,
                adbAutoDetectCandidates = it.adbAutoDetectCandidates,
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
        onAutoDetectAdb = component::onAutoDetectAdb,
        onSelectAutoDetectedAdbPath = component::onSelectAutoDetectedAdbPath,
        onDismissAutoDetectedAdbCandidates = component::onDismissAutoDetectedAdbCandidates,
        onCheckAdb = component::onCheckAdb,
        onBundletoolPathChanged = component::onBundletoolPathChanged,
        onCheckBundletool = component::onCheckBundletool,
        onScrcpyPathChanged = component::onScrcpyPathChanged,
        onCheckScrcpy = component::onCheckScrcpy,
        onOpenGuide = onOpenGuide,
    )
}

@Composable
private fun ToolsSection(
    state: ToolsSectionUiState,
    onAdbPathChanged: (String) -> Unit,
    onAutoDetectAdb: () -> Unit,
    onSelectAutoDetectedAdbPath: (String) -> Unit,
    onDismissAutoDetectedAdbCandidates: () -> Unit,
    onCheckAdb: () -> Unit,
    onBundletoolPathChanged: (String) -> Unit,
    onCheckBundletool: () -> Unit,
    onScrcpyPathChanged: (String) -> Unit,
    onCheckScrcpy: () -> Unit,
    onOpenGuide: (HelpTool) -> Unit,
) {
    val sectionTitle = stringResource(Res.string.settings_section_external_tools)
    val checkLabel = stringResource(Res.string.settings_action_check)
    val checkingLabel = stringResource(Res.string.settings_action_checking)
    val autoDetectAdbLabel = stringResource(Res.string.settings_action_auto_detect_adb)
    val choosePathLabel = stringResource(Res.string.settings_action_choose_path)

    val adbTitle = stringResource(Res.string.settings_tool_adb_title)
    val adbDesc = stringResource(Res.string.settings_field_adb_help)
    val adbPlaceholder = stringResource(Res.string.settings_field_adb_placeholder)
    val bundletoolTitle = stringResource(Res.string.settings_tool_bundletool_title)
    val bundletoolDesc = stringResource(Res.string.settings_field_bundletool_help)
    val bundletoolPlaceholder = stringResource(Res.string.settings_field_bundletool_placeholder)

    val scrcpyTitle = stringResource(Res.string.settings_tool_scrcpy_title)
    val scrcpyDesc = stringResource(Res.string.settings_field_scrcpy_help)
    val scrcpyPlaceholder = stringResource(Res.string.settings_field_scrcpy_placeholder)
    val toolsContainerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.28f)
    val toolsBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)

    AdbSectionCard(
        title = sectionTitle,
        titleUppercase = true,
        titleColor = MaterialTheme.colorScheme.primary,
        titleTextStyle = MaterialTheme.typography.labelSmall,
        shape = MaterialTheme.shapes.medium,
        containerColor = toolsContainerColor,
        border = BorderStroke(1.dp, toolsBorderColor),
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
            autoDetectLabel = autoDetectAdbLabel,
            isAutoDetecting = state.isAdbAutoDetecting,
            autoDetectCandidates = state.adbAutoDetectCandidates,
            choosePathLabel = choosePathLabel,
            checkState = state.adbCheckState,
            onPathChanged = onAdbPathChanged,
            onBrowse = {
                chooseToolExecutablePath(title = adbTitle, currentPath = state.adbPath)
                    ?.let(onAdbPathChanged)
            },
            onAutoDetect = onAutoDetectAdb,
            onSelectAutoDetectedPath = onSelectAutoDetectedAdbPath,
            onDismissAutoDetectCandidates = onDismissAutoDetectedAdbCandidates,
            onOpenGuide = { onOpenGuide(HelpTool.ADB) },
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
            autoDetectLabel = null,
            isAutoDetecting = false,
            autoDetectCandidates = emptyList(),
            choosePathLabel = choosePathLabel,
            checkState = state.bundletoolCheckState,
            onPathChanged = onBundletoolPathChanged,
            onBrowse = {
                chooseToolExecutablePath(title = bundletoolTitle, currentPath = state.bundletoolPath)
                    ?.let(onBundletoolPathChanged)
            },
            onAutoDetect = null,
            onSelectAutoDetectedPath = null,
            onDismissAutoDetectCandidates = null,
            onOpenGuide = { onOpenGuide(HelpTool.BUNDLETOOL) },
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
            autoDetectLabel = null,
            isAutoDetecting = false,
            autoDetectCandidates = emptyList(),
            choosePathLabel = choosePathLabel,
            checkState = state.scrcpyCheckState,
            onPathChanged = onScrcpyPathChanged,
            onBrowse = {
                chooseToolExecutablePath(title = scrcpyTitle, currentPath = state.scrcpyPath)
                    ?.let(onScrcpyPathChanged)
            },
            onAutoDetect = null,
            onSelectAutoDetectedPath = null,
            onDismissAutoDetectCandidates = null,
            onOpenGuide = { onOpenGuide(HelpTool.SCRCPY) },
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
    autoDetectLabel: String?,
    isAutoDetecting: Boolean,
    autoDetectCandidates: List<String>,
    choosePathLabel: String,
    checkState: ToolCheckState,
    onPathChanged: (String) -> Unit,
    onBrowse: () -> Unit,
    onAutoDetect: (() -> Unit)?,
    onSelectAutoDetectedPath: ((String) -> Unit)?,
    onDismissAutoDetectCandidates: (() -> Unit)?,
    onOpenGuide: (() -> Unit)?,
    onCheck: () -> Unit,
) {
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

            if (onOpenGuide != null) {
                IconButton(onClick = onOpenGuide) {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = null,
                        modifier = Modifier.size(Dimensions.iconSizeCard),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        if (!autoDetectLabel.isNullOrBlank() && onAutoDetect != null) {
            Box {
                AdbOutlinedButton(
                    onClick = onAutoDetect,
                    text = autoDetectLabel,
                    loading = isAutoDetecting,
                    enabled = !isAutoDetecting,
                    size = AdbButtonSize.MEDIUM,
                )

                DropdownMenu(
                    expanded = autoDetectCandidates.isNotEmpty(),
                    onDismissRequest = { onDismissAutoDetectCandidates?.invoke() },
                ) {
                    autoDetectCandidates.forEach { candidate ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = candidate,
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            },
                            onClick = {
                                onSelectAutoDetectedPath?.invoke(candidate)
                            },
                        )
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

private sealed interface HelpContentState {
    data object Loading : HelpContentState
    data class Loaded(val html: String) : HelpContentState
    data object Error : HelpContentState
}

@Composable
@OptIn(ExperimentalResourceApi::class)
private fun HelpSidebarPanel(
    tool: HelpTool,
    currentLanguage: AppLanguage,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var contentState by remember(tool, currentLanguage) {
        mutableStateOf<HelpContentState>(HelpContentState.Loading)
    }
    val title = when (tool) {
        HelpTool.ADB -> stringResource(Res.string.settings_action_adb_install_guide)
        HelpTool.BUNDLETOOL -> stringResource(Res.string.settings_action_bundletool_install_guide)
        HelpTool.SCRCPY -> stringResource(Res.string.settings_action_scrcpy_install_guide)
    }
    val loadingText = stringResource(Res.string.settings_help_loading)
    val errorText = stringResource(Res.string.settings_help_load_failed)

    LaunchedEffect(tool, currentLanguage) {
        contentState = runCatching {
            val path = resolveHelpResourcePath(tool, currentLanguage)
            Res.readBytes(path).decodeToString()
        }.fold(
            onSuccess = { HelpContentState.Loaded(it) },
            onFailure = { HelpContentState.Error },
        )
    }

    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = Dimensions.paddingDefault,
                        vertical = Dimensions.paddingSmall,
                    ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Dimensions.paddingSmall),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onClose) {
                    Icon(
                        imageVector = Icons.Outlined.Clear,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            HorizontalDivider()

            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                when (val state = contentState) {
                    HelpContentState.Loading -> Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Dimensions.paddingSmall),
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(Dimensions.iconSizeSmall),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            text = loadingText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    HelpContentState.Error -> Text(
                        text = errorText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(Dimensions.paddingLarge),
                    )

                    is HelpContentState.Loaded -> SwingPanel(
                        modifier = Modifier.fillMaxSize(),
                        factory = {
                            createHelpScrollPane(state.html)
                        },
                        update = { pane ->
                            updateHelpScrollPane(pane, state.html)
                        },
                    )
                }
            }
        }
    }
}

private fun resolveHelpResourcePath(tool: HelpTool, language: AppLanguage): String {
    val isRussian = when (language) {
        AppLanguage.RUSSIAN -> true
        AppLanguage.ENGLISH -> false
        AppLanguage.SYSTEM -> Locale.getDefault().language.startsWith("ru", ignoreCase = true)
    }
    return when (tool) {
        HelpTool.ADB -> if (isRussian) "files/adb_help_ru.html" else "files/adb_help_en.html"
        HelpTool.BUNDLETOOL -> if (isRussian) "files/bundletool_help_ru.html" else "files/bundletool_help_en.html"
        HelpTool.SCRCPY -> if (isRussian) "files/scrcpy_help_ru.html" else "files/scrcpy_help_en.html"
    }
}

private fun createHelpScrollPane(html: String): JScrollPane {
    val editor = JEditorPane("text/html", html).apply {
        isEditable = false
        addHyperlinkListener { event ->
            if (event.eventType == HyperlinkEvent.EventType.ACTIVATED) {
                openHelpHyperlink(event)
            }
        }
        caretPosition = 0
    }
    return JScrollPane(editor).apply {
        verticalScrollBarPolicy = ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED
        horizontalScrollBarPolicy = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED
    }
}

private fun updateHelpScrollPane(scrollPane: JScrollPane, html: String) {
    val editor = scrollPane.viewport.view as? JEditorPane ?: return
    if (editor.text != html) {
        editor.text = html
        editor.caretPosition = 0
    }
}

private fun openHelpHyperlink(event: HyperlinkEvent) {
    val url = event.url ?: return
    if (!Desktop.isDesktopSupported()) return
    val desktop = runCatching { Desktop.getDesktop() }.getOrNull() ?: return
    if (!desktop.isSupported(Desktop.Action.BROWSE)) return
    runCatching { desktop.browse(url.toURI()) }
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

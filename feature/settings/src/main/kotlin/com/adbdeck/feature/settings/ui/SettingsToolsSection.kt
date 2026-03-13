package com.adbdeck.feature.settings.ui

import adbdeck.feature.settings.generated.resources.Res
import adbdeck.feature.settings.generated.resources.settings_action_auto_detect_adb
import adbdeck.feature.settings.generated.resources.settings_action_check
import adbdeck.feature.settings.generated.resources.settings_action_checking
import adbdeck.feature.settings.generated.resources.settings_action_choose_path
import adbdeck.feature.settings.generated.resources.settings_field_adb_help
import adbdeck.feature.settings.generated.resources.settings_field_adb_placeholder
import adbdeck.feature.settings.generated.resources.settings_field_bundletool_help
import adbdeck.feature.settings.generated.resources.settings_field_bundletool_placeholder
import adbdeck.feature.settings.generated.resources.settings_field_scrcpy_help
import adbdeck.feature.settings.generated.resources.settings_field_scrcpy_placeholder
import adbdeck.feature.settings.generated.resources.settings_section_external_tools
import adbdeck.feature.settings.generated.resources.settings_tool_adb_title
import adbdeck.feature.settings.generated.resources.settings_tool_bundletool_title
import adbdeck.feature.settings.generated.resources.settings_tool_scrcpy_title
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.adbdeck.core.ui.filedialogs.HostFileSelectionMode
import com.adbdeck.core.ui.filedialogs.OpenFileDialogConfig
import com.adbdeck.core.ui.filedialogs.showOpenFileDialog
import com.adbdeck.core.ui.sectioncards.AdbSectionCard
import com.adbdeck.feature.settings.SettingsComponent
import com.adbdeck.feature.settings.ToolCheckState
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import org.jetbrains.compose.resources.stringResource

internal enum class HelpTool {
    ADB,
    BUNDLETOOL,
    SCRCPY,
}

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

@Composable
internal fun ToolsSectionHost(
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

private fun chooseToolExecutablePath(title: String, currentPath: String): String? =
    showOpenFileDialog(
        OpenFileDialogConfig(
            title = title,
            initialPath = currentPath,
            selectionMode = HostFileSelectionMode.FILES_ONLY,
            isAcceptAllFileFilterUsed = true,
        ),
    )

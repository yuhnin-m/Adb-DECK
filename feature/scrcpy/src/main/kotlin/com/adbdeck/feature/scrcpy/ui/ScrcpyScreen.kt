package com.adbdeck.feature.scrcpy.ui

import adbdeck.feature.scrcpy.generated.resources.Res
import adbdeck.feature.scrcpy.generated.resources.scrcpy_action_open_settings
import adbdeck.feature.scrcpy.generated.resources.scrcpy_action_start
import adbdeck.feature.scrcpy.generated.resources.scrcpy_action_starting
import adbdeck.feature.scrcpy.generated.resources.scrcpy_action_stop
import adbdeck.feature.scrcpy.generated.resources.scrcpy_action_stopping
import adbdeck.feature.scrcpy.generated.resources.scrcpy_empty_no_device
import adbdeck.feature.scrcpy.generated.resources.scrcpy_error_not_configured
import adbdeck.feature.scrcpy.generated.resources.scrcpy_error_not_configured_hint
import adbdeck.feature.scrcpy.generated.resources.scrcpy_field_bitrate
import adbdeck.feature.scrcpy.generated.resources.scrcpy_field_fps
import adbdeck.feature.scrcpy.generated.resources.scrcpy_field_keyboard_mode
import adbdeck.feature.scrcpy.generated.resources.scrcpy_field_max_resolution
import adbdeck.feature.scrcpy.generated.resources.scrcpy_field_mouse_mode
import adbdeck.feature.scrcpy.generated.resources.scrcpy_field_video_codec
import adbdeck.feature.scrcpy.generated.resources.scrcpy_field_window_height
import adbdeck.feature.scrcpy.generated.resources.scrcpy_field_window_placeholder
import adbdeck.feature.scrcpy.generated.resources.scrcpy_field_window_preset
import adbdeck.feature.scrcpy.generated.resources.scrcpy_field_window_width
import adbdeck.feature.scrcpy.generated.resources.scrcpy_window_preset_auto
import adbdeck.feature.scrcpy.generated.resources.scrcpy_window_preset_custom
import adbdeck.feature.scrcpy.generated.resources.scrcpy_resolution_no_limit
import adbdeck.feature.scrcpy.generated.resources.scrcpy_section_advanced
import adbdeck.feature.scrcpy.generated.resources.scrcpy_section_basic
import adbdeck.feature.scrcpy.generated.resources.scrcpy_section_control
import adbdeck.feature.scrcpy.generated.resources.scrcpy_section_window
import adbdeck.feature.scrcpy.generated.resources.scrcpy_status_device_label
import adbdeck.feature.scrcpy.generated.resources.scrcpy_status_running
import adbdeck.feature.scrcpy.generated.resources.scrcpy_toggle_allow_input
import adbdeck.feature.scrcpy.generated.resources.scrcpy_toggle_always_on_top
import adbdeck.feature.scrcpy.generated.resources.scrcpy_toggle_borderless
import adbdeck.feature.scrcpy.generated.resources.scrcpy_toggle_fullscreen
import adbdeck.feature.scrcpy.generated.resources.scrcpy_toggle_show_touches
import adbdeck.feature.scrcpy.generated.resources.scrcpy_toggle_stay_awake
import adbdeck.feature.scrcpy.generated.resources.scrcpy_toggle_turn_screen_off
import adbdeck.feature.scrcpy.generated.resources.scrcpy_bitrate_auto
import adbdeck.feature.scrcpy.generated.resources.scrcpy_bitrate_value_mbps
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Cast
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.adbdeck.core.designsystem.AdbCornerRadius
import com.adbdeck.core.designsystem.AdbTheme
import com.adbdeck.core.designsystem.Dimensions
import com.adbdeck.core.ui.AdbBanner
import com.adbdeck.core.ui.AdbBannerType
import com.adbdeck.core.ui.EmptyView
import com.adbdeck.core.ui.buttons.AdbButtonSize
import com.adbdeck.core.ui.buttons.AdbFilledButton
import com.adbdeck.core.ui.buttons.AdbOutlinedButton
import com.adbdeck.core.ui.sectioncards.AdbSectionCard
import com.adbdeck.core.ui.settings.AdbSettingRow
import com.adbdeck.core.ui.textfields.AdbDropdownOption
import com.adbdeck.core.ui.textfields.AdbOutlinedDropdownTextField
import com.adbdeck.core.ui.textfields.AdbOutlinedTextField
import com.adbdeck.core.ui.textfields.AdbTextFieldSize
import com.adbdeck.feature.scrcpy.ScrcpyComponent
import com.adbdeck.feature.scrcpy.ScrcpyConfig
import com.adbdeck.feature.scrcpy.ScrcpyFeedback
import com.adbdeck.feature.scrcpy.ScrcpyFps
import com.adbdeck.feature.scrcpy.ScrcpyInputMode
import com.adbdeck.feature.scrcpy.ScrcpyMaxResolution
import com.adbdeck.feature.scrcpy.ScrcpyProcessState
import com.adbdeck.feature.scrcpy.ScrcpyState
import com.adbdeck.feature.scrcpy.ScrcpyVideoCodec
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import org.jetbrains.compose.resources.stringResource

/**
 * Корневой composable экрана scrcpy.
 */
@Composable
fun ScrcpyScreen(component: ScrcpyComponent) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            ScrcpyBodyHost(
                component = component,
                modifier = Modifier.fillMaxSize(),
            )
            ScrcpyFeedbackHost(
                component = component,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(Dimensions.paddingDefault),
            )
        }
    }
}

@Composable
private fun ScrcpyBodyHost(
    component: ScrcpyComponent,
    modifier: Modifier = Modifier,
) {
    val initialUiState = remember(component) { component.state.value.toBodyUiState() }
    val uiState by remember(component) {
        component.state
            .map { it.toBodyUiState() }
            .distinctUntilChanged()
    }.collectAsState(initial = initialUiState)

    when {
        uiState.activeDeviceId == null -> {
            EmptyView(
                message = stringResource(Res.string.scrcpy_empty_no_device),
                modifier = modifier,
            )
        }

        !uiState.isConfigured -> {
            ScrcpyNotConfiguredView(
                onOpenSettings = component::onOpenSettings,
                modifier = modifier,
            )
        }

        else -> {
            ScrcpyContent(
                uiState = uiState,
                component = component,
                modifier = modifier,
            )
        }
    }
}

@Composable
private fun ScrcpyFeedbackHost(
    component: ScrcpyComponent,
    modifier: Modifier = Modifier,
) {
    val initialFeedback = remember(component) { component.state.value.feedback }
    val feedback by remember(component) {
        component.state
            .map { it.feedback }
            .distinctUntilChanged()
    }.collectAsState(initial = initialFeedback)

    feedback?.let {
        ScrcpyFeedbackBanner(
            feedback = it,
            onDismiss = component::onDismissFeedback,
            modifier = modifier,
        )
    }
}

@Composable
private fun ScrcpyFeedbackBanner(
    feedback: ScrcpyFeedback,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AdbBanner(
        message = feedback.message,
        type = if (feedback.isError) AdbBannerType.ERROR else AdbBannerType.SUCCESS,
        onDismiss = onDismiss,
        modifier = modifier,
    )
}

@Composable
private fun ScrcpyNotConfiguredView(
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(Dimensions.paddingLarge),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Outlined.Cast,
            contentDescription = null,
            modifier = Modifier.size(Dimensions.iconSizeLarge),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(Dimensions.paddingDefault))
        Text(
            text = stringResource(Res.string.scrcpy_error_not_configured),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(Dimensions.paddingSmall))
        Text(
            text = stringResource(Res.string.scrcpy_error_not_configured_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(Dimensions.paddingLarge))
        AdbFilledButton(
            onClick = onOpenSettings,
            text = stringResource(Res.string.scrcpy_action_open_settings),
            size = AdbButtonSize.MEDIUM,
            cornerRadius = AdbCornerRadius.MEDIUM,
        )
    }
}

@Composable
private fun ScrcpyContent(
    uiState: ScrcpyBodyUiState,
    component: ScrcpyComponent,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(Dimensions.paddingDefault),
        verticalArrangement = Arrangement.spacedBy(Dimensions.paddingMedium),
    ) {
        val isRunning = uiState.processState == ScrcpyProcessState.RUNNING
        val isTransitioning = uiState.processState == ScrcpyProcessState.STARTING ||
            uiState.processState == ScrcpyProcessState.STOPPING
        val controlsEnabled = !isRunning && !isTransitioning

        if (isRunning) {
            ScrcpyRunningBanner()
        }

        BasicSection(
            config = uiState.config,
            deviceId = uiState.activeDeviceId,
            enabled = controlsEnabled,
            onMaxResolutionChanged = component::onMaxResolutionChanged,
            onFpsChanged = component::onFpsChanged,
            onBitrateChanged = component::onBitrateChanged,
        )

        ControlSection(
            config = uiState.config,
            enabled = controlsEnabled,
            onAllowInputChanged = component::onAllowInputChanged,
            onTurnScreenOffChanged = component::onTurnScreenOffChanged,
            onShowTouchesChanged = component::onShowTouchesChanged,
            onStayAwakeChanged = component::onStayAwakeChanged,
        )

        WindowSection(
            config = uiState.config,
            enabled = controlsEnabled,
            onFullscreenChanged = component::onFullscreenChanged,
            onAlwaysOnTopChanged = component::onAlwaysOnTopChanged,
            onBorderlessChanged = component::onBorderlessChanged,
            onWindowWidthChanged = component::onWindowWidthChanged,
            onWindowHeightChanged = component::onWindowHeightChanged,
        )

        AdvancedSection(
            config = uiState.config,
            enabled = controlsEnabled,
            onVideoCodecChanged = component::onVideoCodecChanged,
            onKeyboardModeChanged = component::onKeyboardModeChanged,
            onMouseModeChanged = component::onMouseModeChanged,
        )

        ScrcpyActionButton(
            processState = uiState.processState,
            onStart = component::startScrcpy,
            onStop = component::stopScrcpy,
        )

        Spacer(Modifier.height(Dimensions.paddingLarge))
    }
}

@Composable
private fun ScrcpyRunningBanner() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = AdbTheme.semanticColors.success.copy(alpha = 0.12f),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(AdbCornerRadius.MEDIUM.value),
            )
            .padding(horizontal = Dimensions.paddingDefault, vertical = Dimensions.paddingMedium),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimensions.paddingSmall),
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(color = AdbTheme.semanticColors.success, shape = CircleShape),
        )
        Text(
            text = stringResource(Res.string.scrcpy_status_running),
            style = MaterialTheme.typography.labelLarge,
            color = AdbTheme.semanticColors.success,
        )
    }
}

@Composable
private fun BasicSection(
    config: ScrcpyConfig,
    deviceId: String?,
    enabled: Boolean,
    onMaxResolutionChanged: (ScrcpyMaxResolution) -> Unit,
    onFpsChanged: (ScrcpyFps) -> Unit,
    onBitrateChanged: (String) -> Unit,
) {
    val noLimitLabel = stringResource(Res.string.scrcpy_resolution_no_limit)
    val bitrateAutoLabel = stringResource(Res.string.scrcpy_bitrate_auto)
    val resolutionOptions = remember(noLimitLabel) {
        ScrcpyMaxResolution.entries.map {
            AdbDropdownOption(
                value = it,
                label = it.displayValue ?: noLimitLabel,
            )
        }
    }
    val fpsOptions = remember {
        ScrcpyFps.entries.map { AdbDropdownOption(value = it, label = it.value.toString()) }
    }
    val bitrateOptions = listOf(
        AdbDropdownOption(value = "", label = bitrateAutoLabel),
        AdbDropdownOption(value = "2", label = stringResource(Res.string.scrcpy_bitrate_value_mbps, 2)),
        AdbDropdownOption(value = "4", label = stringResource(Res.string.scrcpy_bitrate_value_mbps, 4)),
        AdbDropdownOption(value = "8", label = stringResource(Res.string.scrcpy_bitrate_value_mbps, 8)),
        AdbDropdownOption(value = "12", label = stringResource(Res.string.scrcpy_bitrate_value_mbps, 12)),
        AdbDropdownOption(value = "16", label = stringResource(Res.string.scrcpy_bitrate_value_mbps, 16)),
        AdbDropdownOption(value = "20", label = stringResource(Res.string.scrcpy_bitrate_value_mbps, 20)),
        AdbDropdownOption(value = "24", label = stringResource(Res.string.scrcpy_bitrate_value_mbps, 24)),
        AdbDropdownOption(value = "32", label = stringResource(Res.string.scrcpy_bitrate_value_mbps, 32)),
    )
    val selectedBitrate = bitrateOptions.firstOrNull { it.value == config.bitrate }?.value.orEmpty()

    AdbSectionCard(
        title = stringResource(Res.string.scrcpy_section_basic),
        titleUppercase = true,
    ) {
        ScrcpySettingRow(title = stringResource(Res.string.scrcpy_status_device_label)) {
            Text(
                text = deviceId.orEmpty(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        HorizontalDivider()

        ScrcpySettingRow(title = stringResource(Res.string.scrcpy_field_max_resolution)) {
            ScrcpyEnumDropdown(
                selected = config.maxResolution,
                options = resolutionOptions,
                onSelected = onMaxResolutionChanged,
                enabled = enabled,
            )
        }

        HorizontalDivider()

        ScrcpySettingRow(title = stringResource(Res.string.scrcpy_field_fps)) {
            ScrcpyEnumDropdown(
                selected = config.fps,
                options = fpsOptions,
                onSelected = onFpsChanged,
                enabled = enabled,
            )
        }

        HorizontalDivider()

        ScrcpySettingRow(title = stringResource(Res.string.scrcpy_field_bitrate)) {
            ScrcpyEnumDropdown(
                selected = selectedBitrate,
                options = bitrateOptions,
                onSelected = onBitrateChanged,
                enabled = enabled,
            )
        }
    }
}

@Composable
private fun ControlSection(
    config: ScrcpyConfig,
    enabled: Boolean,
    onAllowInputChanged: (Boolean) -> Unit,
    onTurnScreenOffChanged: (Boolean) -> Unit,
    onShowTouchesChanged: (Boolean) -> Unit,
    onStayAwakeChanged: (Boolean) -> Unit,
) {
    AdbSectionCard(
        title = stringResource(Res.string.scrcpy_section_control),
        titleUppercase = true,
    ) {
        ScrcpySettingRow(title = stringResource(Res.string.scrcpy_toggle_allow_input)) {
            ScrcpyCompactSwitch(checked = config.allowInput, onCheckedChange = onAllowInputChanged, enabled = enabled)
        }
        HorizontalDivider()
        ScrcpySettingRow(title = stringResource(Res.string.scrcpy_toggle_turn_screen_off)) {
            ScrcpyCompactSwitch(checked = config.turnScreenOff, onCheckedChange = onTurnScreenOffChanged, enabled = enabled)
        }
        HorizontalDivider()
        ScrcpySettingRow(title = stringResource(Res.string.scrcpy_toggle_show_touches)) {
            ScrcpyCompactSwitch(checked = config.showTouches, onCheckedChange = onShowTouchesChanged, enabled = enabled)
        }
        HorizontalDivider()
        ScrcpySettingRow(title = stringResource(Res.string.scrcpy_toggle_stay_awake)) {
            ScrcpyCompactSwitch(checked = config.stayAwake, onCheckedChange = onStayAwakeChanged, enabled = enabled)
        }
    }
}

@Composable
private fun WindowSection(
    config: ScrcpyConfig,
    enabled: Boolean,
    onFullscreenChanged: (Boolean) -> Unit,
    onAlwaysOnTopChanged: (Boolean) -> Unit,
    onBorderlessChanged: (Boolean) -> Unit,
    onWindowWidthChanged: (String) -> Unit,
    onWindowHeightChanged: (String) -> Unit,
) {
    val customPresetLabel = stringResource(Res.string.scrcpy_window_preset_custom)
    val autoPresetLabel = stringResource(Res.string.scrcpy_window_preset_auto)
    val windowPresetOptions = remember(customPresetLabel, autoPresetLabel) {
        listOf(
            ScrcpyWindowPresetOption(
                value = WindowPresetValue.AUTO,
                label = autoPresetLabel,
                width = "",
                height = "",
            ),
            ScrcpyWindowPresetOption(
                value = WindowPresetValue.P_960_540,
                label = "960 × 540",
                width = "960",
                height = "540",
            ),
            ScrcpyWindowPresetOption(
                value = WindowPresetValue.P_1280_720,
                label = "1280 × 720",
                width = "1280",
                height = "720",
            ),
            ScrcpyWindowPresetOption(
                value = WindowPresetValue.P_1600_900,
                label = "1600 × 900",
                width = "1600",
                height = "900",
            ),
            ScrcpyWindowPresetOption(
                value = WindowPresetValue.P_1920_1080,
                label = "1920 × 1080",
                width = "1920",
                height = "1080",
            ),
            ScrcpyWindowPresetOption(
                value = WindowPresetValue.P_2560_1440,
                label = "2560 × 1440",
                width = "2560",
                height = "1440",
            ),
            ScrcpyWindowPresetOption(
                value = WindowPresetValue.CUSTOM,
                label = customPresetLabel,
                width = null,
                height = null,
            ),
        )
    }
    val selectedWindowPreset = remember(config.windowWidth, config.windowHeight, windowPresetOptions) {
        windowPresetOptions.firstOrNull { option ->
            option.width == config.windowWidth && option.height == config.windowHeight
        }?.value ?: WindowPresetValue.CUSTOM
    }
    val dropdownWindowPresetOptions = remember(windowPresetOptions) {
        windowPresetOptions.map { option ->
            AdbDropdownOption(
                value = option.value,
                label = option.label,
                enabled = option.value != WindowPresetValue.CUSTOM,
            )
        }
    }

    AdbSectionCard(
        title = stringResource(Res.string.scrcpy_section_window),
        titleUppercase = true,
    ) {
        ScrcpySettingRow(title = stringResource(Res.string.scrcpy_toggle_fullscreen)) {
            ScrcpyCompactSwitch(checked = config.fullscreen, onCheckedChange = onFullscreenChanged, enabled = enabled)
        }
        HorizontalDivider()
        ScrcpySettingRow(title = stringResource(Res.string.scrcpy_toggle_always_on_top)) {
            ScrcpyCompactSwitch(checked = config.alwaysOnTop, onCheckedChange = onAlwaysOnTopChanged, enabled = enabled)
        }
        HorizontalDivider()
        ScrcpySettingRow(title = stringResource(Res.string.scrcpy_toggle_borderless)) {
            ScrcpyCompactSwitch(checked = config.borderless, onCheckedChange = onBorderlessChanged, enabled = enabled)
        }
        HorizontalDivider()
        ScrcpySettingRow(title = stringResource(Res.string.scrcpy_field_window_preset)) {
            ScrcpyEnumDropdown(
                selected = selectedWindowPreset,
                options = dropdownWindowPresetOptions,
                onSelected = { selected ->
                    val preset = windowPresetOptions.firstOrNull { it.value == selected } ?: return@ScrcpyEnumDropdown
                    if (preset.value == WindowPresetValue.CUSTOM) return@ScrcpyEnumDropdown
                    onWindowWidthChanged(preset.width.orEmpty())
                    onWindowHeightChanged(preset.height.orEmpty())
                },
                enabled = enabled,
            )
        }
        HorizontalDivider()
        ScrcpySettingRow(title = stringResource(Res.string.scrcpy_field_window_width)) {
            AdbOutlinedTextField(
                value = config.windowWidth,
                onValueChange = onWindowWidthChanged,
                placeholder = stringResource(Res.string.scrcpy_field_window_placeholder),
                modifier = Modifier.width(120.dp),
                cornerRadius = AdbCornerRadius.SMALL,
                size = AdbTextFieldSize.SMALL,
                enabled = enabled,
            )
        }
        HorizontalDivider()
        ScrcpySettingRow(title = stringResource(Res.string.scrcpy_field_window_height)) {
            AdbOutlinedTextField(
                value = config.windowHeight,
                onValueChange = onWindowHeightChanged,
                placeholder = stringResource(Res.string.scrcpy_field_window_placeholder),
                modifier = Modifier.width(120.dp),
                cornerRadius = AdbCornerRadius.SMALL,
                size = AdbTextFieldSize.SMALL,
                enabled = enabled,
            )
        }
    }
}

@Composable
private fun AdvancedSection(
    config: ScrcpyConfig,
    enabled: Boolean,
    onVideoCodecChanged: (ScrcpyVideoCodec) -> Unit,
    onKeyboardModeChanged: (ScrcpyInputMode) -> Unit,
    onMouseModeChanged: (ScrcpyInputMode) -> Unit,
) {
    val codecOptions = remember {
        ScrcpyVideoCodec.entries.map { AdbDropdownOption(value = it, label = it.cliValue) }
    }
    val inputModeOptions = remember {
        ScrcpyInputMode.entries.map { AdbDropdownOption(value = it, label = it.cliValue) }
    }

    AdbSectionCard(
        title = stringResource(Res.string.scrcpy_section_advanced),
        titleUppercase = true,
    ) {
        ScrcpySettingRow(title = stringResource(Res.string.scrcpy_field_video_codec)) {
            ScrcpyEnumDropdown(
                selected = config.videoCodec,
                options = codecOptions,
                onSelected = onVideoCodecChanged,
                enabled = enabled,
            )
        }
        HorizontalDivider()
        ScrcpySettingRow(title = stringResource(Res.string.scrcpy_field_keyboard_mode)) {
            ScrcpyEnumDropdown(
                selected = config.keyboardMode,
                options = inputModeOptions,
                onSelected = onKeyboardModeChanged,
                enabled = enabled,
            )
        }
        HorizontalDivider()
        ScrcpySettingRow(title = stringResource(Res.string.scrcpy_field_mouse_mode)) {
            ScrcpyEnumDropdown(
                selected = config.mouseMode,
                options = inputModeOptions,
                onSelected = onMouseModeChanged,
                enabled = enabled,
            )
        }
    }
}

@Composable
private fun ScrcpySettingRow(
    title: String,
    control: @Composable () -> Unit,
) {
    AdbSettingRow(
        title = title,
        modifier = Modifier.heightIn(min = UNIFIED_ROW_HEIGHT),
        control = control,
    )
}

@Composable
private fun ScrcpyCompactSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean,
) {
    CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
        )
    }
}

@Composable
private fun ScrcpyActionButton(
    processState: ScrcpyProcessState,
    onStart: () -> Unit,
    onStop: () -> Unit,
) {
    val isRunning = processState == ScrcpyProcessState.RUNNING
    val isStarting = processState == ScrcpyProcessState.STARTING
    val isStopping = processState == ScrcpyProcessState.STOPPING
    val isTransitioning = isStarting || isStopping

    val label = when (processState) {
        ScrcpyProcessState.RUNNING -> stringResource(Res.string.scrcpy_action_stop)
        ScrcpyProcessState.STARTING -> stringResource(Res.string.scrcpy_action_starting)
        ScrcpyProcessState.STOPPING -> stringResource(Res.string.scrcpy_action_stopping)
        else -> stringResource(Res.string.scrcpy_action_start)
    }

    if (isRunning) {
        AdbOutlinedButton(
            onClick = onStop,
            text = label,
            enabled = true,
            loading = false,
            size = AdbButtonSize.LARGE,
            cornerRadius = AdbCornerRadius.MEDIUM,
            modifier = Modifier.fillMaxWidth(),
        )
    } else {
        AdbFilledButton(
            onClick = if (isTransitioning) ({}) else onStart,
            text = label,
            enabled = !isTransitioning,
            loading = isTransitioning,
            size = AdbButtonSize.LARGE,
            cornerRadius = AdbCornerRadius.MEDIUM,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun <T> ScrcpyEnumDropdown(
    selected: T,
    options: List<AdbDropdownOption<T>>,
    onSelected: (T) -> Unit,
    enabled: Boolean,
) {
    AdbOutlinedDropdownTextField(
        options = options,
        selectedValue = selected,
        onValueSelected = onSelected,
        modifier = Modifier.width(160.dp),
        cornerRadius = AdbCornerRadius.SMALL,
        size = AdbTextFieldSize.SMALL,
        enabled = enabled,
    )
}

private data class ScrcpyBodyUiState(
    val activeDeviceId: String?,
    val isConfigured: Boolean,
    val processState: ScrcpyProcessState,
    val config: ScrcpyConfig,
)

private data class ScrcpyWindowPresetOption(
    val value: WindowPresetValue,
    val label: String,
    val width: String?,
    val height: String?,
)

private enum class WindowPresetValue {
    AUTO,
    P_960_540,
    P_1280_720,
    P_1600_900,
    P_1920_1080,
    P_2560_1440,
    CUSTOM,
}

private fun ScrcpyState.toBodyUiState(): ScrcpyBodyUiState = ScrcpyBodyUiState(
    activeDeviceId = activeDeviceId,
    isConfigured = isConfigured,
    processState = processState,
    config = config,
)

private val UNIFIED_ROW_HEIGHT = 38.dp

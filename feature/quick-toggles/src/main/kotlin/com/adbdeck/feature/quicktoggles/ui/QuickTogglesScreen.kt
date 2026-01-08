package com.adbdeck.feature.quicktoggles.ui

import adbdeck.feature.quick_toggles.generated.resources.Res
import adbdeck.feature.quick_toggles.generated.resources.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.adbdeck.core.designsystem.AdbCornerRadius
import com.adbdeck.core.designsystem.AdbTheme
import com.adbdeck.core.designsystem.Dimensions
import com.adbdeck.core.i18n.AdbCommonStringRes
import com.adbdeck.core.ui.AdbBanner
import com.adbdeck.core.ui.AdbBannerType
import com.adbdeck.core.ui.buttons.AdbButtonSize
import com.adbdeck.core.ui.buttons.AdbButtonType
import com.adbdeck.core.ui.buttons.AdbFilledButton
import com.adbdeck.core.ui.buttons.AdbOutlinedButton
import com.adbdeck.core.ui.sectioncards.AdbSectionCard
import com.adbdeck.core.ui.segmentedbuttons.AdbSegmentedButtonSize
import com.adbdeck.core.ui.segmentedbuttons.AdbSegmentedOption
import com.adbdeck.core.ui.segmentedbuttons.AdbSingleSegmentedButtons
import com.adbdeck.feature.quicktoggles.ANIMATION_ANIMATOR_SCALE_KEY
import com.adbdeck.feature.quicktoggles.ANIMATION_TRANSITION_SCALE_KEY
import com.adbdeck.feature.quicktoggles.ANIMATION_WINDOW_SCALE_KEY
import com.adbdeck.feature.quicktoggles.AnimationScaleControl
import com.adbdeck.feature.quicktoggles.AnimationScaleStatus
import com.adbdeck.feature.quicktoggles.QuickToggleId
import com.adbdeck.feature.quicktoggles.QuickToggleState
import com.adbdeck.feature.quicktoggles.QuickTogglesComponent
import com.adbdeck.feature.quicktoggles.ToggleItem
import java.util.Locale
import kotlin.math.roundToInt
import org.jetbrains.compose.resources.stringResource

/**
 * Экран быстрых QA-тумблеров для устройства.
 */
@Composable
fun QuickTogglesScreen(component: QuickTogglesComponent) {
    val state by component.state.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            QuickTogglesToolbar(
                isDeviceAvailable = state.isDeviceAvailable,
                isRefreshing = state.isRefreshing,
                onRefresh = component::onRefresh,
            )

            HorizontalDivider()

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(Dimensions.paddingDefault),
                verticalArrangement = Arrangement.spacedBy(Dimensions.paddingDefault),
            ) {
                if (!state.isDeviceAvailable) {
                    item(key = "no_device_hint") {
                        AdbBanner(
                            message = stringResource(Res.string.quick_toggles_empty_no_device_subtitle),
                            type = AdbBannerType.INFO,
                        )
                    }
                }

                items(
                    items = state.items,
                    key = { it.id.name },
                ) { item ->
                    ToggleCard(
                        item = item,
                        isDeviceAvailable = state.isDeviceAvailable,
                        onRequestToggle = component::onRequestToggle,
                        onRefreshToggle = component::onRefreshToggle,
                        onOpenSettings = component::onOpenSettings,
                        onAnimationDraftChanged = component::onAnimationDraftChanged,
                        onSetAnimationScale = component::onSetAnimationScale,
                    )
                }
            }
        }

        state.pendingAction?.let { pending ->
            QuickToggleConfirmDialog(
                toggleId = pending.toggleId,
                targetState = pending.targetState,
                onConfirm = component::onConfirmToggle,
                onDismiss = component::onCancelToggle,
            )
        }

        state.feedback?.let { feedback ->
            AdbBanner(
                message = feedback.message,
                type = if (feedback.isError) AdbBannerType.ERROR else AdbBannerType.SUCCESS,
                onDismiss = component::onDismissFeedback,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(Dimensions.paddingDefault),
            )
        }
    }
}

@Composable
private fun QuickTogglesToolbar(
    isDeviceAvailable: Boolean,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Dimensions.paddingSmall, vertical = Dimensions.paddingXSmall),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimensions.paddingSmall),
    ) {
        Text(
            text = stringResource(Res.string.quick_toggles_screen_title),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f),
        )

        AdbOutlinedButton(
            onClick = onRefresh,
            enabled = isDeviceAvailable,
            loading = isRefreshing,
            text = stringResource(AdbCommonStringRes.actionRefresh),
            leadingIcon = if (isRefreshing) null else Icons.Outlined.Refresh,
            size = AdbButtonSize.SMALL,
        )
    }
}

@Composable
private fun ToggleCard(
    item: ToggleItem,
    isDeviceAvailable: Boolean,
    onRequestToggle: (QuickToggleId, QuickToggleState) -> Unit,
    onRefreshToggle: (QuickToggleId) -> Unit,
    onOpenSettings: (QuickToggleId) -> Unit,
    onAnimationDraftChanged: (String, Float) -> Unit,
    onSetAnimationScale: (String) -> Unit,
) {
    if (item.id == QuickToggleId.ANIMATIONS) {
        AnimationsToggleCard(
            item = item,
            isDeviceAvailable = isDeviceAvailable,
            onRefreshToggle = onRefreshToggle,
            onOpenSettings = onOpenSettings,
            onAnimationDraftChanged = onAnimationDraftChanged,
            onSetAnimationScale = onSetAnimationScale,
        )
    } else {
        StandardToggleCard(
            item = item,
            isDeviceAvailable = isDeviceAvailable,
            onRequestToggle = onRequestToggle,
            onOpenSettings = onOpenSettings,
        )
    }
}

@Composable
private fun StandardToggleCard(
    item: ToggleItem,
    isDeviceAvailable: Boolean,
    onRequestToggle: (QuickToggleId, QuickToggleState) -> Unit,
    onOpenSettings: (QuickToggleId) -> Unit,
) {
    val labelOn = stringResource(Res.string.quick_toggles_state_on)
    val labelOff = stringResource(Res.string.quick_toggles_state_off)
    val labelUnknown = stringResource(Res.string.quick_toggles_state_unknown)
    val canToggle = isDeviceAvailable && item.canToggle && !item.isRunning

    AdbSectionCard(
        title = toggleTitle(item.id, fallback = item.title),
        titleUppercase = false,
        containerColor = AdbTheme.colorScheme.surfaceVariant.copy(alpha = 0.32f),
        border = BorderStroke(
            width = 1.dp,
            color = AdbTheme.colorScheme.outline.copy(alpha = 0.25f),
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(Res.string.quick_toggles_label_current_state, stateLabel(item.state)),
                style = MaterialTheme.typography.bodySmall,
                color = AdbTheme.colorScheme.onSurfaceVariant,
            )
            if (item.isRunning) {
                CircularProgressIndicator(
                    modifier = Modifier.padding(start = 8.dp),
                    strokeWidth = 2.dp,
                )
            }
        }

        AdbSingleSegmentedButtons(
            options = listOf(
                AdbSegmentedOption(
                    value = QuickToggleState.ON,
                    label = labelOn,
                    enabled = canToggle,
                ),
                AdbSegmentedOption(
                    value = QuickToggleState.OFF,
                    label = labelOff,
                    enabled = canToggle,
                ),
                AdbSegmentedOption(
                    value = QuickToggleState.UNKNOWN,
                    label = labelUnknown,
                    enabled = false,
                ),
            ),
            selectedValue = if (item.state == QuickToggleState.CUSTOM) QuickToggleState.UNKNOWN else item.state,
            onValueSelected = { selectedState ->
                if (selectedState == QuickToggleState.ON || selectedState == QuickToggleState.OFF) {
                    if (selectedState != item.state) {
                        onRequestToggle(item.id, selectedState)
                    }
                }
            },
            size = AdbSegmentedButtonSize.MEDIUM,
            cornerRadius = AdbCornerRadius.MEDIUM,
        )

        if (!item.error.isNullOrBlank()) {
            Text(
                text = item.error,
                style = MaterialTheme.typography.bodySmall,
                color = AdbTheme.colorScheme.error,
            )
        }

        if (item.showOpenSettings) {
            AdbOutlinedButton(
                onClick = { onOpenSettings(item.id) },
                text = stringResource(Res.string.quick_toggles_action_open_settings),
                type = AdbButtonType.NEUTRAL,
                size = AdbButtonSize.SMALL,
            )
        }
    }
}

/**
 * Специализированный блок независимой настройки параметров анимаций.
 */
@Composable
private fun AnimationsToggleCard(
    item: ToggleItem,
    isDeviceAvailable: Boolean,
    onRefreshToggle: (QuickToggleId) -> Unit,
    onOpenSettings: (QuickToggleId) -> Unit,
    onAnimationDraftChanged: (String, Float) -> Unit,
    onSetAnimationScale: (String) -> Unit,
) {
    val canToggle = isDeviceAvailable && item.canToggle

    AdbSectionCard(
        title = stringResource(Res.string.quick_toggles_toggle_animations),
        titleUppercase = false,
        containerColor = AdbTheme.colorScheme.surfaceVariant.copy(alpha = 0.32f),
        border = BorderStroke(
            width = 1.dp,
            color = AdbTheme.colorScheme.outline.copy(alpha = 0.25f),
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(Res.string.quick_toggles_animations_independent_title),
                style = MaterialTheme.typography.bodyMedium,
                color = AdbTheme.colorScheme.onSurface,
            )
            AdbOutlinedButton(
                onClick = { onRefreshToggle(QuickToggleId.ANIMATIONS) },
                text = stringResource(Res.string.quick_toggles_animations_action_refresh),
                leadingIcon = Icons.Outlined.Refresh,
                type = AdbButtonType.NEUTRAL,
                size = AdbButtonSize.SMALL,
                enabled = canToggle,
            )
        }

        if (!isDeviceAvailable) {
            Text(
                text = stringResource(Res.string.quick_toggles_animations_no_device_hint),
                style = MaterialTheme.typography.bodySmall,
                color = AdbTheme.colorScheme.onSurfaceVariant,
            )
        }

        item.animationControls.forEach { control ->
            AnimationScaleControlRow(
                control = control,
                enabled = canToggle,
                onDraftChanged = onAnimationDraftChanged,
                onSet = onSetAnimationScale,
            )
        }

        if (!item.error.isNullOrBlank()) {
            Text(
                text = item.error,
                style = MaterialTheme.typography.bodySmall,
                color = AdbTheme.colorScheme.error,
            )
        }

        if (item.showOpenSettings) {
            AdbOutlinedButton(
                onClick = { onOpenSettings(item.id) },
                text = stringResource(Res.string.quick_toggles_action_open_settings),
                type = AdbButtonType.NEUTRAL,
                size = AdbButtonSize.SMALL,
            )
        }
    }
}

@Composable
private fun AnimationScaleControlRow(
    control: AnimationScaleControl,
    enabled: Boolean,
    onDraftChanged: (String, Float) -> Unit,
    onSet: (String) -> Unit,
) {
    val currentValueLabel = control.currentValue
        ?.let { formatScaleValue(it) }
        ?: stringResource(Res.string.quick_toggles_animations_value_default_unset)

    val statusLabel = when (control.status) {
        AnimationScaleStatus.OK -> stringResource(Res.string.quick_toggles_animations_status_ok)
        AnimationScaleStatus.ERROR -> stringResource(Res.string.quick_toggles_animations_status_error)
        AnimationScaleStatus.LOADING -> stringResource(Res.string.quick_toggles_animations_status_loading)
    }

    val statusColor = when (control.status) {
        AnimationScaleStatus.OK -> AdbTheme.colorScheme.tertiary
        AnimationScaleStatus.ERROR -> AdbTheme.colorScheme.error
        AnimationScaleStatus.LOADING -> AdbTheme.colorScheme.primary
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Dimensions.paddingXSmall),
    ) {
        Text(
            text = control.key,
            style = MaterialTheme.typography.bodySmall,
            color = AdbTheme.colorScheme.onSurface,
        )
        Text(
            text = animationDescription(control.key),
            style = MaterialTheme.typography.labelSmall,
            color = AdbTheme.colorScheme.onSurfaceVariant,
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(Res.string.quick_toggles_animations_current_value, currentValueLabel),
                style = MaterialTheme.typography.bodySmall,
                color = AdbTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = statusLabel,
                style = MaterialTheme.typography.labelSmall,
                color = statusColor,
            )
        }

        Slider(
            value = control.draftValue,
            onValueChange = { raw ->
                onDraftChanged(control.key, snapScaleValue(raw))
            },
            enabled = enabled && control.status != AnimationScaleStatus.LOADING,
            valueRange = 0f..10f,
            steps = 19,
            modifier = Modifier.fillMaxWidth(),
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Dimensions.paddingXSmall),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            quickPresets().forEach { preset ->
                AdbOutlinedButton(
                    onClick = { onDraftChanged(control.key, preset) },
                    text = presetLabel(preset),
                    type = AdbButtonType.NEUTRAL,
                    size = AdbButtonSize.XSMALL,
                    enabled = enabled && control.status != AnimationScaleStatus.LOADING,
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AdbFilledButton(
                onClick = { onSet(control.key) },
                text = stringResource(Res.string.quick_toggles_animations_action_set),
                type = AdbButtonType.NEUTRAL,
                size = AdbButtonSize.SMALL,
                loading = control.status == AnimationScaleStatus.LOADING,
                enabled = enabled,
            )
        }

        if (!control.error.isNullOrBlank()) {
            Text(
                text = control.error,
                style = MaterialTheme.typography.bodySmall,
                color = AdbTheme.colorScheme.error,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }

        HorizontalDivider(color = AdbTheme.colorScheme.outline.copy(alpha = 0.15f))
    }
}

@Composable
private fun QuickToggleConfirmDialog(
    toggleId: QuickToggleId,
    targetState: QuickToggleState,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.quick_toggles_confirm_title)) },
        text = {
            Text(
                text = stringResource(
                    Res.string.quick_toggles_confirm_message,
                    toggleTitle(toggleId, fallback = toggleId.name),
                    stateLabel(targetState),
                )
            )
        },
        confirmButton = {
            AdbFilledButton(
                onClick = onConfirm,
                text = stringResource(AdbCommonStringRes.actionConfirm),
                type = AdbButtonType.DANGER,
                size = AdbButtonSize.MEDIUM,
            )
        },
        dismissButton = {
            AdbOutlinedButton(
                onClick = onDismiss,
                text = stringResource(AdbCommonStringRes.actionCancel),
                type = AdbButtonType.NEUTRAL,
                size = AdbButtonSize.MEDIUM,
            )
        },
    )
}

@Composable
private fun toggleTitle(
    toggleId: QuickToggleId,
    fallback: String,
): String {
    return when (toggleId) {
        QuickToggleId.WIFI -> stringResource(Res.string.quick_toggles_toggle_wifi)
        QuickToggleId.MOBILE_DATA -> stringResource(Res.string.quick_toggles_toggle_mobile_data)
        QuickToggleId.BLUETOOTH -> stringResource(Res.string.quick_toggles_toggle_bluetooth)
        QuickToggleId.AIRPLANE_MODE -> stringResource(Res.string.quick_toggles_toggle_airplane_mode)
        QuickToggleId.ANIMATIONS -> stringResource(Res.string.quick_toggles_toggle_animations)
        QuickToggleId.STAY_AWAKE -> stringResource(Res.string.quick_toggles_toggle_stay_awake)
    }.ifBlank { fallback }
}

@Composable
private fun stateLabel(state: QuickToggleState): String {
    return when (state) {
        QuickToggleState.ON -> stringResource(Res.string.quick_toggles_state_on)
        QuickToggleState.OFF -> stringResource(Res.string.quick_toggles_state_off)
        QuickToggleState.CUSTOM -> stringResource(Res.string.quick_toggles_state_custom)
        QuickToggleState.UNKNOWN -> stringResource(Res.string.quick_toggles_state_unknown)
    }
}

@Composable
private fun animationDescription(key: String): String {
    return when (key) {
        ANIMATION_WINDOW_SCALE_KEY -> stringResource(Res.string.quick_toggles_animations_desc_window)
        ANIMATION_TRANSITION_SCALE_KEY -> stringResource(Res.string.quick_toggles_animations_desc_transition)
        ANIMATION_ANIMATOR_SCALE_KEY -> stringResource(Res.string.quick_toggles_animations_desc_animator)
        else -> key
    }
}

private fun quickPresets(): List<Float> = listOf(0f, 0.5f, 1f, 2f, 5f, 10f)

private fun presetLabel(value: Float): String = "${formatScaleValue(value)}x"

private fun snapScaleValue(value: Float): Float {
    val snapped = (value / 0.5f).roundToInt() * 0.5f
    return snapped.coerceIn(0f, 10f)
}

private fun formatScaleValue(value: Float): String {
    return String.format(Locale.US, "%.1f", value)
}

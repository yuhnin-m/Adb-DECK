package com.adbdeck.feature.quicktoggles.ui

import adbdeck.feature.quick_toggles.generated.resources.Res
import adbdeck.feature.quick_toggles.generated.resources.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
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

    val primaryToggleIds = setOf(
        QuickToggleId.WIFI,
        QuickToggleId.MOBILE_DATA,
        QuickToggleId.BLUETOOTH,
        QuickToggleId.AIRPLANE_MODE,
        QuickToggleId.STAY_AWAKE,
    )
    val primaryItems = state.items.filter { it.id in primaryToggleIds }
    val animationsItem = state.items.firstOrNull { it.id == QuickToggleId.ANIMATIONS }
    val extraItems = state.items.filterNot {
        it.id in primaryToggleIds || it.id == QuickToggleId.ANIMATIONS
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
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

                if (primaryItems.isNotEmpty()) {
                    item(key = "primary_toggles_block") {
                        StandardTogglesGroupCard(
                            title = stringResource(Res.string.quick_toggles_group_primary_title),
                            items = primaryItems,
                            isDeviceAvailable = state.isDeviceAvailable,
                            refreshEnabled = state.isDeviceAvailable,
                            isRefreshRunning = state.isRefreshing,
                            onRefresh = component::onRefresh,
                            onRequestToggle = component::onRequestToggle,
                            onOpenSettings = component::onOpenSettings,
                        )
                    }
                }

                animationsItem?.let { item ->
                    item(key = item.id.name) {
                        AnimationsToggleCard(
                            item = item,
                            isDeviceAvailable = state.isDeviceAvailable,
                            onRefreshToggle = component::onRefreshToggle,
                            onOpenSettings = component::onOpenSettings,
                            onAnimationDraftChanged = component::onAnimationDraftChanged,
                            onSetAnimationScale = component::onSetAnimationScale,
                        )
                    }
                }

                extraItems.forEach { extraItem ->
                    item(key = "extra_${extraItem.id.name}") {
                        StandardTogglesGroupCard(
                            title = toggleTitle(extraItem.id, fallback = extraItem.title),
                            items = listOf(extraItem),
                            isDeviceAvailable = state.isDeviceAvailable,
                            refreshEnabled = state.isDeviceAvailable,
                            isRefreshRunning = false,
                            onRefresh = { component.onRefreshToggle(extraItem.id) },
                            onRequestToggle = component::onRequestToggle,
                            onOpenSettings = component::onOpenSettings,
                        )
                    }
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

/**
 * Групповой блок для стандартных тумблеров с внутренними строками.
 */
@Composable
private fun StandardTogglesGroupCard(
    title: String,
    items: List<ToggleItem>,
    isDeviceAvailable: Boolean,
    refreshEnabled: Boolean = false,
    isRefreshRunning: Boolean = false,
    onRefresh: (() -> Unit)? = null,
    onRequestToggle: (QuickToggleId, QuickToggleState) -> Unit,
    onOpenSettings: (QuickToggleId) -> Unit,
) {
    val headerTrailing: (@Composable RowScope.() -> Unit)? = onRefresh?.let { refreshAction ->
        {
            AdbOutlinedButton(
                onClick = refreshAction,
                enabled = refreshEnabled,
                loading = isRefreshRunning,
                text = stringResource(AdbCommonStringRes.actionRefresh),
                leadingIcon = if (isRefreshRunning) null else Icons.Outlined.Refresh,
                size = AdbButtonSize.SMALL,
            )
        }
    }

    AdbSectionCard(
        title = title,
        titleUppercase = false,
        containerColor = AdbTheme.colorScheme.surfaceVariant.copy(alpha = 0.32f),
        border = BorderStroke(
            width = 1.dp,
            color = AdbTheme.colorScheme.outline.copy(alpha = 0.25f),
        ),
        titleTextStyle = MaterialTheme.typography.titleMedium,
        subtitleTextStyle = MaterialTheme.typography.bodyMedium,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            horizontal = Dimensions.paddingLarge,
            vertical = Dimensions.paddingMedium,
        ),
        contentSpacing = Dimensions.paddingMedium,
        headerTrailing = headerTrailing,
        modifier = Modifier.fillMaxWidth(),
    ) {
        items.forEachIndexed { index, toggleItem ->
            StandardToggleRow(
                item = toggleItem,
                isDeviceAvailable = isDeviceAvailable,
                onRequestToggle = onRequestToggle,
                onOpenSettings = onOpenSettings,
            )
            if (index != items.lastIndex) {
                HorizontalDivider(color = AdbTheme.colorScheme.outline.copy(alpha = 0.15f))
            }
        }
    }
}

@Composable
private fun StandardToggleRow(
    item: ToggleItem,
    isDeviceAvailable: Boolean,
    onRequestToggle: (QuickToggleId, QuickToggleState) -> Unit,
    onOpenSettings: (QuickToggleId) -> Unit,
) {
    val currentStateLabel = if (item.state == QuickToggleState.UNKNOWN) {
        stringResource(Res.string.quick_toggles_state_no_data)
    } else {
        stateLabel(item.state)
    }
    val canToggle = isDeviceAvailable && item.canToggle && !item.isRunning

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Dimensions.paddingSmall),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Dimensions.paddingMedium),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = toggleTitle(item.id, fallback = item.title),
                    style = MaterialTheme.typography.titleSmall,
                    color = AdbTheme.colorScheme.onSurface,
                )
                Text(
                    text = stringResource(
                        Res.string.quick_toggles_label_current_state,
                        currentStateLabel,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = AdbTheme.colorScheme.onSurfaceVariant,
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(Dimensions.paddingSmall),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (item.isRunning) {
                    CircularProgressIndicator(
                        modifier = Modifier.padding(start = 8.dp),
                        strokeWidth = 2.dp,
                    )
                }

                Switch(
                    checked = item.state == QuickToggleState.ON,
                    onCheckedChange = { checked ->
                        val targetState = if (checked) {
                            QuickToggleState.ON
                        } else {
                            QuickToggleState.OFF
                        }
                        if (targetState != item.state) {
                            onRequestToggle(item.id, targetState)
                        }
                    },
                    enabled = canToggle,
                )
            }
        }

        if (!item.error.isNullOrBlank()) {
            Text(
                text = item.error,
                style = MaterialTheme.typography.bodySmall,
                color = AdbTheme.colorScheme.error,
            )
        }

        if (item.showOpenSettings) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                AdbOutlinedButton(
                    onClick = { onOpenSettings(item.id) },
                    text = stringResource(Res.string.quick_toggles_action_open_settings),
                    type = AdbButtonType.NEUTRAL,
                    size = AdbButtonSize.SMALL,
                )
            }
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
        subtitle = stringResource(Res.string.quick_toggles_animations_independent_title),
        titleUppercase = false,
        containerColor = AdbTheme.colorScheme.surfaceVariant.copy(alpha = 0.32f),
        titleTextStyle = MaterialTheme.typography.titleMedium,
        subtitleTextStyle = MaterialTheme.typography.bodyMedium,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            horizontal = Dimensions.paddingLarge,
            vertical = Dimensions.paddingMedium,
        ),
        contentSpacing = Dimensions.paddingMedium,
        border = BorderStroke(
            width = 1.dp,
            color = AdbTheme.colorScheme.outline.copy(alpha = 0.25f),
        ),
        headerTrailing = {
            AdbOutlinedButton(
                onClick = { onRefreshToggle(QuickToggleId.ANIMATIONS) },
                text = stringResource(Res.string.quick_toggles_animations_action_refresh),
                leadingIcon = Icons.Outlined.Refresh,
                type = AdbButtonType.NEUTRAL,
                size = AdbButtonSize.SMALL,
                enabled = canToggle,
            )
        },
        modifier = Modifier.fillMaxWidth(),
    ) {
        if (!isDeviceAvailable) {
            Text(
                text = stringResource(Res.string.quick_toggles_animations_no_device_hint),
                style = MaterialTheme.typography.bodySmall,
                color = AdbTheme.colorScheme.onSurfaceVariant,
            )
        }

        item.animationControls.forEachIndexed { index, control ->
            AnimationScaleControlRow(
                control = control,
                enabled = canToggle,
                onDraftChanged = onAnimationDraftChanged,
                onSet = onSetAnimationScale,
            )

            if (index != item.animationControls.lastIndex) {
                HorizontalDivider(color = AdbTheme.colorScheme.outline.copy(alpha = 0.15f))
            }
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
        verticalArrangement = Arrangement.spacedBy(Dimensions.paddingSmall),
    ) {
        Text(
            text = control.key,
            style = MaterialTheme.typography.titleSmall,
            color = AdbTheme.colorScheme.onSurface,
        )
        Text(
            text = animationDescription(control.key),
            style = MaterialTheme.typography.bodySmall,
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
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Dimensions.paddingXSmall)
                .graphicsLayer(scaleY = 0.9f),
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

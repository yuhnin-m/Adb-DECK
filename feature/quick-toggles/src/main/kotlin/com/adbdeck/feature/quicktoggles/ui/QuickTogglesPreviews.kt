package com.adbdeck.feature.quicktoggles.ui

import androidx.compose.runtime.Composable
import com.adbdeck.core.designsystem.AdbDeckTheme
import com.adbdeck.feature.quicktoggles.ANIMATION_ANIMATOR_SCALE_KEY
import com.adbdeck.feature.quicktoggles.ANIMATION_TRANSITION_SCALE_KEY
import com.adbdeck.feature.quicktoggles.ANIMATION_WINDOW_SCALE_KEY
import com.adbdeck.feature.quicktoggles.AnimationScaleControl
import com.adbdeck.feature.quicktoggles.AnimationScaleStatus
import com.adbdeck.feature.quicktoggles.QuickToggleId
import com.adbdeck.feature.quicktoggles.QuickToggleState
import com.adbdeck.feature.quicktoggles.QuickTogglesComponent
import com.adbdeck.feature.quicktoggles.QuickTogglesState
import com.adbdeck.feature.quicktoggles.ToggleItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.jetbrains.compose.ui.tooling.preview.Preview

/**
 * Стаб [QuickTogglesComponent] для превью.
 */
private class PreviewQuickTogglesComponent(
    initialState: QuickTogglesState,
) : QuickTogglesComponent {

    override val state: StateFlow<QuickTogglesState> = MutableStateFlow(initialState)

    override fun onRefresh() = Unit
    override fun onRefreshToggle(toggleId: QuickToggleId) = Unit
    override fun onRequestToggle(toggleId: QuickToggleId, targetState: QuickToggleState) = Unit
    override fun onAnimationDraftChanged(key: String, value: Float) = Unit
    override fun onSetAnimationScale(key: String) = Unit
    override fun onConfirmToggle() = Unit
    override fun onCancelToggle() = Unit
    override fun onOpenSettings(toggleId: QuickToggleId) = Unit
    override fun onDismissFeedback() = Unit
}

private val previewItems = listOf(
    ToggleItem(
        id = QuickToggleId.WIFI,
        title = "Wi-Fi",
        state = QuickToggleState.ON,
    ),
    ToggleItem(
        id = QuickToggleId.MOBILE_DATA,
        title = "Mobile data",
        state = QuickToggleState.OFF,
    ),
    ToggleItem(
        id = QuickToggleId.BLUETOOTH,
        title = "Bluetooth",
        state = QuickToggleState.UNKNOWN,
        error = "Permission denied",
        showOpenSettings = true,
    ),
    ToggleItem(
        id = QuickToggleId.AIRPLANE_MODE,
        title = "Airplane mode",
        state = QuickToggleState.OFF,
    ),
    ToggleItem(
        id = QuickToggleId.ANIMATIONS,
        title = "Animations",
        state = QuickToggleState.CUSTOM,
        animationControls = listOf(
            AnimationScaleControl(
                key = ANIMATION_WINDOW_SCALE_KEY,
                currentValue = null,
                draftValue = 1f,
                status = AnimationScaleStatus.OK,
            ),
            AnimationScaleControl(
                key = ANIMATION_TRANSITION_SCALE_KEY,
                currentValue = 0.5f,
                draftValue = 0.5f,
                status = AnimationScaleStatus.OK,
            ),
            AnimationScaleControl(
                key = ANIMATION_ANIMATOR_SCALE_KEY,
                currentValue = 1f,
                draftValue = 2f,
                status = AnimationScaleStatus.ERROR,
                error = "Unexpected value",
            ),
        ),
    ),
    ToggleItem(
        id = QuickToggleId.STAY_AWAKE,
        title = "Stay awake while charging",
        state = QuickToggleState.ON,
    ),
)

@Preview
@Composable
private fun QuickTogglesLightPreview() {
    AdbDeckTheme(isDarkTheme = false) {
        QuickTogglesScreen(
            component = PreviewQuickTogglesComponent(
                initialState = QuickTogglesState(
                    activeDeviceId = "emulator-5554",
                    items = previewItems,
                )
            )
        )
    }
}

@Preview
@Composable
private fun QuickTogglesDarkPreview() {
    AdbDeckTheme(isDarkTheme = true) {
        QuickTogglesScreen(
            component = PreviewQuickTogglesComponent(
                initialState = QuickTogglesState(
                    activeDeviceId = "emulator-5554",
                    items = previewItems,
                )
            )
        )
    }
}

@Preview
@Composable
private fun QuickTogglesNoDevicePreview() {
    AdbDeckTheme(isDarkTheme = false) {
        QuickTogglesScreen(
            component = PreviewQuickTogglesComponent(
                initialState = QuickTogglesState(activeDeviceId = null, items = previewItems),
            )
        )
    }
}

package com.adbdeck.feature.scrcpy.ui

import androidx.compose.runtime.Composable
import com.adbdeck.core.designsystem.AdbDeckTheme
import com.adbdeck.feature.scrcpy.ScrcpyComponent
import com.adbdeck.feature.scrcpy.ScrcpyConfig
import com.adbdeck.feature.scrcpy.ScrcpyFeedback
import com.adbdeck.feature.scrcpy.ScrcpyFps
import com.adbdeck.feature.scrcpy.ScrcpyInputMode
import com.adbdeck.feature.scrcpy.ScrcpyMaxResolution
import com.adbdeck.feature.scrcpy.ScrcpyProcessState
import com.adbdeck.feature.scrcpy.ScrcpyState
import com.adbdeck.feature.scrcpy.ScrcpyVideoCodec
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.jetbrains.compose.ui.tooling.preview.Preview

/**
 * Стаб [ScrcpyComponent] для использования в превью и тестах.
 * Все методы — no-op.
 */
class PreviewScrcpyComponent(
    initialState: ScrcpyState = ScrcpyState(),
) : ScrcpyComponent {
    override val state: StateFlow<ScrcpyState> = MutableStateFlow(initialState)
    override fun startScrcpy() = Unit
    override fun stopScrcpy() = Unit
    override fun onMaxResolutionChanged(resolution: ScrcpyMaxResolution) = Unit
    override fun onFpsChanged(fps: ScrcpyFps) = Unit
    override fun onBitrateChanged(bitrate: String) = Unit
    override fun onAllowInputChanged(enabled: Boolean) = Unit
    override fun onTurnScreenOffChanged(enabled: Boolean) = Unit
    override fun onShowTouchesChanged(enabled: Boolean) = Unit
    override fun onStayAwakeChanged(enabled: Boolean) = Unit
    override fun onFullscreenChanged(enabled: Boolean) = Unit
    override fun onAlwaysOnTopChanged(enabled: Boolean) = Unit
    override fun onBorderlessChanged(enabled: Boolean) = Unit
    override fun onWindowWidthChanged(width: String) = Unit
    override fun onWindowHeightChanged(height: String) = Unit
    override fun onVideoCodecChanged(codec: ScrcpyVideoCodec) = Unit
    override fun onKeyboardModeChanged(mode: ScrcpyInputMode) = Unit
    override fun onMouseModeChanged(mode: ScrcpyInputMode) = Unit
    override fun onOpenSettings() = Unit
    override fun onDismissFeedback() = Unit
}

// ── Превью: светлая тема, IDLE ────────────────────────────────────────────────

@Preview
@Composable
private fun ScrcpyScreenLightPreview() {
    AdbDeckTheme(isDarkTheme = false) {
        ScrcpyScreen(
            component = PreviewScrcpyComponent(
                ScrcpyState(
                    activeDeviceId = "emulator-5554",
                    processState = ScrcpyProcessState.IDLE,
                    config = ScrcpyConfig(
                        fps = ScrcpyFps.FPS_60,
                        maxResolution = ScrcpyMaxResolution.P1080,
                    ),
                )
            )
        )
    }
}

// ── Превью: тёмная тема, IDLE ─────────────────────────────────────────────────

@Preview
@Composable
private fun ScrcpyScreenDarkPreview() {
    AdbDeckTheme(isDarkTheme = true) {
        ScrcpyScreen(
            component = PreviewScrcpyComponent(
                ScrcpyState(
                    activeDeviceId = "R58N123ABC",
                    processState = ScrcpyProcessState.IDLE,
                )
            )
        )
    }
}

// ── Превью: scrcpy работает (RUNNING) ────────────────────────────────────────

@Preview
@Composable
private fun ScrcpyScreenRunningPreview() {
    AdbDeckTheme(isDarkTheme = false) {
        ScrcpyScreen(
            component = PreviewScrcpyComponent(
                ScrcpyState(
                    activeDeviceId = "emulator-5554",
                    processState = ScrcpyProcessState.RUNNING,
                    feedback = ScrcpyFeedback("scrcpy started"),
                )
            )
        )
    }
}

// ── Превью: нет устройства ────────────────────────────────────────────────────

@Preview
@Composable
private fun ScrcpyNoDevicePreview() {
    AdbDeckTheme(isDarkTheme = false) {
        ScrcpyScreen(
            component = PreviewScrcpyComponent(
                ScrcpyState(activeDeviceId = null)
            )
        )
    }
}

// ── Превью: не настроен ───────────────────────────────────────────────────────

@Preview
@Composable
private fun ScrcpyNotConfiguredPreview() {
    AdbDeckTheme(isDarkTheme = false) {
        ScrcpyScreen(
            component = PreviewScrcpyComponent(
                ScrcpyState(
                    activeDeviceId = "emulator-5554",
                    isConfigured = false,
                )
            )
        )
    }
}

// ── Превью: ошибка запуска ────────────────────────────────────────────────────

@Preview
@Composable
private fun ScrcpyErrorPreview() {
    AdbDeckTheme(isDarkTheme = true) {
        ScrcpyScreen(
            component = PreviewScrcpyComponent(
                ScrcpyState(
                    activeDeviceId = "emulator-5554",
                    processState = ScrcpyProcessState.ERROR,
                    feedback = ScrcpyFeedback("Failed to start scrcpy: exit=1", isError = true),
                )
            )
        )
    }
}

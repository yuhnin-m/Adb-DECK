package com.adbdeck.feature.settings.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.adbdeck.core.designsystem.AdbDeckTheme
import com.adbdeck.core.settings.AppTheme
import com.adbdeck.feature.settings.SettingsComponent
import com.adbdeck.feature.settings.SettingsUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.jetbrains.compose.ui.tooling.preview.Preview

private class SettingsPreviewComponent : SettingsComponent {
    override val state: StateFlow<SettingsUiState> = MutableStateFlow(
        SettingsUiState(
            adbPath = "/opt/homebrew/bin/adb",
            adbCheckResult = "✓ Доступен: Android Debug Bridge version 1.0.41",
            isCheckingAdb = false,
            isSaved = true,
            currentTheme = AppTheme.SYSTEM,
            logcatCompactMode = true,
            logcatShowDate = false,
            logcatShowTime = true,
            logcatShowMillis = true,
            logcatColoredLevels = true,
            logcatMaxBufferedLines = 5000,
            logcatAutoScroll = true,
        )
    )

    override fun onAdbPathChanged(path: String) = Unit
    override fun onSave() = Unit
    override fun onCheckAdb() = Unit
    override fun onThemeChanged(theme: AppTheme) = Unit
    override fun onLogcatCompactModeChanged(value: Boolean) = Unit
    override fun onLogcatShowDateChanged(value: Boolean) = Unit
    override fun onLogcatShowTimeChanged(value: Boolean) = Unit
    override fun onLogcatShowMillisChanged(value: Boolean) = Unit
    override fun onLogcatColoredLevelsChanged(value: Boolean) = Unit
    override fun onLogcatMaxBufferedLinesChanged(lines: Int) = Unit
    override fun onLogcatAutoScrollChanged(value: Boolean) = Unit
    override fun onLogcatFontFamilyChanged(family: String) = Unit
    override fun onLogcatFontSizeChanged(size: Int) = Unit
}

@Composable
private fun SettingsPreviewBody(isDarkTheme: Boolean) {
    val component = SettingsPreviewComponent()
    AdbDeckTheme(isDarkTheme = isDarkTheme) {
        Surface(modifier = Modifier.fillMaxSize()) {
            SettingsScreen(component = component)
        }
    }
}

@Preview
@Composable
private fun SettingsLightPreview() {
    SettingsPreviewBody(isDarkTheme = false)
}

@Preview
@Composable
private fun SettingsDarkPreview() {
    SettingsPreviewBody(isDarkTheme = true)
}

package com.adbdeck.feature.settings

import com.adbdeck.core.settings.AppTheme
import com.adbdeck.core.settings.AppLanguage
import com.adbdeck.core.settings.DEFAULT_ADB_EXECUTABLE

/**
 * Типизированное состояние проверки инструмента (adb / bundletool).
 */
sealed class ToolCheckState {
    data object Idle : ToolCheckState()
    data object Checking : ToolCheckState()
    data class Success(val message: String) : ToolCheckState()
    data class Error(val message: String) : ToolCheckState()
}

/**
 * Баннер обратной связи в настройках.
 */
data class SettingsFeedback(
    val message: String,
    val isError: Boolean,
)

/**
 * Состояние UI экрана настроек.
 */
data class SettingsUiState(
    val adbPath: String = DEFAULT_ADB_EXECUTABLE,
    val isAdbAutoDetecting: Boolean = false,
    val adbAutoDetectCandidates: List<String> = emptyList(),
    val bundletoolPath: String = "bundletool",
    val scrcpyPath: String = "scrcpy",
    val adbCheckState: ToolCheckState = ToolCheckState.Idle,
    val bundletoolCheckState: ToolCheckState = ToolCheckState.Idle,
    val scrcpyCheckState: ToolCheckState = ToolCheckState.Idle,
    val saveFeedback: SettingsFeedback? = null,
    val isSaving: Boolean = false,
    val hasPendingChanges: Boolean = false,
    val currentTheme: AppTheme = AppTheme.SYSTEM,
    val currentLanguage: AppLanguage = AppLanguage.SYSTEM,

    // Logcat
    val logcatCompactMode: Boolean = true,
    val logcatShowDate: Boolean = false,
    val logcatShowTime: Boolean = true,
    val logcatShowMillis: Boolean = true,
    val logcatColoredLevels: Boolean = true,
    val logcatMaxBufferedLines: Int = 5_000,
    val logcatAutoScroll: Boolean = true,
    val logcatFontFamily: String = "MONOSPACE",
    val logcatFontSizeSp: Int = 12,
)

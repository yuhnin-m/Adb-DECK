package com.adbdeck.feature.settings

import com.adbdeck.core.adb.api.AdbCheckResult
import com.adbdeck.core.adb.api.AdbClient
import com.adbdeck.core.settings.AppTheme
import com.adbdeck.core.settings.SettingsRepository
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.lifecycle.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Реализация [SettingsComponent].
 *
 * Инициализирует состояние из [SettingsRepository] при создании.
 * Настройки logcat сохраняются немедленно при каждом изменении (без кнопки «Сохранить»).
 *
 * @param componentContext   Контекст Decompose-компонента.
 * @param adbClient          ADB-клиент для проверки доступности.
 * @param settingsRepository Репозиторий для чтения и записи настроек.
 */
class DefaultSettingsComponent(
    componentContext: ComponentContext,
    private val adbClient: AdbClient,
    private val settingsRepository: SettingsRepository,
) : SettingsComponent, ComponentContext by componentContext {

    private val scope = coroutineScope()

    private val _state = MutableStateFlow(
        settingsRepository.getSettings().let { s ->
            SettingsUiState(
                adbPath = s.adbPath,
                currentTheme = s.theme,
                logcatCompactMode = s.logcatCompactMode,
                logcatShowDate = s.logcatShowDate,
                logcatShowTime = s.logcatShowTime,
                logcatShowMillis = s.logcatShowMillis,
                logcatColoredLevels = s.logcatColoredLevels,
                logcatMaxBufferedLines = s.logcatMaxBufferedLines,
                logcatAutoScroll = s.logcatAutoScroll,
                logcatFontFamily = s.logcatFontFamily,
                logcatFontSizeSp = s.logcatFontSizeSp,
            )
        }
    )
    override val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    // ── ADB path & theme ────────────────────────────────────────

    override fun onAdbPathChanged(path: String) {
        _state.update { it.copy(adbPath = path, adbCheckResult = "", isSaved = false) }
    }

    override fun onSave() {
        scope.launch {
            val current = settingsRepository.getSettings()
            val updated = current.copy(
                adbPath = _state.value.adbPath,
                theme = _state.value.currentTheme,
            )
            settingsRepository.saveSettings(updated)
            _state.update { it.copy(isSaved = true) }
        }
    }

    override fun onCheckAdb() {
        if (_state.value.isCheckingAdb) return
        scope.launch {
            _state.update { it.copy(isCheckingAdb = true, adbCheckResult = "") }
            val result = adbClient.checkAvailability(_state.value.adbPath)

            val resultText = when (result) {
                is AdbCheckResult.Available -> "✓ Доступен: ${result.version}"
                is AdbCheckResult.NotAvailable -> "✗ Не найден: ${result.reason}"
            }
            _state.update { it.copy(isCheckingAdb = false, adbCheckResult = resultText) }
        }
    }

    override fun onThemeChanged(theme: AppTheme) {
        _state.update { it.copy(currentTheme = theme, isSaved = false) }
    }

    // ── Logcat settings (immediate save) ────────────────────────

    override fun onLogcatCompactModeChanged(value: Boolean) {
        _state.update { it.copy(logcatCompactMode = value) }
        saveLogcatSettings()
    }

    override fun onLogcatShowDateChanged(value: Boolean) {
        _state.update { it.copy(logcatShowDate = value) }
        saveLogcatSettings()
    }

    override fun onLogcatShowTimeChanged(value: Boolean) {
        _state.update { it.copy(logcatShowTime = value) }
        saveLogcatSettings()
    }

    override fun onLogcatShowMillisChanged(value: Boolean) {
        _state.update { it.copy(logcatShowMillis = value) }
        saveLogcatSettings()
    }

    override fun onLogcatColoredLevelsChanged(value: Boolean) {
        _state.update { it.copy(logcatColoredLevels = value) }
        saveLogcatSettings()
    }

    override fun onLogcatMaxBufferedLinesChanged(lines: Int) {
        val coerced = lines.coerceAtLeast(100)
        _state.update { it.copy(logcatMaxBufferedLines = coerced) }
        saveLogcatSettings()
    }

    override fun onLogcatAutoScrollChanged(value: Boolean) {
        _state.update { it.copy(logcatAutoScroll = value) }
        saveLogcatSettings()
    }

    override fun onLogcatFontFamilyChanged(family: String) {
        _state.update { it.copy(logcatFontFamily = family) }
        saveLogcatSettings()
    }

    override fun onLogcatFontSizeChanged(size: Int) {
        val coerced = size.coerceIn(8, 24)
        _state.update { it.copy(logcatFontSizeSp = coerced) }
        saveLogcatSettings()
    }

    /** Сохраняет только logcat-поля в [SettingsRepository] асинхронно. */
    private fun saveLogcatSettings() {
        scope.launch {
            val ui = _state.value
            val current = settingsRepository.getSettings()
            settingsRepository.saveSettings(
                current.copy(
                    logcatCompactMode = ui.logcatCompactMode,
                    logcatShowDate = ui.logcatShowDate,
                    logcatShowTime = ui.logcatShowTime,
                    logcatShowMillis = ui.logcatShowMillis,
                    logcatColoredLevels = ui.logcatColoredLevels,
                    logcatMaxBufferedLines = ui.logcatMaxBufferedLines,
                    logcatAutoScroll = ui.logcatAutoScroll,
                    logcatFontFamily = ui.logcatFontFamily,
                    logcatFontSizeSp = ui.logcatFontSizeSp,
                )
            )
        }
    }
}

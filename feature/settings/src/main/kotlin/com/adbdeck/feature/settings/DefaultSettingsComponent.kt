package com.adbdeck.feature.settings

import adbdeck.feature.settings.generated.resources.Res
import adbdeck.feature.settings.generated.resources.settings_feedback_logcat_save_failed
import adbdeck.feature.settings.generated.resources.settings_feedback_save_failed
import adbdeck.feature.settings.generated.resources.settings_feedback_saved
import adbdeck.feature.settings.generated.resources.settings_status_available
import adbdeck.feature.settings.generated.resources.settings_status_not_found
import com.adbdeck.core.adb.api.adb.AdbCheckResult
import com.adbdeck.core.adb.api.adb.AdbClient
import com.adbdeck.core.adb.api.adb.BundletoolCheckResult
import com.adbdeck.core.adb.api.adb.BundletoolClient
import com.adbdeck.core.settings.AppSettings
import com.adbdeck.core.settings.AppLanguage
import com.adbdeck.core.settings.AppTheme
import com.adbdeck.core.settings.SettingsRepository
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.lifecycle.coroutines.coroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.jetbrains.compose.resources.getString

/**
 * Реализация [SettingsComponent].
 *
 * Что исправлено:
 * - проверка bundletool вынесена в [BundletoolClient] (feature не работает с ProcessRunner напрямую)
 * - типизированные статусы проверок (`ToolCheckState`) вместо "✓/✗"-строк
 * - сериализация сохранений через [saveMutex]
 * - debounce для авто-сохранения logcat-настроек
 * - явный флаг [SettingsUiState.hasPendingChanges] для Sidebar
 */
class DefaultSettingsComponent(
    componentContext: ComponentContext,
    private val adbClient: AdbClient,
    private val bundletoolClient: BundletoolClient,
    private val settingsRepository: SettingsRepository,
) : SettingsComponent, ComponentContext by componentContext {

    private val scope = coroutineScope()

    private val saveMutex = Mutex()
    private var persistedEditable = EditableSettingsSnapshot.from(settingsRepository.getSettings())

    private var adbCheckJob: Job? = null
    private var bundletoolCheckJob: Job? = null
    private var logcatSaveJob: Job? = null
    private var feedbackJob: Job? = null

    private val _state = MutableStateFlow(
        settingsRepository.getSettings().toUiState()
    )
    override val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    init {
        observeSettings()
    }

    override fun onAdbPathChanged(path: String) {
        _state.update { current ->
            current.copy(
                adbPath = path,
                adbCheckState = ToolCheckState.Idle,
                hasPendingChanges = hasPendingChanges(
                    adbPath = path,
                    bundletoolPath = current.bundletoolPath,
                    theme = current.currentTheme,
                    language = current.currentLanguage,
                ),
            )
        }
    }

    override fun onBundletoolPathChanged(path: String) {
        _state.update { current ->
            current.copy(
                bundletoolPath = path,
                bundletoolCheckState = ToolCheckState.Idle,
                hasPendingChanges = hasPendingChanges(
                    adbPath = current.adbPath,
                    bundletoolPath = path,
                    theme = current.currentTheme,
                    language = current.currentLanguage,
                ),
            )
        }
    }

    override fun onSave() {
        if (_state.value.isSaving) return
        scope.launch {
            _state.update { it.copy(isSaving = true) }

            val result = saveMutex.withLock {
                runCatching {
                    val currentRepo = settingsRepository.getSettings()
                    val currentUi = _state.value
                    val updated = currentRepo.copy(
                        adbPath = currentUi.adbPath,
                        bundletoolPath = currentUi.bundletoolPath,
                        theme = currentUi.currentTheme,
                        language = currentUi.currentLanguage,
                    )
                    settingsRepository.saveSettings(updated)
                    persistedEditable = EditableSettingsSnapshot.from(updated)
                }
            }

            result.fold(
                onSuccess = {
                    _state.update { current ->
                        current.copy(
                            isSaving = false,
                            hasPendingChanges = hasPendingChanges(
                                adbPath = current.adbPath,
                                bundletoolPath = current.bundletoolPath,
                                theme = current.currentTheme,
                                language = current.currentLanguage,
                            ),
                        )
                    }
                    showFeedback(
                        message = getString(Res.string.settings_feedback_saved),
                        isError = false,
                    )
                },
                onFailure = { error ->
                    _state.update { it.copy(isSaving = false) }
                    showFeedback(
                        message = error.message ?: getString(Res.string.settings_feedback_save_failed),
                        isError = true,
                    )
                },
            )
        }
    }

    override fun onCheckAdb() {
        if (adbCheckJob?.isActive == true) return
        adbCheckJob = scope.launch {
            val requestedPath = _state.value.adbPath
            _state.update { it.copy(adbCheckState = ToolCheckState.Checking) }

            val nextState = when (val result = adbClient.checkAvailability(requestedPath)) {
                is AdbCheckResult.Available ->
                    ToolCheckState.Success(
                        getString(
                            Res.string.settings_status_available,
                            result.version,
                        )
                    )

                is AdbCheckResult.NotAvailable ->
                    ToolCheckState.Error(
                        getString(
                            Res.string.settings_status_not_found,
                            result.reason,
                        )
                    )
            }

            _state.update { current ->
                if (current.adbPath == requestedPath) {
                    current.copy(adbCheckState = nextState)
                } else {
                    current
                }
            }
            adbCheckJob = null
        }
    }

    override fun onCheckBundletool() {
        if (bundletoolCheckJob?.isActive == true) return
        bundletoolCheckJob = scope.launch {
            val requestedPath = _state.value.bundletoolPath
            _state.update { it.copy(bundletoolCheckState = ToolCheckState.Checking) }

            val nextState = when (val result = bundletoolClient.checkAvailability(requestedPath)) {
                is BundletoolCheckResult.Available ->
                    ToolCheckState.Success(
                        getString(
                            Res.string.settings_status_available,
                            result.version,
                        )
                    )

                is BundletoolCheckResult.NotAvailable ->
                    ToolCheckState.Error(
                        getString(
                            Res.string.settings_status_not_found,
                            result.reason,
                        )
                    )
            }

            _state.update { current ->
                if (current.bundletoolPath == requestedPath) {
                    current.copy(bundletoolCheckState = nextState)
                } else {
                    current
                }
            }
            bundletoolCheckJob = null
        }
    }

    override fun onDismissFeedback() {
        feedbackJob?.cancel()
        feedbackJob = null
        _state.update { it.copy(saveFeedback = null) }
    }

    override fun onThemeChanged(theme: AppTheme) {
        _state.update { current ->
            current.copy(
                currentTheme = theme,
                hasPendingChanges = hasPendingChanges(
                    adbPath = current.adbPath,
                    bundletoolPath = current.bundletoolPath,
                    theme = theme,
                    language = current.currentLanguage,
                ),
            )
        }
    }

    override fun onLanguageChanged(language: AppLanguage) {
        _state.update { current ->
            current.copy(
                currentLanguage = language,
                hasPendingChanges = hasPendingChanges(
                    adbPath = current.adbPath,
                    bundletoolPath = current.bundletoolPath,
                    theme = current.currentTheme,
                    language = language,
                ),
            )
        }
    }

    override fun onLogcatCompactModeChanged(value: Boolean) {
        updateLogcatAndScheduleSave { it.copy(logcatCompactMode = value) }
    }

    override fun onLogcatShowDateChanged(value: Boolean) {
        updateLogcatAndScheduleSave { it.copy(logcatShowDate = value) }
    }

    override fun onLogcatShowTimeChanged(value: Boolean) {
        updateLogcatAndScheduleSave { it.copy(logcatShowTime = value) }
    }

    override fun onLogcatShowMillisChanged(value: Boolean) {
        updateLogcatAndScheduleSave { it.copy(logcatShowMillis = value) }
    }

    override fun onLogcatColoredLevelsChanged(value: Boolean) {
        updateLogcatAndScheduleSave { it.copy(logcatColoredLevels = value) }
    }

    override fun onLogcatMaxBufferedLinesChanged(lines: Int) {
        val coerced = lines.coerceAtLeast(100)
        updateLogcatAndScheduleSave { it.copy(logcatMaxBufferedLines = coerced) }
    }

    override fun onLogcatAutoScrollChanged(value: Boolean) {
        updateLogcatAndScheduleSave { it.copy(logcatAutoScroll = value) }
    }

    override fun onLogcatFontFamilyChanged(family: String) {
        updateLogcatAndScheduleSave { it.copy(logcatFontFamily = family) }
    }

    override fun onLogcatFontSizeChanged(size: Int) {
        val coerced = size.coerceIn(8, 24)
        updateLogcatAndScheduleSave { it.copy(logcatFontSizeSp = coerced) }
    }

    private fun observeSettings() {
        scope.launch {
            settingsRepository.settingsFlow.collect { saved ->
                persistedEditable = EditableSettingsSnapshot.from(saved)
                _state.update { current ->
                    val shouldSyncEditable = !current.hasPendingChanges && !current.isSaving
                    val merged = current.copy(
                        adbPath = if (shouldSyncEditable) saved.adbPath else current.adbPath,
                        bundletoolPath = if (shouldSyncEditable) saved.bundletoolPath else current.bundletoolPath,
                        currentTheme = if (shouldSyncEditable) saved.theme else current.currentTheme,
                        currentLanguage = if (shouldSyncEditable) saved.language else current.currentLanguage,
                        logcatCompactMode = saved.logcatCompactMode,
                        logcatShowDate = saved.logcatShowDate,
                        logcatShowTime = saved.logcatShowTime,
                        logcatShowMillis = saved.logcatShowMillis,
                        logcatColoredLevels = saved.logcatColoredLevels,
                        logcatMaxBufferedLines = saved.logcatMaxBufferedLines,
                        logcatAutoScroll = saved.logcatAutoScroll,
                        logcatFontFamily = saved.logcatFontFamily,
                        logcatFontSizeSp = saved.logcatFontSizeSp,
                    )
                    merged.copy(
                        hasPendingChanges = hasPendingChanges(
                            adbPath = merged.adbPath,
                            bundletoolPath = merged.bundletoolPath,
                            theme = merged.currentTheme,
                            language = merged.currentLanguage,
                        ),
                    )
                }
            }
        }
    }

    private fun updateLogcatAndScheduleSave(update: (SettingsUiState) -> SettingsUiState) {
        _state.update(update)
        scheduleLogcatSave()
    }

    private fun scheduleLogcatSave() {
        logcatSaveJob?.cancel()
        logcatSaveJob = scope.launch {
            delay(LOGCAT_SAVE_DEBOUNCE_MS)
            persistLogcatSettings()
        }
    }

    private suspend fun persistLogcatSettings() {
        val snapshot = _state.value
        val result = saveMutex.withLock {
            runCatching {
                val current = settingsRepository.getSettings()
                settingsRepository.saveSettings(
                    current.copy(
                        logcatCompactMode = snapshot.logcatCompactMode,
                        logcatShowDate = snapshot.logcatShowDate,
                        logcatShowTime = snapshot.logcatShowTime,
                        logcatShowMillis = snapshot.logcatShowMillis,
                        logcatColoredLevels = snapshot.logcatColoredLevels,
                        logcatMaxBufferedLines = snapshot.logcatMaxBufferedLines,
                        logcatAutoScroll = snapshot.logcatAutoScroll,
                        logcatFontFamily = snapshot.logcatFontFamily,
                        logcatFontSizeSp = snapshot.logcatFontSizeSp,
                    ),
                )
            }
        }

        result.onFailure { error ->
            showFeedback(
                message = error.message ?: getString(Res.string.settings_feedback_logcat_save_failed),
                isError = true,
            )
        }
    }

    private fun hasPendingChanges(
        adbPath: String,
        bundletoolPath: String,
        theme: AppTheme,
        language: AppLanguage,
    ): Boolean {
        return adbPath != persistedEditable.adbPath ||
            bundletoolPath != persistedEditable.bundletoolPath ||
            theme != persistedEditable.theme ||
            language != persistedEditable.language
    }

    private fun showFeedback(message: String, isError: Boolean) {
        feedbackJob?.cancel()
        _state.update { it.copy(saveFeedback = SettingsFeedback(message = message, isError = isError)) }
        feedbackJob = scope.launch {
            delay(FEEDBACK_AUTO_HIDE_MS)
            _state.update { current ->
                if (current.saveFeedback?.message == message) {
                    current.copy(saveFeedback = null)
                } else {
                    current
                }
            }
        }
    }

    private fun AppSettings.toUiState(): SettingsUiState = SettingsUiState(
        adbPath = adbPath,
        bundletoolPath = bundletoolPath,
        currentTheme = theme,
        currentLanguage = language,
        hasPendingChanges = false,
        logcatCompactMode = logcatCompactMode,
        logcatShowDate = logcatShowDate,
        logcatShowTime = logcatShowTime,
        logcatShowMillis = logcatShowMillis,
        logcatColoredLevels = logcatColoredLevels,
        logcatMaxBufferedLines = logcatMaxBufferedLines,
        logcatAutoScroll = logcatAutoScroll,
        logcatFontFamily = logcatFontFamily,
        logcatFontSizeSp = logcatFontSizeSp,
    )

    private data class EditableSettingsSnapshot(
        val adbPath: String,
        val bundletoolPath: String,
        val theme: AppTheme,
        val language: AppLanguage,
    ) {
        companion object {
            fun from(settings: AppSettings): EditableSettingsSnapshot = EditableSettingsSnapshot(
                adbPath = settings.adbPath,
                bundletoolPath = settings.bundletoolPath,
                theme = settings.theme,
                language = settings.language,
            )
        }
    }

    private companion object {
        const val LOGCAT_SAVE_DEBOUNCE_MS = 350L
        const val FEEDBACK_AUTO_HIDE_MS = 3_000L
    }
}

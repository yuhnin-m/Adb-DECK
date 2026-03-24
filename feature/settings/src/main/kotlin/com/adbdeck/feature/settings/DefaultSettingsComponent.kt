package com.adbdeck.feature.settings

import adbdeck.feature.settings.generated.resources.Res
import adbdeck.feature.settings.generated.resources.settings_feedback_adb_autodetect_applied
import adbdeck.feature.settings.generated.resources.settings_feedback_adb_autodetect_multiple
import adbdeck.feature.settings.generated.resources.settings_feedback_adb_autodetect_not_found
import adbdeck.feature.settings.generated.resources.settings_feedback_logcat_save_failed
import adbdeck.feature.settings.generated.resources.settings_feedback_updates_available
import adbdeck.feature.settings.generated.resources.settings_feedback_updates_check_failed
import adbdeck.feature.settings.generated.resources.settings_feedback_updates_latest
import adbdeck.feature.settings.generated.resources.settings_feedback_save_failed
import adbdeck.feature.settings.generated.resources.settings_feedback_saved
import adbdeck.feature.settings.generated.resources.settings_status_available
import adbdeck.feature.settings.generated.resources.settings_status_launch_failed
import adbdeck.feature.settings.generated.resources.settings_status_not_found
import adbdeck.feature.settings.generated.resources.settings_status_wrong_tool
import adbdeck.feature.settings.generated.resources.settings_tool_adb_title
import adbdeck.feature.settings.generated.resources.settings_tool_bundletool_title
import adbdeck.feature.settings.generated.resources.settings_tool_scrcpy_title
import com.adbdeck.core.adb.api.ToolCheckFailureKind
import com.adbdeck.core.adb.api.adb.AdbCheckResult
import com.adbdeck.core.adb.api.adb.AdbClient
import com.adbdeck.core.adb.api.adb.BundletoolCheckResult
import com.adbdeck.core.adb.api.adb.BundletoolClient
import com.adbdeck.core.adb.api.scrcpy.ScrcpyCheckResult
import com.adbdeck.core.adb.api.scrcpy.ScrcpyClient
import com.adbdeck.core.settings.AppSettings
import com.adbdeck.core.settings.AppLanguage
import com.adbdeck.core.settings.AppTheme
import com.adbdeck.core.settings.SettingsRepository
import com.adbdeck.core.utils.runCatchingPreserveCancellation
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.lifecycle.coroutines.coroutineScope
import kotlinx.coroutines.Dispatchers
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
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.StringResource
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
    private val scrcpyClient: ScrcpyClient,
    private val settingsRepository: SettingsRepository,
    private val checkForAppUpdates: suspend () -> Result<Boolean> = { Result.success(false) },
) : SettingsComponent, ComponentContext by componentContext {

    private val scope = coroutineScope()

    private val saveMutex = Mutex()
    private var persistedEditable = EditableSettingsSnapshot.from(settingsRepository.getSettings())

    private var adbCheckJob: Job? = null
    private var adbAutoDetectJob: Job? = null
    private var bundletoolCheckJob: Job? = null
    private var scrcpyCheckJob: Job? = null
    private var appUpdateCheckJob: Job? = null
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
                isAdbAutoDetecting = false,
                adbAutoDetectCandidates = emptyList(),
                adbCheckState = ToolCheckState.Idle,
                hasPendingChanges = hasPendingChanges(
                    adbPath = path,
                    bundletoolPath = current.bundletoolPath,
                    scrcpyPath = current.scrcpyPath,
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
                    scrcpyPath = current.scrcpyPath,
                    theme = current.currentTheme,
                    language = current.currentLanguage,
                ),
            )
        }
    }

    override fun onScrcpyPathChanged(path: String) {
        _state.update { current ->
            current.copy(
                scrcpyPath = path,
                scrcpyCheckState = ToolCheckState.Idle,
                hasPendingChanges = hasPendingChanges(
                    adbPath = current.adbPath,
                    bundletoolPath = current.bundletoolPath,
                    scrcpyPath = path,
                    theme = current.currentTheme,
                    language = current.currentLanguage,
                ),
            )
        }
    }

    override fun onCheckScrcpy() {
        if (scrcpyCheckJob?.isActive == true) return
        scrcpyCheckJob = scope.launch {
            val requestedPath = _state.value.scrcpyPath.ifBlank { "scrcpy" }
            _state.update { it.copy(scrcpyCheckState = ToolCheckState.Checking) }

            val nextState = when (val result = scrcpyClient.checkAvailability(requestedPath)) {
                is ScrcpyCheckResult.Available ->
                    ToolCheckState.Success(
                        getString(
                            Res.string.settings_status_available,
                            result.version.ifBlank { requestedPath },
                        )
                    )

                is ScrcpyCheckResult.NotAvailable ->
                    ToolCheckState.Error(
                        buildToolCheckErrorMessage(
                            toolTitleRes = Res.string.settings_tool_scrcpy_title,
                            kind = result.kind,
                            rawDetails = result.reason,
                            fallbackDetails = requestedPath,
                        )
                    )
            }

            _state.update { current ->
                if (current.scrcpyPath == requestedPath) {
                    current.copy(scrcpyCheckState = nextState)
                } else {
                    current
                }
            }
            scrcpyCheckJob = null
        }
    }

    override fun onSave() {
        if (_state.value.isSaving) return
        scope.launch {
            _state.update { it.copy(isSaving = true) }

            val result = saveMutex.withLock {
                runCatchingPreserveCancellation {
                    val currentRepo = settingsRepository.getSettings()
                    val currentUi = _state.value
                    val updated = currentRepo.copy(
                        adbPath = currentUi.adbPath,
                        bundletoolPath = currentUi.bundletoolPath,
                        scrcpyPath = currentUi.scrcpyPath,
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
                                scrcpyPath = current.scrcpyPath,
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
                        buildToolCheckErrorMessage(
                            toolTitleRes = Res.string.settings_tool_adb_title,
                            kind = result.kind,
                            rawDetails = result.reason,
                            fallbackDetails = requestedPath,
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

    override fun onAutoDetectAdb() {
        if (adbAutoDetectJob?.isActive == true) return

        adbAutoDetectJob = scope.launch {
            _state.update {
                it.copy(
                    isAdbAutoDetecting = true,
                    adbAutoDetectCandidates = emptyList(),
                )
            }

            val candidates = runCatchingPreserveCancellation {
                withContext(Dispatchers.IO) {
                    AdbAutoDetect.detectCandidates()
                }
            }.getOrElse {
                emptyList()
            }

            when (candidates.size) {
                0 -> {
                    _state.update {
                        it.copy(
                            isAdbAutoDetecting = false,
                            adbAutoDetectCandidates = emptyList(),
                        )
                    }
                    showFeedback(
                        message = getString(Res.string.settings_feedback_adb_autodetect_not_found),
                        isError = true,
                    )
                }

                1 -> {
                    val detectedPath = candidates.first()
                    onAdbPathChanged(detectedPath)
                    showFeedback(
                        message = getString(
                            Res.string.settings_feedback_adb_autodetect_applied,
                            detectedPath,
                        ),
                        isError = false,
                    )
                }

                else -> {
                    _state.update {
                        it.copy(
                            isAdbAutoDetecting = false,
                            adbAutoDetectCandidates = candidates,
                        )
                    }
                    showFeedback(
                        message = getString(
                            Res.string.settings_feedback_adb_autodetect_multiple,
                            candidates.size,
                        ),
                        isError = false,
                    )
                }
            }

            adbAutoDetectJob = null
        }
    }

    override fun onSelectAutoDetectedAdbPath(path: String) {
        val normalizedPath = path.trim()
        if (normalizedPath.isBlank()) return

        onAdbPathChanged(normalizedPath)
        scope.launch {
            showFeedback(
                message = getString(
                    Res.string.settings_feedback_adb_autodetect_applied,
                    normalizedPath,
                ),
                isError = false,
            )
        }
    }

    override fun onDismissAutoDetectedAdbCandidates() {
        _state.update {
            it.copy(adbAutoDetectCandidates = emptyList())
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
                        buildToolCheckErrorMessage(
                            toolTitleRes = Res.string.settings_tool_bundletool_title,
                            kind = result.kind,
                            rawDetails = result.reason,
                            fallbackDetails = requestedPath.ifBlank { "bundletool" },
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

    override fun onCheckForUpdates() {
        if (appUpdateCheckJob?.isActive == true) return

        appUpdateCheckJob = scope.launch {
            _state.update { it.copy(isCheckingUpdates = true) }

            val checkResult = runCatchingPreserveCancellation {
                checkForAppUpdates()
            }.getOrElse { error ->
                Result.failure(error)
            }

            checkResult.fold(
                onSuccess = { hasUpdate ->
                    val message = if (hasUpdate) {
                        getString(Res.string.settings_feedback_updates_available)
                    } else {
                        getString(Res.string.settings_feedback_updates_latest)
                    }
                    showFeedback(message = message, isError = false)
                },
                onFailure = { error ->
                    val details = error.message
                        ?.trim()
                        ?.ifBlank { null }
                        ?.take(MAX_TOOL_ERROR_DETAILS_LENGTH)
                        ?: error::class.simpleName.orEmpty().ifBlank { "Unknown error" }

                    showFeedback(
                        message = getString(Res.string.settings_feedback_updates_check_failed, details),
                        isError = true,
                    )
                },
            )

            _state.update { it.copy(isCheckingUpdates = false) }
            appUpdateCheckJob = null
        }
    }

    override fun onThemeChanged(theme: AppTheme) {
        _state.update { current ->
            current.copy(
                currentTheme = theme,
                hasPendingChanges = hasPendingChanges(
                    adbPath = current.adbPath,
                    bundletoolPath = current.bundletoolPath,
                    scrcpyPath = current.scrcpyPath,
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
                    scrcpyPath = current.scrcpyPath,
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
                        scrcpyPath = if (shouldSyncEditable) saved.scrcpyPath else current.scrcpyPath,
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
                            scrcpyPath = merged.scrcpyPath,
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
            runCatchingPreserveCancellation {
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
        scrcpyPath: String,
        theme: AppTheme,
        language: AppLanguage,
    ): Boolean {
        return adbPath != persistedEditable.adbPath ||
            bundletoolPath != persistedEditable.bundletoolPath ||
            scrcpyPath != persistedEditable.scrcpyPath ||
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

    /**
     * Формирует локализованное сообщение для статуса проверки инструмента.
     *
     * Детали из backend-слоя считаем диагностикой и ограничиваем длину,
     * чтобы не раздувать UI длинными stderr-выводами.
     */
    private suspend fun buildToolCheckErrorMessage(
        toolTitleRes: StringResource,
        kind: ToolCheckFailureKind,
        rawDetails: String,
        fallbackDetails: String,
    ): String {
        val toolName = getString(toolTitleRes)
        val details = rawDetails.take(MAX_TOOL_ERROR_DETAILS_LENGTH).ifBlank { fallbackDetails }
        return when (kind) {
            ToolCheckFailureKind.WRONG_EXECUTABLE -> getString(
                Res.string.settings_status_wrong_tool,
                toolName,
                details,
            )

            ToolCheckFailureKind.START_FAILED -> getString(
                Res.string.settings_status_launch_failed,
                toolName,
                details,
            )

            ToolCheckFailureKind.COMMAND_FAILED,
            ToolCheckFailureKind.UNKNOWN,
            -> getString(
                Res.string.settings_status_not_found,
                details,
            )
        }
    }

    private fun AppSettings.toUiState(): SettingsUiState = SettingsUiState(
        adbPath = adbPath,
        bundletoolPath = bundletoolPath,
        scrcpyPath = scrcpyPath,
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
        val scrcpyPath: String,
        val theme: AppTheme,
        val language: AppLanguage,
    ) {
        companion object {
            fun from(settings: AppSettings): EditableSettingsSnapshot = EditableSettingsSnapshot(
                adbPath = settings.adbPath,
                bundletoolPath = settings.bundletoolPath,
                scrcpyPath = settings.scrcpyPath,
                theme = settings.theme,
                language = settings.language,
            )
        }
    }

    private companion object {
        const val LOGCAT_SAVE_DEBOUNCE_MS = 350L
        const val FEEDBACK_AUTO_HIDE_MS = 3_000L
        const val MAX_TOOL_ERROR_DETAILS_LENGTH = 120
    }
}

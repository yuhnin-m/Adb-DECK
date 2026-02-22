package com.adbdeck.feature.scrcpy

import com.adbdeck.core.adb.api.device.DeviceManager
import com.adbdeck.core.adb.api.device.DeviceState
import com.adbdeck.core.adb.api.scrcpy.ScrcpyClient
import com.adbdeck.core.adb.api.scrcpy.ScrcpyExitResult
import com.adbdeck.core.adb.api.scrcpy.ScrcpyLaunchRequest
import com.adbdeck.core.adb.api.scrcpy.ScrcpySession
import com.adbdeck.core.settings.SettingsRepository
import com.adbdeck.core.utils.runCatchingPreserveCancellation
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.lifecycle.coroutines.coroutineScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.NonCancellable

/**
 * Реализация [ScrcpyComponent].
 *
 * Логика управления процессом вынесена в [ScrcpyClient] (core/adb-impl),
 * а feature-слой работает только с [ScrcpySession].
 */
class DefaultScrcpyComponent(
    componentContext: ComponentContext,
    private val deviceManager: DeviceManager,
    private val settingsRepository: SettingsRepository,
    private val scrcpyClient: ScrcpyClient,
    private val onOpenSettings: () -> Unit,
    private val messageProvider: ScrcpyMessageProvider = ResourceScrcpyMessageProvider,
) : ScrcpyComponent, ComponentContext by componentContext {

    private val scope = coroutineScope()

    private val _state = MutableStateFlow(ScrcpyState())
    override val state: StateFlow<ScrcpyState> = _state.asStateFlow()

    private val sessionMutex = Mutex()
    private var activeSession: ScrcpySession? = null
    private var expectedStopSessionId: String? = null

    private var monitorJob: Job? = null
    private var feedbackJob: Job? = null

    init {
        observeDevice()
        observeSettings()
    }

    // ── Наблюдение ───────────────────────────────────────────────────────────

    private fun observeDevice() {
        scope.launch {
            deviceManager.selectedDeviceFlow.collect { device ->
                val deviceId = device
                    ?.takeIf { it.state == DeviceState.DEVICE }
                    ?.deviceId
                _state.update { it.copy(activeDeviceId = deviceId) }
            }
        }
    }

    private fun observeSettings() {
        scope.launch {
            settingsRepository.settingsFlow.collect { settings ->
                val configured = settings.scrcpyPath.isNotBlank()
                val path = settings.scrcpyPath.ifBlank { DEFAULT_SCRCPY_PATH }
                _state.update {
                    it.copy(
                        scrcpyPath = path,
                        isConfigured = configured,
                    )
                }
            }
        }
    }

    // ── Управление процессом ──────────────────────────────────────────────────

    override fun startScrcpy() {
        val processState = _state.value.processState
        if (processState == ScrcpyProcessState.RUNNING || processState == ScrcpyProcessState.STARTING) return
        if (processState == ScrcpyProcessState.STOPPING) return

        scope.launch {
            startScrcpyInternal()
        }
    }

    override fun stopScrcpy() {
        if (_state.value.processState != ScrcpyProcessState.RUNNING) return

        scope.launch {
            val session = sessionMutex.withLock {
                val current = activeSession ?: return@launch
                expectedStopSessionId = current.sessionId
                current
            }

            _state.update { it.copy(processState = ScrcpyProcessState.STOPPING) }

            val stopResult = session.stop(STOP_GRACEFUL_TIMEOUT_MS)
            if (stopResult.isFailure && session.isAlive()) {
                val reason = sanitizeReason(
                    stopResult.exceptionOrNull()?.message.orEmpty(),
                    fallback = "stop failed",
                )
                _state.update { it.copy(processState = ScrcpyProcessState.ERROR) }
                showFeedback(
                    messageProvider.stopFailed(reason),
                    isError = true,
                )
                sessionMutex.withLock {
                    if (expectedStopSessionId == session.sessionId) {
                        expectedStopSessionId = null
                    }
                }
            }
        }
    }

    private suspend fun startScrcpyInternal() {
        val snapshot = _state.value
        val deviceId = snapshot.activeDeviceId
            ?: run {
                showFeedback(
                    message = messageProvider.noDevice(),
                    isError = true,
                )
                return
            }

        if (!snapshot.isConfigured) {
            showFeedback(
                message = messageProvider.notConfigured(),
                isError = true,
            )
            return
        }

        validateConfig(snapshot.config)?.let { validationError ->
            _state.update { it.copy(processState = ScrcpyProcessState.ERROR) }
            showFeedback(
                message = messageProvider.startFailed(validationError),
                isError = true,
            )
            return
        }

        _state.update { it.copy(processState = ScrcpyProcessState.STARTING) }

        val request = snapshot.config.toLaunchRequest(deviceId)
        val scrcpyPathOverride = snapshot.scrcpyPath.takeIf { it.isNotBlank() }

        val startResult = runCatchingPreserveCancellation {
            scrcpyClient.startSession(
                request = request,
                scrcpyPathOverride = scrcpyPathOverride,
            ).getOrThrow()
        }

        startResult.fold(
            onSuccess = { session ->
                sessionMutex.withLock {
                    activeSession = session
                    expectedStopSessionId = null
                }
                _state.update { it.copy(processState = ScrcpyProcessState.RUNNING) }
                showFeedback(messageProvider.started())
                observeSession(session)
            },
            onFailure = { error ->
                val reason = sanitizeReason(
                    value = error.message ?: error.toString(),
                    fallback = "unknown error",
                )
                _state.update { it.copy(processState = ScrcpyProcessState.ERROR) }
                showFeedback(
                    message = messageProvider.startFailed(reason),
                    isError = true,
                )
            },
        )
    }

    private fun observeSession(session: ScrcpySession) {
        monitorJob?.cancel()
        monitorJob = scope.launch {
            var exitResult: ScrcpyExitResult? = null
            var crashReason: String? = null

            try {
                exitResult = session.awaitExit()
            } catch (cancel: CancellationException) {
                throw cancel
            } catch (error: Throwable) {
                crashReason = sanitizeReason(
                    value = error.message ?: error.toString(),
                    fallback = "session failed",
                )
            } finally {
                withContext(NonCancellable) {
                    if (session.isAlive()) {
                        session.stop(STOP_GRACEFUL_TIMEOUT_MS)
                    }
                }

                val release = sessionMutex.withLock {
                    val isCurrent = activeSession?.sessionId == session.sessionId
                    val expectedStop = expectedStopSessionId == session.sessionId
                    if (isCurrent) {
                        activeSession = null
                    }
                    if (expectedStop) {
                        expectedStopSessionId = null
                    }
                    SessionReleaseResult(
                        isCurrent = isCurrent,
                        expectedStop = expectedStop,
                    )
                }

                if (!release.isCurrent) return@launch

                _state.update { it.copy(processState = ScrcpyProcessState.IDLE) }

                when {
                    crashReason != null -> {
                        showFeedback(
                            message = messageProvider.crashed(crashReason.orEmpty()),
                            isError = true,
                        )
                    }

                    exitResult != null && exitResult?.exitCode != 0 && !release.expectedStop -> {
                        val reason = sanitizeReason(
                            value = exitResult?.output.orEmpty(),
                            fallback = "exit=${exitResult?.exitCode}",
                        )
                        showFeedback(
                            message = messageProvider.crashed(reason),
                            isError = true,
                        )
                    }

                    release.expectedStop -> {
                        showFeedback(messageProvider.stopped())
                    }
                }
            }
        }
    }

    // ── Конфигурация ──────────────────────────────────────────────────────────

    override fun onMaxResolutionChanged(resolution: ScrcpyMaxResolution) {
        _state.update { it.copy(config = it.config.copy(maxResolution = resolution)) }
    }

    override fun onFpsChanged(fps: ScrcpyFps) {
        _state.update { it.copy(config = it.config.copy(fps = fps)) }
    }

    override fun onBitrateChanged(bitrate: String) {
        val trimmedInput = bitrate.trim()
        val sanitizedDigits = trimmedInput
            .filter(Char::isDigit)
            .take(MAX_NUMERIC_INPUT_LENGTH)
        val normalized = when {
            trimmedInput.isBlank() -> ""
            sanitizedDigits.isBlank() -> null
            sanitizedDigits in ALLOWED_BITRATE_PRESETS_MBPS -> sanitizedDigits
            else -> null
        }

        if (normalized == null) return
        _state.update { it.copy(config = it.config.copy(bitrate = normalized)) }
    }

    override fun onAllowInputChanged(enabled: Boolean) {
        _state.update { it.copy(config = it.config.copy(allowInput = enabled)) }
    }

    override fun onTurnScreenOffChanged(enabled: Boolean) {
        _state.update { it.copy(config = it.config.copy(turnScreenOff = enabled)) }
    }

    override fun onShowTouchesChanged(enabled: Boolean) {
        _state.update { it.copy(config = it.config.copy(showTouches = enabled)) }
    }

    override fun onStayAwakeChanged(enabled: Boolean) {
        _state.update { it.copy(config = it.config.copy(stayAwake = enabled)) }
    }

    override fun onFullscreenChanged(enabled: Boolean) {
        _state.update { it.copy(config = it.config.copy(fullscreen = enabled)) }
    }

    override fun onAlwaysOnTopChanged(enabled: Boolean) {
        _state.update { it.copy(config = it.config.copy(alwaysOnTop = enabled)) }
    }

    override fun onBorderlessChanged(enabled: Boolean) {
        _state.update { it.copy(config = it.config.copy(borderless = enabled)) }
    }

    override fun onWindowWidthChanged(width: String) {
        _state.update { it.copy(config = it.config.copy(windowWidth = sanitizeNumericInput(width))) }
    }

    override fun onWindowHeightChanged(height: String) {
        _state.update { it.copy(config = it.config.copy(windowHeight = sanitizeNumericInput(height))) }
    }

    override fun onVideoCodecChanged(codec: ScrcpyVideoCodec) {
        _state.update { it.copy(config = it.config.copy(videoCodec = codec)) }
    }

    override fun onKeyboardModeChanged(mode: ScrcpyInputMode) {
        _state.update { it.copy(config = it.config.copy(keyboardMode = mode)) }
    }

    override fun onMouseModeChanged(mode: ScrcpyInputMode) {
        _state.update { it.copy(config = it.config.copy(mouseMode = mode)) }
    }

    // ── Навигация ────────────────────────────────────────────────────────────

    override fun onOpenSettings() {
        onOpenSettings.invoke()
    }

    // ── Обратная связь ───────────────────────────────────────────────────────

    override fun onDismissFeedback() {
        feedbackJob?.cancel()
        feedbackJob = null
        _state.update { current ->
            val nextProcess = if (current.processState == ScrcpyProcessState.ERROR) {
                ScrcpyProcessState.IDLE
            } else {
                current.processState
            }
            current.copy(feedback = null, processState = nextProcess)
        }
    }

    // ── Приватные утилиты ─────────────────────────────────────────────────────

    private fun showFeedback(message: String, isError: Boolean = false) {
        feedbackJob?.cancel()
        _state.update { it.copy(feedback = ScrcpyFeedback(message = message, isError = isError)) }
        feedbackJob = scope.launch {
            delay(FEEDBACK_AUTO_HIDE_MS)
            _state.update { it.copy(feedback = null) }
        }
    }

    private fun sanitizeReason(value: String, fallback: String): String {
        val normalized = value.trim().ifBlank { fallback }
        return if (normalized.length <= MAX_REASON_LENGTH) {
            normalized
        } else {
            normalized.take(MAX_REASON_LENGTH) + "…"
        }
    }

    private suspend fun validateConfig(config: ScrcpyConfig): String? {
        val width = config.windowWidth.toIntOrNull()
        if (width != null && width !in SCRCPY_MIN_WINDOW_DIMENSION..SCRCPY_MAX_WINDOW_DIMENSION) {
            return messageProvider.windowWidthRange(
                min = SCRCPY_MIN_WINDOW_DIMENSION,
                max = SCRCPY_MAX_WINDOW_DIMENSION,
            )
        }

        val height = config.windowHeight.toIntOrNull()
        if (height != null && height !in SCRCPY_MIN_WINDOW_DIMENSION..SCRCPY_MAX_WINDOW_DIMENSION) {
            return messageProvider.windowHeightRange(
                min = SCRCPY_MIN_WINDOW_DIMENSION,
                max = SCRCPY_MAX_WINDOW_DIMENSION,
            )
        }

        return null
    }

    private fun sanitizeNumericInput(value: String): String =
        value
            .trim()
            .filter(Char::isDigit)
            .take(MAX_NUMERIC_INPUT_LENGTH)

    private fun ScrcpyConfig.toLaunchRequest(deviceId: String): ScrcpyLaunchRequest = ScrcpyLaunchRequest(
        deviceId = deviceId,
        maxSize = maxResolution.pixels,
        maxFps = fps.value,
        bitrateMbps = bitrate.toIntOrNull()?.takeIf { it > 0 },
        allowInput = allowInput,
        turnScreenOff = turnScreenOff,
        showTouches = showTouches,
        stayAwake = stayAwake,
        fullscreen = fullscreen,
        alwaysOnTop = alwaysOnTop,
        borderless = borderless,
        windowWidth = windowWidth.toIntOrNull()?.takeIf { it > 0 },
        windowHeight = windowHeight.toIntOrNull()?.takeIf { it > 0 },
        videoCodec = videoCodec.cliValue,
        keyboardMode = keyboardMode.cliValue,
        mouseMode = mouseMode.cliValue,
    )

    private data class SessionReleaseResult(
        val isCurrent: Boolean,
        val expectedStop: Boolean,
    )

    private companion object {
        const val DEFAULT_SCRCPY_PATH = "scrcpy"
        const val STOP_GRACEFUL_TIMEOUT_MS = 1_500L
        const val FEEDBACK_AUTO_HIDE_MS = 3_000L
        const val MAX_REASON_LENGTH = 180
        const val MAX_NUMERIC_INPUT_LENGTH = 5

        val ALLOWED_BITRATE_PRESETS_MBPS: Set<String> = setOf(
            "2",
            "4",
            "8",
            "12",
            "16",
            "20",
            "24",
            "32",
        )
    }
}

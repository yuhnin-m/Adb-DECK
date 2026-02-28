package com.adbdeck.feature.screentools

import adbdeck.feature.screen_tools.generated.resources.Res
import adbdeck.feature.screen_tools.generated.resources.*
import com.adbdeck.core.adb.api.device.AdbDevice
import com.adbdeck.core.adb.api.device.DeviceManager
import com.adbdeck.core.adb.api.device.DeviceState
import com.adbdeck.core.adb.api.screen.ScreenrecordOptions
import com.adbdeck.core.adb.api.screen.ScreenrecordSession
import com.adbdeck.core.adb.api.screen.ScreenshotOptions
import com.adbdeck.core.settings.SettingsRepository
import com.adbdeck.feature.screentools.service.HostFileService
import com.adbdeck.feature.screentools.service.ScreenrecordStopError
import com.adbdeck.feature.screentools.service.ScreenrecordService
import com.adbdeck.feature.screentools.service.ScreenshotService
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.lifecycle.coroutines.coroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.nio.file.Path
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString

/**
 * Реализация [ScreenToolsComponent].
 *
 * Отвечает за:
 * - синхронизацию с активным устройством из [DeviceManager]
 * - запуск screenshot/screenrecord операций без блокировки UI
 * - хранение статусов и прогресса операций
 */
class DefaultScreenToolsComponent(
    componentContext: ComponentContext,
    private val deviceManager: DeviceManager,
    private val settingsRepository: SettingsRepository,
    private val screenshotService: ScreenshotService,
    private val screenrecordService: ScreenrecordService,
    private val hostFileService: HostFileService,
) : ScreenToolsComponent, ComponentContext by componentContext {

    private companion object {
        private val FILE_NAME_FORMATTER: DateTimeFormatter =
            DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss")
                .withZone(ZoneId.systemDefault())
    }

    private val scope = coroutineScope()

    private val _state = MutableStateFlow(
        run {
            val settings = settingsRepository.getSettings()
            val screenshotDir = settings.screenToolsScreenshotOutputDir
                .ifBlank { hostFileService.defaultScreenshotDirectory() }
            val screenrecordDir = settings.screenToolsScreenrecordOutputDir
                .ifBlank { hostFileService.defaultScreenrecordDirectory() }
            val defaultDeviceMessage = runBlocking {
                getString(Res.string.screen_tools_device_not_selected)
            }
            val defaultScreenshotStatus = runBlocking {
                getString(Res.string.screen_tools_status_screenshot_ready)
            }
            val defaultScreenrecordStatus = runBlocking {
                getString(Res.string.screen_tools_status_screenrecord_idle)
            }

            ScreenToolsState(
                deviceMessage = defaultDeviceMessage,
                screenshot = ScreenshotSectionState(
                    outputDirectory = screenshotDir,
                    status = ScreenToolsStatus(message = defaultScreenshotStatus),
                ),
                screenrecord = ScreenrecordSectionState(
                    outputDirectory = screenrecordDir,
                    status = ScreenToolsStatus(message = defaultScreenrecordStatus),
                ),
            )
        }
    )
    override val state: StateFlow<ScreenToolsState> = _state.asStateFlow()

    private var screenshotJob: Job? = null
    private var hostActionJob: Job? = null
    private var directoryPickerJob: Job? = null
    private var persistDirectoriesJob: Job? = null
    private var recordingJob: Job? = null
    private var recordingTickerJob: Job? = null
    private var feedbackJob: Job? = null
    private val feedbackMutex = Mutex()

    init {
        scope.launch {
            deviceManager.selectedDeviceFlow.collect { device ->
                handleDeviceChanged(device)
            }
        }
    }

    override fun onSelectTab(tab: ScreenToolsTab) {
        if (tab == ScreenToolsTab.SCREENSHOT && isScreenrecordBusy()) {
            showFeedbackResource(
                messageRes = Res.string.screen_tools_feedback_tab_switch_blocked_recording,
                isError = true,
            )
            return
        }
        _state.update { it.copy(selectedTab = tab) }
    }

    override fun onScreenshotOutputDirectoryChanged(path: String) {
        val normalized = path.trim().ifBlank { hostFileService.defaultScreenshotDirectory() }
        _state.update {
            it.copy(screenshot = it.screenshot.copy(outputDirectory = normalized))
        }
        persistDirectories(
            screenshotDir = normalized,
            screenrecordDir = _state.value.screenrecord.outputDirectory,
        )
    }

    override fun onPickScreenshotOutputDirectory() {
        val initialPath = _state.value.screenshot.outputDirectory
        pickOutputDirectory(initialPath) { selected ->
            onScreenshotOutputDirectoryChanged(selected)
        }
    }

    override fun onScreenshotQualityChanged(quality: ScreenshotQualityPreset) {
        _state.update {
            it.copy(screenshot = it.screenshot.copy(quality = quality))
        }
    }

    override fun onScreenrecordOutputDirectoryChanged(path: String) {
        val normalized = path.trim().ifBlank { hostFileService.defaultScreenrecordDirectory() }
        _state.update {
            it.copy(screenrecord = it.screenrecord.copy(outputDirectory = normalized))
        }
        persistDirectories(
            screenshotDir = _state.value.screenshot.outputDirectory,
            screenrecordDir = normalized,
        )
    }

    override fun onPickScreenrecordOutputDirectory() {
        val initialPath = _state.value.screenrecord.outputDirectory
        pickOutputDirectory(initialPath) { selected ->
            onScreenrecordOutputDirectoryChanged(selected)
        }
    }

    override fun onScreenrecordQualityChanged(quality: ScreenrecordQualityPreset) {
        _state.update {
            it.copy(screenrecord = it.screenrecord.copy(quality = quality))
        }
    }

    override fun onTakeScreenshot() {
        if (screenshotJob?.isActive == true) {
            showFeedbackResource(
                messageRes = Res.string.screen_tools_feedback_screenshot_running,
                isError = true,
            )
            return
        }

        val deviceId = _state.value.activeDeviceId
        if (deviceId == null) {
            showFeedbackResource(
                messageRes = Res.string.screen_tools_choose_active_device,
                isError = true,
            )
            return
        }

        val screenshotQuality = _state.value.screenshot.quality
        val outputDirectory = _state.value.screenshot.outputDirectory
        val outputPath = Path.of(
            outputDirectory,
            buildScreenshotFileName(extension = screenshotQuality.extension),
        ).toString()

        screenshotJob = scope.launch {
            val creatingMessage = getString(Res.string.screen_tools_status_screenshot_creating)
            _state.update { current ->
                current.copy(
                    screenshot = current.screenshot.copy(
                        isCapturing = true,
                        status = ScreenToolsStatus(creatingMessage, progress = null),
                    )
                )
            }

            val ensureResult = hostFileService.ensureDirectory(outputDirectory)
            if (!ensureResult.isSuccess) {
                val message = ensureResult.exceptionOrNull()?.message
                    ?: getString(Res.string.screen_tools_error_prepare_screenshot_dir)
                _state.update { current ->
                    current.copy(
                        screenshot = current.screenshot.copy(
                            isCapturing = false,
                            status = ScreenToolsStatus(message = message, isError = true),
                        )
                    )
                }
                showFeedback(message, isError = true)
                return@launch
            }

            val result = screenshotService.capture(
                deviceId = deviceId,
                localOutputPath = outputPath,
                adbPath = adbPath(),
                options = ScreenshotOptions(
                    format = screenshotQuality.format,
                    jpegQualityPercent = screenshotQuality.jpegQualityPercent,
                ),
            )

            result.fold(
                onSuccess = {
                    val fileName = File(outputPath).name
                    val statusMessage = getString(Res.string.screen_tools_status_screenshot_saved, fileName)
                    _state.update { current ->
                        current.copy(
                            screenshot = current.screenshot.copy(
                                isCapturing = false,
                                lastFilePath = outputPath,
                                status = ScreenToolsStatus(statusMessage),
                            )
                        )
                    }
                    showFeedbackResource(
                        messageRes = Res.string.screen_tools_feedback_screenshot_created,
                        isError = false,
                    )
                },
                onFailure = { error ->
                    val statusMessage = error.message
                        ?: getString(Res.string.screen_tools_error_screenshot_failed)
                    _state.update { current ->
                        current.copy(
                            screenshot = current.screenshot.copy(
                                isCapturing = false,
                                status = ScreenToolsStatus(
                                    message = statusMessage,
                                    isError = true,
                                ),
                            )
                        )
                    }
                    showFeedback(
                        message = error.message ?: getString(Res.string.screen_tools_feedback_screenshot_failed),
                        isError = true,
                    )
                },
            )
        }
    }

    override fun onCopyLastScreenshotToClipboard() {
        val path = _state.value.screenshot.lastFilePath?.takeIf { hostFileService.isFile(it) }
        if (path == null) {
            showFeedbackResource(
                messageRes = Res.string.screen_tools_error_screenshot_file_not_found,
                isError = true,
            )
            return
        }

        runHostAction(
            successMessageRes = Res.string.screen_tools_feedback_screenshot_copied,
            action = { hostFileService.copyImageToClipboard(path) },
        )
    }

    override fun onOpenLastScreenshotFile() {
        val path = _state.value.screenshot.lastFilePath?.takeIf { hostFileService.isFile(it) }
        if (path == null) {
            showFeedbackResource(
                messageRes = Res.string.screen_tools_error_screenshot_file_not_found,
                isError = true,
            )
            return
        }

        runHostAction(
            successMessageRes = Res.string.screen_tools_feedback_screenshot_opened,
            action = { hostFileService.openFile(path) },
        )
    }

    override fun onOpenScreenshotFolder() {
        val directory = _state.value.screenshot.outputDirectory
        runHostAction(
            successMessageRes = Res.string.screen_tools_feedback_screenshot_folder_opened,
            action = { hostFileService.openFolder(directory) },
        )
    }

    override fun onStartRecording() {
        if (isScreenrecordBusy()) {
            showFeedbackResource(
                messageRes = Res.string.screen_tools_feedback_recording_running,
                isError = true,
            )
            return
        }

        val deviceId = _state.value.activeDeviceId
        if (deviceId == null) {
            showFeedbackResource(
                messageRes = Res.string.screen_tools_choose_active_device,
                isError = true,
            )
            return
        }

        val outputDirectory = _state.value.screenrecord.outputDirectory
        val quality = _state.value.screenrecord.quality
        val localOutputPath = Path.of(outputDirectory, buildScreenrecordFileName()).toString()
        val remoteOutputPath = "/data/local/tmp/adbdeck_screenrecord_${System.currentTimeMillis()}.mp4"

        recordingJob?.cancel()
        recordingJob = scope.launch {
            val startingMessage = getString(Res.string.screen_tools_status_recording_starting)
            _state.update { current ->
                current.copy(
                    screenrecord = current.screenrecord.copy(
                        phase = RecordingPhase.STARTING,
                        activeSession = null,
                        elapsedSeconds = 0,
                        recordingDeviceId = null,
                        currentLocalTargetPath = localOutputPath,
                        status = ScreenToolsStatus(startingMessage, progress = null),
                    )
                )
            }

            val ensureResult = hostFileService.ensureDirectory(outputDirectory)
            if (!ensureResult.isSuccess) {
                val message = ensureResult.exceptionOrNull()?.message
                    ?: getString(Res.string.screen_tools_error_prepare_recordings_dir)
                _state.update { current ->
                    current.copy(
                        screenrecord = current.screenrecord.copy(
                            phase = RecordingPhase.IDLE,
                            activeSession = null,
                            status = ScreenToolsStatus(message = message, isError = true),
                        )
                    )
                }
                showFeedback(message, isError = true)
                return@launch
            }

            val startResult = screenrecordService.start(
                deviceId = deviceId,
                remoteOutputPath = remoteOutputPath,
                adbPath = adbPath(),
                options = ScreenrecordOptions(
                    bitRateMbps = quality.bitRateMbps,
                    videoSize = quality.videoSize,
                ),
            )

            if (!isActive) return@launch

            startResult.fold(
                onSuccess = { session ->
                    startRecordingTicker(session.startedAtEpochMillis)
                    val activeMessage = getString(Res.string.screen_tools_status_recording_active)
                    _state.update { current ->
                        current.copy(
                            screenrecord = current.screenrecord.copy(
                                phase = RecordingPhase.RECORDING,
                                activeSession = session,
                                recordingDeviceId = session.deviceId,
                                currentLocalTargetPath = localOutputPath,
                                status = ScreenToolsStatus(activeMessage),
                            )
                        )
                    }
                    showFeedbackResource(
                        messageRes = Res.string.screen_tools_feedback_recording_started,
                        isError = false,
                    )
                },
                onFailure = { error ->
                    val statusMessage = error.message
                        ?: getString(Res.string.screen_tools_error_recording_start_failed)
                    _state.update { current ->
                        current.copy(
                            screenrecord = current.screenrecord.copy(
                                phase = RecordingPhase.IDLE,
                                activeSession = null,
                                status = ScreenToolsStatus(
                                    message = statusMessage,
                                    isError = true,
                                ),
                            )
                        )
                    }
                    showFeedback(
                        message = error.message ?: getString(Res.string.screen_tools_feedback_recording_start_failed),
                        isError = true,
                    )
                },
            )
        }
    }

    override fun onStopRecording() {
        val session = _state.value.screenrecord.activeSession
        if (session == null) {
            showFeedbackResource(
                messageRes = Res.string.screen_tools_feedback_recording_not_started,
                isError = true,
            )
            return
        }

        val localOutputPath = _state.value.screenrecord.currentLocalTargetPath
            ?: Path.of(_state.value.screenrecord.outputDirectory, buildScreenrecordFileName()).toString()

        recordingTickerJob?.cancel()

        recordingJob?.cancel()
        recordingJob = scope.launch {
            val stoppingMessage = getString(Res.string.screen_tools_status_recording_stopping)
            _state.update { current ->
                current.copy(
                    screenrecord = current.screenrecord.copy(
                        phase = RecordingPhase.STOPPING,
                        status = ScreenToolsStatus(
                            message = stoppingMessage,
                            progress = 0.1f,
                        ),
                    )
                )
            }

            val result = screenrecordService.stopAndSave(
                session = session,
                localOutputPath = localOutputPath,
                adbPath = adbPath(),
                onProgress = { progress, message ->
                    _state.update { current ->
                        current.copy(
                            screenrecord = current.screenrecord.copy(
                                status = ScreenToolsStatus(
                                    message = message,
                                    progress = progress,
                                ),
                            )
                        )
                    }
                },
            )

            if (!isActive) return@launch

            result.fold(
                onSuccess = {
                    val fileName = File(localOutputPath).name
                    val statusMessage = getString(Res.string.screen_tools_status_recording_saved, fileName)
                    _state.update { current ->
                        current.copy(
                            screenrecord = current.screenrecord.copy(
                                phase = RecordingPhase.IDLE,
                                activeSession = null,
                                recordingDeviceId = null,
                                elapsedSeconds = 0,
                                currentLocalTargetPath = null,
                                lastFilePath = localOutputPath,
                                status = ScreenToolsStatus(
                                    message = statusMessage,
                                    progress = 1f,
                                ),
                            )
                        )
                    }
                    showFeedbackResource(
                        messageRes = Res.string.screen_tools_feedback_recording_saved,
                        isError = false,
                    )
                },
                onFailure = { error ->
                    when (error) {
                        is ScreenrecordStopError.StopOnDevice -> {
                            startRecordingTicker(session.startedAtEpochMillis)
                            _state.update { current ->
                                current.copy(
                                    screenrecord = current.screenrecord.copy(
                                        phase = RecordingPhase.RECORDING,
                                        activeSession = session,
                                        recordingDeviceId = session.deviceId,
                                        currentLocalTargetPath = localOutputPath,
                                        status = ScreenToolsStatus(
                                            message = error.message
                                                ?: getString(Res.string.screen_tools_error_recording_stop_failed_retry),
                                            isError = true,
                                        ),
                                    )
                                )
                            }
                            showFeedbackResource(
                                messageRes = Res.string.screen_tools_feedback_recording_stop_retry,
                                isError = true,
                            )
                        }

                        is ScreenrecordStopError.CopyToHost -> {
                            _state.update { current ->
                                current.copy(
                                    screenrecord = current.screenrecord.copy(
                                        phase = RecordingPhase.IDLE,
                                        activeSession = null,
                                        recordingDeviceId = null,
                                        elapsedSeconds = 0,
                                        currentLocalTargetPath = null,
                                        status = ScreenToolsStatus(
                                            message = error.message
                                                ?: getString(Res.string.screen_tools_error_recording_copy_failed),
                                            isError = true,
                                        ),
                                    )
                                )
                            }
                            showFeedbackResource(
                                messageRes = Res.string.screen_tools_feedback_recording_copy_failed,
                                isError = true,
                            )
                        }

                        else -> {
                            // На неизвестной ошибке не теряем session-контекст:
                            // безопаснее считать, что запись может ещё идти.
                            startRecordingTicker(session.startedAtEpochMillis)
                            val statusMessage = error.message
                                ?: getString(Res.string.screen_tools_error_recording_stop_failed)
                            _state.update { current ->
                                current.copy(
                                    screenrecord = current.screenrecord.copy(
                                        phase = RecordingPhase.RECORDING,
                                        activeSession = session,
                                        recordingDeviceId = session.deviceId,
                                        currentLocalTargetPath = localOutputPath,
                                        status = ScreenToolsStatus(
                                            message = statusMessage,
                                            isError = true,
                                        ),
                                    )
                                )
                            }
                            showFeedback(
                                message = error.message ?: getString(Res.string.screen_tools_feedback_recording_stop_failed),
                                isError = true,
                            )
                        }
                    }
                },
            )
        }
    }

    override fun onOpenLastVideoFile() {
        val path = _state.value.screenrecord.lastFilePath?.takeIf { hostFileService.isFile(it) }
        if (path == null) {
            showFeedbackResource(
                messageRes = Res.string.screen_tools_error_video_file_not_found,
                isError = true,
            )
            return
        }

        runHostAction(
            successMessageRes = Res.string.screen_tools_feedback_video_opened,
            action = { hostFileService.openFile(path) },
        )
    }

    override fun onOpenVideoFolder() {
        val directory = _state.value.screenrecord.outputDirectory
        runHostAction(
            successMessageRes = Res.string.screen_tools_feedback_video_folder_opened,
            action = { hostFileService.openFolder(directory) },
        )
    }

    override fun onDismissFeedback() {
        feedbackJob?.cancel()
        feedbackJob = null
        _state.update { it.copy(feedback = null) }
    }

    /**
     * Обрабатывает смену активного устройства из общего [DeviceManager].
     */
    private suspend fun handleDeviceChanged(device: AdbDevice?) {
        val nextActiveDeviceId = when {
            device == null -> null
            device.state != DeviceState.DEVICE -> null
            else -> device.deviceId
        }
        val nextMessage = when {
            device == null -> getString(Res.string.screen_tools_device_not_selected)
            device.state != DeviceState.DEVICE -> getString(
                Res.string.screen_tools_device_unavailable,
                device.state.rawValue,
            )
            else -> getString(Res.string.screen_tools_device_active, device.deviceId)
        }

        _state.update {
            it.copy(
                activeDeviceId = nextActiveDeviceId,
                deviceMessage = nextMessage,
            )
        }

        val session = _state.value.screenrecord.activeSession
        if (session != null && session.deviceId != nextActiveDeviceId) {
            interruptRecordingBecauseDeviceChanged(session)
        }
    }

    /**
     * Прерывает локальный контекст записи, если устройство сменилось/пропало.
     */
    private fun interruptRecordingBecauseDeviceChanged(session: ScreenrecordSession) {
        recordingTickerJob?.cancel()
        recordingTickerJob = null
        recordingJob?.cancel()
        recordingJob = null

        scope.launch {
            val interruptedStatus = getString(Res.string.screen_tools_status_recording_interrupted)
            _state.update { current ->
                current.copy(
                    screenrecord = current.screenrecord.copy(
                        phase = RecordingPhase.IDLE,
                        activeSession = null,
                        recordingDeviceId = null,
                        elapsedSeconds = 0,
                        currentLocalTargetPath = null,
                        status = ScreenToolsStatus(
                            message = interruptedStatus,
                            isError = true,
                        ),
                    )
                )
            }
            showFeedbackResource(
                messageRes = Res.string.screen_tools_feedback_recording_interrupted,
                isError = true,
            )
        }

        // Останавливаем удаленную запись best-effort, чтобы не оставлять временный файл.
        scope.launch {
            screenrecordService.abort(
                session = session,
                adbPath = adbPath(),
            )
        }
    }

    /**
     * Запускает тикер времени записи.
     */
    private fun startRecordingTicker(startedAtEpochMillis: Long) {
        recordingTickerJob?.cancel()
        recordingTickerJob = scope.launch {
            while (isActive && _state.value.screenrecord.activeSession != null) {
                val elapsed = ((System.currentTimeMillis() - startedAtEpochMillis) / 1_000L)
                    .coerceAtLeast(0L)
                _state.update { current ->
                    current.copy(
                        screenrecord = current.screenrecord.copy(elapsedSeconds = elapsed)
                    )
                }
                delay(1_000L)
            }
        }
    }

    /** `true`, если запись экрана сейчас выполняется или находится в переходном состоянии. */
    private fun isScreenrecordBusy(): Boolean {
        val screenrecord = _state.value.screenrecord
        return screenrecord.activeSession != null || screenrecord.phase != RecordingPhase.IDLE
    }

    /**
     * Выполнить host-side действие (open/copy) с единым feedback.
     */
    private fun runHostAction(
        successMessageRes: StringResource,
        action: suspend () -> Result<Unit>,
    ) {
        hostActionJob?.cancel()
        hostActionJob = scope.launch {
            val result = action()
            val successMessage = getString(successMessageRes)
            val fallbackError = getString(Res.string.screen_tools_error_host_operation_failed)
            result.fold(
                onSuccess = { showFeedback(successMessage, isError = false) },
                onFailure = { showFeedback(it.message ?: fallbackError, isError = true) },
            )
        }
    }

    /**
     * Открывает системный диалог выбора директории и применяет выбранный путь.
     */
    private fun pickOutputDirectory(
        initialPath: String,
        onSelected: (String) -> Unit,
    ) {
        directoryPickerJob?.cancel()
        directoryPickerJob = scope.launch {
            val fallbackError = getString(Res.string.screen_tools_error_host_operation_failed)
            hostFileService.selectDirectory(initialPath).fold(
                onSuccess = { selected ->
                    if (!selected.isNullOrBlank()) {
                        onSelected(selected)
                    }
                },
                onFailure = { error ->
                    showFeedback(error.message ?: fallbackError, isError = true)
                },
            )
        }
    }

    /**
     * Сохранить пути output directory в общие настройки приложения.
     */
    private fun persistDirectories(
        screenshotDir: String,
        screenrecordDir: String,
    ) {
        // Сохраняем только последнее состояние директорий:
        // если пользователь быстро меняет пути, старые save-задачи
        // не должны перетирать более новые значения.
        persistDirectoriesJob?.cancel()
        persistDirectoriesJob = scope.launch {
            val current = settingsRepository.getSettings()
            settingsRepository.saveSettings(
                current.copy(
                    screenToolsScreenshotOutputDir = screenshotDir,
                    screenToolsScreenrecordOutputDir = screenrecordDir,
                )
            )
        }
    }

    /** Текущий путь к adb из настроек приложения. */
    private fun adbPath(): String =
        settingsRepository.resolvedAdbPath()

    /** Имя файла скриншота с читаемым timestamp. */
    private fun buildScreenshotFileName(
        now: Instant = Instant.now(),
        extension: String,
    ): String = "screenshot_${FILE_NAME_FORMATTER.format(now)}.$extension"

    /** Имя файла видеозаписи с читаемым timestamp. */
    private fun buildScreenrecordFileName(now: Instant = Instant.now()): String =
        "screenrecord_${FILE_NAME_FORMATTER.format(now)}.mp4"

    /**
     * Показать feedback по строковому ресурсу.
     *
     * Mutex сохраняет порядок сообщений, если несколько корутин
     * отправляют feedback почти одновременно.
     */
    private fun showFeedbackResource(
        messageRes: StringResource,
        isError: Boolean,
        vararg args: Any,
    ) {
        scope.launch {
            feedbackMutex.withLock {
                val message = getString(messageRes, *args)
                showFeedback(message = message, isError = isError)
            }
        }
    }

    /**
     * Показать краткоживущее сообщение в нижнем feedback-баннере.
     */
    private fun showFeedback(
        message: String,
        isError: Boolean,
    ) {
        feedbackJob?.cancel()
        _state.update {
            it.copy(feedback = ScreenToolsFeedback(message = message, isError = isError))
        }

        feedbackJob = scope.launch {
            delay(5_000L)
            _state.update { current ->
                if (current.feedback?.message == message) {
                    current.copy(feedback = null)
                } else {
                    current
                }
            }
        }
    }
}

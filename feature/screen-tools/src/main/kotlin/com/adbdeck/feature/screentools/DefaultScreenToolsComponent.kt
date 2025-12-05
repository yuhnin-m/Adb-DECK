package com.adbdeck.feature.screentools

import com.adbdeck.core.adb.api.device.AdbDevice
import com.adbdeck.core.adb.api.device.DeviceManager
import com.adbdeck.core.adb.api.device.DeviceState
import com.adbdeck.core.adb.api.screen.ScreenrecordOptions
import com.adbdeck.core.adb.api.screen.ScreenrecordSession
import com.adbdeck.core.adb.api.screen.ScreenshotOptions
import com.adbdeck.core.settings.SettingsRepository
import com.adbdeck.feature.screentools.service.HostFileService
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
import java.io.File
import java.nio.file.Path
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

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

            ScreenToolsState(
                screenshot = ScreenshotSectionState(outputDirectory = screenshotDir),
                screenrecord = ScreenrecordSectionState(outputDirectory = screenrecordDir),
            )
        }
    )
    override val state: StateFlow<ScreenToolsState> = _state.asStateFlow()

    private var screenshotJob: Job? = null
    private var hostActionJob: Job? = null
    private var recordingJob: Job? = null
    private var recordingTickerJob: Job? = null
    private var feedbackJob: Job? = null

    private var activeRecordingSession: ScreenrecordSession? = null

    init {
        scope.launch {
            deviceManager.selectedDeviceFlow.collect { device ->
                handleDeviceChanged(device)
            }
        }
    }

    override fun onSelectTab(tab: ScreenToolsTab) {
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

    override fun onScreenrecordQualityChanged(quality: ScreenrecordQualityPreset) {
        _state.update {
            it.copy(screenrecord = it.screenrecord.copy(quality = quality))
        }
    }

    override fun onTakeScreenshot() {
        if (screenshotJob?.isActive == true) {
            showFeedback("Скриншот уже выполняется", isError = true)
            return
        }

        val deviceId = _state.value.activeDeviceId
        if (deviceId == null) {
            showFeedback("Выберите доступное активное устройство", isError = true)
            return
        }

        val screenshotQuality = _state.value.screenshot.quality
        val outputDirectory = _state.value.screenshot.outputDirectory
        val outputPath = Path.of(
            outputDirectory,
            buildScreenshotFileName(extension = screenshotQuality.extension),
        ).toString()

        screenshotJob = scope.launch {
            _state.update { current ->
                current.copy(
                    screenshot = current.screenshot.copy(
                        isCapturing = true,
                        status = ScreenToolsStatus("Создаём скриншот…", progress = null),
                    )
                )
            }

            val ensureResult = hostFileService.ensureDirectory(outputDirectory)
            if (!ensureResult.isSuccess) {
                val message = ensureResult.exceptionOrNull()?.message ?: "Не удалось подготовить папку скриншотов"
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
                    _state.update { current ->
                        current.copy(
                            screenshot = current.screenshot.copy(
                                isCapturing = false,
                                lastFilePath = outputPath,
                                status = ScreenToolsStatus("Скриншот сохранён: $fileName"),
                            )
                        )
                    }
                    showFeedback("Скриншот создан", isError = false)
                },
                onFailure = { error ->
                    _state.update { current ->
                        current.copy(
                            screenshot = current.screenshot.copy(
                                isCapturing = false,
                                status = ScreenToolsStatus(
                                    message = error.message ?: "Не удалось сделать скриншот",
                                    isError = true,
                                ),
                            )
                        )
                    }
                    showFeedback(error.message ?: "Ошибка screenshot", isError = true)
                },
            )
        }
    }

    override fun onCopyLastScreenshotToClipboard() {
        val path = _state.value.screenshot.lastFilePath
        if (!hostFileService.isFile(path)) {
            showFeedback("Файл скриншота не найден", isError = true)
            return
        }

        runHostAction(
            successMessage = "Скриншот скопирован в буфер обмена",
            action = { hostFileService.copyImageToClipboard(path!!) },
        )
    }

    override fun onOpenLastScreenshotFile() {
        val path = _state.value.screenshot.lastFilePath
        if (!hostFileService.isFile(path)) {
            showFeedback("Файл скриншота не найден", isError = true)
            return
        }

        runHostAction(
            successMessage = "Скриншот открыт",
            action = { hostFileService.openFile(path!!) },
        )
    }

    override fun onOpenScreenshotFolder() {
        val directory = _state.value.screenshot.outputDirectory
        runHostAction(
            successMessage = "Открыта папка скриншотов",
            action = { hostFileService.openFolder(directory) },
        )
    }

    override fun onStartRecording() {
        if (activeRecordingSession != null || _state.value.screenrecord.isRecording || _state.value.screenrecord.isStarting) {
            showFeedback("Запись уже выполняется", isError = true)
            return
        }

        val deviceId = _state.value.activeDeviceId
        if (deviceId == null) {
            showFeedback("Выберите доступное активное устройство", isError = true)
            return
        }

        val outputDirectory = _state.value.screenrecord.outputDirectory
        val quality = _state.value.screenrecord.quality
        val localOutputPath = Path.of(outputDirectory, buildScreenrecordFileName()).toString()
        val remoteOutputPath = "/data/local/tmp/adbdeck_screenrecord_${System.currentTimeMillis()}.mp4"

        recordingJob?.cancel()
        recordingJob = scope.launch {
            _state.update { current ->
                current.copy(
                    screenrecord = current.screenrecord.copy(
                        isStarting = true,
                        isRecording = false,
                        isStopping = false,
                        elapsedSeconds = 0,
                        currentLocalTargetPath = localOutputPath,
                        status = ScreenToolsStatus("Запускаем запись…", progress = null),
                    )
                )
            }

            val ensureResult = hostFileService.ensureDirectory(outputDirectory)
            if (!ensureResult.isSuccess) {
                val message = ensureResult.exceptionOrNull()?.message ?: "Не удалось подготовить папку записей"
                _state.update { current ->
                    current.copy(
                        screenrecord = current.screenrecord.copy(
                            isStarting = false,
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
                    activeRecordingSession = session
                    startRecordingTicker(session.startedAtEpochMillis)
                    _state.update { current ->
                        current.copy(
                            screenrecord = current.screenrecord.copy(
                                isStarting = false,
                                isRecording = true,
                                isStopping = false,
                                recordingDeviceId = session.deviceId,
                                currentLocalTargetPath = localOutputPath,
                                status = ScreenToolsStatus("Идёт запись экрана…"),
                            )
                        )
                    }
                    showFeedback("Запись запущена", isError = false)
                },
                onFailure = { error ->
                    _state.update { current ->
                        current.copy(
                            screenrecord = current.screenrecord.copy(
                                isStarting = false,
                                status = ScreenToolsStatus(
                                    message = error.message ?: "Не удалось запустить запись",
                                    isError = true,
                                ),
                            )
                        )
                    }
                    showFeedback(error.message ?: "Ошибка запуска записи", isError = true)
                },
            )
        }
    }

    override fun onStopRecording() {
        val session = activeRecordingSession
        if (session == null) {
            showFeedback("Запись не запущена", isError = true)
            return
        }

        val localOutputPath = _state.value.screenrecord.currentLocalTargetPath
            ?: Path.of(_state.value.screenrecord.outputDirectory, buildScreenrecordFileName()).toString()

        recordingTickerJob?.cancel()

        recordingJob?.cancel()
        recordingJob = scope.launch {
            _state.update { current ->
                current.copy(
                    screenrecord = current.screenrecord.copy(
                        isStarting = false,
                        isRecording = true,
                        isStopping = true,
                        status = ScreenToolsStatus(
                            message = "Останавливаем запись…",
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

            activeRecordingSession = null

            result.fold(
                onSuccess = {
                    val fileName = File(localOutputPath).name
                    _state.update { current ->
                        current.copy(
                            screenrecord = current.screenrecord.copy(
                                isStarting = false,
                                isRecording = false,
                                isStopping = false,
                                recordingDeviceId = null,
                                elapsedSeconds = 0,
                                currentLocalTargetPath = null,
                                lastFilePath = localOutputPath,
                                status = ScreenToolsStatus(
                                    message = "Запись сохранена: $fileName",
                                    progress = 1f,
                                ),
                            )
                        )
                    }
                    showFeedback("Видео сохранено", isError = false)
                },
                onFailure = { error ->
                    _state.update { current ->
                        current.copy(
                            screenrecord = current.screenrecord.copy(
                                isStarting = false,
                                isRecording = false,
                                isStopping = false,
                                recordingDeviceId = null,
                                elapsedSeconds = 0,
                                currentLocalTargetPath = null,
                                status = ScreenToolsStatus(
                                    message = error.message ?: "Не удалось остановить запись",
                                    isError = true,
                                ),
                            )
                        )
                    }
                    showFeedback(error.message ?: "Ошибка остановки записи", isError = true)
                },
            )
        }
    }

    override fun onOpenLastVideoFile() {
        val path = _state.value.screenrecord.lastFilePath
        if (!hostFileService.isFile(path)) {
            showFeedback("Видеофайл не найден", isError = true)
            return
        }

        runHostAction(
            successMessage = "Видео открыто",
            action = { hostFileService.openFile(path!!) },
        )
    }

    override fun onOpenVideoFolder() {
        val directory = _state.value.screenrecord.outputDirectory
        runHostAction(
            successMessage = "Открыта папка видеозаписей",
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
    private fun handleDeviceChanged(device: AdbDevice?) {
        val nextActiveDeviceId = when {
            device == null -> null
            device.state != DeviceState.DEVICE -> null
            else -> device.deviceId
        }
        val nextMessage = when {
            device == null -> "Активное устройство не выбрано"
            device.state != DeviceState.DEVICE -> "Устройство недоступно: ${device.state.rawValue}"
            else -> "Активное устройство: ${device.deviceId}"
        }

        _state.update {
            it.copy(
                activeDeviceId = nextActiveDeviceId,
                deviceMessage = nextMessage,
            )
        }

        val session = activeRecordingSession
        if (session != null && session.deviceId != nextActiveDeviceId) {
            interruptRecordingBecauseDeviceChanged(session)
        }
    }

    /**
     * Прерывает локальный контекст записи, если устройство сменилось/пропало.
     */
    private fun interruptRecordingBecauseDeviceChanged(session: ScreenrecordSession) {
        activeRecordingSession = null
        recordingTickerJob?.cancel()
        recordingTickerJob = null
        recordingJob?.cancel()
        recordingJob = null

        _state.update { current ->
            current.copy(
                screenrecord = current.screenrecord.copy(
                    isStarting = false,
                    isRecording = false,
                    isStopping = false,
                    recordingDeviceId = null,
                    elapsedSeconds = 0,
                    currentLocalTargetPath = null,
                    status = ScreenToolsStatus(
                        message = "Запись прервана: активное устройство изменилось или стало недоступно",
                        isError = true,
                    ),
                )
            )
        }
        showFeedback("Запись прервана из-за смены устройства", isError = true)

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
            while (isActive && activeRecordingSession != null) {
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

    /**
     * Выполнить host-side действие (open/copy) с единым feedback.
     */
    private fun runHostAction(
        successMessage: String,
        action: suspend () -> Result<Unit>,
    ) {
        hostActionJob?.cancel()
        hostActionJob = scope.launch {
            val result = action()
            result.fold(
                onSuccess = { showFeedback(successMessage, isError = false) },
                onFailure = { showFeedback(it.message ?: "Ошибка host-операции", isError = true) },
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
        scope.launch {
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
        settingsRepository.getSettings().adbPath.ifBlank { "adb" }

    /** Имя файла скриншота с читаемым timestamp. */
    private fun buildScreenshotFileName(
        now: Instant = Instant.now(),
        extension: String,
    ): String = "screenshot_${FILE_NAME_FORMATTER.format(now)}.$extension"

    /** Имя файла видеозаписи с читаемым timestamp. */
    private fun buildScreenrecordFileName(now: Instant = Instant.now()): String =
        "screenrecord_${FILE_NAME_FORMATTER.format(now)}.mp4"

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

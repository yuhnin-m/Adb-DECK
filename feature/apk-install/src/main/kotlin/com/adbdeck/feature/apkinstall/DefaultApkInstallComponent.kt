package com.adbdeck.feature.apkinstall

import adbdeck.feature.apk_install.generated.resources.Res
import adbdeck.feature.apk_install.generated.resources.*
import com.adbdeck.core.adb.api.apkinstall.ApkInstallOptions
import com.adbdeck.core.adb.api.device.AdbDevice
import com.adbdeck.core.adb.api.device.DeviceManager
import com.adbdeck.core.adb.api.device.DeviceState
import com.adbdeck.core.settings.SettingsRepository
import com.adbdeck.feature.apkinstall.service.ApkFileValidationError
import com.adbdeck.feature.apkinstall.service.ApkFileValidationResult
import com.adbdeck.feature.apkinstall.service.ApkInstallHostFileService
import com.adbdeck.feature.apkinstall.service.ApkInstallService
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.lifecycle.coroutines.coroutineScope
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.io.File
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString

/**
 * Реализация [ApkInstallComponent].
 *
 * Отвечает за:
 * - синхронизацию с active device из [DeviceManager];
 * - запуск `adb install` без блокировки UI;
 * - валидацию APK через host-side [ApkInstallHostFileService];
 * - показ прогресса, лога и ошибок.
 */
class DefaultApkInstallComponent(
    componentContext: ComponentContext,
    private val deviceManager: DeviceManager,
    private val settingsRepository: SettingsRepository,
    private val apkInstallService: ApkInstallService,
    private val hostFileService: ApkInstallHostFileService,
) : ApkInstallComponent, ComponentContext by componentContext {

    private val scope = coroutineScope()

    private val _state = MutableStateFlow(ApkInstallState())
    override val state: StateFlow<ApkInstallState> = _state.asStateFlow()

    private var installJob: Job? = null
    private var feedbackJob: Job? = null
    private var pickApkJob: Job? = null
    private var pathStatusJob: Job? = null
    private val feedbackMutex = Mutex()

    init {
        scope.launch {
            // Первичный локализованный статус до первого ввода пути.
            val initialDeviceMessage = getString(Res.string.apk_install_device_not_selected)
            val initialStatus = getString(Res.string.apk_install_status_select_apk)
            _state.update { current ->
                current.copy(
                    deviceMessage = if (current.deviceMessage.isBlank()) {
                        initialDeviceMessage
                    } else {
                        current.deviceMessage
                    },
                    status = if (current.status.message.isBlank()) {
                        current.status.copy(message = initialStatus)
                    } else {
                        current.status
                    },
                )
            }
        }

        scope.launch {
            deviceManager.selectedDeviceFlow.collect { device ->
                handleDeviceChanged(device)
            }
        }
    }

    override fun onApkPathChanged(path: String) {
        val normalized = path.trim()
        _state.update { current ->
            current.copy(
                apkPath = normalized,
            )
        }
        updateIdleStatusByPath(normalized)
    }

    override fun onPickApkFile() {
        pickApkJob?.cancel()
        pickApkJob = scope.launch {
            val initialPath = _state.value.apkPath
            hostFileService.selectApkFile(initialPath).fold(
                onSuccess = { selected ->
                    if (!selected.isNullOrBlank()) {
                        onApkPathChanged(selected)
                    }
                },
                onFailure = { error ->
                    handleError(
                        error = ApkInstallError.PickerFailed(error.message),
                        updateStatus = false,
                    )
                },
            )
        }
    }

    override fun onApkPathDropped(path: String) {
        // DropTarget callback приходит с AWT-потока. Обновление состояния
        // фичи выполняем через основной scope компонента.
        scope.launch {
            onApkPathChanged(path)
        }
    }

    override fun onAllowTestOnlyChanged(allow: Boolean) {
        _state.update { current ->
            if (current.isInstalling) {
                current
            } else {
                current.copy(allowTestOnlyInstall = allow)
            }
        }
    }

    override fun onInstallApk() {
        if (installJob?.isActive == true || _state.value.isInstalling) {
            showFeedbackResource(
                messageRes = Res.string.apk_install_feedback_install_running,
                isError = true,
            )
            return
        }

        val deviceId = _state.value.activeDeviceId
        if (deviceId == null) {
            showFeedbackResource(
                messageRes = Res.string.apk_install_feedback_choose_device,
                isError = true,
            )
            return
        }

        val allowTestOnly = _state.value.allowTestOnlyInstall
        installJob = scope.launch {
            val rawPath = _state.value.apkPath
            when (val validation = hostFileService.validateApkPath(rawPath)) {
                is ApkFileValidationResult.Invalid -> {
                    handleError(
                        error = validation.toApkInstallError(),
                        updateStatus = false,
                    )
                    return@launch
                }

                is ApkFileValidationResult.Valid -> {
                    startInstall(
                        deviceId = deviceId,
                        apkPath = validation.absolutePath,
                        apkFileName = validation.fileName,
                        allowTestOnly = allowTestOnly,
                    )
                }
            }
        }
    }

    override fun onCopyStatusResult() {
        val statusMessage = _state.value.status.message.trim()
        if (statusMessage.isBlank()) {
            showFeedbackResource(
                messageRes = Res.string.apk_install_feedback_copy_status_failed,
                isError = true,
            )
            return
        }

        scope.launch {
            copyToClipboard(statusMessage).onSuccess {
                showFeedbackResource(
                    messageRes = Res.string.apk_install_feedback_copy_status_success,
                    isError = false,
                )
            }.onFailure {
                showFeedbackResource(
                    messageRes = Res.string.apk_install_feedback_copy_status_failed,
                    isError = true,
                )
            }
        }
    }

    override fun onClearLog() {
        _state.update { it.copy(logLines = emptyList()) }
    }

    override fun onDismissFeedback() {
        feedbackJob?.cancel()
        feedbackJob = null
        _state.update { it.copy(feedback = null) }
    }

    /** Обновляет idle-статус в зависимости от введенного пути к APK. */
    private fun updateIdleStatusByPath(path: String) {
        pathStatusJob?.cancel()
        pathStatusJob = scope.launch {
            val message = if (path.isBlank()) {
                getString(Res.string.apk_install_status_select_apk)
            } else {
                getString(Res.string.apk_install_status_ready, File(path).name)
            }
            _state.update { current ->
                if (current.isInstalling) {
                    current
                } else {
                    current.copy(status = ApkInstallStatus(message = message))
                }
            }
        }
    }

    /** Синхронизация состояния с текущим active device. */
    private suspend fun handleDeviceChanged(device: AdbDevice?) {
        val nextActiveDeviceId = when {
            device == null -> null
            device.state != DeviceState.DEVICE -> null
            else -> device.deviceId
        }
        val nextMessage = when {
            device == null -> getString(Res.string.apk_install_device_not_selected)
            device.state != DeviceState.DEVICE -> getString(
                Res.string.apk_install_device_unavailable,
                device.state.rawValue,
            )

            else -> getString(Res.string.apk_install_device_active, device.deviceId)
        }

        _state.update {
            it.copy(
                activeDeviceId = nextActiveDeviceId,
                deviceMessage = nextMessage,
            )
        }

        val installingDeviceId = _state.value.installingDeviceId
        if (installingDeviceId != null && installingDeviceId != nextActiveDeviceId) {
            interruptInstallBecauseDeviceChanged()
        }
    }

    /** Запустить установку APK после успешной host-валидации пути. */
    private suspend fun startInstall(
        deviceId: String,
        apkPath: String,
        apkFileName: String,
        allowTestOnly: Boolean,
    ) {
        val statusInstalling = getString(Res.string.apk_install_status_installing)
        val logStarted = getString(Res.string.apk_install_log_started, apkPath)
        _state.update { current ->
            current.copy(
                isInstalling = true,
                installingDeviceId = deviceId,
                status = ApkInstallStatus(
                    message = statusInstalling,
                    progress = 0f,
                ),
                logLines = listOf(logStarted),
            )
        }

        val result = apkInstallService.install(
            deviceId = deviceId,
            localApkPath = apkPath,
            adbPath = adbPath(),
            options = ApkInstallOptions(
                allowTestOnly = allowTestOnly,
                bundletoolPath = settingsRepository.getSettings().bundletoolPath,
            ),
        ) { progress, message ->
            _state.update { current ->
                current.copy(
                    status = ApkInstallStatus(
                        message = message,
                        progress = progress,
                    ),
                    logLines = appendLog(
                        current = current.logLines,
                        line = message,
                    ),
                )
            }
        }

        if (!coroutineContext.isActive) return

        result.fold(
            onSuccess = {
                val statusInstalled = getString(Res.string.apk_install_status_installed, apkFileName)
                val logSuccess = getString(Res.string.apk_install_log_success)
                _state.update { current ->
                    current.copy(
                        isInstalling = false,
                        installingDeviceId = null,
                        lastInstalledApkPath = apkPath,
                        status = ApkInstallStatus(
                            message = statusInstalled,
                            progress = 1f,
                        ),
                        logLines = appendLog(
                            current = current.logLines,
                            line = logSuccess,
                        ),
                    )
                }
                showFeedbackResource(
                    messageRes = Res.string.apk_install_feedback_install_success,
                    isError = false,
                )
            },
            onFailure = { error ->
                handleError(
                    error = ApkInstallError.InstallFailed(error.message),
                    updateStatus = true,
                    appendLogOnError = true,
                )
            },
        )
    }

    /** Прерывает установку, если устройство сменилось или стало недоступно. */
    private suspend fun interruptInstallBecauseDeviceChanged() {
        installJob?.cancel()
        installJob = null

        val statusMessage = getString(Res.string.apk_install_status_interrupted)
        val logInterrupted = getString(Res.string.apk_install_log_interrupted)
        _state.update { current ->
            current.copy(
                isInstalling = false,
                installingDeviceId = null,
                status = ApkInstallStatus(
                    message = statusMessage,
                    isError = true,
                ),
                logLines = appendLog(
                    current = current.logLines,
                    line = logInterrupted,
                ),
            )
        }
        showFeedbackResource(
            messageRes = Res.string.apk_install_feedback_install_interrupted,
            isError = true,
        )
    }

    /** Текущий путь к adb из настроек приложения. */
    private fun adbPath(): String =
        settingsRepository.resolvedAdbPath()

    /** Обработать typed-ошибку с единым UX-пайплайном. */
    private suspend fun handleError(
        error: ApkInstallError,
        updateStatus: Boolean,
        appendLogOnError: Boolean = false,
    ) {
        val message = resolveErrorMessage(error)

        if (updateStatus) {
            _state.update { current ->
                current.copy(
                    isInstalling = false,
                    installingDeviceId = null,
                    status = ApkInstallStatus(
                        message = message,
                        isError = true,
                    ),
                    logLines = if (appendLogOnError) {
                        val logError = getString(Res.string.apk_install_log_error, message)
                        appendLog(current.logLines, logError)
                    } else {
                        current.logLines
                    },
                )
            }
        }

        showFeedback(message = message, isError = true)
    }

    /** Сопоставляет typed-ошибку с локализованным текстом. */
    private suspend fun resolveErrorMessage(error: ApkInstallError): String =
        when (error) {
            ApkInstallError.InstallAlreadyRunning ->
                getString(Res.string.apk_install_feedback_install_running)

            ApkInstallError.ActiveDeviceMissing ->
                getString(Res.string.apk_install_feedback_choose_device)

            ApkInstallError.ApkPathMissing ->
                getString(Res.string.apk_install_feedback_choose_apk)

            is ApkInstallError.ApkFileNotFound ->
                getString(Res.string.apk_install_feedback_apk_not_found, error.path)

            ApkInstallError.InvalidApkExtension ->
                getString(Res.string.apk_install_feedback_invalid_apk)

            ApkInstallError.ApkPathAccessFailed ->
                getString(Res.string.apk_install_feedback_apk_access_error)

            is ApkInstallError.InstallFailed -> {
                val details = error.details?.trim().orEmpty()
                if (details.isBlank()) {
                    getString(Res.string.apk_install_feedback_install_failed)
                } else {
                    getString(Res.string.apk_install_error_install_with_details, details)
                }
            }

            ApkInstallError.InstallInterruptedByDeviceChange ->
                getString(Res.string.apk_install_feedback_install_interrupted)

            is ApkInstallError.PickerFailed -> {
                val details = error.details?.trim().orEmpty()
                if (details.isBlank()) {
                    getString(Res.string.apk_install_feedback_picker_failed)
                } else {
                    getString(Res.string.apk_install_error_picker_with_details, details)
                }
            }
        }

    /** Показывает feedback по ресурсу и сохраняет порядок при конкуренции корутин. */
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

    /** Показывает feedback и автоматически скрывает через 5 секунд. */
    private fun showFeedback(message: String, isError: Boolean) {
        feedbackJob?.cancel()
        _state.update {
            it.copy(feedback = ApkInstallFeedback(message = message, isError = isError))
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

    private fun copyToClipboard(text: String): Result<Unit> = runCatching {
        val selection = StringSelection(text)
        Toolkit.getDefaultToolkit().systemClipboard.setContents(selection, null)
    }

    /** Добавляет строку в лог с ограничением размера списка. */
    private fun appendLog(
        current: List<String>,
        line: String,
        maxLines: Int = 200,
    ): List<String> {
        val normalized = line.trim()
        if (normalized.isBlank()) return current

        val next = current + normalized
        return if (next.size > maxLines) {
            next.takeLast(maxLines)
        } else {
            next
        }
    }

    /** Преобразует ошибку валидации host-файла в typed-ошибку экрана. */
    private fun ApkFileValidationResult.Invalid.toApkInstallError(): ApkInstallError =
        when (reason) {
            ApkFileValidationError.EMPTY_PATH -> ApkInstallError.ApkPathMissing
            ApkFileValidationError.FILE_NOT_FOUND -> ApkInstallError.ApkFileNotFound(originalPath)
            ApkFileValidationError.UNSUPPORTED_FORMAT -> ApkInstallError.InvalidApkExtension
            ApkFileValidationError.IO_ACCESS_ERROR -> ApkInstallError.ApkPathAccessFailed
        }
}

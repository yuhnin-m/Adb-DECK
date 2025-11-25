package com.adbdeck.feature.apkinstall

import com.adbdeck.core.adb.api.AdbDevice
import com.adbdeck.core.adb.api.DeviceManager
import com.adbdeck.core.adb.api.DeviceState
import com.adbdeck.core.settings.SettingsRepository
import com.adbdeck.feature.apkinstall.service.ApkInstallService
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

/**
 * Реализация [ApkInstallComponent].
 *
 * Отвечает за:
 * - синхронизацию с active device из [DeviceManager];
 * - запуск `adb install` без блокировки UI;
 * - показ прогресса, лога и ошибок.
 */
class DefaultApkInstallComponent(
    componentContext: ComponentContext,
    private val deviceManager: DeviceManager,
    private val settingsRepository: SettingsRepository,
    private val apkInstallService: ApkInstallService,
) : ApkInstallComponent, ComponentContext by componentContext {

    private val scope = coroutineScope()

    private val _state = MutableStateFlow(ApkInstallState())
    override val state: StateFlow<ApkInstallState> = _state.asStateFlow()

    private var installJob: Job? = null
    private var feedbackJob: Job? = null

    init {
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
                status = if (normalized.isBlank()) {
                    ApkInstallStatus("Выберите APK для установки")
                } else {
                    ApkInstallStatus("Готово к установке: ${File(normalized).name}")
                },
            )
        }
    }

    override fun onInstallApk() {
        if (installJob?.isActive == true || _state.value.isInstalling) {
            showFeedback("Установка APK уже выполняется", isError = true)
            return
        }

        val deviceId = _state.value.activeDeviceId
        if (deviceId == null) {
            showFeedback("Выберите доступное активное устройство", isError = true)
            return
        }

        val apkPath = _state.value.apkPath.trim()
        if (apkPath.isBlank()) {
            showFeedback("Выберите APK-файл", isError = true)
            return
        }

        val apkFile = File(apkPath)
        if (!apkFile.isFile) {
            showFeedback("APK-файл не найден: $apkPath", isError = true)
            return
        }
        if (!apkFile.name.endsWith(".apk", ignoreCase = true)) {
            showFeedback("Нужен файл с расширением .apk", isError = true)
            return
        }

        installJob = scope.launch {
            _state.update { current ->
                current.copy(
                    isInstalling = true,
                    installingDeviceId = deviceId,
                    status = ApkInstallStatus(
                        message = "Запускаем установку APK…",
                        progress = 0f,
                    ),
                    logLines = listOf("Запуск установки: ${apkFile.absolutePath}"),
                )
            }

            val result = apkInstallService.install(
                deviceId = deviceId,
                localApkPath = apkFile.absolutePath,
                adbPath = adbPath(),
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

            if (!isActive) return@launch

            result.fold(
                onSuccess = {
                    _state.update { current ->
                        current.copy(
                            isInstalling = false,
                            installingDeviceId = null,
                            lastInstalledApkPath = apkFile.absolutePath,
                            status = ApkInstallStatus(
                                message = "APK установлен: ${apkFile.name}",
                                progress = 1f,
                            ),
                            logLines = appendLog(
                                current = current.logLines,
                                line = "Установка завершена успешно",
                            ),
                        )
                    }
                    showFeedback("APK успешно установлен", isError = false)
                },
                onFailure = { error ->
                    val message = error.message ?: "Не удалось установить APK"
                    _state.update { current ->
                        current.copy(
                            isInstalling = false,
                            installingDeviceId = null,
                            status = ApkInstallStatus(
                                message = message,
                                isError = true,
                            ),
                            logLines = appendLog(
                                current = current.logLines,
                                line = "Ошибка: $message",
                            ),
                        )
                    }
                    showFeedback(message, isError = true)
                },
            )
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

    /** Синхронизация состояния с текущим active device. */
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

        val installingDeviceId = _state.value.installingDeviceId
        if (installingDeviceId != null && installingDeviceId != nextActiveDeviceId) {
            interruptInstallBecauseDeviceChanged()
        }
    }

    /** Прерывает установку, если устройство сменилось или стало недоступно. */
    private fun interruptInstallBecauseDeviceChanged() {
        installJob?.cancel()
        installJob = null

        _state.update { current ->
            current.copy(
                isInstalling = false,
                installingDeviceId = null,
                status = ApkInstallStatus(
                    message = "Установка APK прервана: активное устройство изменилось или стало недоступно",
                    isError = true,
                ),
                logLines = appendLog(
                    current = current.logLines,
                    line = "Установка прервана из-за смены устройства",
                ),
            )
        }
        showFeedback("Установка APK прервана из-за смены устройства", isError = true)
    }

    /** Текущий путь к adb из настроек приложения. */
    private fun adbPath(): String =
        settingsRepository.getSettings().adbPath.ifBlank { "adb" }

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
}

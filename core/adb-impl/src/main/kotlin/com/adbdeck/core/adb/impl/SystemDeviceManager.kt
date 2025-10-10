package com.adbdeck.core.adb.impl

import com.adbdeck.core.adb.api.AdbClient
import com.adbdeck.core.adb.api.AdbDevice
import com.adbdeck.core.adb.api.DeviceEndpoint
import com.adbdeck.core.adb.api.DeviceManager
import com.adbdeck.core.process.ProcessRunner
import com.adbdeck.core.settings.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Реализация [DeviceManager] через системный adb-процесс.
 *
 * Использует [AdbClient] для получения списка устройств (`adb devices`) и
 * [ProcessRunner] для операций подключения/отключения/tcpip.
 *
 * Singleton — живёт всё время работы приложения.
 *
 * @param adbClient          ADB-клиент для получения списка устройств.
 * @param processRunner      Исполнитель внешних процессов.
 * @param settingsRepository Репозиторий настроек (путь к adb, сохранённые endpoints).
 */
class SystemDeviceManager(
    private val adbClient: AdbClient,
    private val processRunner: ProcessRunner,
    private val settingsRepository: SettingsRepository,
) : DeviceManager {

    private val _devicesFlow = MutableStateFlow<List<AdbDevice>>(emptyList())
    override val devicesFlow: StateFlow<List<AdbDevice>> = _devicesFlow.asStateFlow()

    private val _selectedDeviceFlow = MutableStateFlow<AdbDevice?>(null)
    override val selectedDeviceFlow: StateFlow<AdbDevice?> = _selectedDeviceFlow.asStateFlow()

    private val _isConnecting = MutableStateFlow(false)
    override val isConnecting: StateFlow<Boolean> = _isConnecting.asStateFlow()

    private val _errorFlow = MutableStateFlow<String?>(null)
    override val errorFlow: StateFlow<String?> = _errorFlow.asStateFlow()

    private val _savedEndpointsFlow = MutableStateFlow(loadEndpointsFromSettings())
    override val savedEndpointsFlow: StateFlow<List<DeviceEndpoint>> = _savedEndpointsFlow.asStateFlow()

    /** Читает актуальный путь к adb из настроек. */
    private fun adbPath(): String =
        settingsRepository.getSettings().adbPath.ifBlank { "adb" }

    /** Загружает сохранённые endpoints из настроек. */
    private fun loadEndpointsFromSettings(): List<DeviceEndpoint> =
        settingsRepository.getSettings().knownEndpoints
            .mapNotNull { DeviceEndpoint.fromAddress(it) }

    // ── refresh ────────────────────────────────────────────────────────────

    override suspend fun refresh() {
        _isConnecting.value = true
        try {
            adbClient.getDevices()
                .onSuccess { devices ->
                    _devicesFlow.value = devices
                    reconcileSelectedDevice(devices)
                }
                .onFailure { e ->
                    _errorFlow.value = e.message ?: "Не удалось получить список устройств"
                }
        } finally {
            _isConnecting.value = false
        }
    }

    /**
     * После обновления списка устройств синхронизирует [_selectedDeviceFlow]:
     * - Если выбранное устройство ещё присутствует — обновляет его состояние.
     * - Если пропало — сбрасывает выбор.
     * - Если ничего не выбрано и устройство одно — выбирает автоматически.
     */
    private fun reconcileSelectedDevice(devices: List<AdbDevice>) {
        val currentId = _selectedDeviceFlow.value?.deviceId
        _selectedDeviceFlow.value = when {
            currentId != null -> devices.find { it.deviceId == currentId }
            devices.size == 1 -> devices.first()
            else -> null
        }
    }

    // ── connect ────────────────────────────────────────────────────────────

    override suspend fun connect(host: String, port: Int): Result<String> {
        _isConnecting.value = true
        return try {
            val result = processRunner.run(adbPath(), "connect", "$host:$port")
            val output = result.stdout.trim()
            // adb connect возвращает код 0 даже при неудаче; проверяем stdout
            if (output.startsWith("connected") || output.startsWith("already connected")) {
                refresh()
                // Автоматически выбрать только что подключённое устройство
                _devicesFlow.value.find { it.deviceId == "$host:$port" }?.let {
                    _selectedDeviceFlow.value = it
                }
                Result.success(output)
            } else {
                val errorMsg = output.ifBlank {
                    result.stderr.trim().ifBlank { "Не удалось подключиться к $host:$port" }
                }
                _errorFlow.value = errorMsg
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            val msg = "Ошибка подключения к $host:$port — ${e.message}"
            _errorFlow.value = msg
            Result.failure(Exception(msg, e))
        } finally {
            _isConnecting.value = false
        }
    }

    // ── disconnect ─────────────────────────────────────────────────────────

    override suspend fun disconnect(deviceId: String): Result<Unit> {
        _isConnecting.value = true
        return try {
            processRunner.run(adbPath(), "disconnect", deviceId)
            // Сброс выбора если отключили активное устройство
            if (_selectedDeviceFlow.value?.deviceId == deviceId) {
                _selectedDeviceFlow.value = null
            }
            refresh()
            Result.success(Unit)
        } catch (e: Exception) {
            val msg = "Ошибка отключения устройства $deviceId — ${e.message}"
            _errorFlow.value = msg
            Result.failure(Exception(msg, e))
        } finally {
            _isConnecting.value = false
        }
    }

    // ── tcpip ──────────────────────────────────────────────────────────────

    override suspend fun switchToTcpIp(serialId: String, port: Int): Result<Unit> {
        _isConnecting.value = true
        return try {
            val result = processRunner.run(adbPath(), "-s", serialId, "tcpip", port.toString())
            if (result.isSuccess) {
                Result.success(Unit)
            } else {
                val errorMsg = result.stderr.trim()
                    .ifBlank { result.stdout.trim() }
                    .ifBlank { "Не удалось переключить $serialId в TCP/IP режим" }
                _errorFlow.value = errorMsg
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            val msg = "Ошибка переключения в TCP/IP — ${e.message}"
            _errorFlow.value = msg
            Result.failure(Exception(msg, e))
        } finally {
            _isConnecting.value = false
        }
    }

    // ── selection ──────────────────────────────────────────────────────────

    override fun selectDevice(device: AdbDevice) {
        _selectedDeviceFlow.value = device
    }

    // ── saved endpoints ────────────────────────────────────────────────────

    override suspend fun saveEndpoint(endpoint: DeviceEndpoint) {
        val settings = settingsRepository.getSettings()
        val existing = settings.knownEndpoints.toMutableList()
        if (!existing.contains(endpoint.address)) {
            existing.add(endpoint.address)
            settingsRepository.saveSettings(settings.copy(knownEndpoints = existing))
            _savedEndpointsFlow.value = loadEndpointsFromSettings()
        }
    }

    override suspend fun removeEndpoint(endpoint: DeviceEndpoint) {
        val settings = settingsRepository.getSettings()
        val updated = settings.knownEndpoints.filter { it != endpoint.address }
        settingsRepository.saveSettings(settings.copy(knownEndpoints = updated))
        _savedEndpointsFlow.value = loadEndpointsFromSettings()
    }

    // ── error ──────────────────────────────────────────────────────────────

    override fun clearError() {
        _errorFlow.value = null
    }
}

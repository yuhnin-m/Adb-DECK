package com.adbdeck.core.adb.impl.device

import com.adbdeck.core.adb.api.adb.AdbClient
import com.adbdeck.core.adb.api.device.AdbDevice
import com.adbdeck.core.adb.api.device.DeviceEndpoint
import com.adbdeck.core.adb.api.device.DeviceManager
import com.adbdeck.core.adb.api.device.SavedWifiDevice
import com.adbdeck.core.process.ProcessRunner
import com.adbdeck.core.settings.SavedWifiDeviceSettingsEntry
import com.adbdeck.core.settings.SettingsRepository
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Реализация [com.adbdeck.core.adb.api.device.DeviceManager] через системный adb-процесс.
 *
 * Использует [com.adbdeck.core.adb.api.adb.AdbClient] для получения списка устройств (`adb devices`) и
 * [com.adbdeck.core.process.ProcessRunner] для операций подключения/отключения/tcpip.
 *
 * Singleton — живет все время работы приложения.
 *
 * @param adbClient          ADB-клиент для получения списка устройств.
 * @param processRunner      Исполнитель внешних процессов.
 * @param settingsRepository Репозиторий настроек (путь к adb, сохраненные endpoints).
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

    private val _wifiHistoryFlow = MutableStateFlow(loadWifiHistoryFromSettings())
    override val wifiHistoryFlow: StateFlow<List<SavedWifiDevice>> = _wifiHistoryFlow.asStateFlow()

    /** Читает актуальный путь к adb из настроек. */
    private fun adbPath(): String =
        settingsRepository.resolvedAdbPath()

    /** Загружает сохраненные endpoints из настроек. */
    private fun loadEndpointsFromSettings(): List<DeviceEndpoint> =
        settingsRepository.getSettings().knownEndpoints
            .mapNotNull { DeviceEndpoint.Companion.fromAddress(it) }
            .deduplicateByHostKeepLatest()

    /** Загружает историю Wi-Fi-устройств из настроек. */
    private fun loadWifiHistoryFromSettings(): List<SavedWifiDevice> =
        settingsRepository.getSettings().knownWifiDevices
            .mapNotNull { it.toDomainOrNull() }
            .sortedByDescending { it.lastSeenAt }

    // ── refresh ────────────────────────────────────────────────────────────

    override suspend fun refresh() {
        _isConnecting.value = true
        try {
            val result = adbClient.getDevices()
            result
                .onSuccess { devices ->
                    _devicesFlow.value = devices
                    reconcileSelectedDevice(devices)
                }
                .onFailure { e ->
                    _errorFlow.value = e.message ?: "Не удалось получить список устройств"
                }
            result.getOrNull()?.let { devices ->
                recordConnectedWifiDevices(devices)
            }
        } finally {
            _isConnecting.value = false
        }
    }

    /**
     * После обновления списка устройств синхронизирует [_selectedDeviceFlow]:
     * - Если выбранное устройство еще присутствует — обновляет его состояние.
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

    /**
     * Обновляет историю на основе текущего списка подключенных Wi-Fi-устройств.
     *
     * Источник данных — `adb devices`, поэтому displayName может быть неполным;
     * более точное имя позже может быть перезаписано из DeviceInfo.
     */
    private suspend fun recordConnectedWifiDevices(devices: List<AdbDevice>) {
        if (devices.isEmpty()) return

        val now = System.currentTimeMillis()
        val current = loadWifiHistoryFromSettings()
            .associateBy { it.address }
            .toMutableMap()

        var changed = false
        devices
            .asSequence()
            .filter { it.deviceId.contains(':') && !it.deviceId.startsWith("emulator-") }
            .forEach { device ->
                val address = device.deviceId.trim()
                if (address.isEmpty()) return@forEach

                val displayName = parseDisplayNameFromAdbInfo(device.info)
                    ?: device.info.ifBlank { device.deviceId }

                val existing = current[address]
                val shouldRefreshTimestamp = existing == null ||
                    existing.deviceId != device.deviceId ||
                    existing.displayName != displayName
                val updated = SavedWifiDevice(
                    address = address,
                    deviceId = device.deviceId,
                    displayName = displayName,
                    lastSeenAt = if (shouldRefreshTimestamp) now else existing.lastSeenAt,
                )

                if (existing == null || existing != updated) {
                    current[address] = updated
                    changed = true
                }
            }

        if (!changed) return

        val normalized = current.values
            .sortedByDescending { it.lastSeenAt }
            .take(MAX_WIFI_HISTORY_ITEMS)
            .map { it.toSettings() }

        val settings = settingsRepository.getSettings()
        settingsRepository.saveSettings(settings.copy(knownWifiDevices = normalized))
        _wifiHistoryFlow.value = loadWifiHistoryFromSettings()
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
                // Автоматически выбрать только что подключенное устройство
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
        } catch (e: CancellationException) {
            throw e
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
            val result = processRunner.run(adbPath(), "disconnect", deviceId)
            val output = sequenceOf(result.stdout, result.stderr)
                .joinToString(separator = "\n")
                .trim()

            val failedByOutput = output.contains("error", ignoreCase = true)
            if (!result.isSuccess || failedByOutput) {
                val message = output.ifBlank {
                    "Не удалось отключить устройство $deviceId (exitCode=${result.exitCode})"
                }
                _errorFlow.value = message
                return Result.failure(Exception(message))
            }

            // Сброс выбора если отключили активное устройство
            if (_selectedDeviceFlow.value?.deviceId == deviceId) {
                _selectedDeviceFlow.value = null
            }
            refresh()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
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
        } catch (e: CancellationException) {
            throw e
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
        existing.removeAll { savedAddress ->
            DeviceEndpoint.fromAddress(savedAddress)
                ?.host
                ?.equals(endpoint.host, ignoreCase = true)
                ?: false
        }
        existing.add(endpoint.address)

        settingsRepository.saveSettings(settings.copy(knownEndpoints = existing))
        _savedEndpointsFlow.value = loadEndpointsFromSettings()
    }

    override suspend fun removeEndpoint(endpoint: DeviceEndpoint) {
        val settings = settingsRepository.getSettings()
        val updated = settings.knownEndpoints.filterNot { savedAddress ->
            DeviceEndpoint.fromAddress(savedAddress)
                ?.host
                ?.equals(endpoint.host, ignoreCase = true)
                ?: false
        }
        settingsRepository.saveSettings(settings.copy(knownEndpoints = updated))
        _savedEndpointsFlow.value = loadEndpointsFromSettings()
    }

    // ── wifi history ────────────────────────────────────────────────────────

    override suspend fun upsertWifiHistory(entry: SavedWifiDevice) {
        val normalizedAddress = entry.address.trim()
        if (normalizedAddress.isEmpty()) return

        val now = System.currentTimeMillis()
        val normalizedEntry = entry.copy(
            address = normalizedAddress,
            deviceId = entry.deviceId.ifBlank { normalizedAddress },
            displayName = entry.displayName.ifBlank { entry.deviceId.ifBlank { normalizedAddress } },
            lastSeenAt = if (entry.lastSeenAt > 0L) entry.lastSeenAt else now,
        )

        val settings = settingsRepository.getSettings()
        val merged = settings.knownWifiDevices
            .toMutableList()
            .apply {
                removeAll { it.address == normalizedAddress }
                add(normalizedEntry.toSettings())
            }
            .sortedByDescending { it.lastSeenAt }
            .take(MAX_WIFI_HISTORY_ITEMS)

        settingsRepository.saveSettings(settings.copy(knownWifiDevices = merged))
        _wifiHistoryFlow.value = loadWifiHistoryFromSettings()
    }

    override suspend fun removeWifiHistory(address: String) {
        val normalizedAddress = address.trim()
        if (normalizedAddress.isEmpty()) return

        val settings = settingsRepository.getSettings()
        val updated = settings.knownWifiDevices
            .filterNot { it.address == normalizedAddress }

        settingsRepository.saveSettings(settings.copy(knownWifiDevices = updated))
        _wifiHistoryFlow.value = loadWifiHistoryFromSettings()
    }

    // ── error ──────────────────────────────────────────────────────────────

    override fun clearError() {
        _errorFlow.value = null
    }

    private companion object {
        private const val MAX_WIFI_HISTORY_ITEMS = 50
    }
}

private fun SavedWifiDeviceSettingsEntry.toDomainOrNull(): SavedWifiDevice? {
    val normalizedAddress = address.trim()
    if (normalizedAddress.isEmpty()) return null

    val normalizedDeviceId = deviceId.ifBlank { normalizedAddress }
    val normalizedDisplayName = displayName.ifBlank { normalizedDeviceId }

    return SavedWifiDevice(
        address = normalizedAddress,
        deviceId = normalizedDeviceId,
        displayName = normalizedDisplayName,
        lastSeenAt = lastSeenAt,
    )
}

private fun SavedWifiDevice.toSettings(): SavedWifiDeviceSettingsEntry =
    SavedWifiDeviceSettingsEntry(
        address = address,
        deviceId = deviceId,
        displayName = displayName,
        lastSeenAt = lastSeenAt,
    )

private fun parseDisplayNameFromAdbInfo(rawInfo: String): String? {
    if (rawInfo.isBlank()) return null
    val modelToken = rawInfo
        .split(' ')
        .firstOrNull { it.startsWith("model:") }
        ?.substringAfter("model:")
        ?.trim()
    return modelToken?.takeIf { it.isNotEmpty() }
}

private fun List<DeviceEndpoint>.deduplicateByHostKeepLatest(): List<DeviceEndpoint> {
    if (size <= 1) return this

    val seenHosts = hashSetOf<String>()
    val reversedUnique = mutableListOf<DeviceEndpoint>()
    asReversed().forEach { endpoint ->
        val hostKey = endpoint.host.trim().lowercase(Locale.ROOT)
        if (hostKey.isNotEmpty() && seenHosts.add(hostKey)) {
            reversedUnique += endpoint
        }
    }
    return reversedUnique.asReversed()
}

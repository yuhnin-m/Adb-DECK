package com.adbdeck.core.adb.api.device

import kotlinx.coroutines.flow.StateFlow

/**
 * Менеджер подключения и управления ADB-устройствами.
 *
 * Централизованный компонент, отвечающий за все операции с устройствами:
 * - Обновление списка устройств (`adb devices`)
 * - Wi-Fi подключение (`adb connect ip:port`)
 * - Отключение (`adb disconnect`)
 * - Перевод USB-устройства в TCP/IP-режим (`adb tcpip`)
 * - Хранение избранных endpoint-ов (сохраненные IP-адреса)
 * - Выбор активного устройства для целевых команд
 *
 * Живет как singleton на весь lifecycle приложения.
 */
interface DeviceManager {

    /** Актуальный список всех видимых ADB-устройств (из `adb devices`). */
    val devicesFlow: StateFlow<List<AdbDevice>>

    /**
     * Текущее выбранное (активное) устройство.
     * `null` если ни одно устройство не выбрано.
     * Автоматически выбирается, если в системе только одно устройство.
     */
    val selectedDeviceFlow: StateFlow<AdbDevice?>

    /** `true` во время выполнения любой adb-операции (refresh, connect, disconnect). */
    val isConnecting: StateFlow<Boolean>

    /** Последнее сообщение об ошибке. `null` если ошибок нет. */
    val errorFlow: StateFlow<String?>

    /** Поток сохраненных endpoint-ов (из настроек приложения). */
    val savedEndpointsFlow: StateFlow<List<DeviceEndpoint>>

    /** История ранее подключенных Wi-Fi-устройств (из настроек приложения). */
    val wifiHistoryFlow: StateFlow<List<SavedWifiDevice>>

    /**
     * Обновить список устройств через `adb devices`.
     * Автоматически обновляет [selectedDeviceFlow]: если выбранное устройство
     * пропало — сбрасывает выбор; если устройство одно — выбирает автоматически.
     */
    suspend fun refresh()

    /**
     * Подключиться к устройству по Wi-Fi через `adb connect host:port`.
     *
     * @param host Хост или IP-адрес устройства.
     * @param port Порт adb (по умолчанию 5555).
     * @return [Result.success] с ответом adb при успехе, [Result.failure] при ошибке.
     */
    suspend fun connect(host: String, port: Int = 5555): Result<String>

    /**
     * Отключить устройство через `adb disconnect deviceId`.
     * Если устройство было активным — сбрасывает [selectedDeviceFlow].
     *
     * @param deviceId Serial-номер или "host:port" устройства.
     */
    suspend fun disconnect(deviceId: String): Result<Unit>

    /**
     * Перевести USB-устройство в режим TCP/IP через `adb -s serial tcpip port`.
     * После этого можно подключиться по IP через [connect].
     *
     * @param serialId Serial-номер USB-устройства.
     * @param port     Порт для TCP/IP (по умолчанию 5555).
     */
    suspend fun switchToTcpIp(serialId: String, port: Int = 5555): Result<Unit>

    /**
     * Установить текущее активное устройство.
     * Используется при ручном выборе из списка.
     */
    fun selectDevice(device: AdbDevice)

    /**
     * Добавить endpoint в список сохраненных (если еще не сохранен).
     * Сохраняется в настройках приложения.
     */
    suspend fun saveEndpoint(endpoint: DeviceEndpoint)

    /**
     * Удалить endpoint из списка сохраненных.
     * Изменения сохраняются в настройках приложения.
     */
    suspend fun removeEndpoint(endpoint: DeviceEndpoint)

    /**
     * Добавить или обновить запись в истории Wi-Fi-устройств.
     *
     * Обновляет `lastSeenAt`, если адрес уже существует.
     */
    suspend fun upsertWifiHistory(entry: SavedWifiDevice)

    /**
     * Удалить запись из истории Wi-Fi-устройств по адресу `host:port`.
     */
    suspend fun removeWifiHistory(address: String)

    /** Сбросить текущую ошибку ([errorFlow] → `null`). */
    fun clearError()
}

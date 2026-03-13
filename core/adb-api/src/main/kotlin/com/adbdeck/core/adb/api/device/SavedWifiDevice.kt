package com.adbdeck.core.adb.api.device

/**
 * Запись о ранее подключенном ADB-устройстве по Wi-Fi.
 *
 * Используется для секции "Previously connected network devices" на экране Devices.
 *
 * @param address     Сетевой адрес в формате `host:port`.
 * @param deviceId    Последний известный deviceId устройства.
 * @param displayName Человекочитаемое имя устройства (модель/алиас).
 * @param lastSeenAt  Время последнего обнаружения устройства (Unix ms).
 */
data class SavedWifiDevice(
    val address: String,
    val deviceId: String,
    val displayName: String,
    val lastSeenAt: Long,
)

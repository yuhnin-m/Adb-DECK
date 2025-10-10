package com.adbdeck.core.adb.api

/**
 * Модель подключённого ADB-устройства.
 *
 * @param deviceId Уникальный идентификатор устройства (serial number или IP:port).
 * @param state    Текущее состояние устройства.
 * @param info     Дополнительная информация (модель, продукт и т.д.), если доступна.
 */
data class AdbDevice(
    val deviceId: String,
    val state: DeviceState,
    val info: String = "",
)

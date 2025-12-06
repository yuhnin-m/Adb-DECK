package com.adbdeck.core.adb.api.device

/**
 * Тип транспортного соединения ADB-устройства.
 *
 * Определяется эвристически по формату [AdbDevice.deviceId]:
 * - `emulator-XXXX` → [EMULATOR]
 * - `IP:PORT` (содержит двоеточие) → [WIFI]
 * - Всё остальное → [USB]
 */
enum class DeviceTransportType {
    /** USB-кабель. */
    USB,

    /** Беспроводное подключение по Wi-Fi (adb over TCP/IP). */
    WIFI,

    /** Android-эмулятор (emulator-5554 и т.д.). */
    EMULATOR,

    /** Не удалось определить тип. */
    UNKNOWN,
}

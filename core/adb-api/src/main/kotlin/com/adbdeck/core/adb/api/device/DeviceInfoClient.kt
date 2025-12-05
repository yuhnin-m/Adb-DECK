package com.adbdeck.core.adb.api.device

/**
 * Контракт клиента для получения расширенной информации об ADB-устройстве.
 *
 * ## Используемые ADB-команды (реализация):
 * - `adb -s <id> shell getprop` — все системные свойства устройства
 * - `adb -s <id> shell wm size` — разрешение экрана
 * - `adb -s <id> shell dumpsys battery` — уровень и статус батареи
 *
 * ## Производительность:
 * Каждый вызов делает несколько ADB shell запросов — не вызывать в hot loop.
 * Результаты следует кешировать на уровне компонента.
 *
 * @see DeviceInfo
 */
interface DeviceInfoClient {

    /**
     * Получить расширенную информацию об устройстве.
     *
     * @param deviceId Серийный номер или адрес устройства (для `adb -s`).
     * @param adbPath  Путь к исполняемому файлу `adb`.
     * @return [Result.success] с [DeviceInfo] при успехе,
     *         [Result.failure] если устройство недоступно.
     */
    suspend fun fetchDeviceInfo(
        deviceId: String,
        adbPath: String = "adb",
    ): Result<DeviceInfo>
}

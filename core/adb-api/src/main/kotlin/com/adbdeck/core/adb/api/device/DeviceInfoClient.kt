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

    /**
     * Получить карту системных свойств из `adb shell getprop`.
     *
     * Возвращает уже разобранную структуру `ключ -> значение`.
     * Нужен для feature-экранов, где требуется выборка нескольких
     * независимых разделов на основе `ro.*` и `persist.*` свойств.
     *
     * @param deviceId Серийный номер или адрес устройства (для `adb -s`).
     * @param adbPath  Путь к исполняемому файлу `adb`.
     */
    suspend fun getSystemProperties(
        deviceId: String,
        adbPath: String = "adb",
    ): Result<Map<String, String>>

    /**
     * Выполнить произвольную shell-команду на устройстве через `adb shell`.
     *
     * Пример:
     * `runShellCommand(deviceId, listOf("dumpsys", "battery"), adbPath)`.
     *
     * @param deviceId Серийный номер или адрес устройства (для `adb -s`).
     * @param command  Аргументы shell-команды (без префикса `adb shell`).
     * @param adbPath  Путь к исполняемому файлу `adb`.
     * @return [Result.success] с stdout команды.
     */
    suspend fun runShellCommand(
        deviceId: String,
        command: List<String>,
        adbPath: String = "adb",
    ): Result<String>
}

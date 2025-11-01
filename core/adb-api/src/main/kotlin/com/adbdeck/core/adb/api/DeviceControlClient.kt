package com.adbdeck.core.adb.api

/**
 * Режим перезагрузки устройства.
 */
enum class RebootMode {
    /** Обычная перезагрузка (`adb reboot`). */
    NORMAL,

    /** Перезагрузка в Recovery (`adb reboot recovery`). */
    RECOVERY,

    /** Перезагрузка в Bootloader / Fastboot (`adb reboot bootloader`). */
    BOOTLOADER,
}

/**
 * Контракт клиента для управления Android-устройством через ADB.
 *
 * ## Используемые ADB-команды (реализация):
 * - `adb -s <id> reboot` — обычная перезагрузка
 * - `adb -s <id> reboot recovery` — в Recovery
 * - `adb -s <id> reboot bootloader` — в Bootloader / Fastboot
 *
 * ## Важно:
 * После любой перезагрузки устройство исчезнет из `adb devices` на время.
 * UI должен сообщить пользователю об ожидаемом поведении.
 */
interface DeviceControlClient {

    /**
     * Перезагрузить устройство.
     *
     * @param deviceId Серийный номер или адрес устройства.
     * @param mode     Режим перезагрузки (обычный / Recovery / Bootloader).
     * @param adbPath  Путь к исполняемому файлу `adb`.
     * @return [Result.success] при успешной отправке команды,
     *         [Result.failure] если команда не выполнилась.
     */
    suspend fun reboot(
        deviceId: String,
        mode: RebootMode = RebootMode.NORMAL,
        adbPath: String = "adb",
    ): Result<Unit>
}

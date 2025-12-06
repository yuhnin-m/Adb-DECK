package com.adbdeck.core.adb.api.device

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

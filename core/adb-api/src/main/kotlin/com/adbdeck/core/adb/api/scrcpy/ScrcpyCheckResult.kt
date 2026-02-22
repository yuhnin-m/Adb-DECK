package com.adbdeck.core.adb.api.scrcpy

/**
 * Типизированный результат проверки доступности scrcpy.
 */
sealed class ScrcpyCheckResult {

    /** scrcpy доступен и вернул строку версии. */
    data class Available(val version: String) : ScrcpyCheckResult()

    /** scrcpy недоступен. [reason] содержит диагностику для UI. */
    data class NotAvailable(val reason: String) : ScrcpyCheckResult()
}

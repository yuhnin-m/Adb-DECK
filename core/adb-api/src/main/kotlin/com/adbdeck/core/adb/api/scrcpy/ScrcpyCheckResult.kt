package com.adbdeck.core.adb.api.scrcpy

import com.adbdeck.core.adb.api.ToolCheckFailureKind

/**
 * Типизированный результат проверки доступности scrcpy.
 */
sealed class ScrcpyCheckResult {

    /** scrcpy доступен и вернул строку версии. */
    data class Available(val version: String) : ScrcpyCheckResult()

    /** scrcpy недоступен. [reason] содержит диагностику для UI. */
    data class NotAvailable(
        val reason: String,
        val kind: ToolCheckFailureKind = ToolCheckFailureKind.UNKNOWN,
    ) : ScrcpyCheckResult()
}

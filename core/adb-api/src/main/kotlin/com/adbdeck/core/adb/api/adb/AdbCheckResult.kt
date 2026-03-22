package com.adbdeck.core.adb.api.adb

import com.adbdeck.core.adb.api.ToolCheckFailureKind

/**
 * Результат проверки доступности исполняемого файла adb.
 */
sealed class AdbCheckResult {

    /**
     * adb найден и доступен.
     *
     * @param version Строка версии из вывода `adb version`.
     */
    data class Available(val version: String) : AdbCheckResult()

    /**
     * adb не найден или недоступен.
     *
     * @param reason Описание причины недоступности.
     * @param kind Категория ошибки для UI-локализации.
     */
    data class NotAvailable(
        val reason: String,
        val kind: ToolCheckFailureKind = ToolCheckFailureKind.UNKNOWN,
    ) : AdbCheckResult()
}

package com.adbdeck.core.adb.api

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
     */
    data class NotAvailable(val reason: String) : AdbCheckResult()
}

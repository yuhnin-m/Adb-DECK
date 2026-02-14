package com.adbdeck.core.adb.api.adb

/**
 * Результат проверки доступности bundletool.
 */
sealed class BundletoolCheckResult {

    /**
     * bundletool найден и доступен.
     *
     * @param version Строка версии (как правило, результат `bundletool version`).
     */
    data class Available(val version: String) : BundletoolCheckResult()

    /**
     * bundletool не найден или недоступен.
     *
     * @param reason Описание причины недоступности.
     */
    data class NotAvailable(val reason: String) : BundletoolCheckResult()
}

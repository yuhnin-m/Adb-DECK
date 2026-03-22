package com.adbdeck.core.adb.api.adb

import com.adbdeck.core.adb.api.ToolCheckFailureKind

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
     * @param kind Категория ошибки для UI-локализации.
     */
    data class NotAvailable(
        val reason: String,
        val kind: ToolCheckFailureKind = ToolCheckFailureKind.UNKNOWN,
    ) : BundletoolCheckResult()
}

package com.adbdeck.core.adb.api.contacts

/**
 * Результат операции импорта контактов на устройство.
 *
 * @param successCount Количество успешно импортированных контактов.
 * @param failedCount  Количество контактов с ошибками.
 * @param errors       Описания ошибок для конкретных контактов.
 */
data class ImportResult(
    val successCount: Int,
    val failedCount: Int,
    val errors: List<String>,
)

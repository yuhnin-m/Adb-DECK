package com.adbdeck.core.process

/**
 * Запись истории выполнения внешней команды.
 *
 * Хранит только метаданные выполнения и короткий текст ошибки.
 * Полные `stdout/stderr` намеренно не сохраняются, чтобы не раздувать память.
 */
data class ProcessHistoryEntry(
    val id: Long,
    val timestampEpochMs: Long,
    val command: List<String>,
    val commandText: String,
    val timeoutMs: Long,
    val durationMs: Long,
    val exitCode: Int?,
    val deviceId: String?,
    val shortError: String?,
) {
    /** Успешное завершение OS-процесса. */
    val isSuccess: Boolean
        get() = exitCode == 0 && shortError.isNullOrBlank()
}

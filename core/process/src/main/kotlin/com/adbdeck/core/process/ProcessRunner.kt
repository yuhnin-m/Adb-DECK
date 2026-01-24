package com.adbdeck.core.process

/**
 * Интерфейс запуска внешних системных процессов.
 *
 * UI и бизнес-логика не должны напрямую работать с [ProcessBuilder] —
 * все проходит через этот контракт, что упрощает подмену реализации.
 */
interface ProcessRunner {

    companion object {
        /** Таймаут по умолчанию для запуска внешней команды. */
        const val DEFAULT_TIMEOUT_MS: Long = 60_000L
    }

    /**
     * Запускает процесс с указанными аргументами и возвращает результат.
     *
     * @param command Список аргументов команды (первый элемент — исполняемый файл).
     * @param timeoutMs Максимальное время ожидания завершения процесса.
     * @return [ProcessResult] с кодом завершения, stdout и stderr.
     */
    suspend fun run(
        command: List<String>,
        timeoutMs: Long = DEFAULT_TIMEOUT_MS,
    ): ProcessResult

    /**
     * Удобная перегрузка: принимает аргументы через vararg.
     *
     * @param command Аргументы команды.
     * @param timeoutMs Максимальное время ожидания завершения процесса.
     */
    suspend fun run(
        vararg command: String,
    ): ProcessResult = run(command.toList(), DEFAULT_TIMEOUT_MS)

    /**
     * Перегрузка vararg с явным таймаутом.
     */
    suspend fun run(
        timeoutMs: Long,
        vararg command: String,
    ): ProcessResult = run(command.toList(), timeoutMs)
}

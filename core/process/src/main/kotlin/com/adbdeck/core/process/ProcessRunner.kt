package com.adbdeck.core.process

/**
 * Интерфейс запуска внешних системных процессов.
 *
 * UI и бизнес-логика не должны напрямую работать с [ProcessBuilder] —
 * все проходит через этот контракт, что упрощает подмену реализации.
 */
interface ProcessRunner {

    /**
     * Запускает процесс с указанными аргументами и возвращает результат.
     *
     * @param command Список аргументов команды (первый элемент — исполняемый файл).
     * @return [ProcessResult] с кодом завершения, stdout и stderr.
     */
    suspend fun run(command: List<String>): ProcessResult

    /**
     * Удобная перегрузка: принимает аргументы через vararg.
     *
     * @param command Аргументы команды.
     */
    suspend fun run(vararg command: String): ProcessResult = run(command.toList())
}

package com.adbdeck.core.process

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Реализация [ProcessRunner] на основе стандартного [ProcessBuilder] JVM.
 *
 * Запуск происходит на [Dispatchers.IO], чтобы не блокировать основной поток.
 * stdout и stderr читаются параллельно во избежание deadlock при большом выводе.
 */
class SystemProcessRunner : ProcessRunner {

    override suspend fun run(command: List<String>): ProcessResult = withContext(Dispatchers.IO) {
        val process = ProcessBuilder(command)
            .redirectErrorStream(false)
            .start()

        // Читаем оба потока до завершения процесса
        val stdoutFuture = process.inputStream.bufferedReader().readText()
        val stderrFuture = process.errorStream.bufferedReader().readText()
        val exitCode = process.waitFor()

        ProcessResult(
            exitCode = exitCode,
            stdout = stdoutFuture,
            stderr = stderrFuture,
        )
    }
}

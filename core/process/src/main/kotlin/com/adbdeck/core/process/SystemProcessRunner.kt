package com.adbdeck.core.process

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext

/**
 * Реализация [ProcessRunner] на основе стандартного [ProcessBuilder] JVM.
 *
 * Запуск происходит на [Dispatchers.IO], чтобы не блокировать основной поток.
 * stdout и stderr читаются параллельно во избежание deadlock при большом выводе.
 */
class SystemProcessRunner : ProcessRunner {

    override suspend fun run(command: List<String>): ProcessResult = withContext(Dispatchers.IO) {
        coroutineScope {
            val process = ProcessBuilder(command)
                .redirectErrorStream(false)
                .start()

            try {
                // Важно читать stdout/stderr параллельно: иначе можно зависнуть на заполненном буфере.
                val stdoutDeferred = async(Dispatchers.IO) {
                    process.inputStream.bufferedReader().use { it.readText() }
                }
                val stderrDeferred = async(Dispatchers.IO) {
                    process.errorStream.bufferedReader().use { it.readText() }
                }

                val exitCode = process.waitFor()

                ProcessResult(
                    exitCode = exitCode,
                    stdout = stdoutDeferred.await(),
                    stderr = stderrDeferred.await(),
                )
            } catch (e: Exception) {
                process.destroyForcibly()
                throw e
            }
        }
    }
}

package com.adbdeck.core.process

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

/**
 * Реализация [ProcessRunner] на основе стандартного [ProcessBuilder] JVM.
 *
 * Запуск происходит на [Dispatchers.IO], чтобы не блокировать основной поток.
 * stdout и stderr читаются параллельно во избежание deadlock при большом выводе.
 */
class SystemProcessRunner : ProcessRunner {

    private companion object {
        private const val POLL_TIMEOUT_MS = 200L
    }

    override suspend fun run(
        command: List<String>,
        timeoutMs: Long,
    ): ProcessResult = withContext(Dispatchers.IO) {
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

                val timeoutDeadlineNs = System.nanoTime() + timeoutMs * 1_000_000L
                while (true) {
                    currentCoroutineContext().ensureActive()
                    val finished = process.waitFor(POLL_TIMEOUT_MS, TimeUnit.MILLISECONDS)
                    if (finished) break
                    if (System.nanoTime() >= timeoutDeadlineNs) {
                        process.destroyForcibly()
                        stdoutDeferred.cancel()
                        stderrDeferred.cancel()
                        error("Command timed out after ${timeoutMs / 1000}s: ${command.joinToString(" ")}")
                    }
                }

                val exitCode = process.exitValue()

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

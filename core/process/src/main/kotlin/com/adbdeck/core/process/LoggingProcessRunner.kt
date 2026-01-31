package com.adbdeck.core.process

import kotlinx.coroutines.CancellationException
import java.util.concurrent.atomic.AtomicLong

/**
 * Декоратор [ProcessRunner], который пишет метаданные запусков в [ProcessHistoryStore].
 *
 * Назначение:
 * - дать пользователю сквозную историю выполненных команд;
 * - не хранить тяжелые payload'ы (`stdout/stderr`) в памяти.
 */
class LoggingProcessRunner(
    private val delegate: ProcessRunner,
    private val historyStore: ProcessHistoryStore,
    private val maxShortErrorLength: Int = DEFAULT_MAX_SHORT_ERROR_LENGTH,
) : ProcessRunner {

    private val nextId = AtomicLong(1L)

    override suspend fun run(
        command: List<String>,
        timeoutMs: Long,
    ): ProcessResult {
        val startedAt = System.currentTimeMillis()

        return try {
            val result = delegate.run(command = command, timeoutMs = timeoutMs)
            historyStore.append(
                ProcessHistoryEntry(
                    id = nextId.getAndIncrement(),
                    timestampEpochMs = startedAt,
                    command = command,
                    commandText = command.toShellLikeString(),
                    timeoutMs = timeoutMs,
                    durationMs = (System.currentTimeMillis() - startedAt).coerceAtLeast(0L),
                    exitCode = result.exitCode,
                    deviceId = command.extractDeviceId(),
                    shortError = if (result.exitCode == 0) {
                        null
                    } else {
                        resolveShortError(
                            stderr = result.stderr,
                            stdout = result.stdout,
                            fallback = "Process exited with code ${result.exitCode}.",
                        )
                    },
                )
            )
            result
        } catch (cancellation: CancellationException) {
            // Отмены не логируем, чтобы не зашумлять историю служебными событиями.
            throw cancellation
        } catch (error: Throwable) {
            historyStore.append(
                ProcessHistoryEntry(
                    id = nextId.getAndIncrement(),
                    timestampEpochMs = startedAt,
                    command = command,
                    commandText = command.toShellLikeString(),
                    timeoutMs = timeoutMs,
                    durationMs = (System.currentTimeMillis() - startedAt).coerceAtLeast(0L),
                    exitCode = null,
                    deviceId = command.extractDeviceId(),
                    shortError = resolveShortError(
                        stderr = error.message.orEmpty(),
                        stdout = "",
                        fallback = error::class.simpleName ?: "Unknown error.",
                    ),
                )
            )
            throw error
        }
    }

    private fun resolveShortError(
        stderr: String,
        stdout: String,
        fallback: String,
    ): String {
        val source = stderr.trim().ifBlank {
            stdout.trim()
        }.ifBlank {
            fallback
        }
        return source.take(maxShortErrorLength)
    }

    private fun List<String>.extractDeviceId(): String? {
        val markerIndex = indexOf("-s")
        if (markerIndex < 0) return null
        return getOrNull(markerIndex + 1)?.takeIf { it.isNotBlank() }
    }

    private fun List<String>.toShellLikeString(): String = joinToString(separator = " ") { token ->
        if (token.any { character -> character.isWhitespace() || character == '"' }) {
            "\"${token.replace("\"", "\\\"")}\""
        } else {
            token
        }
    }

    private companion object {
        const val DEFAULT_MAX_SHORT_ERROR_LENGTH: Int = 8 * 1024
    }
}

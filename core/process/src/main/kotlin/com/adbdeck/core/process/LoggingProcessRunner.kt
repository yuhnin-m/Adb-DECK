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
        val historyCommand = command.sanitizeForHistory()
        val historyCommandText = historyCommand.toShellLikeString()

        return try {
            val result = delegate.run(command = command, timeoutMs = timeoutMs)
            historyStore.append(
                ProcessHistoryEntry(
                    id = nextId.getAndIncrement(),
                    timestampEpochMs = startedAt,
                    command = historyCommand,
                    commandText = historyCommandText,
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
                    command = historyCommand,
                    commandText = historyCommandText,
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

    private fun List<String>.sanitizeForHistory(): List<String> = map { token ->
        token.sanitizeTokenForHistory()
    }

    private fun String.sanitizeTokenForHistory(): String {
        if (isDataBase64Uri()) {
            val header = substringBefore(',', missingDelimiterValue = DATA_BASE64_HEADER)
            return "$header,<omitted $length chars>"
        }
        if (length <= MAX_HISTORY_TOKEN_LENGTH) return this

        val head = take(MAX_HISTORY_TOKEN_HEAD_LENGTH)
        val tail = takeLast(MAX_HISTORY_TOKEN_TAIL_LENGTH)
        val hiddenCount = (length - head.length - tail.length).coerceAtLeast(0)
        return "$head<...$hiddenCount chars...>$tail"
    }

    private fun String.isDataBase64Uri(): Boolean {
        val lower = lowercase()
        return lower.startsWith("data:base64,") || (lower.startsWith("data:") && lower.contains(";base64,"))
    }

    private fun List<String>.toShellLikeString(): String {
        val text = joinToString(separator = " ") { token ->
            if (token.any { character -> character.isWhitespace() || character == '"' }) {
                "\"${token.replace("\"", "\\\"")}\""
            } else {
                token
            }
        }
        if (text.length <= MAX_HISTORY_COMMAND_TEXT_LENGTH) return text
        val omitted = text.length - MAX_HISTORY_COMMAND_TEXT_LENGTH
        return text.take(MAX_HISTORY_COMMAND_TEXT_LENGTH) + "<...$omitted chars omitted...>"
    }

    private companion object {
        const val DEFAULT_MAX_SHORT_ERROR_LENGTH: Int = 8 * 1024
        const val MAX_HISTORY_TOKEN_LENGTH: Int = 1_024
        const val MAX_HISTORY_TOKEN_HEAD_LENGTH: Int = 320
        const val MAX_HISTORY_TOKEN_TAIL_LENGTH: Int = 120
        const val MAX_HISTORY_COMMAND_TEXT_LENGTH: Int = 4 * 1_024
        const val DATA_BASE64_HEADER: String = "data:base64"
    }
}

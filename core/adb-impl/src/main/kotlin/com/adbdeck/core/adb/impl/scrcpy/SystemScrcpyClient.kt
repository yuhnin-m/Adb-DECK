package com.adbdeck.core.adb.impl.scrcpy

import com.adbdeck.core.adb.api.scrcpy.ScrcpyCheckResult
import com.adbdeck.core.adb.api.scrcpy.ScrcpyClient
import com.adbdeck.core.adb.api.scrcpy.ScrcpyExitResult
import com.adbdeck.core.adb.api.scrcpy.ScrcpyLaunchRequest
import com.adbdeck.core.adb.api.scrcpy.ScrcpySession
import com.adbdeck.core.process.ProcessHistoryEntry
import com.adbdeck.core.process.ProcessHistoryStore
import com.adbdeck.core.process.ProcessRunner
import com.adbdeck.core.settings.SettingsRepository
import com.adbdeck.core.utils.runCatchingPreserveCancellation
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.UUID
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

/**
 * Реализация [ScrcpyClient] через системный исполняемый файл scrcpy.
 */
class SystemScrcpyClient(
    private val processRunner: ProcessRunner,
    private val settingsRepository: SettingsRepository,
    private val historyStore: ProcessHistoryStore,
) : ScrcpyClient {

    private val nextHistoryId = AtomicLong(-1L)

    override suspend fun checkAvailability(scrcpyPathOverride: String?): ScrcpyCheckResult {
        val path = resolveScrcpyPath(scrcpyPathOverride)
        return runCatchingPreserveCancellation {
            processRunner.run(path, "--version")
        }.fold(
            onSuccess = { result ->
                if (result.isSuccess) {
                    ScrcpyCheckResult.Available(
                        version = result.firstOutputLine.ifBlank { path },
                    )
                } else {
                    ScrcpyCheckResult.NotAvailable(
                        reason = result.combinedOutput().ifBlank {
                            "scrcpy завершился с кодом ${result.exitCode}"
                        },
                    )
                }
            },
            onFailure = { error ->
                ScrcpyCheckResult.NotAvailable(
                    reason = "Не удалось запустить scrcpy: ${error.message}",
                )
            },
        )
    }

    override suspend fun startSession(
        request: ScrcpyLaunchRequest,
        scrcpyPathOverride: String?,
    ): Result<ScrcpySession> = runCatchingPreserveCancellation {
        val command = buildCommand(
            scrcpyPath = resolveScrcpyPath(scrcpyPathOverride),
            request = request,
        )
        val startedAtMs = System.currentTimeMillis()
        var historyAppended = false

        fun appendStartHistory(
            exitCode: Int?,
            shortError: String?,
        ) {
            if (historyAppended) return
            historyAppended = true
            historyStore.append(
                ProcessHistoryEntry(
                    id = nextHistoryId.getAndDecrement(),
                    timestampEpochMs = startedAtMs,
                    command = command,
                    commandText = command.toShellLikeString(),
                    timeoutMs = ProcessRunner.DEFAULT_TIMEOUT_MS,
                    durationMs = (System.currentTimeMillis() - startedAtMs).coerceAtLeast(0L),
                    exitCode = exitCode,
                    deviceId = request.deviceId,
                    shortError = shortError?.take(MAX_SHORT_ERROR_LENGTH),
                )
            )
        }

        try {
            val process = withContext(Dispatchers.IO) {
                ProcessBuilder(command)
                    .redirectErrorStream(true)
                    .start()
            }

            val session = ProcessScrcpySession(
                process = process,
                deviceId = request.deviceId,
            )

            // Ранняя проверка на «мгновенное падение», чтобы UI сразу получил причину.
            delay(600L)
            if (!session.isAlive()) {
                val exit = session.awaitExit()
                val reason = exit.output.ifBlank { "exit=${exit.exitCode}" }
                appendStartHistory(
                    exitCode = exit.exitCode,
                    shortError = reason,
                )
                error(reason)
            }

            appendStartHistory(exitCode = 0, shortError = null)
            session
        } catch (cancel: CancellationException) {
            throw cancel
        } catch (error: Throwable) {
            appendStartHistory(
                exitCode = null,
                shortError = error.message ?: error::class.simpleName ?: "Failed to start scrcpy",
            )
            throw error
        }
    }

    private fun resolveScrcpyPath(scrcpyPathOverride: String? = null): String {
        val override = scrcpyPathOverride?.trim()?.ifBlank { null }
        return override ?: settingsRepository.getSettings().scrcpyPath.ifBlank { "scrcpy" }
    }

    private fun buildCommand(
        scrcpyPath: String,
        request: ScrcpyLaunchRequest,
    ): List<String> = buildList {
        add(scrcpyPath)
        add("-s")
        add(request.deviceId)

        request.maxSize
            ?.takeIf { it > 0 }
            ?.let { size ->
                add("--max-size")
                add(size.toString())
            }

        add("--max-fps")
        add(request.maxFps.coerceIn(1, 240).toString())

        request.bitrateMbps
            ?.takeIf { it > 0 }
            ?.let { bitrate ->
                add("--video-bit-rate")
                add("${bitrate}M")
            }

        if (!request.allowInput) add("--no-control")
        if (request.turnScreenOff) add("--turn-screen-off")
        if (request.showTouches) add("--show-touches")
        if (request.stayAwake) add("--stay-awake")

        if (request.fullscreen) add("--fullscreen")
        if (request.alwaysOnTop) add("--always-on-top")
        if (request.borderless) add("--window-borderless")

        request.windowWidth
            ?.takeIf { it > 0 }
            ?.let { width ->
                add("--window-width")
                add(width.toString())
            }

        request.windowHeight
            ?.takeIf { it > 0 }
            ?.let { height ->
                add("--window-height")
                add(height.toString())
            }

        add("--video-codec")
        add(request.videoCodec)

        add("--keyboard")
        add(request.keyboardMode)

        add("--mouse")
        add(request.mouseMode)
    }

    private class ProcessScrcpySession(
        private val process: Process,
        override val deviceId: String,
    ) : ScrcpySession {

        override val sessionId: String = UUID.randomUUID().toString()

        private val exitMutex = Mutex()
        private var cachedExit: ScrcpyExitResult? = null

        override fun isAlive(): Boolean = process.isAlive

        override suspend fun awaitExit(): ScrcpyExitResult = exitMutex.withLock {
            cachedExit?.let { return it }

            coroutineScope {
                val outputReader = async(Dispatchers.IO) {
                    process.inputStream.bufferedReader().use { it.readText() }
                }

                val exitCode = withContext(Dispatchers.IO) {
                    process.waitFor()
                    process.exitValue()
                }

                val output = runCatchingPreserveCancellation {
                    outputReader.await().trim()
                }.getOrElse {
                    if (it is CancellationException) throw it
                    ""
                }

                ScrcpyExitResult(
                    exitCode = exitCode,
                    output = output,
                ).also { cachedExit = it }
            }
        }

        override suspend fun stop(gracefulTimeoutMs: Long): Result<Unit> = runCatchingPreserveCancellation {
            if (!process.isAlive) return@runCatchingPreserveCancellation

            withContext(Dispatchers.IO) {
                process.destroy()
            }

            val deadline = System.nanoTime() + gracefulTimeoutMs.coerceAtLeast(0L) * 1_000_000L
            while (process.isAlive && System.nanoTime() < deadline) {
                delay(100L)
            }

            if (process.isAlive) {
                withContext(Dispatchers.IO) {
                    process.destroyForcibly()
                }
                withContext(Dispatchers.IO) {
                    process.waitFor(1, TimeUnit.SECONDS)
                }
            }
        }
    }

    private fun com.adbdeck.core.process.ProcessResult.combinedOutput(): String =
        sequenceOf(stdout, stderr)
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .joinToString(separator = "\n")

    private fun List<String>.toShellLikeString(): String {
        val text = joinToString(separator = " ") { token ->
            if (token.any { char -> char.isWhitespace() || char == '"' }) {
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
        const val MAX_HISTORY_COMMAND_TEXT_LENGTH = 4 * 1_024
        const val MAX_SHORT_ERROR_LENGTH = 8 * 1_024
    }
}

package com.adbdeck.core.adb.impl.screen

import com.adbdeck.core.adb.api.screen.ApkInstallOptions
import com.adbdeck.core.adb.api.screen.ApkInstallProgress
import com.adbdeck.core.adb.api.screen.ScreenToolsClient
import com.adbdeck.core.adb.api.screen.ScreenrecordOptions
import com.adbdeck.core.adb.api.screen.ScreenrecordSession
import com.adbdeck.core.adb.api.screen.ScreenshotFormat
import com.adbdeck.core.adb.api.screen.ScreenshotOptions
import com.adbdeck.core.process.ProcessResult
import com.adbdeck.core.process.ProcessRunner
import com.adbdeck.core.utils.runCatchingPreserveCancellation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import javax.imageio.IIOImage
import javax.imageio.ImageIO
import javax.imageio.ImageWriteParam

/**
 * Реализация [com.adbdeck.core.adb.api.screen.ScreenToolsClient] через системный `adb`.
 *
 * Ключевые моменты:
 * - screenshot снимается через `adb exec-out screencap -p`
 * - screenrecord запускается в фоне на устройстве c сохранением remote PID
 * - остановка записи выполняется через `SIGINT` (kill -2), что корректно
 *   финализирует mp4-файл перед pull
 */
class SystemScreenToolsClient(
    private val processRunner: ProcessRunner,
) : ScreenToolsClient {

    private companion object {
        private const val SCREENSHOT_TIMEOUT_MS = 25_000L
        private const val APK_INSTALL_TIMEOUT_MS = 8 * 60_000L
        private const val PROCESS_POLL_TIMEOUT_MS = 200L

        private const val START_PID_PREFIX = "__PID__:"
        private const val START_ERR_PREFIX = "__ERR__START_FAILED__"
        private val APK_PROGRESS_REGEX = Regex("""\[\s*(\d{1,3})%\]""")
    }

    /**
     * Внутренняя мета-информация активной записи.
     */
    private data class SessionMeta(
        val deviceId: String,
        val adbPath: String,
        val remotePid: String,
        val remotePath: String,
    )

    private val sessionMutex = Mutex()
    private val sessionsById = ConcurrentHashMap<String, SessionMeta>()
    private val sessionIdByDevice = ConcurrentHashMap<String, String>()

    override suspend fun takeScreenshot(
        deviceId: String,
        localOutputPath: String,
        adbPath: String,
        options: ScreenshotOptions,
    ): Result<Unit> = runCatchingPreserveCancellation {
        val targetFile = File(localOutputPath)
        val tempPng = if (options.format == ScreenshotFormat.PNG) {
            targetFile
        } else {
            File.createTempFile("adbdeck_screenshot_", ".png")
        }

        withContext(Dispatchers.IO) {
            targetFile.parentFile?.mkdirs()
            captureRawPng(
                deviceId = deviceId,
                adbPath = adbPath,
                targetPng = tempPng,
            )

            if (options.format == ScreenshotFormat.JPEG) {
                convertPngToJpeg(
                    sourcePng = tempPng,
                    targetJpeg = targetFile,
                    qualityPercent = options.jpegQualityPercent,
                )
                tempPng.delete()
            }

            if (!targetFile.exists() || targetFile.length() <= 0L) {
                targetFile.delete()
                error("ADB вернул пустой screenshot")
            }
        }
    }

    override suspend fun startScreenrecord(
        deviceId: String,
        remoteOutputPath: String,
        adbPath: String,
        options: ScreenrecordOptions,
    ): Result<ScreenrecordSession> = runCatchingPreserveCancellation {
        sessionMutex.withLock {
            if (sessionIdByDevice.containsKey(deviceId)) {
                error("Для устройства '$deviceId' запись уже выполняется")
            }
        }

        val bitRateMbps = options.bitRateMbps.coerceIn(1, 100)
        val bitRateBps = bitRateMbps * 1_000_000
        val size = options.videoSize
            ?.trim()
            ?.takeIf { it.matches(Regex("^\\d{2,5}x\\d{2,5}$")) }
            .orEmpty()

        val script = buildString {
            appendLine("out=${shellQuote(remoteOutputPath)}")
            appendLine("size=${shellQuote(size)}")
            appendLine("rm -f \"${'$'}out\"")
            appendLine("if [ -n \"${'$'}size\" ]; then")
            appendLine("  screenrecord --bit-rate $bitRateBps --size \"${'$'}size\" \"${'$'}out\" >/dev/null 2>&1 &")
            appendLine("else")
            appendLine("  screenrecord --bit-rate $bitRateBps \"${'$'}out\" >/dev/null 2>&1 &")
            appendLine("fi")
            appendLine("pid=${'$'}!")
            appendLine("sleep 0.3")
            appendLine("if [ -z \"${'$'}pid\" ]; then")
            appendLine("  echo \"$START_ERR_PREFIX\"")
            appendLine("  exit 21")
            appendLine("fi")
            appendLine("if ! kill -0 \"${'$'}pid\" >/dev/null 2>&1; then")
            appendLine("  wait \"${'$'}pid\" >/dev/null 2>&1 || true")
            appendLine("  echo \"$START_ERR_PREFIX\"")
            appendLine("  exit 22")
            appendLine("fi")
            appendLine("echo \"$START_PID_PREFIX${'$'}pid\"")
        }

        val result = runAdbShell(
            deviceId = deviceId,
            adbPath = adbPath,
            script = script,
        )

        if (!result.isSuccess || result.stdout.contains(START_ERR_PREFIX)) {
            error(
                buildString {
                    append("Не удалось запустить screenrecord. ")
                    append("exitCode=")
                    append(result.exitCode)
                    append("; stdout=")
                    append(result.stdout.trim().ifBlank { "пусто" })
                    append("; stderr=")
                    append(result.stderr.trim().ifBlank { "пусто" })
                }
            )
        }

        val remotePid = result.stdout
            .lineSequence()
            .map { it.trim() }
            .firstOrNull { it.startsWith(START_PID_PREFIX) }
            ?.removePrefix(START_PID_PREFIX)
            ?.trim()
            ?.takeIf { it.all(Char::isDigit) }
            ?: error("ADB не вернул PID screenrecord-процесса")

        val session = ScreenrecordSession(
            sessionId = UUID.randomUUID().toString(),
            deviceId = deviceId,
            remotePath = remoteOutputPath,
            remotePid = remotePid,
            startedAtEpochMillis = System.currentTimeMillis(),
        )

        val registered = sessionMutex.withLock {
            if (sessionIdByDevice.containsKey(deviceId)) {
                false
            } else {
                sessionsById[session.sessionId] = SessionMeta(
                    deviceId = deviceId,
                    adbPath = adbPath,
                    remotePid = remotePid,
                    remotePath = remoteOutputPath,
                )
                sessionIdByDevice[deviceId] = session.sessionId
                true
            }
        }

        if (!registered) {
            stopRemoteRecordProcess(deviceId = deviceId, adbPath = adbPath, remotePid = remotePid)
            error("Для устройства '$deviceId' запись уже выполняется")
        }

        session
    }

    override suspend fun stopScreenrecord(
        sessionId: String,
    ): Result<Unit> = runCatchingPreserveCancellation {
        val meta = sessionMutex.withLock {
            val current = sessionsById.remove(sessionId)
                ?: error("Сессия screenrecord не найдена: $sessionId")
            sessionIdByDevice.remove(current.deviceId, sessionId)
            current
        }

        val stopResult = stopRemoteRecordProcess(
            deviceId = meta.deviceId,
            adbPath = meta.adbPath,
            remotePid = meta.remotePid,
        )

        if (!stopResult.isSuccess) {
            error(
                buildString {
                    append("Не удалось остановить screenrecord (pid=")
                    append(meta.remotePid)
                    append("). ")
                    append("exitCode=")
                    append(stopResult.exitCode)
                    append("; stdout=")
                    append(stopResult.stdout.trim().ifBlank { "пусто" })
                    append("; stderr=")
                    append(stopResult.stderr.trim().ifBlank { "пусто" })
                }
            )
        }
    }

    override suspend fun installApk(
        deviceId: String,
        localApkPath: String,
        adbPath: String,
        options: ApkInstallOptions,
        onProgress: (ApkInstallProgress) -> Unit,
    ): Result<Unit> = runCatchingPreserveCancellation {
        val apkFile = File(localApkPath.trim())
        if (!apkFile.isFile) {
            error("APK файл не найден: ${apkFile.absolutePath}")
        }
        if (!apkFile.name.endsWith(".apk", ignoreCase = true)) {
            error("Ожидался .apk файл: ${apkFile.absolutePath}")
        }

        val command = buildList {
            add(adbPath)
            add("-s")
            add(deviceId)
            add("install")
            if (options.reinstall) add("-r")
            if (options.allowDowngrade) add("-d")
            if (options.grantRuntimePermissions) add("-g")
            add(apkFile.absolutePath)
        }

        withContext(Dispatchers.IO) {
            onProgress(ApkInstallProgress(progress = 0f, message = "Запускаем adb install…"))

            val process = ProcessBuilder(command)
                .redirectErrorStream(true)
                .start()

            coroutineScope {
                val output = StringBuilder()
                val readerDeferred = async(Dispatchers.IO) {
                    process.inputStream.bufferedReader().useLines { lines ->
                        lines.forEach { rawLine ->
                            val line = rawLine.trim()
                            if (line.isEmpty()) return@forEach
                            output.appendLine(line)

                            onProgress(
                                ApkInstallProgress(
                                    progress = parseApkInstallProgress(line),
                                    message = line,
                                )
                            )
                        }
                    }
                }

                try {
                    if (!waitForProcess(process, APK_INSTALL_TIMEOUT_MS)) {
                        process.destroyForcibly()
                        readerDeferred.cancel()
                        error("Установка APK превысила таймаут ${APK_INSTALL_TIMEOUT_MS / 1000}с")
                    }

                    readerDeferred.await()
                    val exitCode = process.exitValue()
                    val outputText = output.toString().trim()
                    val success = exitCode == 0 &&
                            outputText.lineSequence().any { it.contains("Success", ignoreCase = true) }

                    if (!success) {
                        error(
                            buildString {
                                append("Не удалось установить APK. ")
                                append("exitCode=")
                                append(exitCode)
                                append(". ADB: ")
                                append(outputText.ifBlank { "детали не получены" })
                            }
                        )
                    }

                    onProgress(ApkInstallProgress(progress = 1f, message = "Установка APK завершена"))
                } catch (t: Throwable) {
                    process.destroyForcibly()
                    throw t
                } finally {
                    process.destroy()
                }
            }
        }
    }

    /**
     * Снимает raw PNG с устройства в [targetPng].
     */
    private suspend fun captureRawPng(
        deviceId: String,
        adbPath: String,
        targetPng: File,
    ) {
        val command = listOf(adbPath, "-s", deviceId, "exec-out", "screencap", "-p")
        val process = ProcessBuilder(command)
            .redirectErrorStream(false)
            .start()

        coroutineScope {
            val stderrDeferred = async(Dispatchers.IO) {
                process.errorStream.bufferedReader().use { it.readText() }
            }
            val copyDeferred = async(Dispatchers.IO) {
                targetPng.outputStream().buffered().use { output ->
                    process.inputStream.use { input ->
                        input.copyTo(output)
                    }
                }
            }

            try {
                if (!waitForProcess(process, SCREENSHOT_TIMEOUT_MS)) {
                    process.destroyForcibly()
                    copyDeferred.cancel()
                    stderrDeferred.cancel()
                    error("Скриншот не завершился за отведённое время")
                }

                copyDeferred.await()
                val stderrText = stderrDeferred.await().trim()
                val exitCode = process.exitValue()

                if (exitCode != 0) {
                    targetPng.delete()
                    error(
                        "ADB screenshot завершился с ошибкой (exitCode=$exitCode). " +
                                "Причина: ${stderrText.ifBlank { "детали не получены" }}"
                    )
                }
            } catch (t: Throwable) {
                targetPng.delete()
                process.destroyForcibly()
                throw t
            } finally {
                process.destroy()
            }
        }
    }

    /**
     * Конвертирует PNG в JPEG с указанным quality.
     */
    private fun convertPngToJpeg(
        sourcePng: File,
        targetJpeg: File,
        qualityPercent: Int,
    ) {
        val image = ImageIO.read(sourcePng)
            ?: error("Не удалось прочитать PNG для конвертации")

        val quality = (qualityPercent.coerceIn(1, 100) / 100f)
        val writer = ImageIO.getImageWritersByFormatName("jpeg").asSequence().firstOrNull()
            ?: error("JPEG writer недоступен в текущей JVM")

        ImageIO.createImageOutputStream(targetJpeg).use { output ->
            writer.output = output
            val params = writer.defaultWriteParam
            if (params.canWriteCompressed()) {
                params.compressionMode = ImageWriteParam.MODE_EXPLICIT
                params.compressionQuality = quality
            }
            writer.write(null, IIOImage(image, null, null), params)
            writer.dispose()
        }
    }

    /**
     * Останавливает remote screenrecord-процесс по PID через SIGINT.
     */
    private suspend fun stopRemoteRecordProcess(
        deviceId: String,
        adbPath: String,
        remotePid: String,
    ): ProcessResult {
        val pid = remotePid.trim().takeIf { it.all(Char::isDigit) }
            ?: return ProcessResult(
                exitCode = 1,
                stdout = "",
                stderr = "Некорректный PID: $remotePid",
            )

        val script = """
            pid=$pid
            if kill -0 "${'$'}pid" >/dev/null 2>&1; then
              kill -2 "${'$'}pid" >/dev/null 2>&1 || true
              i=0
              while kill -0 "${'$'}pid" >/dev/null 2>&1; do
                i=$((i + 1))
                if [ "${'$'}i" -ge 120 ]; then
                  kill -15 "${'$'}pid" >/dev/null 2>&1 || true
                  sleep 0.2
                  kill -9 "${'$'}pid" >/dev/null 2>&1 || true
                  break
                fi
                sleep 0.1
              done
            fi
            exit 0
        """.trimIndent()

        return runAdbShell(
            deviceId = deviceId,
            adbPath = adbPath,
            script = script,
        )
    }

    /** Выполняет shell-скрипт на устройстве через `adb shell sh -c`. */
    private suspend fun runAdbShell(
        deviceId: String,
        adbPath: String,
        script: String,
    ): ProcessResult {
        val quotedScript = shellQuote(script)
        return processRunner.run(
            adbPath,
            "-s",
            deviceId,
            "shell",
            "sh",
            "-c",
            quotedScript,
        )
    }

    /**
     * Ожидает завершение процесса с периодическим poll,
     * чтобы уважать отмену корутины.
     */
    private suspend fun waitForProcess(process: Process, timeoutMs: Long): Boolean {
        val deadlineNs = System.nanoTime() + timeoutMs * 1_000_000L
        while (true) {
            currentCoroutineContext().ensureActive()
            val finished = process.waitFor(PROCESS_POLL_TIMEOUT_MS, TimeUnit.MILLISECONDS)
            if (finished) return true
            if (System.nanoTime() >= deadlineNs) return false
        }
    }

    /** Экранирует строку для безопасной вставки в shell-команду. */
    private fun shellQuote(value: String): String =
        "'" + value.replace("'", "'\"'\"'") + "'"

    /** Пытается извлечь процент установки из строки вывода adb. */
    private fun parseApkInstallProgress(line: String): Float? {
        val percent = APK_PROGRESS_REGEX.find(line)
            ?.groupValues
            ?.getOrNull(1)
            ?.toIntOrNull()
            ?.coerceIn(0, 100)
            ?: return null

        return percent / 100f
    }
}
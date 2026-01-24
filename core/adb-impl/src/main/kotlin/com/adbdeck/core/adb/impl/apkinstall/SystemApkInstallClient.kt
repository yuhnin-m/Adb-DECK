package com.adbdeck.core.adb.impl.apkinstall

import com.adbdeck.core.adb.api.apkinstall.ApkInstallClient
import com.adbdeck.core.adb.api.apkinstall.ApkInstallOptions
import com.adbdeck.core.adb.api.apkinstall.ApkInstallProgress
import com.adbdeck.core.process.ProcessRunner
import com.adbdeck.core.utils.runCatchingPreserveCancellation
import java.io.File

/**
 * Реализация [ApkInstallClient] через системный `adb install`.
 */
class SystemApkInstallClient(
    private val processRunner: ProcessRunner,
) : ApkInstallClient {

    private companion object {
        private const val APK_INSTALL_TIMEOUT_MS = 8 * 60_000L
        private val APK_PROGRESS_REGEX = Regex("""\[\s*(\d{1,3})%\]""")
        private const val SUCCESS_MARKER = "Success"
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
            error("APK file not found: ${apkFile.absolutePath}")
        }
        if (!apkFile.name.endsWith(".apk", ignoreCase = true)) {
            error("Expected .apk file: ${apkFile.absolutePath}")
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

        val result = processRunner.run(
            command = command,
            timeoutMs = APK_INSTALL_TIMEOUT_MS,
        )

        val outputLines = collectOutputLines(
            stdout = result.stdout,
            stderr = result.stderr,
        )

        outputLines.forEach { line ->
            onProgress(
                ApkInstallProgress(
                    progress = parseApkInstallProgress(line),
                    message = line,
                )
            )
        }

        val success = result.exitCode == 0 &&
            outputLines.any { it.contains(SUCCESS_MARKER, ignoreCase = true) }
        if (!success) {
            val details = outputLines.joinToString(separator = " | ")
                .ifBlank { "details not available" }
            error(
                buildString {
                    append("Failed to install APK. ")
                    append("exitCode=")
                    append(result.exitCode)
                    append(". ADB: ")
                    append(details)
                }
            )
        }
    }

    /** Объединяет stdout/stderr в компактный список строк для UI и ошибок. */
    private fun collectOutputLines(
        stdout: String,
        stderr: String,
    ): List<String> = buildList {
        stdout.lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .forEach(::add)
        stderr.lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .forEach(::add)
    }

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

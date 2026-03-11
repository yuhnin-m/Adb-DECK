package com.adbdeck.core.adb.impl.apkinstall

import com.adbdeck.core.adb.api.apkinstall.ApkInstallClient
import com.adbdeck.core.adb.api.apkinstall.ApkInstallOptions
import com.adbdeck.core.adb.api.apkinstall.ApkInstallProgress
import com.adbdeck.core.process.ProcessRunner
import com.adbdeck.core.utils.runCatchingPreserveCancellation
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.file.Files
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

/**
 * Реализация [ApkInstallClient] через системный `adb install`.
 */
class SystemApkInstallClient(
    private val processRunner: ProcessRunner,
) : ApkInstallClient {

    private companion object {
        private const val APK_INSTALL_TIMEOUT_MS = 8 * 60_000L
        private const val SPLIT_INSTALL_TIMEOUT_MS = 12 * 60_000L
        private const val AAB_INSTALL_TIMEOUT_MS = 15 * 60_000L
        private val APK_PROGRESS_REGEX = Regex("""\[\s*(\d{1,3})%\]""")
        private const val SUCCESS_MARKER = "Success"
        private val INSTALLABLE_EXTENSIONS = setOf("apk", "aab", "apks", "xapk")
        private val ARCHIVE_EXTENSIONS = setOf("apks", "xapk")
        private const val APK_EXTENSION = "apk"
        private const val AAB_EXTENSION = "aab"
    }

    override suspend fun installApk(
        deviceId: String,
        localApkPath: String,
        adbPath: String,
        options: ApkInstallOptions,
        onProgress: (ApkInstallProgress) -> Unit,
    ): Result<Unit> = runCatchingPreserveCancellation {
        val target = File(localApkPath.trim())
        if (!target.exists()) {
            error("Install target not found: ${target.absolutePath}")
        }

        when {
            target.isDirectory -> installSplitDirectory(
                deviceId = deviceId,
                splitDir = target,
                adbPath = adbPath,
                options = options,
                onProgress = onProgress,
            )

            target.isFile -> {
                val extension = target.extension.lowercase()
                when (extension) {
                    APK_EXTENSION -> installSingleApk(
                        deviceId = deviceId,
                        apkFile = target,
                        adbPath = adbPath,
                        options = options,
                        onProgress = onProgress,
                    )
                    in ARCHIVE_EXTENSIONS -> installArchiveWithSplits(
                        deviceId = deviceId,
                        archiveFile = target,
                        adbPath = adbPath,
                        options = options,
                        bundletoolPath = options.bundletoolPath,
                        onProgress = onProgress,
                    )
                    AAB_EXTENSION -> installAabBundle(
                        deviceId = deviceId,
                        aabFile = target,
                        adbPath = adbPath,
                        bundletoolPath = options.bundletoolPath,
                        onProgress = onProgress,
                    )
                    else -> error(
                        "Unsupported package format: ${target.absolutePath}. " +
                                "Supported: ${INSTALLABLE_EXTENSIONS.joinToString(", ")} or split directory"
                    )
                }
            }

            else -> error("Install target is not file/directory: ${target.absolutePath}")
        }
    }

    /** Установка одиночного APK через `adb install`. */
    private suspend fun installSingleApk(
        deviceId: String,
        apkFile: File,
        adbPath: String,
        options: ApkInstallOptions,
        onProgress: (ApkInstallProgress) -> Unit,
    ) {
        if (!apkFile.name.endsWith(".apk", ignoreCase = true)) {
            error("Expected .apk file: ${apkFile.absolutePath}")
        }
        val command = buildInstallCommand(
            deviceId = deviceId,
            adbPath = adbPath,
            options = options,
            installMultiple = false,
            targets = listOf(apkFile.absolutePath),
        )
        runInstallCommand(
            command = command,
            timeoutMs = APK_INSTALL_TIMEOUT_MS,
            onProgress = onProgress,
        )
    }

    /** Установка split-пакета из каталога APK-файлов через `adb install-multiple`. */
    private suspend fun installSplitDirectory(
        deviceId: String,
        splitDir: File,
        adbPath: String,
        options: ApkInstallOptions,
        onProgress: (ApkInstallProgress) -> Unit,
    ) {
        val splitApks = collectSplitApkFiles(splitDir)
        if (splitApks.isEmpty()) {
            error("Split directory does not contain .apk files: ${splitDir.absolutePath}")
        }
        val command = buildInstallCommand(
            deviceId = deviceId,
            adbPath = adbPath,
            options = options,
            installMultiple = true,
            targets = splitApks.map { it.absolutePath },
        )
        runInstallCommand(
            command = command,
            timeoutMs = SPLIT_INSTALL_TIMEOUT_MS,
            onProgress = onProgress,
        )
    }

    /**
     * Установка архива split APK (`.apks` / `.xapk`).
     *
     * Для `.apks` сначала пробуем `bundletool install-apks` (более корректно для формата).
     * При недоступности bundletool — fallback на распаковку и `adb install-multiple`.
     */
    private suspend fun installArchiveWithSplits(
        deviceId: String,
        archiveFile: File,
        adbPath: String,
        options: ApkInstallOptions,
        bundletoolPath: String?,
        onProgress: (ApkInstallProgress) -> Unit,
    ) {
        if (archiveFile.extension.equals("apks", ignoreCase = true)) {
            val installedByBundletool = runCatchingPreserveCancellation {
                onProgress(ApkInstallProgress(null, "Using bundletool install-apks..."))
                installApksArchiveWithBundletool(
                    deviceId = deviceId,
                    apksFile = archiveFile,
                    adbPath = adbPath,
                    bundletoolPath = bundletoolPath,
                    onProgress = onProgress,
                )
            }
            if (installedByBundletool.isSuccess) return
            val details = installedByBundletool.exceptionOrNull()?.message.orEmpty()
            onProgress(
                ApkInstallProgress(
                    progress = null,
                    message = if (details.isBlank()) {
                        "bundletool install-apks failed, fallback to adb install-multiple"
                    } else {
                        "bundletool install-apks failed: $details; fallback to adb install-multiple"
                    },
                )
            )
        }

        val tempDir = Files.createTempDirectory("adbdeck-install-archive-").toFile()
        try {
            unzipArchive(archiveFile = archiveFile, targetDir = tempDir)
            installSplitDirectory(
                deviceId = deviceId,
                splitDir = tempDir,
                adbPath = adbPath,
                options = options,
                onProgress = onProgress,
            )
        } finally {
            tempDir.deleteRecursively()
        }
    }

    /** Установка `.aab` через bundletool (`build-apks` + `install-apks`). */
    private suspend fun installAabBundle(
        deviceId: String,
        aabFile: File,
        adbPath: String,
        bundletoolPath: String?,
        onProgress: (ApkInstallProgress) -> Unit,
    ) {
        if (!aabFile.name.endsWith(".aab", ignoreCase = true)) {
            error("Expected .aab file: ${aabFile.absolutePath}")
        }

        val outputApks = File.createTempFile("adbdeck-bundletool-", ".apks")
        try {
            onProgress(ApkInstallProgress(null, "Building APKS from AAB via bundletool..."))
            val buildCommand = bundletoolCommand(
                bundletoolPath = bundletoolPath,
                "build-apks",
                "--bundle=${aabFile.absolutePath}",
                "--output=${outputApks.absolutePath}",
                "--connected-device",
                "--device-id=$deviceId",
                "--adb=$adbPath",
            )
            runBundletoolCommand(
                command = buildCommand,
                timeoutMs = AAB_INSTALL_TIMEOUT_MS,
                onProgress = onProgress,
                failurePrefix = "bundletool build-apks failed",
            )

            onProgress(ApkInstallProgress(null, "Installing generated APKS on device..."))
            installApksArchiveWithBundletool(
                deviceId = deviceId,
                apksFile = outputApks,
                adbPath = adbPath,
                bundletoolPath = bundletoolPath,
                onProgress = onProgress,
            )
        } finally {
            outputApks.delete()
        }
    }

    /** Устанавливает `.apks` через `bundletool install-apks`. */
    private suspend fun installApksArchiveWithBundletool(
        deviceId: String,
        apksFile: File,
        adbPath: String,
        bundletoolPath: String?,
        onProgress: (ApkInstallProgress) -> Unit,
    ) {
        val installCommand = bundletoolCommand(
            bundletoolPath = bundletoolPath,
            "install-apks",
            "--apks=${apksFile.absolutePath}",
            "--device-id=$deviceId",
            "--adb=$adbPath",
        )
        runBundletoolCommand(
            command = installCommand,
            timeoutMs = AAB_INSTALL_TIMEOUT_MS,
            onProgress = onProgress,
            failurePrefix = "bundletool install-apks failed",
        )
    }

    /** Выполняет команду bundletool и валидирует успешность по коду/выводу. */
    private suspend fun runBundletoolCommand(
        command: List<String>,
        timeoutMs: Long,
        onProgress: (ApkInstallProgress) -> Unit,
        failurePrefix: String,
    ) {
        val result = processRunner.run(
            command = command,
            timeoutMs = timeoutMs,
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
        val success = result.exitCode == 0 && outputLines.none { it.contains("Exception", ignoreCase = true) }
        if (!success) {
            val details = outputLines.joinToString(separator = " | ")
                .ifBlank { "details not available" }
            error("$failurePrefix. exitCode=${result.exitCode}. $details")
        }
    }

    /** Выполняет `adb install` / `adb install-multiple` и валидирует результат. */
    private suspend fun runInstallCommand(
        command: List<String>,
        timeoutMs: Long,
        onProgress: (ApkInstallProgress) -> Unit,
    ) {
        val result = processRunner.run(
            command = command,
            timeoutMs = timeoutMs,
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

    /** Формирует команду `adb install` или `adb install-multiple` с общими флагами. */
    private fun buildInstallCommand(
        deviceId: String,
        adbPath: String,
        options: ApkInstallOptions,
        installMultiple: Boolean,
        targets: List<String>,
    ): List<String> = buildList {
        add(adbPath)
        add("-s")
        add(deviceId)
        add(if (installMultiple) "install-multiple" else "install")
        if (options.reinstall) add("-r")
        if (options.allowDowngrade) add("-d")
        if (options.grantRuntimePermissions) add("-g")
        if (options.allowTestOnly) add("-t")
        addAll(targets)
    }

    /** Возвращает полную команду bundletool с учетом переменной окружения пути. */
    private fun bundletoolCommand(
        bundletoolPath: String?,
        vararg args: String,
    ): List<String> {
        val customPath = sequenceOf(
            bundletoolPath,
            System.getenv("ADBDECK_BUNDLETOOL_PATH"),
            System.getenv("BUNDLETOOL_PATH"),
        )
            .map { it?.trim().orEmpty() }
            .firstOrNull { it.isNotBlank() }

        val prefix = when {
            customPath.isNullOrBlank() -> listOf("bundletool")
            customPath.endsWith(".jar", ignoreCase = true) -> listOf("java", "-jar", customPath)
            else -> listOf(customPath)
        }
        return prefix + args
    }

    /** Собирает список split APK-файлов из каталога, начиная с base.apk. */
    private fun collectSplitApkFiles(rootDir: File): List<File> {
        val apks = rootDir.walkTopDown()
            .filter { file -> file.isFile && file.name.endsWith(".apk", ignoreCase = true) }
            .toList()

        val prioritized = apks.sortedWith(
            compareBy<File> { file ->
                val lowerName = file.name.lowercase()
                when {
                    lowerName == "base.apk" -> 0
                    lowerName.startsWith("base-") -> 1
                    lowerName.contains("base") -> 2
                    else -> 3
                }
            }.thenBy { it.name.lowercase() }
        )
        return prioritized
    }

    /** Распаковывает zip-архив в целевую директорию. */
    private fun unzipArchive(
        archiveFile: File,
        targetDir: File,
    ) {
        val targetRoot = targetDir.canonicalFile
        ZipInputStream(FileInputStream(archiveFile)).use { input ->
            var entry: ZipEntry? = input.nextEntry
            while (entry != null) {
                val outFile = File(targetDir, entry.name)
                val canonicalOutFile = outFile.canonicalFile
                if (!canonicalOutFile.path.startsWith(targetRoot.path + File.separator) &&
                    canonicalOutFile != targetRoot
                ) {
                    error("Archive entry escapes target directory: ${entry.name}")
                }
                if (entry.isDirectory) {
                    canonicalOutFile.mkdirs()
                } else {
                    canonicalOutFile.parentFile?.mkdirs()
                    FileOutputStream(canonicalOutFile).use { output ->
                        input.copyTo(output)
                    }
                }
                input.closeEntry()
                entry = input.nextEntry
            }
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

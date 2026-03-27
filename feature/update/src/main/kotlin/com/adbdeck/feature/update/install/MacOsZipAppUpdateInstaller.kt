package com.adbdeck.feature.update.install

import com.adbdeck.feature.update.logging.AppUpdateLogger
import com.adbdeck.feature.update.logging.NoOpAppUpdateLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.PosixFilePermission
import kotlin.io.path.exists
import kotlin.io.path.extension

/**
 * [Отключено до лучших времен]
 * In-app установщик обновления для macOS ZIP-ассетов.
 *
 * Логика:
 * 1. Распаковывает ZIP в staging-каталог.
 * 2. Находит `.app` в распакованном содержимом.
 * 3. Генерирует и запускает внешний shell-скрипт, который дождется завершения текущего
 *    процесса, заменит `.app` bundle и снова откроет приложение.
 */
class MacOsZipAppUpdateInstaller(
    private val appUpdateLogger: AppUpdateLogger = NoOpAppUpdateLogger,
) : AppUpdateInstaller {

    override fun canInstallInApp(downloadUrl: String): Boolean {
        return isMacOs() && downloadUrl.lowercase().endsWith(".zip")
    }

    override suspend fun preflightInstall(downloadUrl: String): Unit = withContext(Dispatchers.IO) {
        if (!canInstallInApp(downloadUrl)) {
            throw AppUpdatePreflightException(
                reason = AppUpdatePreflightFailureReason.UNSUPPORTED_PLATFORM_OR_ASSET,
            )
        }

        val currentAppBundle = resolveCurrentAppBundlePath()
            ?: throw AppUpdatePreflightException(
                reason = AppUpdatePreflightFailureReason.CURRENT_APP_BUNDLE_NOT_FOUND,
            )
        if (!Files.isDirectory(currentAppBundle)) {
            throw AppUpdatePreflightException(
                reason = AppUpdatePreflightFailureReason.CURRENT_APP_BUNDLE_INVALID,
            )
        }

        verifyTargetDirectoryWritable(currentAppBundle)
        appUpdateLogger.info("Update preflight passed for app bundle: $currentAppBundle")
    }

    override suspend fun installFromDownloadedPackage(packageFile: Path): Unit = withContext(Dispatchers.IO) {
        check(isMacOs()) { "In-app installer is only supported on macOS." }
        check(packageFile.exists()) { "Update package file does not exist: $packageFile" }
        check(packageFile.extension.lowercase() == "zip") { "Expected ZIP update package: $packageFile" }

        val currentAppBundle = resolveCurrentAppBundlePath()
            ?: error("Cannot locate current .app bundle path for self-update")
        check(Files.isDirectory(currentAppBundle)) {
            "Current app bundle path is not a directory: $currentAppBundle"
        }

        val stagingDir = Files.createTempDirectory("adbdeck-update-staging-")
        val installScript = try {
            unpackWithDitto(zipFile = packageFile, targetDir = stagingDir)
            val unpackedAppBundle = findFirstAppBundle(stagingDir)
                ?: error("No .app bundle found inside update ZIP: $packageFile")

            val script = createInstallScript(
                sourceAppBundle = unpackedAppBundle,
                targetAppBundle = currentAppBundle,
                stagingRoot = stagingDir,
                waitPid = ProcessHandle.current().pid(),
            )
            launchInstallScript(script)
            script
        } catch (error: Throwable) {
            runCatching {
                Files.walk(stagingDir).use { paths ->
                    paths.sorted(Comparator.reverseOrder()).forEach { path ->
                        Files.deleteIfExists(path)
                    }
                }
            }
            throw error
        }

        appUpdateLogger.info("macOS update install script started: $installScript")
    }

    private fun verifyTargetDirectoryWritable(currentAppBundle: Path) {
        val targetDirectory = currentAppBundle.parent
            ?: throw AppUpdatePreflightException(
                reason = AppUpdatePreflightFailureReason.TARGET_DIRECTORY_MISSING,
            )

        if (!Files.isDirectory(targetDirectory)) {
            throw AppUpdatePreflightException(
                reason = AppUpdatePreflightFailureReason.TARGET_DIRECTORY_MISSING,
            )
        }
        if (!Files.isWritable(targetDirectory)) {
            throw AppUpdatePreflightException(
                reason = AppUpdatePreflightFailureReason.TARGET_DIRECTORY_NOT_WRITABLE,
            )
        }

        try {
            val probeFile = Files.createTempFile(
                targetDirectory,
                ".adbdeck-update-permission-check-",
                ".tmp",
            )
            Files.deleteIfExists(probeFile)
        } catch (error: Throwable) {
            throw AppUpdatePreflightException(
                reason = AppUpdatePreflightFailureReason.TARGET_DIRECTORY_NOT_WRITABLE,
                cause = error,
            )
        }
    }

    private fun unpackWithDitto(zipFile: Path, targetDir: Path) {
        val process = ProcessBuilder(
            "ditto",
            "-x",
            "-k",
            zipFile.toString(),
            targetDir.toString(),
        )
            .redirectErrorStream(true)
            .start()

        val output = process.inputStream.bufferedReader().use { it.readText() }
        val exitCode = process.waitFor()
        if (exitCode != 0) {
            error("Failed to unpack update ZIP with ditto (exit=$exitCode): ${output.trim()}")
        }
    }

    private fun launchInstallScript(scriptFile: Path) {
        ProcessBuilder("/bin/bash", scriptFile.toString())
            .directory(scriptFile.parent.toFile())
            .redirectErrorStream(true)
            .start()
    }

    private fun createInstallScript(
        sourceAppBundle: Path,
        targetAppBundle: Path,
        stagingRoot: Path,
        waitPid: Long,
    ): Path {
        val scriptFile = Files.createTempFile("adbdeck-update-install-", ".sh")
        val scriptBody = """
            |#!/usr/bin/env bash
            |set -euo pipefail
            |
            |SOURCE_APP=${quoteForShell(sourceAppBundle.toString())}
            |TARGET_APP=${quoteForShell(targetAppBundle.toString())}
            |STAGING_ROOT=${quoteForShell(stagingRoot.toString())}
            |WAIT_PID=$waitPid
            |TMP_APP="${'$'}{TARGET_APP}.new"
            |
            |while kill -0 "${'$'}WAIT_PID" >/dev/null 2>&1; do
            |  sleep 0.2
            |done
            |
            |rm -rf "${'$'}TMP_APP"
            |ditto "${'$'}SOURCE_APP" "${'$'}TMP_APP"
            |rm -rf "${'$'}TARGET_APP"
            |mv "${'$'}TMP_APP" "${'$'}TARGET_APP"
            |open "${'$'}TARGET_APP"
            |rm -rf "${'$'}STAGING_ROOT"
            |rm -f "$0"
            |""".trimMargin()

        Files.writeString(scriptFile, scriptBody, StandardCharsets.UTF_8)
        markExecutable(scriptFile)
        return scriptFile
    }

    private fun markExecutable(file: Path) {
        val permissions = setOf(
            PosixFilePermission.OWNER_READ,
            PosixFilePermission.OWNER_WRITE,
            PosixFilePermission.OWNER_EXECUTE,
            PosixFilePermission.GROUP_READ,
            PosixFilePermission.GROUP_EXECUTE,
            PosixFilePermission.OTHERS_READ,
            PosixFilePermission.OTHERS_EXECUTE,
        )
        runCatching { Files.setPosixFilePermissions(file, permissions) }
            .onFailure { file.toFile().setExecutable(true, false) }
    }

    private fun findFirstAppBundle(root: Path): Path? {
        Files.walk(root).use { stream ->
            return stream
                .filter { path ->
                    Files.isDirectory(path) && path.fileName?.toString()?.endsWith(".app", ignoreCase = true) == true
                }
                .findFirst()
                .orElse(null)
        }
    }

    private fun resolveCurrentAppBundlePath(): Path? {
        val candidates = buildList {
            addAll(resolveFromJavaClassPath())
            add(resolveFromJavaHome())
            add(resolveFromProcessCommand())
        }
            .filterNotNull()
            .distinct()

        return candidates.firstOrNull { it.exists() }
    }

    private fun resolveFromJavaClassPath(): List<Path?> {
        val raw = System.getProperty("java.class.path").orEmpty()
        if (raw.isBlank()) return emptyList()

        return raw
            .split(File.pathSeparator)
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .map { extractAppBundleFromPath(Path.of(it).toAbsolutePath()) }
    }

    private fun resolveFromJavaHome(): Path? {
        val javaHome = System.getProperty("java.home").orEmpty().trim()
        if (javaHome.isBlank()) return null
        return extractAppBundleFromPath(Path.of(javaHome).toAbsolutePath())
    }

    private fun resolveFromProcessCommand(): Path? {
        val command = ProcessHandle.current().info().command().orElse("").trim()
        if (command.isBlank()) return null
        return extractAppBundleFromPath(Path.of(command).toAbsolutePath())
    }

    private fun extractAppBundleFromPath(path: Path): Path? {
        var cursor: Path? = path
        while (cursor != null) {
            val name = cursor.fileName?.toString().orEmpty()
            if (name.endsWith(".app", ignoreCase = true)) return cursor
            cursor = cursor.parent
        }
        return null
    }

    private fun quoteForShell(value: String): String {
        return "'" + value.replace("'", "'\"'\"'") + "'"
    }

    private fun isMacOs(): Boolean {
        val os = System.getProperty("os.name").orEmpty().lowercase()
        return os.contains("mac") || os.contains("darwin")
    }
}

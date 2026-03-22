package com.adbdeck.core.adb.impl.adb

import com.adbdeck.core.adb.api.ToolCheckFailureKind
import com.adbdeck.core.adb.api.adb.BundletoolCheckResult
import com.adbdeck.core.adb.api.adb.BundletoolClient
import com.adbdeck.core.process.ProcessRunner
import com.adbdeck.core.settings.SettingsRepository
import com.adbdeck.core.utils.runCatchingPreserveCancellation

/**
 * Реализация [BundletoolClient] через вызов системного bundletool.
 *
 * Поддерживает запуск:
 * - исполняемого файла (`bundletool`)
 * - JAR-артефакта (`java -jar bundletool-all.jar`)
 */
class SystemBundletoolClient(
    private val processRunner: ProcessRunner,
    private val settingsRepository: SettingsRepository,
) : BundletoolClient {

    private fun bundletoolPath(bundletoolPathOverride: String? = null): String {
        val normalizedOverride = bundletoolPathOverride?.trim()?.ifBlank { null }
        return normalizedOverride ?: settingsRepository.getSettings().bundletoolPath.ifBlank { "bundletool" }
    }

    override suspend fun checkAvailability(bundletoolPathOverride: String?): BundletoolCheckResult {
        val path = bundletoolPath(bundletoolPathOverride)
        val baseCommand = buildBundletoolCommand(path)
        val versionCommand = baseCommand + COMMAND_VERSION
        val helpCommand = baseCommand + COMMAND_HELP

        return runCatchingPreserveCancellation {
            val versionResult = processRunner.run(versionCommand)
            if (!versionResult.isSuccess) {
                val details = versionResult.combinedOutput()
                    .firstMeaningfulLine()
                    .ifBlank { path }
                return@runCatchingPreserveCancellation BundletoolCheckResult.NotAvailable(
                    reason = details,
                    kind = ToolCheckFailureKind.COMMAND_FAILED,
                )
            }

            // `bundletool version` может вернуть просто номер версии.
            // Чтобы исключить ложные срабатывания (например, путь на adb), проверяем
            // help-вывод на характерные команды bundletool.
            val helpResult = processRunner.run(helpCommand)
            val helpOutput = helpResult.combinedOutput()
            if (!helpResult.isSuccess || !helpOutput.looksLikeBundletoolHelp()) {
                val details = helpOutput.firstMeaningfulLine().ifBlank { path }
                return@runCatchingPreserveCancellation BundletoolCheckResult.NotAvailable(
                    reason = details,
                    kind = ToolCheckFailureKind.WRONG_EXECUTABLE,
                )
            }

            val version = versionResult.combinedOutput()
                .firstMeaningfulLine()
                .ifBlank { path }
            BundletoolCheckResult.Available(version)
        }.fold(
            onSuccess = { it },
            onFailure = { error ->
                BundletoolCheckResult.NotAvailable(
                    reason = error.message.orEmpty().ifBlank { path },
                    kind = ToolCheckFailureKind.START_FAILED,
                )
            },
        )
    }

    private fun buildBundletoolCommand(path: String): List<String> =
        when {
            path.endsWith(JAR_EXTENSION, ignoreCase = true) -> listOf(COMMAND_JAVA, JAVA_ARG_JAR, path)
            else -> listOf(path)
        }

    private fun String.looksLikeBundletoolHelp(): Boolean {
        val normalized = lowercase()
        return BUNDLETOOL_HELP_MARKERS.any { normalized.contains(it) }
    }

    private fun String.firstMeaningfulLine(): String =
        lineSequence()
            .map(String::trim)
            .firstOrNull { it.isNotEmpty() }
            .orEmpty()

    private fun com.adbdeck.core.process.ProcessResult.combinedOutput(): String =
        sequenceOf(stdout, stderr)
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .joinToString(separator = "\n")

    private companion object {
        const val COMMAND_VERSION = "version"
        const val COMMAND_HELP = "help"
        const val COMMAND_JAVA = "java"
        const val JAVA_ARG_JAR = "-jar"
        const val JAR_EXTENSION = ".jar"
        val BUNDLETOOL_HELP_MARKERS = listOf(
            "build-apks",
            "install-apks",
            "extract-apks",
            "get-device-spec",
        )
    }
}

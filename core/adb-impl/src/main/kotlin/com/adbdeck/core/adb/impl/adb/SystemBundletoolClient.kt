package com.adbdeck.core.adb.impl.adb

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
        val command = buildBundletoolCommand(path) + "version"

        return runCatchingPreserveCancellation {
            processRunner.run(command)
        }.fold(
            onSuccess = { result ->
                if (result.isSuccess) {
                    val version = result.stdout.lineSequence()
                        .map(String::trim)
                        .firstOrNull { it.isNotEmpty() }
                        ?: result.stderr.lineSequence()
                            .map(String::trim)
                            .firstOrNull { it.isNotEmpty() }
                            ?: "unknown"
                    BundletoolCheckResult.Available(version)
                } else {
                    val details = result.stderr.ifBlank { result.stdout }
                        .lineSequence()
                        .map(String::trim)
                        .firstOrNull { it.isNotEmpty() }
                        ?: "failed to get version"
                    BundletoolCheckResult.NotAvailable(details)
                }
            },
            onFailure = { error ->
                BundletoolCheckResult.NotAvailable(error.message ?: "failed to run command")
            },
        )
    }

    private fun buildBundletoolCommand(path: String): List<String> =
        when {
            path.endsWith(".jar", ignoreCase = true) -> listOf("java", "-jar", path)
            else -> listOf(path)
        }
}

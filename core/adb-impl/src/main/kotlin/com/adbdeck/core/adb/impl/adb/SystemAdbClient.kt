package com.adbdeck.core.adb.impl.adb

import com.adbdeck.core.adb.api.adb.AdbCheckResult
import com.adbdeck.core.adb.api.adb.AdbClient
import com.adbdeck.core.adb.api.adb.AdbServerState
import com.adbdeck.core.adb.api.adb.AdbServerStatusResult
import com.adbdeck.core.adb.api.device.AdbDevice
import com.adbdeck.core.adb.api.device.DeviceState
import com.adbdeck.core.process.ProcessRunner
import com.adbdeck.core.settings.SettingsRepository
import com.adbdeck.core.utils.runCatchingPreserveCancellation

/**
 * Реализация [com.adbdeck.core.adb.api.adb.AdbClient] через вызов системного исполняемого файла adb
 *
 * Путь к adb берется из [com.adbdeck.core.settings.SettingsRepository]. Если пользователь указал
 * абсолютный путь — используется он; иначе adb ищется в системном PATH
 *
 * @param processRunner Исполнитель внешних процессов
 * @param settingsRepository Репозиторий настроек для получения пути к adb
 */
class SystemAdbClient(
    private val processRunner: ProcessRunner,
    private val settingsRepository: SettingsRepository,
) : AdbClient {

    /** Возвращает путь к adb: override (если задан) или значение из настроек. */
    private fun adbPath(adbPathOverride: String? = null): String {
        val normalizedOverride = adbPathOverride?.trim()?.ifBlank { null }
        return normalizedOverride ?: settingsRepository.resolvedAdbPath()
    }

    override suspend fun checkAvailability(adbPathOverride: String?): AdbCheckResult {
        return runCatchingPreserveCancellation {
            processRunner.run(adbPath(adbPathOverride), "version")
        }.fold(
            onSuccess = { result ->
                if (result.isSuccess) {
                    AdbCheckResult.Available(result.firstOutputLine)
                } else {
                    AdbCheckResult.NotAvailable(
                        result.stderr.ifBlank { "adb завершился с кодом ${result.exitCode}" }
                    )
                }
            },
            onFailure = { e ->
                AdbCheckResult.NotAvailable("Не удалось запустить adb: ${e.message}")
            },
        )
    }

    override suspend fun getServerStatus(adbPathOverride: String?): AdbServerStatusResult {
        val resolvedAdbPath = adbPath(adbPathOverride)
        return runCatchingPreserveCancellation {
            processRunner.run(resolvedAdbPath, "server-status")
        }.fold(
            onSuccess = { result ->
                val output = result.combinedOutput()
                if (result.isSuccess) {
                    val parsed = parseServerStatusFromSuccessfulOutput(output)
                    if (parsed.state == AdbServerState.UNKNOWN) {
                        inferServerStatusViaDevices(
                            adbPath = resolvedAdbPath,
                            fallbackMessage = output,
                        )
                    } else {
                        parsed
                    }
                } else {
                    when {
                        output.looksLikeStoppedServer() -> AdbServerStatusResult(
                            state = AdbServerState.STOPPED,
                            message = output,
                        )

                        output.looksLikeUnsupportedServerStatus() -> inferServerStatusViaDevices(
                            adbPath = resolvedAdbPath,
                            fallbackMessage = output.ifBlank {
                                "Команда adb server-status не поддерживается этой версией adb"
                            },
                        )

                        else -> AdbServerStatusResult(
                            state = AdbServerState.ERROR,
                            message = output.ifBlank { "adb server-status завершился с кодом ${result.exitCode}" },
                        )
                    }
                }
            },
            onFailure = { e ->
                AdbServerStatusResult(
                    state = AdbServerState.ERROR,
                    message = "Не удалось получить статус ADB server: ${e.message}",
                )
            },
        )
    }

    override suspend fun startServer(adbPathOverride: String?): Result<String> {
        return runCatchingPreserveCancellation {
            val result = processRunner.run(adbPath(adbPathOverride), "start-server")
            if (result.isSuccess) {
                result.combinedOutput().ifBlank { "ADB server started" }
            } else {
                error(result.combinedOutput().ifBlank { "adb start-server завершился с кодом ${result.exitCode}" })
            }
        }
    }

    override suspend fun stopServer(adbPathOverride: String?): Result<String> {
        return runCatchingPreserveCancellation {
            val result = processRunner.run(adbPath(adbPathOverride), "kill-server")
            if (result.isSuccess) {
                result.combinedOutput().ifBlank { "ADB server stopped" }
            } else {
                error(result.combinedOutput().ifBlank { "adb kill-server завершился с кодом ${result.exitCode}" })
            }
        }
    }

    override suspend fun restartServer(adbPathOverride: String?): Result<String> {
        val stopResult = stopServer(adbPathOverride)
        if (stopResult.isFailure) {
            val stopReason = stopResult.exceptionOrNull()?.message.orEmpty()
            if (!stopReason.looksLikeStoppedServer()) {
                return Result.failure(
                    IllegalStateException(
                        stopReason.ifBlank { "Не удалось остановить ADB server перед перезапуском" }
                    ),
                )
            }
        }
        return startServer(adbPathOverride)
    }

    override suspend fun getDevices(): Result<List<AdbDevice>> =
        runCatchingPreserveCancellation {
            val result = processRunner.run(adbPath(), "devices")
            if (result.isSuccess) {
                parseDevicesOutput(result.stdout)
            } else {
                error(result.stderr.ifBlank { "adb devices завершился с ошибкой" })
            }
        }

    /**
     * Парсит вывод команды `adb devices` в список [AdbDevice].
     *
     * Формат строк: `<deviceId>\t<state>[\t<info>]`
     * Первая строка ("List of devices attached") пропускается.
     *
     * @param output Полный вывод команды `adb devices`.
     */
    private fun parseDevicesOutput(output: String): List<AdbDevice> {
        return output.lines()
            .drop(1) // пропускаем заголовок "List of devices attached"
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .mapNotNull { line ->
                val parts = line.split("\t", limit = 3)
                if (parts.size >= 2) {
                    AdbDevice(
                        deviceId = parts[0].trim(),
                        state = DeviceState.Companion.fromRawValue(parts[1].trim()),
                        info = parts.getOrNull(2)?.trim() ?: "",
                    )
                } else null
            }
    }

    private fun parseServerStatusFromSuccessfulOutput(output: String): AdbServerStatusResult {
        if (output.isBlank()) {
            return AdbServerStatusResult(state = AdbServerState.RUNNING)
        }

        val normalized = output.lowercase()
        return when {
            normalized.contains("is_running: false") ||
                normalized.contains("server is not running") ||
                normalized.contains("stopped") -> {
                AdbServerStatusResult(
                    state = AdbServerState.STOPPED,
                    message = output,
                )
            }

            normalized.contains("is_running: true") ||
                normalized.contains("running") ||
                normalized.looksLikeServerStatusDump() -> {
                AdbServerStatusResult(
                    state = AdbServerState.RUNNING,
                    message = output,
                )
            }

            else -> AdbServerStatusResult(
                state = AdbServerState.UNKNOWN,
                message = output,
            )
        }
    }

    private suspend fun inferServerStatusViaDevices(
        adbPath: String,
        fallbackMessage: String,
    ): AdbServerStatusResult {
        return runCatchingPreserveCancellation {
            processRunner.run(adbPath, "devices")
        }.fold(
            onSuccess = { result ->
                val output = result.combinedOutput()
                if (result.isSuccess) {
                    AdbServerStatusResult(
                        state = AdbServerState.RUNNING,
                        message = fallbackMessage.ifBlank { output },
                    )
                } else if (output.looksLikeStoppedServer()) {
                    AdbServerStatusResult(
                        state = AdbServerState.STOPPED,
                        message = output.ifBlank { fallbackMessage },
                    )
                } else {
                    AdbServerStatusResult(
                        state = AdbServerState.ERROR,
                        message = output.ifBlank { fallbackMessage },
                    )
                }
            },
            onFailure = { e ->
                AdbServerStatusResult(
                    state = AdbServerState.ERROR,
                    message = fallbackMessage.ifBlank {
                        "Не удалось определить состояние ADB server: ${e.message}"
                    },
                )
            },
        )
    }

    private fun String.looksLikeStoppedServer(): Boolean {
        val normalized = lowercase()
        return normalized.contains("daemon not running") ||
            normalized.contains("cannot connect to daemon") ||
            normalized.contains("connection refused") ||
            normalized.contains("failed to connect to")
    }

    private fun String.looksLikeUnsupportedServerStatus(): Boolean {
        val normalized = lowercase()
        return normalized.contains("unknown command") ||
            normalized.contains("unrecognized command") ||
            (normalized.contains("usage:") && normalized.contains("server-status"))
    }

    private fun String.looksLikeServerStatusDump(): Boolean {
        if (isBlank()) return false
        val markers = listOf(
            "usb_backend",
            "mdns_backend",
            "mdns_enabled",
            "version:",
            "\"version\"",
            "executable_absolute_path",
            "log_absolute_path",
            "keystore_path",
            "known_hosts_path",
        )
        return markers.count { contains(it) } >= 2
    }

    private fun com.adbdeck.core.process.ProcessResult.combinedOutput(): String =
        sequenceOf(stdout, stderr)
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .joinToString(separator = "\n")
}

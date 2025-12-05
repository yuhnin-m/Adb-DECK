package com.adbdeck.core.adb.impl

import com.adbdeck.core.adb.api.adb.AdbCheckResult
import com.adbdeck.core.adb.api.adb.AdbClient
import com.adbdeck.core.adb.api.device.AdbDevice
import com.adbdeck.core.adb.api.device.DeviceState
import com.adbdeck.core.process.ProcessRunner
import com.adbdeck.core.settings.SettingsRepository
import com.adbdeck.core.utils.runCatchingPreserveCancellation

/**
 * Реализация [AdbClient] через вызов системного исполняемого файла adb
 *
 * Путь к adb берется из [SettingsRepository]. Если пользователь указал
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
        return normalizedOverride ?: settingsRepository.getSettings().adbPath.ifBlank { "adb" }
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
                        state = DeviceState.fromRawValue(parts[1].trim()),
                        info = parts.getOrNull(2)?.trim() ?: "",
                    )
                } else null
            }
    }
}

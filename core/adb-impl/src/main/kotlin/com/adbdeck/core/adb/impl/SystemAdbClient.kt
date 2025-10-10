package com.adbdeck.core.adb.impl

import com.adbdeck.core.adb.api.AdbCheckResult
import com.adbdeck.core.adb.api.AdbClient
import com.adbdeck.core.adb.api.AdbDevice
import com.adbdeck.core.adb.api.DeviceState
import com.adbdeck.core.process.ProcessRunner
import com.adbdeck.core.settings.SettingsRepository

/**
 * Реализация [AdbClient] через вызов системного исполняемого файла `adb`.
 *
 * Путь к adb берётся из [SettingsRepository]. Если пользователь указал
 * абсолютный путь — используется он; иначе adb ищется в системном PATH.
 *
 * @param processRunner Исполнитель внешних процессов.
 * @param settingsRepository Репозиторий настроек для получения пути к adb.
 */
class SystemAdbClient(
    private val processRunner: ProcessRunner,
    private val settingsRepository: SettingsRepository,
) : AdbClient {

    /** Возвращает актуальный путь к adb из настроек. */
    private fun adbPath(): String = settingsRepository.getSettings().adbPath.ifBlank { "adb" }

    override suspend fun checkAvailability(): AdbCheckResult {
        return try {
            val result = processRunner.run(adbPath(), "version")
            if (result.isSuccess) {
                val version = result.firstOutputLine
                AdbCheckResult.Available(version)
            } else {
                AdbCheckResult.NotAvailable(
                    result.stderr.ifBlank { "adb завершился с кодом ${result.exitCode}" }
                )
            }
        } catch (e: Exception) {
            AdbCheckResult.NotAvailable("Не удалось запустить adb: ${e.message}")
        }
    }

    override suspend fun getDevices(): Result<List<AdbDevice>> {
        return try {
            val result = processRunner.run(adbPath(), "devices")
            if (result.isSuccess) {
                Result.success(parseDevicesOutput(result.stdout))
            } else {
                Result.failure(Exception(result.stderr.ifBlank { "adb devices завершился с ошибкой" }))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Не удалось получить список устройств: ${e.message}"))
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

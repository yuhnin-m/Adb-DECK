package com.adbdeck.core.adb.impl.device

import com.adbdeck.core.adb.api.device.DeviceInfo
import com.adbdeck.core.adb.api.device.DeviceInfoClient
import com.adbdeck.core.adb.api.device.DeviceTransportType
import com.adbdeck.core.process.ProcessRunner
import com.adbdeck.core.utils.runCatchingPreserveCancellation

/**
 * Реализация [com.adbdeck.core.adb.api.device.DeviceInfoClient] через ADB shell.
 *
 * ## Стратегия получения данных
 *
 * 1. `adb -s <id> shell getprop` — все системные свойства одним вызовом.
 *    Парсим результат в Map, извлекаем нужные ключи.
 * 2. `adb -s <id> shell wm size` — физическое разрешение экрана
 *    (например `Physical size: 1080x2400`). Отдельный вызов, т.к. не доступен через getprop.
 * 3. `adb -s <id> shell dumpsys battery` — уровень заряда и статус зарядки.
 *    Парсим строки `level: N` и `status: N`.
 *
 * При ошибке любого отдельного вызова поля остаются пустыми — [fetchDeviceInfo] не падает.
 *
 * @param processRunner Исполнитель внешних процессов.
 */
class SystemDeviceInfoClient(
    private val processRunner: ProcessRunner,
) : DeviceInfoClient {

    override suspend fun fetchDeviceInfo(
        deviceId: String,
        adbPath: String,
    ): Result<DeviceInfo> = runCatchingPreserveCancellation {
        val props = fetchProps(deviceId, adbPath)
        val resolution = fetchScreenResolution(deviceId, adbPath)
        val battery = fetchBattery(deviceId, adbPath)
        val transport = detectTransportType(deviceId)

        DeviceInfo(
            deviceId = deviceId,
            model = props["ro.product.model"] ?: "",
            manufacturer = props["ro.product.manufacturer"] ?: "",
            brand = props["ro.product.brand"] ?: "",
            productName = props["ro.product.name"] ?: "",
            androidVersion = props["ro.build.version.release"] ?: "",
            sdkVersion = props["ro.build.version.sdk"]?.toIntOrNull() ?: 0,
            buildFingerprint = props["ro.build.fingerprint"] ?: "",
            securityPatch = props["ro.build.version.security_patch"] ?: "",
            cpuAbiList = props["ro.product.cpu.abilist"]
                ?: props["ro.product.cpu.abi"] ?: "",
            screenResolution = resolution,
            screenDensity = props["ro.sf.lcd_density"]?.toIntOrNull() ?: 0,
            batteryLevel = battery.first,
            batteryCharging = battery.second,
            transportType = transport,
        )
    }

    override suspend fun getSystemProperties(
        deviceId: String,
        adbPath: String,
    ): Result<Map<String, String>> = runCatchingPreserveCancellation {
        val result = processRunner.run(adbPath, "-s", deviceId, "shell", "getprop")
        if (!result.isSuccess) {
            error(result.stderr.ifBlank { "Не удалось выполнить adb shell getprop" })
        }
        parseProps(result.stdout)
    }

    override suspend fun runShellCommand(
        deviceId: String,
        command: List<String>,
        adbPath: String,
    ): Result<String> = runCatchingPreserveCancellation {
        require(command.isNotEmpty()) { "Команда shell не может быть пустой" }

        val result = processRunner.run(
            listOf(adbPath, "-s", deviceId, "shell") + command,
        )
        if (!result.isSuccess) {
            error(result.stderr.ifBlank { "Не удалось выполнить adb shell ${command.joinToString(" ")}" })
        }
        result.stdout
    }

    // ── Вспомогательные функции ───────────────────────────────────────────────

    /**
     * Выполняет `adb shell getprop` и парсит вывод в Map[ключ → значение].
     *
     * Формат строки: `[ro.product.model]: [Pixel 8]`
     */
    private suspend fun fetchProps(deviceId: String, adbPath: String): Map<String, String> {
        return getSystemProperties(deviceId, adbPath).getOrDefault(emptyMap())
    }

    /**
     * Получает физическое разрешение экрана через `wm size`.
     *
     * Пример вывода: `Physical size: 1080x2400`
     * Возвращает строку «1080x2400» или «» при ошибке.
     */
    private suspend fun fetchScreenResolution(deviceId: String, adbPath: String): String {
        val result = runCatchingPreserveCancellation {
            processRunner.run(adbPath, "-s", deviceId, "shell", "wm", "size")
        }.getOrNull() ?: return ""
        if (!result.isSuccess) return ""

        // Ищем строку «Physical size: ...» или «Override size: ...»
        return result.stdout.lines().asSequence()
            .map { it.trim() }
            .firstOrNull { it.startsWith("Physical size:") || it.startsWith("Override size:") }
            ?.substringAfter(":")
            ?.trim()
            ?: ""
    }

    /**
     * Получает уровень заряда батареи и статус зарядки через `dumpsys battery`.
     *
     * Парсим строки:
     * - `level: 85` → уровень заряда
     * - `status: 2` → 2 = CHARGING, 1 = UNKNOWN, 3 = DISCHARGING, 4 = NOT_CHARGING, 5 = FULL
     *
     * @return Pair(уровень 0–100 или -1, isCharging)
     */
    private suspend fun fetchBattery(deviceId: String, adbPath: String): Pair<Int, Boolean> {
        val result = runCatchingPreserveCancellation {
            processRunner.run(adbPath, "-s", deviceId, "shell", "dumpsys", "battery")
        }.getOrNull() ?: return Pair(-1, false)
        if (!result.isSuccess) return Pair(-1, false)

        var level = -1
        var charging = false

        result.stdout.lines().forEach { line ->
            val trimmed = line.trim()
            when {
                trimmed.startsWith("level:") ->
                    level = trimmed.substringAfter("level:").trim().toIntOrNull() ?: -1
                trimmed.startsWith("status:") -> {
                    val status = trimmed.substringAfter("status:").trim().toIntOrNull() ?: 0
                    charging = status == 2 || status == 5 // CHARGING or FULL
                }
            }
        }
        return Pair(level, charging)
    }

    /**
     * Определяет тип транспорта по формату deviceId:
     * - `emulator-XXXX` → [com.adbdeck.core.adb.api.device.DeviceTransportType.EMULATOR]
     * - `IP:PORT` (содержит двоеточие) → [com.adbdeck.core.adb.api.device.DeviceTransportType.WIFI]
     * - Всё остальное (серийный номер) → [com.adbdeck.core.adb.api.device.DeviceTransportType.USB]
     */
    private fun detectTransportType(deviceId: String): DeviceTransportType = when {
        deviceId.startsWith("emulator-") -> DeviceTransportType.EMULATOR
        deviceId.contains(':')           -> DeviceTransportType.WIFI
        else                             -> DeviceTransportType.USB
    }

    companion object {
        /** Регулярка для разбора строк вывода `adb shell getprop`. */
        private val PROP_REGEX = Regex("""^\[(.+?)\]:\s*\[(.*)?\]$""")
    }

    /** Разбирает stdout `getprop` в карту `ключ -> значение`. */
    private fun parseProps(output: String): Map<String, String> {
        return output.lines()
            .mapNotNull { line -> PROP_REGEX.matchEntire(line.trim()) }
            .associate { match -> match.groupValues[1] to match.groupValues[2] }
    }
}

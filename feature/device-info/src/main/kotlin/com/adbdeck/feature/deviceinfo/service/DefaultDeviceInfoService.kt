package com.adbdeck.feature.deviceinfo.service

import adbdeck.feature.device_info.generated.resources.Res
import adbdeck.feature.device_info.generated.resources.*
import com.adbdeck.core.adb.api.device.DeviceInfoClient
import com.adbdeck.feature.deviceinfo.DeviceInfoRow
import com.adbdeck.feature.deviceinfo.DeviceInfoSectionKind
import com.adbdeck.feature.deviceinfo.parser.BatteryHighlights
import com.adbdeck.feature.deviceinfo.parser.findMostUsedStorageEntry
import com.adbdeck.feature.deviceinfo.parser.normalizeForUi
import com.adbdeck.feature.deviceinfo.parser.parseBatteryHighlights
import com.adbdeck.feature.deviceinfo.parser.parseCpuHighlights
import com.adbdeck.feature.deviceinfo.parser.parseDisplayHighlights
import com.adbdeck.feature.deviceinfo.parser.parseDiskstatsHighlight
import com.adbdeck.feature.deviceinfo.parser.parseNetworkHighlights
import com.adbdeck.feature.deviceinfo.parser.parseRamHighlights
import com.adbdeck.feature.deviceinfo.parser.parseRuntimeHighlights
import com.adbdeck.feature.deviceinfo.parser.parseStorageEntries
import com.adbdeck.feature.deviceinfo.parser.parseWmDensity
import com.adbdeck.feature.deviceinfo.parser.parseWmSize
import java.util.Locale
import kotlin.math.roundToInt
import kotlinx.coroutines.CancellationException
import org.jetbrains.compose.resources.getString

/**
 * Реализация [DeviceInfoService] поверх [DeviceInfoClient].
 *
 * Принципы:
 * - best-effort сбор секций;
 * - парсинг команд вынесен в `parser`-пакет;
 * - секция считается успешной, если удалось собрать хотя бы одну строку.
 */
class DefaultDeviceInfoService(
    private val deviceInfoClient: DeviceInfoClient,
) : DeviceInfoService {

    override suspend fun loadSection(
        section: DeviceInfoSectionKind,
        deviceId: String,
        adbPath: String,
    ): Result<List<DeviceInfoRow>> = runSection {
        val rows = when (section) {
            DeviceInfoSectionKind.OVERVIEW -> loadOverview(deviceId, adbPath)
            DeviceInfoSectionKind.BUILD -> loadBuild(deviceId, adbPath)
            DeviceInfoSectionKind.DISPLAY -> loadDisplay(deviceId, adbPath)
            DeviceInfoSectionKind.CPU_RAM -> loadCpuRam(deviceId, adbPath)
            DeviceInfoSectionKind.BATTERY -> loadBattery(deviceId, adbPath)
            DeviceInfoSectionKind.NETWORK -> loadNetwork(deviceId, adbPath)
            DeviceInfoSectionKind.STORAGE -> loadStorage(deviceId, adbPath)
            DeviceInfoSectionKind.SECURITY -> loadSecurity(deviceId, adbPath)
            DeviceInfoSectionKind.SYSTEM -> loadSystemRuntime(deviceId, adbPath)
        }

        if (rows.isEmpty()) {
            error(getString(Res.string.device_info_section_no_data))
        }

        rows
    }

    // ── Section: Overview ────────────────────────────────────────────────────

    private suspend fun loadOverview(
        deviceId: String,
        adbPath: String,
    ): List<DeviceInfoRow> {
        val info = deviceInfoClient.fetchDeviceInfo(deviceId, adbPath).getOrNull()
        val props = deviceInfoClient.getSystemProperties(deviceId, adbPath).getOrDefault(emptyMap())

        val androidVersion = info?.androidVersion.orIfBlank(props["ro.build.version.release"])
        val sdk = info?.sdkVersion?.takeIf { it > 0 }
        val androidValue = when {
            androidVersion != null && sdk != null -> getString(
                Res.string.device_info_value_android_sdk_format,
                androidVersion,
                sdk,
            )
            androidVersion != null -> androidVersion
            else -> null
        }

        return buildRows(
            section = DeviceInfoSectionKind.OVERVIEW,
            values = listOf(
                getString(Res.string.device_info_row_device_id) to deviceId,
                getString(Res.string.device_info_row_model) to info?.model,
                getString(Res.string.device_info_row_manufacturer) to info?.manufacturer,
                getString(Res.string.device_info_row_brand) to info?.brand,
                getString(Res.string.device_info_row_android_version) to androidValue,
                getString(Res.string.device_info_row_security_patch) to info?.securityPatch.orIfBlank(props["ro.build.version.security_patch"]),
                getString(Res.string.device_info_row_fingerprint) to info?.buildFingerprint.orIfBlank(props["ro.build.fingerprint"]),
                getString(Res.string.device_info_row_abi_list) to info?.cpuAbiList.orIfBlank(props["ro.product.cpu.abilist"]),
            ),
        )
    }

    // ── Section: Build ───────────────────────────────────────────────────────

    private suspend fun loadBuild(
        deviceId: String,
        adbPath: String,
    ): List<DeviceInfoRow> {
        val props = deviceInfoClient.getSystemProperties(deviceId, adbPath).getOrDefault(emptyMap())
        val locale = resolveLocale(props)

        return buildRows(
            section = DeviceInfoSectionKind.BUILD,
            values = listOf(
                getString(Res.string.device_info_row_fingerprint) to props["ro.build.fingerprint"],
                getString(Res.string.device_info_row_build_id) to props["ro.build.id"],
                getString(Res.string.device_info_row_build_type) to props["ro.build.type"],
                getString(Res.string.device_info_row_debuggable) to props["ro.debuggable"]?.asYesNo(),
                getString(Res.string.device_info_row_build_tags) to props["ro.build.tags"],
                getString(Res.string.device_info_row_timezone) to props["persist.sys.timezone"],
                getString(Res.string.device_info_row_locale) to locale,
            ),
        )
    }

    // ── Section: Display ─────────────────────────────────────────────────────

    private suspend fun loadDisplay(
        deviceId: String,
        adbPath: String,
    ): List<DeviceInfoRow> {
        val wmSize = runShell(deviceId, adbPath, "wm", "size")?.let(::parseWmSize)
        val wmDensity = runShell(deviceId, adbPath, "wm", "density")?.let(::parseWmDensity)
        val dumpsysDisplay = runShell(deviceId, adbPath, "dumpsys", "display").orEmpty()
        val highlights = parseDisplayHighlights(dumpsysDisplay)

        return buildRows(
            section = DeviceInfoSectionKind.DISPLAY,
            values = listOf(
                getString(Res.string.device_info_row_wm_size) to wmSize,
                getString(Res.string.device_info_row_wm_density) to wmDensity,
                getString(Res.string.device_info_row_display_base_info) to highlights.baseInfo,
                getString(Res.string.device_info_row_display_state) to highlights.state,
            ),
        )
    }

    // ── Section: CPU / RAM ───────────────────────────────────────────────────

    private suspend fun loadCpuRam(
        deviceId: String,
        adbPath: String,
    ): List<DeviceInfoRow> {
        val uname = runShell(deviceId, adbPath, "uname", "-a")
        val cpuInfo = runShell(deviceId, adbPath, "cat", "/proc/cpuinfo").orEmpty()
        val memInfo = runShell(deviceId, adbPath, "dumpsys", "meminfo").orEmpty()

        val cpu = parseCpuHighlights(cpuInfo)
        val ram = parseRamHighlights(memInfo)

        return buildRows(
            section = DeviceInfoSectionKind.CPU_RAM,
            values = listOf(
                getString(Res.string.device_info_row_kernel) to uname,
                getString(Res.string.device_info_row_cpu_model) to cpu.model,
                getString(Res.string.device_info_row_cpu_cores) to cpu.cores?.toString(),
                getString(Res.string.device_info_row_cpu_features) to cpu.features,
                getString(Res.string.device_info_row_ram_total) to formatRamMegabytes(ram.total),
                getString(Res.string.device_info_row_ram_used) to formatRamMegabytes(ram.used),
                getString(Res.string.device_info_row_ram_free) to formatRamMegabytes(ram.free),
                getString(Res.string.device_info_row_ram_lost) to formatRamMegabytes(ram.lost),
            ),
        )
    }

    // ── Section: Battery ─────────────────────────────────────────────────────

    private suspend fun loadBattery(
        deviceId: String,
        adbPath: String,
    ): List<DeviceInfoRow> {
        val batteryOutput = runShell(deviceId, adbPath, "dumpsys", "battery").orEmpty()
        val battery = parseBatteryHighlights(batteryOutput)

        return buildRows(
            section = DeviceInfoSectionKind.BATTERY,
            values = listOf(
                getString(Res.string.device_info_row_battery_level) to battery.level?.let(::formatPercent),
                getString(Res.string.device_info_row_battery_status) to batteryStatusText(battery),
                getString(Res.string.device_info_row_battery_health) to battery.healthCode?.toString(),
                getString(Res.string.device_info_row_battery_temp) to battery.temperatureTenthsC?.let {
                    formatCelsius(it / 10f)
                },
                getString(Res.string.device_info_row_battery_voltage) to battery.voltageMv?.let {
                    getString(Res.string.device_info_value_mv_format, it)
                },
                getString(Res.string.device_info_row_battery_technology) to battery.technology,
                getString(Res.string.device_info_row_battery_powered) to batteryPoweredText(battery),
            ),
        )
    }

    // ── Section: Network ─────────────────────────────────────────────────────

    private suspend fun loadNetwork(
        deviceId: String,
        adbPath: String,
    ): List<DeviceInfoRow> {
        val ipAddr = runShell(deviceId, adbPath, "ip", "addr").orEmpty()
        val ipRoute = runShell(deviceId, adbPath, "ip", "route").orEmpty()
        val dumpsysWifi = runShell(deviceId, adbPath, "dumpsys", "wifi").orEmpty()
        val dumpsysConnectivity = runShell(deviceId, adbPath, "dumpsys", "connectivity").orEmpty()

        val network = parseNetworkHighlights(
            ipAddrOutput = ipAddr,
            ipRouteOutput = ipRoute,
            dumpsysWifiOutput = dumpsysWifi,
            dumpsysConnectivityOutput = dumpsysConnectivity,
        )

        return buildRows(
            section = DeviceInfoSectionKind.NETWORK,
            values = listOf(
                getString(Res.string.device_info_row_ip_addresses) to network.ipAddresses.joinToString(),
                getString(Res.string.device_info_row_default_route) to network.defaultRoute,
                getString(Res.string.device_info_row_wifi_state) to network.wifiState,
                getString(Res.string.device_info_row_wifi_ssid) to network.wifiSsid,
                getString(Res.string.device_info_row_wifi_bssid) to network.wifiBssid,
                getString(Res.string.device_info_row_wifi_rssi) to network.wifiRssi,
                getString(Res.string.device_info_row_connectivity) to network.connectivity,
            ),
        )
    }

    // ── Section: Storage ─────────────────────────────────────────────────────

    private suspend fun loadStorage(
        deviceId: String,
        adbPath: String,
    ): List<DeviceInfoRow> {
        val dfOutput = runShell(deviceId, adbPath, "df", "-h").orEmpty()
        val diskstatsOutput = runShell(deviceId, adbPath, "dumpsys", "diskstats").orEmpty()

        val entries = parseStorageEntries(dfOutput).take(12)
        val mostUsed = findMostUsedStorageEntry(entries)
        val diskstats = parseDiskstatsHighlight(diskstatsOutput)

        val baseRows = mutableListOf(
            getString(Res.string.device_info_row_storage_partitions_count) to entries.size.toString(),
            getString(Res.string.device_info_row_storage_most_used) to mostUsed?.let {
                getString(
                    Res.string.device_info_value_most_used_format,
                    it.mountPoint,
                    it.usePercent,
                )
            },
            getString(Res.string.device_info_row_storage_diskstats) to diskstats,
        )

        entries.forEach { entry ->
            baseRows += entry.mountPoint to getString(
                Res.string.device_info_value_storage_entry_format,
                entry.used,
                entry.size,
                entry.usePercent,
            )
        }

        return buildRows(
            section = DeviceInfoSectionKind.STORAGE,
            values = baseRows,
        )
    }

    // ── Section: Security ────────────────────────────────────────────────────

    private suspend fun loadSecurity(
        deviceId: String,
        adbPath: String,
    ): List<DeviceInfoRow> {
        val props = deviceInfoClient.getSystemProperties(deviceId, adbPath).getOrDefault(emptyMap())
        val selinux = runShell(deviceId, adbPath, "getenforce")
            ?.lineSequence()
            ?.firstOrNull()
            ?.trim()

        return buildRows(
            section = DeviceInfoSectionKind.SECURITY,
            values = listOf(
                getString(Res.string.device_info_row_selinux) to selinux,
                getString(Res.string.device_info_row_security_patch) to props["ro.build.version.security_patch"],
                getString(Res.string.device_info_row_verified_boot) to props["ro.boot.verifiedbootstate"],
                getString(Res.string.device_info_row_debuggable) to props["ro.debuggable"]?.asYesNo(),
            ),
        )
    }

    // ── Section: System runtime ──────────────────────────────────────────────

    private suspend fun loadSystemRuntime(
        deviceId: String,
        adbPath: String,
    ): List<DeviceInfoRow> {
        val activity = runShell(deviceId, adbPath, "dumpsys", "activity").orEmpty()
        val window = runShell(deviceId, adbPath, "dumpsys", "window").orEmpty()

        val runtime = parseRuntimeHighlights(
            dumpsysActivityOutput = activity,
            dumpsysWindowOutput = window,
        )

        return buildRows(
            section = DeviceInfoSectionKind.SYSTEM,
            values = listOf(
                getString(Res.string.device_info_row_top_resumed_activity) to runtime.topResumedActivity,
                getString(Res.string.device_info_row_current_focus) to runtime.currentFocus,
                getString(Res.string.device_info_row_focused_app) to runtime.focusedApp,
            ),
        )
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private suspend fun runShell(
        deviceId: String,
        adbPath: String,
        vararg command: String,
    ): String? {
        return deviceInfoClient.runShellCommand(
            deviceId = deviceId,
            adbPath = adbPath,
            command = command.toList(),
        ).getOrNull()
    }

    private suspend fun buildRows(
        section: DeviceInfoSectionKind,
        values: List<Pair<String, String?>>,
    ): List<DeviceInfoRow> {
        return values
            .mapNotNull { (key, value) ->
                val normalized = value?.trim()?.takeIf { it.isNotEmpty() } ?: return@mapNotNull null
                key to normalizeForUi(normalized)
            }
            .mapIndexed { index, (key, value) ->
                DeviceInfoRow(
                    id = "${section.id}:$index:${key.hashCode()}",
                    key = key,
                    value = value,
                )
            }
    }

    private fun resolveLocale(props: Map<String, String>): String? {
        props["persist.sys.locale"]?.trim()?.takeIf { it.isNotEmpty() }?.let { return it }

        val lang = props["persist.sys.language"]?.trim().orEmpty()
        val country = props["persist.sys.country"]?.trim().orEmpty()
        if (lang.isNotEmpty()) {
            return if (country.isNotEmpty()) "$lang-$country" else lang
        }

        return props["ro.product.locale"]?.trim()?.takeIf { it.isNotEmpty() }
    }

    private suspend fun batteryStatusText(highlights: BatteryHighlights): String {
        return when (highlights.statusCode) {
            2 -> getString(Res.string.device_info_battery_status_charging)
            3 -> getString(Res.string.device_info_battery_status_discharging)
            4 -> getString(Res.string.device_info_battery_status_not_charging)
            5 -> getString(Res.string.device_info_battery_status_full)
            else -> getString(Res.string.device_info_battery_status_unknown)
        }
    }

    private suspend fun batteryPoweredText(highlights: BatteryHighlights): String {
        val yes = getString(Res.string.device_info_value_yes)
        val no = getString(Res.string.device_info_value_no)

        return getString(
            Res.string.device_info_value_battery_powered_format,
            if (highlights.acPowered == true) yes else no,
            if (highlights.usbPowered == true) yes else no,
            if (highlights.wirelessPowered == true) yes else no,
        )
    }

    /**
     * Приводит строковое значение RAM из `dumpsys meminfo` к отображению в мегабайтах.
     */
    private suspend fun formatRamMegabytes(raw: String?): String? {
        val value = raw?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        val megabytes = parseMegabytes(value) ?: return normalizeForUi(value)
        return getString(
            Res.string.device_info_value_ram_mb_format,
            megabytes.roundToInt(),
        )
    }

    /**
     * Парсит объем памяти в MB. На входе ожидается строка вроде `5,586,656K`.
     */
    private fun parseMegabytes(raw: String): Double? {
        val match = RAM_VALUE_REGEX.find(raw) ?: return null
        val numeric = match.groupValues[1]
            .replace(",", "")
            .trim()
            .toDoubleOrNull() ?: return null

        val unit = match.groupValues.getOrElse(2) { "" }.uppercase(Locale.ROOT)
        val multiplier = when (unit) {
            "", "K", "KB", "KIB" -> 1.0 / 1024.0
            "M", "MB", "MIB" -> 1.0
            "G", "GB", "GIB" -> 1024.0
            "T", "TB", "TIB" -> 1024.0 * 1024.0
            else -> return null
        }

        return numeric * multiplier
    }

    /**
     * Возвращает строку процента для UI без `String.format`, чтобы избежать двойного `%`.
     */
    private fun formatPercent(value: Int): String = "$value%"

    /**
     * Форматирует температуру в градусах Цельсия.
     */
    private fun formatCelsius(valueCelsius: Float): String {
        return String.format(Locale.getDefault(), "%.1f °C", valueCelsius)
    }

    private suspend fun String.asYesNo(): String {
        val normalized = trim().lowercase()
        return when (normalized) {
            "1", "true", "yes", "y" -> getString(Res.string.device_info_value_yes)
            "0", "false", "no", "n" -> getString(Res.string.device_info_value_no)
            else -> getString(Res.string.device_info_value_unknown)
        }
    }

    private fun String?.orIfBlank(fallback: String?): String? {
        val primary = this?.trim().orEmpty()
        return if (primary.isNotEmpty()) primary else fallback?.trim()?.takeIf { it.isNotEmpty() }
    }

    private suspend fun <T> runSection(block: suspend () -> T): Result<T> {
        return try {
            Result.success(block())
        } catch (ce: CancellationException) {
            throw ce
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }

    private companion object {
        private val RAM_VALUE_REGEX = Regex(
            pattern = """([0-9][0-9,]*(?:\.[0-9]+)?)\s*([KMGT]i?B?|[KMGT])?""",
            option = RegexOption.IGNORE_CASE,
        )
    }
}

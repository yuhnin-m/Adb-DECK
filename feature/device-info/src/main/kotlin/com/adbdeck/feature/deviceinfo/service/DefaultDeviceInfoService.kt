package com.adbdeck.feature.deviceinfo.service

import adbdeck.feature.device_info.generated.resources.Res
import adbdeck.feature.device_info.generated.resources.*
import com.adbdeck.core.adb.api.device.DeviceInfo
import com.adbdeck.core.adb.api.device.DeviceInfoClient
import com.adbdeck.feature.deviceinfo.DeviceInfoRow
import com.adbdeck.feature.deviceinfo.DeviceInfoSectionKind
import com.adbdeck.feature.deviceinfo.parser.findMostUsedStorageEntry
import com.adbdeck.feature.deviceinfo.parser.normalizeForUi
import com.adbdeck.feature.deviceinfo.parser.parseBatteryHighlights
import com.adbdeck.feature.deviceinfo.parser.parseBatteryStatsHighlights
import com.adbdeck.feature.deviceinfo.parser.parseCellularHighlights
import com.adbdeck.feature.deviceinfo.parser.parseCpuHighlights
import com.adbdeck.feature.deviceinfo.parser.parseCpuIndices
import com.adbdeck.feature.deviceinfo.parser.parseCpuLoadAverage
import com.adbdeck.feature.deviceinfo.parser.parseCpuPolicyNames
import com.adbdeck.feature.deviceinfo.parser.parseDisplayHighlights
import com.adbdeck.feature.deviceinfo.parser.parseDiskstatsHighlight
import com.adbdeck.feature.deviceinfo.parser.parseImsRcsHighlights
import com.adbdeck.feature.deviceinfo.parser.parseNetworkHighlights
import com.adbdeck.feature.deviceinfo.parser.parsePackageUidMap
import com.adbdeck.feature.deviceinfo.parser.parseRamHighlights
import com.adbdeck.feature.deviceinfo.parser.parseRuntimeHighlights
import com.adbdeck.feature.deviceinfo.parser.parseStorageEntries
import com.adbdeck.feature.deviceinfo.parser.parseWmDensity
import com.adbdeck.feature.deviceinfo.parser.parseWmSize
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.jetbrains.compose.resources.getString

/**
 * Реализация [DeviceInfoService] поверх [DeviceInfoClient].
 *
 * Принципы:
 * - best-effort сбор секций;
 * - парсинг команд вынесен в `parser`-пакет;
 * - форматирование вынесено в [DeviceInfoFormatter];
 * - cpufreq-логика вынесена в [CpuFrequencyLoader];
 * - тяжёлые ресурсы (`getprop`/`fetchDeviceInfo`) кэшируются на короткий TTL.
 */
class DefaultDeviceInfoService(
    private val deviceInfoClient: DeviceInfoClient,
) : DeviceInfoService {

    private val cachedDeviceDataMutex = Mutex()
    private val cachedDeviceDataByKey = mutableMapOf<String, CachedDeviceDataEntry>()
    private val inFlightCachedDeviceDataByKey = mutableMapOf<String, Deferred<CachedDeviceData>>()

    private val formatterMutex = Mutex()
    private var cachedFormatter: CachedFormatter? = null

    override suspend fun loadSection(
        section: DeviceInfoSectionKind,
        deviceId: String,
        adbPath: String,
    ): Result<List<DeviceInfoRow>> = runSection {
        val formatter = loadFormatter()
        val cachedData = if (section.requiresCachedDeviceData()) {
            loadCachedResources(deviceId = deviceId, adbPath = adbPath)
        } else {
            null
        }

        val rows = when (section) {
            DeviceInfoSectionKind.OVERVIEW -> loadOverview(
                deviceId = deviceId,
                cachedData = cachedData ?: CachedDeviceData.EMPTY,
            )

            DeviceInfoSectionKind.BUILD -> loadBuild(
                props = (cachedData ?: CachedDeviceData.EMPTY).props,
                formatter = formatter,
            )

            DeviceInfoSectionKind.DISPLAY -> loadDisplay(deviceId, adbPath)

            DeviceInfoSectionKind.CPU_RAM -> loadCpuRam(
                deviceId = deviceId,
                adbPath = adbPath,
                props = (cachedData ?: CachedDeviceData.EMPTY).props,
                formatter = formatter,
            )

            DeviceInfoSectionKind.BATTERY -> loadBattery(
                deviceId = deviceId,
                adbPath = adbPath,
                formatter = formatter,
            )

            DeviceInfoSectionKind.NETWORK -> loadNetwork(deviceId, adbPath)

            DeviceInfoSectionKind.CELLULAR -> loadCellular(
                deviceId = deviceId,
                adbPath = adbPath,
                formatter = formatter,
            )

            DeviceInfoSectionKind.MODEM -> loadModem(
                deviceId = deviceId,
                adbPath = adbPath,
                props = (cachedData ?: CachedDeviceData.EMPTY).props,
                formatter = formatter,
            )

            DeviceInfoSectionKind.IMS_RCS -> loadImsRcs(
                deviceId = deviceId,
                adbPath = adbPath,
                formatter = formatter,
            )
            DeviceInfoSectionKind.STORAGE -> loadStorage(deviceId, adbPath)

            DeviceInfoSectionKind.SECURITY -> loadSecurity(
                deviceId = deviceId,
                adbPath = adbPath,
                props = (cachedData ?: CachedDeviceData.EMPTY).props,
                formatter = formatter,
            )

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
        cachedData: CachedDeviceData,
    ): List<DeviceInfoRow> {
        val info = cachedData.info
        val props = cachedData.props

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
        props: Map<String, String>,
        formatter: DeviceInfoFormatter,
    ): List<DeviceInfoRow> {
        val locale = formatter.resolveLocale(props)

        return buildRows(
            section = DeviceInfoSectionKind.BUILD,
            values = listOf(
                getString(Res.string.device_info_row_fingerprint) to props["ro.build.fingerprint"],
                getString(Res.string.device_info_row_build_id) to props["ro.build.id"],
                getString(Res.string.device_info_row_build_type) to props["ro.build.type"],
                getString(Res.string.device_info_row_debuggable) to props["ro.debuggable"]?.let(formatter::asYesNo),
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
        props: Map<String, String>,
        formatter: DeviceInfoFormatter,
    ): List<DeviceInfoRow> {
        val uname = runShell(deviceId, adbPath, "uname", "-a")
        val arch = runShell(deviceId, adbPath, "uname", "-m")
            ?.lineSequence()
            ?.firstOrNull()
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
        val cpuInfo = runShell(deviceId, adbPath, "cat", "/proc/cpuinfo").orEmpty()
        val memInfo = runShell(deviceId, adbPath, "dumpsys", "meminfo").orEmpty()
        val loadAverage = parseCpuLoadAverage(
            runShell(deviceId, adbPath, "cat", "/proc/loadavg").orEmpty(),
        )
        val presentCpuIndices = parseCpuIndices(
            runShell(deviceId, adbPath, "cat", "/sys/devices/system/cpu/present"),
        )
        val cpufreqPolicies = parseCpuPolicyNames(
            runShell(deviceId, adbPath, "ls", "/sys/devices/system/cpu/cpufreq/").orEmpty(),
        )

        val cpu = parseCpuHighlights(cpuInfo)
        val ram = parseRamHighlights(memInfo)
        val coresCount = presentCpuIndices.size
            .takeIf { it > 0 }
            ?: cpu.cores

        suspend fun readProp(key: String): String? {
            val fromMap = props[key]?.trim()?.takeIf { it.isNotEmpty() }
            if (fromMap != null) return fromMap
            return runShell(deviceId, adbPath, "getprop", key)
                ?.lineSequence()
                ?.firstOrNull()
                ?.trim()
                ?.takeIf { it.isNotEmpty() }
        }

        val cpuFrequency = CpuFrequencyLoader(
            runShell = { command -> runShell(deviceId, adbPath, *command.toTypedArray()) },
        ).loadSummary(
            policyNames = cpufreqPolicies,
            presentCpuIndices = presentCpuIndices,
            coresCountFallback = coresCount,
        )

        val loadAverageValue = loadAverage?.let {
            "${it.oneMinute} / ${it.fiveMinutes} / ${it.fifteenMinutes}"
        }

        return buildRows(
            section = DeviceInfoSectionKind.CPU_RAM,
            values = listOf(
                getString(Res.string.device_info_row_kernel) to uname,
                getString(Res.string.device_info_row_cpu_arch) to arch,
                getString(Res.string.device_info_row_cpu_cores) to coresCount?.toString(),
                getString(Res.string.device_info_row_cpu_soc_model) to readProp("ro.soc.model").orIfBlank(cpu.model),
                getString(Res.string.device_info_row_cpu_soc_manufacturer) to readProp("ro.soc.manufacturer"),
                getString(Res.string.device_info_row_cpu_hardware) to readProp("ro.hardware"),
                getString(Res.string.device_info_row_cpu_load_avg) to loadAverageValue,
                getString(Res.string.device_info_row_cpu_governor) to cpuFrequency.governor,
                getString(Res.string.device_info_row_cpu_min_max_freq) to cpuFrequency.minMax,
                getString(Res.string.device_info_row_cpu_current_freq) to cpuFrequency.current,
                getString(Res.string.device_info_row_cpu_online_cores) to cpuFrequency.onlineCores,
                getString(Res.string.device_info_row_cpu_features) to cpu.features,
                getString(Res.string.device_info_row_ram_total) to formatter.formatRamMegabytes(ram.total),
                getString(Res.string.device_info_row_ram_used) to formatter.formatRamMegabytes(ram.used),
                getString(Res.string.device_info_row_ram_free) to formatter.formatRamMegabytes(ram.free),
                getString(Res.string.device_info_row_ram_lost) to formatter.formatRamMegabytes(ram.lost),
            ),
        )
    }

    // ── Section: Battery ─────────────────────────────────────────────────────

    private suspend fun loadBattery(
        deviceId: String,
        adbPath: String,
        formatter: DeviceInfoFormatter,
    ): List<DeviceInfoRow> {
        val batteryOutput = runShell(deviceId, adbPath, "dumpsys", "battery").orEmpty()
        val batteryStatsOutput = runShell(deviceId, adbPath, "dumpsys", "batterystats").orEmpty()
        val packageUidOutput = runShell(deviceId, adbPath, "pm", "list", "packages", "-U")
            .orIfBlank(runShell(deviceId, adbPath, "cmd", "package", "list", "packages", "-U"))
            .orEmpty()
        val battery = parseBatteryHighlights(batteryOutput)
        val batteryStats = parseBatteryStatsHighlights(batteryStatsOutput)
        val packagesByUid = parsePackageUidMap(packageUidOutput)

        val unavailable = getString(Res.string.device_info_value_unavailable)
        val topAppsByPower = batteryStats.topAppsByPower
            .take(5)
            .map { row ->
                formatTopAppByPowerRow(
                    uidToken = row.uidToken,
                    powerMahRaw = row.powerMahRaw,
                    packagesByUid = packagesByUid,
                )
            }
            .joinToString(separator = "; ")
            .ifBlank { unavailable }

        return buildRows(
            section = DeviceInfoSectionKind.BATTERY,
            values = listOf(
                getString(Res.string.device_info_row_battery_level) to battery.level?.let(formatter::formatPercent).orUnavailable(unavailable),
                getString(Res.string.device_info_row_battery_status) to formatter.formatBatteryStatusWithCode(
                    highlights = battery,
                    unavailableValue = unavailable,
                ),
                getString(Res.string.device_info_row_battery_plugged) to formatter.batteryPluggedText(
                    highlights = battery,
                    unavailableValue = unavailable,
                ),
                getString(Res.string.device_info_row_battery_temp) to battery.temperatureTenthsC?.let {
                    formatter.formatCelsius(it / 10f)
                }.orUnavailable(unavailable),
                getString(Res.string.device_info_row_battery_voltage) to battery.voltageMv?.let {
                    getString(Res.string.device_info_value_mv_format, it)
                }.orUnavailable(unavailable),
                getString(Res.string.device_info_row_battery_health) to formatter.formatBatteryHealthWithCode(
                    highlights = battery,
                    unavailableValue = unavailable,
                ),
                getString(Res.string.device_info_row_battery_technology) to battery.technology.orUnavailable(unavailable),
                getString(Res.string.device_info_row_battery_current) to formatter.formatBatteryCurrentMa(
                    currentNowRaw = battery.currentNowRaw,
                    currentRaw = battery.currentRaw,
                ).orUnavailable(unavailable),
                getString(Res.string.device_info_row_battery_charge_counter) to battery.chargeCounterUah?.let {
                    getString(Res.string.device_info_value_uah_format, it)
                }.orUnavailable(unavailable),
                getString(Res.string.device_info_row_battery_capacity_estimate) to formatter.formatCapacityEstimate(
                    capacityRaw = battery.capacityRaw,
                    estimatedCapacityRaw = battery.estimatedCapacityRaw,
                ).orUnavailable(unavailable),
                getString(Res.string.device_info_row_battery_max_charging_current) to formatter.formatMicroAmpereToMilliAmpere(
                    raw = battery.maxChargingCurrentRaw,
                ).orUnavailable(unavailable),
                getString(Res.string.device_info_row_battery_max_charging_voltage) to formatter.formatMicroVoltToMilliVolt(
                    raw = battery.maxChargingVoltageRaw,
                ).orUnavailable(unavailable),
                getString(Res.string.device_info_row_battery_present) to battery.present?.let(formatter::yesNo).orUnavailable(unavailable),
                getString(Res.string.device_info_row_batterystats_time_on_battery) to batteryStats.timeOnBattery.orUnavailable(unavailable),
                getString(Res.string.device_info_row_batterystats_screen_on_time) to batteryStats.screenOnTime.orUnavailable(unavailable),
                getString(Res.string.device_info_row_batterystats_wifi_on_time) to batteryStats.wifiOnTime.orUnavailable(unavailable),
                getString(Res.string.device_info_row_batterystats_mobile_radio_active_time) to batteryStats.mobileRadioActiveTime.orUnavailable(unavailable),
                getString(Res.string.device_info_row_batterystats_wakelocks_summary) to batteryStats.wakelocksSummary.orUnavailable(unavailable),
                getString(Res.string.device_info_row_batterystats_top_apps) to topAppsByPower,
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

    // ── Section: Cellular ────────────────────────────────────────────────────

    private suspend fun loadCellular(
        deviceId: String,
        adbPath: String,
        formatter: DeviceInfoFormatter,
    ): List<DeviceInfoRow> {
        val telephonyRegistry = runShell(deviceId, adbPath, "dumpsys", "telephony.registry").orEmpty()
        val highlights = parseCellularHighlights(telephonyRegistry)

        val hidden = getString(Res.string.device_info_value_hidden)
        val unavailable = getString(Res.string.device_info_value_unavailable)

        return buildRows(
            section = DeviceInfoSectionKind.CELLULAR,
            values = listOf(
                getString(Res.string.device_info_row_cellular_operator) to formatter.formatOperator(
                    longName = highlights.operatorAlphaLong,
                    shortName = highlights.operatorAlphaShort,
                    hiddenValue = hidden,
                    unavailableValue = unavailable,
                ),
                getString(Res.string.device_info_row_cellular_mcc_mnc) to formatter.formatMccMnc(
                    mcc = highlights.mcc,
                    mnc = highlights.mnc,
                    rplmn = highlights.rplmn,
                    hiddenValue = hidden,
                    unavailableValue = unavailable,
                ),
                getString(Res.string.device_info_row_cellular_voice_reg_state) to formatter.displayValueOrFallback(
                    raw = highlights.voiceRegState,
                    hiddenValue = hidden,
                    unavailableValue = unavailable,
                ),
                getString(Res.string.device_info_row_cellular_data_reg_state) to formatter.displayValueOrFallback(
                    raw = highlights.dataRegState,
                    hiddenValue = hidden,
                    unavailableValue = unavailable,
                ),
                getString(Res.string.device_info_row_cellular_roaming) to formatter.displayValueOrFallback(
                    raw = highlights.roamingType,
                    hiddenValue = hidden,
                    unavailableValue = unavailable,
                ),
                getString(Res.string.device_info_row_cellular_voice_tech) to formatter.displayValueOrFallback(
                    raw = highlights.voiceTechnology,
                    hiddenValue = hidden,
                    unavailableValue = unavailable,
                ),
                getString(Res.string.device_info_row_cellular_data_tech) to formatter.displayValueOrFallback(
                    raw = highlights.dataTechnology,
                    hiddenValue = hidden,
                    unavailableValue = unavailable,
                ),
                getString(Res.string.device_info_row_cellular_channel_number) to formatter.displayValueOrFallback(
                    raw = highlights.channelNumber,
                    hiddenValue = hidden,
                    unavailableValue = unavailable,
                ),
                getString(Res.string.device_info_row_cellular_lte_bands) to formatter.displayValueOrFallback(
                    raw = highlights.lteBands,
                    hiddenValue = hidden,
                    unavailableValue = unavailable,
                ),
                getString(Res.string.device_info_row_cellular_bandwidth) to formatter.formatCombinedValues(
                    first = highlights.cellBandwidths,
                    second = highlights.bandwidth,
                    hiddenValue = hidden,
                    unavailableValue = unavailable,
                ),
                getString(Res.string.device_info_row_cellular_cell_identity) to formatter.formatCellIdentity(
                    ci = highlights.cellCi,
                    pci = highlights.cellPci,
                    tac = highlights.cellTac,
                    earfcn = highlights.cellEarfcn,
                    hiddenValue = hidden,
                    unavailableValue = unavailable,
                ),
                getString(Res.string.device_info_row_cellular_nr_available) to formatter.displayValueOrFallback(
                    raw = highlights.nrAvailable,
                    hiddenValue = hidden,
                    unavailableValue = unavailable,
                ),
                getString(Res.string.device_info_row_cellular_endc_available) to formatter.displayValueOrFallback(
                    raw = highlights.enDcAvailable,
                    hiddenValue = hidden,
                    unavailableValue = unavailable,
                ),
                getString(Res.string.device_info_row_cellular_dcnr_restricted) to formatter.displayValueOrFallback(
                    raw = highlights.dcNrRestricted,
                    hiddenValue = hidden,
                    unavailableValue = unavailable,
                ),
                getString(Res.string.device_info_row_cellular_nr_frequency_range) to formatter.displayValueOrFallback(
                    raw = highlights.nrFrequencyRange,
                    hiddenValue = hidden,
                    unavailableValue = unavailable,
                ),
                getString(Res.string.device_info_row_cellular_carrier_aggregation) to formatter.displayValueOrFallback(
                    raw = highlights.carrierAggregation,
                    hiddenValue = hidden,
                    unavailableValue = unavailable,
                ),
            ),
        )
    }

    // ── Section: Modem ───────────────────────────────────────────────────────

    private suspend fun loadModem(
        deviceId: String,
        adbPath: String,
        props: Map<String, String>,
        formatter: DeviceInfoFormatter,
    ): List<DeviceInfoRow> {
        val hidden = getString(Res.string.device_info_value_hidden)
        val unavailable = getString(Res.string.device_info_value_unavailable)

        suspend fun readProp(key: String): String? {
            val fromMap = props[key]
            if (!fromMap.isNullOrBlank()) return fromMap
            return runShell(deviceId, adbPath, "getprop", key)
                ?.lineSequence()
                ?.firstOrNull()
                ?.trim()
                ?.takeIf { it.isNotEmpty() }
        }

        return buildRows(
            section = DeviceInfoSectionKind.MODEM,
            values = listOf(
                getString(Res.string.device_info_row_modem_baseband) to formatter.displayValueOrFallback(
                    raw = readProp("gsm.version.baseband"),
                    hiddenValue = hidden,
                    unavailableValue = unavailable,
                ),
                getString(Res.string.device_info_row_modem_expected_baseband) to formatter.displayValueOrFallback(
                    raw = readProp("ro.build.expect.baseband"),
                    hiddenValue = hidden,
                    unavailableValue = unavailable,
                ),
                getString(Res.string.device_info_row_modem_ril_impl) to formatter.displayValueOrFallback(
                    raw = readProp("gsm.version.ril-impl"),
                    hiddenValue = hidden,
                    unavailableValue = unavailable,
                ),
                getString(Res.string.device_info_row_modem_ril_daemon) to formatter.displayValueOrFallback(
                    raw = readProp("init.svc.ril-daemon"),
                    hiddenValue = hidden,
                    unavailableValue = unavailable,
                ),
                getString(Res.string.device_info_row_modem_max_modems) to formatter.displayValueOrFallback(
                    raw = readProp("telephony.active_modems.max_count"),
                    hiddenValue = hidden,
                    unavailableValue = unavailable,
                ),
                getString(Res.string.device_info_row_modem_multi_sim) to formatter.displayValueOrFallback(
                    raw = readProp("persist.radio.multisim.config"),
                    hiddenValue = hidden,
                    unavailableValue = unavailable,
                ),
                getString(Res.string.device_info_row_modem_airplane_mode) to formatter.displayValueOrFallback(
                    raw = readProp("persist.radio.airplane_mode_on"),
                    hiddenValue = hidden,
                    unavailableValue = unavailable,
                ),
                getString(Res.string.device_info_row_modem_vonr_enabled_slot0) to formatter.displayValueOrFallback(
                    raw = readProp("persist.radio.is_vonr_enabled_0"),
                    hiddenValue = hidden,
                    unavailableValue = unavailable,
                ),
            ),
        )
    }

    // ── Section: IMS / RCS ───────────────────────────────────────────────────

    private suspend fun loadImsRcs(
        deviceId: String,
        adbPath: String,
        formatter: DeviceInfoFormatter,
    ): List<DeviceInfoRow> {
        val dumpsysPhone = runShell(deviceId, adbPath, "dumpsys", "phone").orEmpty()
        val highlights = parseImsRcsHighlights(dumpsysPhone)

        val hidden = getString(Res.string.device_info_value_hidden)
        val unavailable = getString(Res.string.device_info_value_unavailable)

        val imsServices = if (highlights.imsServices.isNotEmpty()) {
            highlights.imsServices.joinToString()
        } else {
            unavailable
        }

        val capabilities = if (highlights.capabilities.isNotEmpty()) {
            highlights.capabilities.joinToString()
        } else {
            unavailable
        }

        return buildRows(
            section = DeviceInfoSectionKind.IMS_RCS,
            values = listOf(
                getString(Res.string.device_info_row_ims_default_data_sub_id) to formatter.displayValueOrFallback(
                    raw = highlights.defaultDataSubId,
                    hiddenValue = hidden,
                    unavailableValue = unavailable,
                ),
                getString(Res.string.device_info_row_ims_services) to imsServices,
                getString(Res.string.device_info_row_ims_mmtel_state) to formatter.displayValueOrFallback(
                    raw = highlights.mmTelState,
                    hiddenValue = hidden,
                    unavailableValue = unavailable,
                ),
                getString(Res.string.device_info_row_ims_rcs_state) to formatter.displayValueOrFallback(
                    raw = highlights.rcsState,
                    hiddenValue = hidden,
                    unavailableValue = unavailable,
                ),
                getString(Res.string.device_info_row_ims_capabilities) to capabilities,
                getString(Res.string.device_info_row_ims_slot_sub_id_map) to formatter.displayValueOrFallback(
                    raw = highlights.slotToSubIdMap,
                    hiddenValue = hidden,
                    unavailableValue = unavailable,
                ),
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
        props: Map<String, String>,
        formatter: DeviceInfoFormatter,
    ): List<DeviceInfoRow> {
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
                getString(Res.string.device_info_row_debuggable) to props["ro.debuggable"]?.let(formatter::asYesNo),
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

    private suspend fun loadFormatter(): DeviceInfoFormatter {
        val localeTag = Locale.getDefault().toLanguageTag()

        var cached: DeviceInfoFormatter? = null
        formatterMutex.withLock {
            cached = cachedFormatter
                ?.takeIf { it.localeTag == localeTag }
                ?.formatter
        }
        if (cached != null) return cached!!

        val formatter = DeviceInfoFormatter.fromResources()
        formatterMutex.withLock {
            cachedFormatter = CachedFormatter(
                localeTag = localeTag,
                formatter = formatter,
            )
        }
        return formatter
    }

    private suspend fun loadCachedResources(
        deviceId: String,
        adbPath: String,
    ): CachedDeviceData = coroutineScope {
        val key = cacheKey(deviceId, adbPath)
        val now = System.currentTimeMillis()

        var cached: CachedDeviceData? = null
        cachedDeviceDataMutex.withLock {
            val entry = cachedDeviceDataByKey[key]
            if (entry != null && now - entry.loadedAtMillis <= CACHED_DEVICE_DATA_TTL_MILLIS) {
                cached = entry.data
            }
        }
        if (cached != null) return@coroutineScope cached!!

        val deferred = cachedDeviceDataMutex.withLock {
            inFlightCachedDeviceDataByKey[key] ?: async {
                val propsDeferred = async {
                    deviceInfoClient.getSystemProperties(deviceId, adbPath).getOrDefault(emptyMap())
                }
                val infoDeferred = async {
                    deviceInfoClient.fetchDeviceInfo(deviceId, adbPath).getOrNull()
                }
                CachedDeviceData(
                    props = propsDeferred.await(),
                    info = infoDeferred.await(),
                )
            }.also { inFlightCachedDeviceDataByKey[key] = it }
        }

        try {
            val loaded = deferred.await()
            cachedDeviceDataMutex.withLock {
                cachedDeviceDataByKey[key] = CachedDeviceDataEntry(
                    loadedAtMillis = System.currentTimeMillis(),
                    data = loaded,
                )
                trimCachedDataLocked()
            }
            loaded
        } finally {
            cachedDeviceDataMutex.withLock {
                if (inFlightCachedDeviceDataByKey[key] === deferred) {
                    inFlightCachedDeviceDataByKey.remove(key)
                }
            }
        }
    }

    private fun trimCachedDataLocked() {
        if (cachedDeviceDataByKey.size <= MAX_CACHED_DEVICES) return
        val oldestKey = cachedDeviceDataByKey
            .minByOrNull { (_, value) -> value.loadedAtMillis }
            ?.key
            ?: return
        cachedDeviceDataByKey.remove(oldestKey)
    }

    private fun cacheKey(deviceId: String, adbPath: String): String {
        return "$adbPath::$deviceId"
    }

    private fun DeviceInfoSectionKind.requiresCachedDeviceData(): Boolean {
        return when (this) {
            DeviceInfoSectionKind.OVERVIEW,
            DeviceInfoSectionKind.BUILD,
            DeviceInfoSectionKind.CPU_RAM,
            DeviceInfoSectionKind.MODEM,
            DeviceInfoSectionKind.SECURITY,
            -> true

            else -> false
        }
    }

    private fun buildRows(
        section: DeviceInfoSectionKind,
        values: List<Pair<String, String?>>,
    ): List<DeviceInfoRow> {
        val keyHashCounts = mutableMapOf<Int, Int>()
        return values.mapNotNull { (key, value) ->
            val normalized = value?.trim()?.takeIf { it.isNotEmpty() } ?: return@mapNotNull null
            val keyHash = key.hashCode()
            val duplicateIndex = keyHashCounts.getOrDefault(keyHash, 0)
            keyHashCounts[keyHash] = duplicateIndex + 1
            val duplicateSuffix = if (duplicateIndex == 0) "" else ":$duplicateIndex"

            DeviceInfoRow(
                id = "${section.id}:$keyHash$duplicateSuffix",
                key = key,
                value = normalizeForUi(normalized),
            )
        }
    }

    private fun String?.orIfBlank(fallback: String?): String? {
        val primary = this?.trim().orEmpty()
        return if (primary.isNotEmpty()) primary else fallback?.trim()?.takeIf { it.isNotEmpty() }
    }

    /**
     * Формирует строку топ-потребителя энергии в формате:
     * `package.name: 123.4 mAh` или `package.name (+N): 123.4 mAh`.
     */
    private fun formatTopAppByPowerRow(
        uidToken: String,
        powerMahRaw: String,
        packagesByUid: Map<Int, List<String>>,
    ): String {
        val label = resolveUidDisplayLabel(
            uidToken = uidToken,
            packagesByUid = packagesByUid,
        )
        return "$label: $powerMahRaw mAh"
    }

    /**
     * Возвращает читаемый label для UID:
     * - найден пакет -> `package` или `package (+N)`;
     * - пакет не найден -> исходный UID-токен.
     */
    private fun resolveUidDisplayLabel(
        uidToken: String,
        packagesByUid: Map<Int, List<String>>,
    ): String {
        val uid = parseUidToken(uidToken) ?: return uidToken
        val packages = packagesByUid[uid]
            ?.distinct()
            ?.sortedWith(
                compareBy<String>(
                    { it.contains("auto_generated_rro", ignoreCase = true) || it.contains(".overlay", ignoreCase = true) },
                    { it.length },
                    { it },
                ),
            )
            .orEmpty()

        if (packages.isEmpty()) return uidToken
        val primary = packages.first()
        val extraCount = packages.size - 1
        return if (extraCount > 0) "$primary (+$extraCount)" else primary
    }

    /**
     * Конвертирует UID-токен batterystats (`u0a301`, `1000`, `u10s1000`) в numeric UID.
     */
    private fun parseUidToken(uidToken: String): Int? {
        val normalized = uidToken.trim()
        if (normalized.isEmpty()) return null

        normalized.toIntOrNull()?.let { return it }

        val match = UID_TOKEN_REGEX.matchEntire(normalized.lowercase()) ?: return null
        val userId = match.groupValues.getOrElse(1) { "" }.toIntOrNull() ?: return null
        val kind = match.groupValues.getOrElse(2) { "" }.firstOrNull() ?: return null
        val id = match.groupValues.getOrElse(3) { "" }.toIntOrNull() ?: return null

        val appId = when (kind) {
            'a' -> 10_000 + id
            's' -> id
            'i' -> 99_000 + id
            else -> return null
        }
        return userId * 100_000 + appId
    }

    private fun String?.orUnavailable(unavailable: String): String = this ?: unavailable

    private suspend fun <T> runSection(block: suspend () -> T): Result<T> {
        return try {
            Result.success(block())
        } catch (ce: CancellationException) {
            throw ce
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }

    private data class CachedDeviceData(
        val props: Map<String, String>,
        val info: DeviceInfo?,
    ) {
        companion object {
            val EMPTY = CachedDeviceData(
                props = emptyMap(),
                info = null,
            )
        }
    }

    private data class CachedDeviceDataEntry(
        val loadedAtMillis: Long,
        val data: CachedDeviceData,
    )

    private data class CachedFormatter(
        val localeTag: String,
        val formatter: DeviceInfoFormatter,
    )

    private companion object {
        private const val CACHED_DEVICE_DATA_TTL_MILLIS = 3_000L
        private const val MAX_CACHED_DEVICES = 6

        private val UID_TOKEN_REGEX = Regex(
            pattern = """^u(\d+)([asi])(\d+)$""",
            option = RegexOption.IGNORE_CASE,
        )
    }
}

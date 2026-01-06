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
import com.adbdeck.feature.deviceinfo.parser.parsePackageUidMap
import com.adbdeck.feature.deviceinfo.parser.parseBatteryStatsHighlights
import com.adbdeck.feature.deviceinfo.parser.parseCellularHighlights
import com.adbdeck.feature.deviceinfo.parser.parseCpuHighlights
import com.adbdeck.feature.deviceinfo.parser.parseCpuIndices
import com.adbdeck.feature.deviceinfo.parser.parseCpuLoadAverage
import com.adbdeck.feature.deviceinfo.parser.parseCpuOnlineState
import com.adbdeck.feature.deviceinfo.parser.parseCpuPolicyNames
import com.adbdeck.feature.deviceinfo.parser.parseDisplayHighlights
import com.adbdeck.feature.deviceinfo.parser.parseDiskstatsHighlight
import com.adbdeck.feature.deviceinfo.parser.parseImsRcsHighlights
import com.adbdeck.feature.deviceinfo.parser.parseNetworkHighlights
import com.adbdeck.feature.deviceinfo.parser.parseRamHighlights
import com.adbdeck.feature.deviceinfo.parser.parseRuntimeHighlights
import com.adbdeck.feature.deviceinfo.parser.parseStorageEntries
import com.adbdeck.feature.deviceinfo.parser.parseWmDensity
import com.adbdeck.feature.deviceinfo.parser.parseWmSize
import java.util.Locale
import kotlin.math.abs
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
            DeviceInfoSectionKind.CELLULAR -> loadCellular(deviceId, adbPath)
            DeviceInfoSectionKind.MODEM -> loadModem(deviceId, adbPath)
            DeviceInfoSectionKind.IMS_RCS -> loadImsRcs(deviceId, adbPath)
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
        val props = deviceInfoClient.getSystemProperties(deviceId, adbPath).getOrDefault(emptyMap())
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

        val cpuFrequency = loadCpuFrequencySummary(
            deviceId = deviceId,
            adbPath = adbPath,
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
                getString(Res.string.device_info_row_battery_level) to battery.level?.let(::formatPercent).orUnavailable(unavailable),
                getString(Res.string.device_info_row_battery_status) to formatBatteryStatusWithCode(
                    highlights = battery,
                    unavailableValue = unavailable,
                ),
                getString(Res.string.device_info_row_battery_plugged) to batteryPluggedText(
                    highlights = battery,
                    unavailableValue = unavailable,
                ),
                getString(Res.string.device_info_row_battery_temp) to battery.temperatureTenthsC?.let {
                    formatCelsius(it / 10f)
                }.orUnavailable(unavailable),
                getString(Res.string.device_info_row_battery_voltage) to battery.voltageMv?.let {
                    getString(Res.string.device_info_value_mv_format, it)
                }.orUnavailable(unavailable),
                getString(Res.string.device_info_row_battery_health) to formatBatteryHealthWithCode(
                    highlights = battery,
                    unavailableValue = unavailable,
                ),
                getString(Res.string.device_info_row_battery_technology) to battery.technology.orUnavailable(unavailable),
                getString(Res.string.device_info_row_battery_current) to formatBatteryCurrentMa(
                    currentNowRaw = battery.currentNowRaw,
                    currentRaw = battery.currentRaw,
                ).orUnavailable(unavailable),
                getString(Res.string.device_info_row_battery_charge_counter) to battery.chargeCounterUah?.let {
                    getString(Res.string.device_info_value_uah_format, it)
                }.orUnavailable(unavailable),
                getString(Res.string.device_info_row_battery_capacity_estimate) to formatCapacityEstimate(
                    capacityRaw = battery.capacityRaw,
                    estimatedCapacityRaw = battery.estimatedCapacityRaw,
                ).orUnavailable(unavailable),
                getString(Res.string.device_info_row_battery_max_charging_current) to formatMicroAmpereToMilliAmpere(
                    raw = battery.maxChargingCurrentRaw,
                ).orUnavailable(unavailable),
                getString(Res.string.device_info_row_battery_max_charging_voltage) to formatMicroVoltToMilliVolt(
                    raw = battery.maxChargingVoltageRaw,
                ).orUnavailable(unavailable),
                getString(Res.string.device_info_row_battery_present) to battery.present?.let {
                    if (it) getString(Res.string.device_info_value_yes) else getString(Res.string.device_info_value_no)
                }.orUnavailable(unavailable),
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
    ): List<DeviceInfoRow> {
        val telephonyRegistry = runShell(deviceId, adbPath, "dumpsys", "telephony.registry").orEmpty()
        val highlights = parseCellularHighlights(telephonyRegistry)

        val hidden = getString(Res.string.device_info_value_hidden)
        val unavailable = getString(Res.string.device_info_value_unavailable)

        return buildRows(
            section = DeviceInfoSectionKind.CELLULAR,
            values = listOf(
                getString(Res.string.device_info_row_cellular_operator) to formatOperator(
                    longName = highlights.operatorAlphaLong,
                    shortName = highlights.operatorAlphaShort,
                    hiddenValue = hidden,
                    unavailableValue = unavailable,
                ),
                getString(Res.string.device_info_row_cellular_mcc_mnc) to formatMccMnc(
                    mcc = highlights.mcc,
                    mnc = highlights.mnc,
                    rplmn = highlights.rplmn,
                    hiddenValue = hidden,
                    unavailableValue = unavailable,
                ),
                getString(Res.string.device_info_row_cellular_voice_reg_state) to displayValueOrFallback(
                    raw = highlights.voiceRegState,
                    hiddenValue = hidden,
                    unavailableValue = unavailable,
                ),
                getString(Res.string.device_info_row_cellular_data_reg_state) to displayValueOrFallback(
                    raw = highlights.dataRegState,
                    hiddenValue = hidden,
                    unavailableValue = unavailable,
                ),
                getString(Res.string.device_info_row_cellular_roaming) to displayValueOrFallback(
                    raw = highlights.roamingType,
                    hiddenValue = hidden,
                    unavailableValue = unavailable,
                ),
                getString(Res.string.device_info_row_cellular_voice_tech) to displayValueOrFallback(
                    raw = highlights.voiceTechnology,
                    hiddenValue = hidden,
                    unavailableValue = unavailable,
                ),
                getString(Res.string.device_info_row_cellular_data_tech) to displayValueOrFallback(
                    raw = highlights.dataTechnology,
                    hiddenValue = hidden,
                    unavailableValue = unavailable,
                ),
                getString(Res.string.device_info_row_cellular_channel_number) to displayValueOrFallback(
                    raw = highlights.channelNumber,
                    hiddenValue = hidden,
                    unavailableValue = unavailable,
                ),
                getString(Res.string.device_info_row_cellular_lte_bands) to displayValueOrFallback(
                    raw = highlights.lteBands,
                    hiddenValue = hidden,
                    unavailableValue = unavailable,
                ),
                getString(Res.string.device_info_row_cellular_bandwidth) to formatCombinedValues(
                    first = highlights.cellBandwidths,
                    second = highlights.bandwidth,
                    hiddenValue = hidden,
                    unavailableValue = unavailable,
                ),
                getString(Res.string.device_info_row_cellular_cell_identity) to formatCellIdentity(
                    ci = highlights.cellCi,
                    pci = highlights.cellPci,
                    tac = highlights.cellTac,
                    earfcn = highlights.cellEarfcn,
                    hiddenValue = hidden,
                    unavailableValue = unavailable,
                ),
                getString(Res.string.device_info_row_cellular_nr_available) to displayValueOrFallback(
                    raw = highlights.nrAvailable,
                    hiddenValue = hidden,
                    unavailableValue = unavailable,
                ),
                getString(Res.string.device_info_row_cellular_endc_available) to displayValueOrFallback(
                    raw = highlights.enDcAvailable,
                    hiddenValue = hidden,
                    unavailableValue = unavailable,
                ),
                getString(Res.string.device_info_row_cellular_dcnr_restricted) to displayValueOrFallback(
                    raw = highlights.dcNrRestricted,
                    hiddenValue = hidden,
                    unavailableValue = unavailable,
                ),
                getString(Res.string.device_info_row_cellular_nr_frequency_range) to displayValueOrFallback(
                    raw = highlights.nrFrequencyRange,
                    hiddenValue = hidden,
                    unavailableValue = unavailable,
                ),
                getString(Res.string.device_info_row_cellular_carrier_aggregation) to displayValueOrFallback(
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
    ): List<DeviceInfoRow> {
        val props = deviceInfoClient.getSystemProperties(deviceId, adbPath).getOrDefault(emptyMap())
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
                getString(Res.string.device_info_row_modem_baseband) to displayValueOrFallback(
                    raw = readProp("gsm.version.baseband"),
                    hiddenValue = hidden,
                    unavailableValue = unavailable,
                ),
                getString(Res.string.device_info_row_modem_expected_baseband) to displayValueOrFallback(
                    raw = readProp("ro.build.expect.baseband"),
                    hiddenValue = hidden,
                    unavailableValue = unavailable,
                ),
                getString(Res.string.device_info_row_modem_ril_impl) to displayValueOrFallback(
                    raw = readProp("gsm.version.ril-impl"),
                    hiddenValue = hidden,
                    unavailableValue = unavailable,
                ),
                getString(Res.string.device_info_row_modem_ril_daemon) to displayValueOrFallback(
                    raw = readProp("init.svc.ril-daemon"),
                    hiddenValue = hidden,
                    unavailableValue = unavailable,
                ),
                getString(Res.string.device_info_row_modem_max_modems) to displayValueOrFallback(
                    raw = readProp("telephony.active_modems.max_count"),
                    hiddenValue = hidden,
                    unavailableValue = unavailable,
                ),
                getString(Res.string.device_info_row_modem_multi_sim) to displayValueOrFallback(
                    raw = readProp("persist.radio.multisim.config"),
                    hiddenValue = hidden,
                    unavailableValue = unavailable,
                ),
                getString(Res.string.device_info_row_modem_airplane_mode) to displayValueOrFallback(
                    raw = readProp("persist.radio.airplane_mode_on"),
                    hiddenValue = hidden,
                    unavailableValue = unavailable,
                ),
                getString(Res.string.device_info_row_modem_vonr_enabled_slot0) to displayValueOrFallback(
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
                getString(Res.string.device_info_row_ims_default_data_sub_id) to displayValueOrFallback(
                    raw = highlights.defaultDataSubId,
                    hiddenValue = hidden,
                    unavailableValue = unavailable,
                ),
                getString(Res.string.device_info_row_ims_services) to imsServices,
                getString(Res.string.device_info_row_ims_mmtel_state) to displayValueOrFallback(
                    raw = highlights.mmTelState,
                    hiddenValue = hidden,
                    unavailableValue = unavailable,
                ),
                getString(Res.string.device_info_row_ims_rcs_state) to displayValueOrFallback(
                    raw = highlights.rcsState,
                    hiddenValue = hidden,
                    unavailableValue = unavailable,
                ),
                getString(Res.string.device_info_row_ims_capabilities) to capabilities,
                getString(Res.string.device_info_row_ims_slot_sub_id_map) to displayValueOrFallback(
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

    /**
     * Компактные строки для блока CPU frequency.
     */
    private data class CpuFrequencySummary(
        val governor: String?,
        val minMax: String?,
        val current: String?,
        val onlineCores: String?,
    )

    /**
     * Параметры cpufreq policy.
     */
    private data class CpuPolicyFrequency(
        val policyName: String,
        val governor: String?,
        val minKHz: Long?,
        val maxKHz: Long?,
        val currentKHz: Long?,
        val relatedCpus: List<Int>,
    )

    /**
     * Частоты одного CPU core (fallback без policy*).
     */
    private data class CpuCoreFrequency(
        val cpuIndex: Int,
        val minKHz: Long?,
        val maxKHz: Long?,
        val currentKHz: Long?,
    )

    private suspend fun loadCpuFrequencySummary(
        deviceId: String,
        adbPath: String,
        policyNames: List<String>,
        presentCpuIndices: List<Int>,
        coresCountFallback: Int?,
    ): CpuFrequencySummary {
        if (policyNames.isNotEmpty()) {
            val policies = policyNames.map { policyName ->
                val basePath = "/sys/devices/system/cpu/cpufreq/$policyName"
                CpuPolicyFrequency(
                    policyName = policyName,
                    governor = runShell(deviceId, adbPath, "cat", "$basePath/scaling_governor"),
                    minKHz = parseLongFlexible(runShell(deviceId, adbPath, "cat", "$basePath/scaling_min_freq")),
                    maxKHz = parseLongFlexible(runShell(deviceId, adbPath, "cat", "$basePath/scaling_max_freq")),
                    currentKHz = parseLongFlexible(runShell(deviceId, adbPath, "cat", "$basePath/scaling_cur_freq")),
                    relatedCpus = parseCpuIndices(runShell(deviceId, adbPath, "cat", "$basePath/related_cpus")),
                )
            }

            val governorValue = policies.mapNotNull { policy ->
                val governor = policy.governor?.lineSequence()?.firstOrNull()?.trim()?.takeIf { it.isNotEmpty() }
                governor?.let { "${policy.policyName}: ${normalizeForUi(it)}" }
            }.joinCompactOrNull()

            val minMaxValue = policies.mapNotNull { policy ->
                val min = formatCpuFrequency(policy.minKHz)
                val max = formatCpuFrequency(policy.maxKHz)
                formatCpuMinMax(min = min, max = max)?.let { value ->
                    "${policy.policyName}: $value"
                }
            }.joinCompactOrNull()

            val currentValue = policies.mapNotNull { policy ->
                val current = formatCpuFrequency(policy.currentKHz) ?: return@mapNotNull null
                "${policy.policyName}: $current"
            }.joinCompactOrNull()

            val onlineCandidateCores = presentCpuIndices.ifEmpty {
                policies.flatMap { it.relatedCpus }.distinct().sorted()
            }
            val onlineValue = formatOnlineCoresValue(
                deviceId = deviceId,
                adbPath = adbPath,
                cpuIndices = onlineCandidateCores.ifEmpty {
                    coresCountFallback?.let { count -> (0 until count).toList() } ?: emptyList()
                },
            )

            return CpuFrequencySummary(
                governor = governorValue,
                minMax = minMaxValue,
                current = currentValue,
                onlineCores = onlineValue,
            )
        }

        val fallbackCpuIndices = presentCpuIndices.ifEmpty {
            coresCountFallback?.let { count -> (0 until count).toList() } ?: emptyList()
        }

        val fallbackPerCore = fallbackCpuIndices.mapNotNull { cpuIndex ->
            val basePath = "/sys/devices/system/cpu/cpu$cpuIndex/cpufreq"
            val min = parseLongFlexible(runShell(deviceId, adbPath, "cat", "$basePath/scaling_min_freq"))
            val max = parseLongFlexible(runShell(deviceId, adbPath, "cat", "$basePath/scaling_max_freq"))
            val current = parseLongFlexible(runShell(deviceId, adbPath, "cat", "$basePath/scaling_cur_freq"))
            if (min == null && max == null && current == null) return@mapNotNull null
            CpuCoreFrequency(
                cpuIndex = cpuIndex,
                minKHz = min,
                maxKHz = max,
                currentKHz = current,
            )
        }

        val minMaxValue = fallbackPerCore
            .groupBy { it.minKHz to it.maxKHz }
            .entries
            .sortedBy { entry -> entry.key.second ?: Long.MAX_VALUE }
            .mapNotNull { (minMaxPair, rows) ->
                val min = formatCpuFrequency(minMaxPair.first)
                val max = formatCpuFrequency(minMaxPair.second)
                val value = formatCpuMinMax(min = min, max = max) ?: return@mapNotNull null
                val cores = rows.map { it.cpuIndex }
                "${formatCpuIndexRanges(cores)}: $value"
            }
            .joinCompactOrNull()

        val currentValue = fallbackPerCore
            .mapNotNull { row ->
                val current = formatCpuFrequency(row.currentKHz) ?: return@mapNotNull null
                row.cpuIndex to current
            }
            .groupBy(keySelector = { it.second }, valueTransform = { it.first })
            .entries
            .sortedBy { it.key }
            .map { (freq, cores) -> "${formatCpuIndexRanges(cores)}: $freq" }
            .joinCompactOrNull()

        val onlineValue = formatOnlineCoresValue(
            deviceId = deviceId,
            adbPath = adbPath,
            cpuIndices = fallbackCpuIndices,
        )

        return CpuFrequencySummary(
            governor = null,
            minMax = minMaxValue,
            current = currentValue,
            onlineCores = onlineValue,
        )
    }

    private suspend fun formatOnlineCoresValue(
        deviceId: String,
        adbPath: String,
        cpuIndices: List<Int>,
    ): String? {
        if (cpuIndices.isEmpty()) return null

        val online = cpuIndices.sorted().filter { cpuIndex ->
            if (cpuIndex == 0) {
                true
            } else {
                val rawState = runShell(deviceId, adbPath, "cat", "/sys/devices/system/cpu/cpu$cpuIndex/online")
                parseCpuOnlineState(rawState) ?: true
            }
        }
        if (online.isEmpty()) return null
        return formatCpuIndexRanges(online)
    }

    private fun formatCpuMinMax(
        min: String?,
        max: String?,
    ): String? {
        return when {
            min != null && max != null -> "$min / $max"
            min != null -> min
            max != null -> max
            else -> null
        }
    }

    private fun formatCpuFrequency(khz: Long?): String? {
        val raw = khz?.takeIf { it > 0 } ?: return null
        val mhz = raw / 1_000.0
        return if (mhz >= 1_000.0) {
            val ghz = mhz / 1_000.0
            "${formatDecimal(ghz, 2)} GHz"
        } else {
            "${formatDecimal(mhz, 0)} MHz"
        }
    }

    private fun formatCpuIndexRanges(indices: List<Int>): String {
        if (indices.isEmpty()) return ""
        val sorted = indices.distinct().sorted()
        val chunks = mutableListOf<String>()
        var rangeStart = sorted.first()
        var previous = sorted.first()

        for (index in sorted.drop(1)) {
            if (index == previous + 1) {
                previous = index
                continue
            }
            chunks += if (rangeStart == previous) {
                rangeStart.toString()
            } else {
                "$rangeStart-$previous"
            }
            rangeStart = index
            previous = index
        }

        chunks += if (rangeStart == previous) {
            rangeStart.toString()
        } else {
            "$rangeStart-$previous"
        }
        return chunks.joinToString(separator = ",")
    }

    private fun parseLongFlexible(raw: String?): Long? {
        val value = raw?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        val numeric = Regex("""-?\d+""")
            .find(value.replace(",", ""))
            ?.value
            ?: return null
        return numeric.toLongOrNull()
    }

    private fun formatDecimal(
        value: Double,
        fractionDigits: Int,
    ): String {
        val pattern = when (fractionDigits) {
            0 -> "%.0f"
            1 -> "%.1f"
            else -> "%.2f"
        }
        val formatted = String.format(Locale.getDefault(), pattern, value)
        if (fractionDigits <= 0) return formatted
        return formatted.trimEnd('0').trimEnd('.')
    }

    private fun List<String>.joinCompactOrNull(): String? {
        return map { it.trim() }
            .filter { it.isNotEmpty() }
            .joinToString(separator = "; ")
            .takeIf { it.isNotEmpty() }
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

    private suspend fun formatBatteryStatusWithCode(
        highlights: BatteryHighlights,
        unavailableValue: String,
    ): String {
        val statusCode = highlights.statusCode ?: return unavailableValue
        val statusText = batteryStatusText(highlights)
        return getString(
            Res.string.device_info_value_with_code_format,
            statusText,
            statusCode,
        )
    }

    private suspend fun formatBatteryHealthWithCode(
        highlights: BatteryHighlights,
        unavailableValue: String,
    ): String {
        val healthCode = highlights.healthCode ?: return unavailableValue
        val healthText = when (healthCode) {
            2 -> getString(Res.string.device_info_battery_health_good)
            3 -> getString(Res.string.device_info_battery_health_overheat)
            4 -> getString(Res.string.device_info_battery_health_dead)
            5 -> getString(Res.string.device_info_battery_health_over_voltage)
            6 -> getString(Res.string.device_info_battery_health_unspecified_failure)
            7 -> getString(Res.string.device_info_battery_health_cold)
            else -> getString(Res.string.device_info_battery_health_unknown)
        }
        return getString(
            Res.string.device_info_value_with_code_format,
            healthText,
            healthCode,
        )
    }

    private suspend fun batteryPluggedText(
        highlights: BatteryHighlights,
        unavailableValue: String,
    ): String {
        val pluggedSources = mutableListOf<String>()
        if (highlights.acPowered == true) pluggedSources += getString(Res.string.device_info_value_plugged_ac)
        if (highlights.usbPowered == true) pluggedSources += getString(Res.string.device_info_value_plugged_usb)
        if (highlights.wirelessPowered == true) pluggedSources += getString(Res.string.device_info_value_plugged_wireless)

        if (pluggedSources.isNotEmpty()) return pluggedSources.joinToString(separator = "/")

        val pluggedCode = highlights.pluggedCode
        if (pluggedCode != null) {
            if (pluggedCode == 0) {
                return getString(Res.string.device_info_value_plugged_none)
            }

            if ((pluggedCode and 1) != 0) pluggedSources += getString(Res.string.device_info_value_plugged_ac)
            if ((pluggedCode and 2) != 0) pluggedSources += getString(Res.string.device_info_value_plugged_usb)
            if ((pluggedCode and 4) != 0) pluggedSources += getString(Res.string.device_info_value_plugged_wireless)
            if ((pluggedCode and 8) != 0) pluggedSources += getString(Res.string.device_info_value_plugged_dock)
            if (pluggedSources.isNotEmpty()) return pluggedSources.joinToString(separator = "/")
        }

        return unavailableValue
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

    /**
     * Форматирует значение current_now/current в mA (best-effort).
     */
    private suspend fun formatBatteryCurrentMa(
        currentNowRaw: String?,
        currentRaw: String?,
    ): String? {
        val source = currentNowRaw ?: currentRaw ?: return null
        val numeric = extractNumeric(source) ?: return normalizeForUi(source)

        val milliAmps = if (abs(numeric) >= 10_000.0) {
            numeric / 1000.0
        } else {
            numeric
        }

        return getString(
            Res.string.device_info_value_ma_format,
            milliAmps.roundToInt(),
        )
    }

    /**
     * Форматирует microampere/milliampere значение в mA.
     */
    private suspend fun formatMicroAmpereToMilliAmpere(raw: String?): String? {
        val value = raw?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        val numeric = extractNumeric(value) ?: return normalizeForUi(value)
        val milliAmps = if (abs(numeric) >= 10_000.0) numeric / 1000.0 else numeric
        return getString(
            Res.string.device_info_value_ma_format,
            milliAmps.roundToInt(),
        )
    }

    /**
     * Форматирует microvolt/millivolt значение в mV.
     */
    private suspend fun formatMicroVoltToMilliVolt(raw: String?): String? {
        val value = raw?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        val numeric = extractNumeric(value) ?: return normalizeForUi(value)
        val milliVolts = if (abs(numeric) >= 10_000.0) numeric / 1000.0 else numeric
        return getString(
            Res.string.device_info_value_mv_format,
            milliVolts.roundToInt(),
        )
    }

    /**
     * Форматирует оценку емкости из `dumpsys battery`.
     */
    private suspend fun formatCapacityEstimate(
        capacityRaw: String?,
        estimatedCapacityRaw: String?,
    ): String? {
        val value = estimatedCapacityRaw?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?: capacityRaw?.trim()?.takeIf { it.isNotEmpty() }
            ?: return null

        if (value.contains('%') || value.contains("mah", ignoreCase = true)) {
            return normalizeForUi(value)
        }

        val numeric = extractNumeric(value) ?: return normalizeForUi(value)
        return if (numeric in 0.0..100.0) {
            formatPercent(numeric.roundToInt())
        } else {
            getString(
                Res.string.device_info_value_mah_format,
                numeric.roundToInt(),
            )
        }
    }

    private fun extractNumeric(raw: String): Double? {
        val normalized = raw.replace(",", "")
        return Regex("""-?\d+(?:\.\d+)?""")
            .find(normalized)
            ?.value
            ?.toDoubleOrNull()
    }

    /**
     * Возвращает UI-представление значения с fallback:
     * - `****`/redacted -> Hidden;
     * - пустое/unknown -> Unavailable.
     */
    private fun displayValueOrFallback(
        raw: String?,
        hiddenValue: String,
        unavailableValue: String,
    ): String {
        val value = raw?.trim().orEmpty()
        if (value.isEmpty()) return unavailableValue
        if (isHiddenValue(value)) return hiddenValue
        if (isUnavailableValue(value)) return unavailableValue
        return normalizeForUi(value)
    }

    /**
     * Формирует строку оператора в формате "long / short".
     */
    private fun formatOperator(
        longName: String?,
        shortName: String?,
        hiddenValue: String,
        unavailableValue: String,
    ): String {
        val hasHidden = isHiddenValue(longName) || isHiddenValue(shortName)
        val values = listOfNotNull(
            sanitizeVisibleValue(longName),
            sanitizeVisibleValue(shortName),
        )
            .map(::normalizeForUi)
            .distinct()

        if (values.isNotEmpty()) return values.joinToString(" / ")
        return if (hasHidden) hiddenValue else unavailableValue
    }

    /**
     * Формирует MCC/MNC из mMcc+mMnc или fallback на rRplmn.
     */
    private fun formatMccMnc(
        mcc: String?,
        mnc: String?,
        rplmn: String?,
        hiddenValue: String,
        unavailableValue: String,
    ): String {
        if (isHiddenValue(mcc) || isHiddenValue(mnc) || isHiddenValue(rplmn)) {
            return hiddenValue
        }

        val mccValue = sanitizeVisibleValue(mcc)
        val mncValue = sanitizeVisibleValue(mnc)
        if (!mccValue.isNullOrBlank() && !mncValue.isNullOrBlank()) {
            return "${normalizeForUi(mccValue)}/${normalizeForUi(mncValue)}"
        }

        val rplmnValue = sanitizeVisibleValue(rplmn)
        if (!rplmnValue.isNullOrBlank()) {
            val digits = rplmnValue.filter { it.isDigit() }
            if (digits.length >= 5) {
                return "${digits.take(3)}/${digits.drop(3)}"
            }
            return normalizeForUi(rplmnValue)
        }

        return unavailableValue
    }

    /**
     * Объединяет два альтернативных значения (например mCellBandwidths и mBandwidth).
     */
    private fun formatCombinedValues(
        first: String?,
        second: String?,
        hiddenValue: String,
        unavailableValue: String,
    ): String {
        val hasHidden = isHiddenValue(first) || isHiddenValue(second)
        val values = listOfNotNull(sanitizeVisibleValue(first), sanitizeVisibleValue(second))
            .map(::normalizeForUi)
            .distinct()

        if (values.isNotEmpty()) return values.joinToString(" / ")
        return if (hasHidden) hiddenValue else unavailableValue
    }

    /**
     * Формирует компактную строку Cell Identity.
     */
    private fun formatCellIdentity(
        ci: String?,
        pci: String?,
        tac: String?,
        earfcn: String?,
        hiddenValue: String,
        unavailableValue: String,
    ): String {
        return listOf(
            "CI=${displayValueOrFallback(ci, hiddenValue, unavailableValue)}",
            "PCI=${displayValueOrFallback(pci, hiddenValue, unavailableValue)}",
            "TAC=${displayValueOrFallback(tac, hiddenValue, unavailableValue)}",
            "EARFCN=${displayValueOrFallback(earfcn, hiddenValue, unavailableValue)}",
        ).joinToString(", ")
    }

    private fun isHiddenValue(raw: String?): Boolean {
        return raw?.contains('*') == true || raw?.contains("redacted", ignoreCase = true) == true
    }

    private fun isUnavailableValue(raw: String?): Boolean {
        val normalized = raw?.trim()?.lowercase().orEmpty()
        return normalized.isEmpty() ||
            normalized == "null" ||
            normalized == "unknown" ||
            normalized == "unavailable" ||
            normalized == "n/a" ||
            normalized == "none"
    }

    private fun sanitizeVisibleValue(raw: String?): String? {
        val value = raw?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        if (isHiddenValue(value) || isUnavailableValue(value)) return null
        return value
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

    private companion object {
        private val UID_TOKEN_REGEX = Regex(
            pattern = """^u(\d+)([asi])(\d+)$""",
            option = RegexOption.IGNORE_CASE,
        )

        private val RAM_VALUE_REGEX = Regex(
            pattern = """([0-9][0-9,]*(?:\.[0-9]+)?)\s*([KMGT]i?B?|[KMGT])?""",
            option = RegexOption.IGNORE_CASE,
        )
    }
}

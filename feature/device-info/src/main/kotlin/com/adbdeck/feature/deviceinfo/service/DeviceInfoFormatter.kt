package com.adbdeck.feature.deviceinfo.service

import adbdeck.feature.device_info.generated.resources.Res
import adbdeck.feature.device_info.generated.resources.*
import com.adbdeck.feature.deviceinfo.parser.BatteryHighlights
import com.adbdeck.feature.deviceinfo.parser.normalizeForUi
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt
import org.jetbrains.compose.resources.getString

/**
 * Форматирование Device Info значений без прямой зависимости от ADB-слоя.
 * Все строковые ресурсы загружаются один раз и переиспользуются.
 */
internal class DeviceInfoFormatter private constructor(
    private val strings: Strings,
) {

    fun resolveLocale(props: Map<String, String>): String? {
        props["persist.sys.locale"]?.trim()?.takeIf { it.isNotEmpty() }?.let { return it }

        val lang = props["persist.sys.language"]?.trim().orEmpty()
        val country = props["persist.sys.country"]?.trim().orEmpty()
        if (lang.isNotEmpty()) {
            return if (country.isNotEmpty()) "$lang-$country" else lang
        }

        return props["ro.product.locale"]?.trim()?.takeIf { it.isNotEmpty() }
    }

    fun yesNo(value: Boolean): String = if (value) strings.yes else strings.no

    fun asYesNo(raw: String): String {
        val normalized = raw.trim().lowercase()
        return when (normalized) {
            "1", "true", "yes", "y" -> strings.yes
            "0", "false", "no", "n" -> strings.no
            else -> strings.unknown
        }
    }

    fun formatBatteryStatusWithCode(
        highlights: BatteryHighlights,
        unavailableValue: String,
    ): String {
        val statusCode = highlights.statusCode ?: return unavailableValue
        val statusText = batteryStatusText(statusCode)
        return strings.withCodeFormat.localizedFormat(statusText, statusCode)
    }

    fun formatBatteryHealthWithCode(
        highlights: BatteryHighlights,
        unavailableValue: String,
    ): String {
        val healthCode = highlights.healthCode ?: return unavailableValue
        val healthText = when (healthCode) {
            2 -> strings.batteryHealthGood
            3 -> strings.batteryHealthOverheat
            4 -> strings.batteryHealthDead
            5 -> strings.batteryHealthOverVoltage
            6 -> strings.batteryHealthUnspecifiedFailure
            7 -> strings.batteryHealthCold
            else -> strings.batteryHealthUnknown
        }
        return strings.withCodeFormat.localizedFormat(healthText, healthCode)
    }

    fun batteryPluggedText(
        highlights: BatteryHighlights,
        unavailableValue: String,
    ): String {
        val pluggedSources = mutableListOf<String>()
        if (highlights.acPowered == true) pluggedSources += strings.pluggedAc
        if (highlights.usbPowered == true) pluggedSources += strings.pluggedUsb
        if (highlights.wirelessPowered == true) pluggedSources += strings.pluggedWireless

        if (pluggedSources.isNotEmpty()) return pluggedSources.joinToString(separator = "/")

        val pluggedCode = highlights.pluggedCode
        if (pluggedCode != null) {
            if (pluggedCode == 0) {
                return strings.pluggedNone
            }

            if ((pluggedCode and 1) != 0) pluggedSources += strings.pluggedAc
            if ((pluggedCode and 2) != 0) pluggedSources += strings.pluggedUsb
            if ((pluggedCode and 4) != 0) pluggedSources += strings.pluggedWireless
            if ((pluggedCode and 8) != 0) pluggedSources += strings.pluggedDock
            if (pluggedSources.isNotEmpty()) return pluggedSources.joinToString(separator = "/")
        }

        return unavailableValue
    }

    fun formatRamMegabytes(raw: String?): String? {
        val value = raw?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        val megabytes = parseMegabytes(value) ?: return normalizeForUi(value)
        return strings.ramMbFormat.localizedFormat(megabytes.roundToInt())
    }

    fun formatPercent(value: Int): String = strings.percentFormat.localizedFormat(value)

    fun formatCelsius(valueCelsius: Float): String = strings.celsiusFormat.localizedFormat(valueCelsius)

    fun formatBatteryCurrentMa(
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

        return strings.maFormat.localizedFormat(milliAmps.roundToInt())
    }

    fun formatMicroAmpereToMilliAmpere(raw: String?): String? {
        val value = raw?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        val numeric = extractNumeric(value) ?: return normalizeForUi(value)
        val milliAmps = if (abs(numeric) >= 10_000.0) numeric / 1000.0 else numeric
        return strings.maFormat.localizedFormat(milliAmps.roundToInt())
    }

    fun formatMicroVoltToMilliVolt(raw: String?): String? {
        val value = raw?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        val numeric = extractNumeric(value) ?: return normalizeForUi(value)
        val milliVolts = if (abs(numeric) >= 10_000.0) numeric / 1000.0 else numeric
        return strings.mvFormat.localizedFormat(milliVolts.roundToInt())
    }

    fun formatCapacityEstimate(
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
            strings.mahFormat.localizedFormat(numeric.roundToInt())
        }
    }

    fun displayValueOrFallback(
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

    fun formatOperator(
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

    fun formatMccMnc(
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

    fun formatCombinedValues(
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

    fun formatCellIdentity(
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

    private fun batteryStatusText(statusCode: Int): String {
        return when (statusCode) {
            2 -> strings.batteryStatusCharging
            3 -> strings.batteryStatusDischarging
            4 -> strings.batteryStatusNotCharging
            5 -> strings.batteryStatusFull
            else -> strings.batteryStatusUnknown
        }
    }

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

    private fun extractNumeric(raw: String): Double? {
        val normalized = raw.replace(",", "")
        return NUMERIC_VALUE_REGEX
            .find(normalized)
            ?.value
            ?.toDoubleOrNull()
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

    private fun String.localizedFormat(vararg args: Any): String {
        return String.format(Locale.getDefault(), this, *args)
    }

    private data class Strings(
        val yes: String,
        val no: String,
        val unknown: String,
        val percentFormat: String,
        val celsiusFormat: String,
        val mvFormat: String,
        val maFormat: String,
        val mahFormat: String,
        val withCodeFormat: String,
        val ramMbFormat: String,
        val pluggedAc: String,
        val pluggedUsb: String,
        val pluggedWireless: String,
        val pluggedDock: String,
        val pluggedNone: String,
        val batteryStatusUnknown: String,
        val batteryStatusCharging: String,
        val batteryStatusDischarging: String,
        val batteryStatusNotCharging: String,
        val batteryStatusFull: String,
        val batteryHealthUnknown: String,
        val batteryHealthGood: String,
        val batteryHealthOverheat: String,
        val batteryHealthDead: String,
        val batteryHealthOverVoltage: String,
        val batteryHealthUnspecifiedFailure: String,
        val batteryHealthCold: String,
    )

    companion object {
        private val NUMERIC_VALUE_REGEX = Regex("""-?\d+(?:\.\d+)?""")

        private val RAM_VALUE_REGEX = Regex(
            pattern = """([0-9][0-9,]*(?:\.[0-9]+)?)\s*([KMGT]i?B?|[KMGT])?""",
            option = RegexOption.IGNORE_CASE,
        )

        suspend fun fromResources(): DeviceInfoFormatter {
            val strings = Strings(
                yes = getString(Res.string.device_info_value_yes),
                no = getString(Res.string.device_info_value_no),
                unknown = getString(Res.string.device_info_value_unknown),
                percentFormat = getString(Res.string.device_info_value_percent_format),
                celsiusFormat = getString(Res.string.device_info_value_celsius_format),
                mvFormat = getString(Res.string.device_info_value_mv_format),
                maFormat = getString(Res.string.device_info_value_ma_format),
                mahFormat = getString(Res.string.device_info_value_mah_format),
                withCodeFormat = getString(Res.string.device_info_value_with_code_format),
                ramMbFormat = getString(Res.string.device_info_value_ram_mb_format),
                pluggedAc = getString(Res.string.device_info_value_plugged_ac),
                pluggedUsb = getString(Res.string.device_info_value_plugged_usb),
                pluggedWireless = getString(Res.string.device_info_value_plugged_wireless),
                pluggedDock = getString(Res.string.device_info_value_plugged_dock),
                pluggedNone = getString(Res.string.device_info_value_plugged_none),
                batteryStatusUnknown = getString(Res.string.device_info_battery_status_unknown),
                batteryStatusCharging = getString(Res.string.device_info_battery_status_charging),
                batteryStatusDischarging = getString(Res.string.device_info_battery_status_discharging),
                batteryStatusNotCharging = getString(Res.string.device_info_battery_status_not_charging),
                batteryStatusFull = getString(Res.string.device_info_battery_status_full),
                batteryHealthUnknown = getString(Res.string.device_info_battery_health_unknown),
                batteryHealthGood = getString(Res.string.device_info_battery_health_good),
                batteryHealthOverheat = getString(Res.string.device_info_battery_health_overheat),
                batteryHealthDead = getString(Res.string.device_info_battery_health_dead),
                batteryHealthOverVoltage = getString(Res.string.device_info_battery_health_over_voltage),
                batteryHealthUnspecifiedFailure = getString(Res.string.device_info_battery_health_unspecified_failure),
                batteryHealthCold = getString(Res.string.device_info_battery_health_cold),
            )
            return DeviceInfoFormatter(strings)
        }
    }
}

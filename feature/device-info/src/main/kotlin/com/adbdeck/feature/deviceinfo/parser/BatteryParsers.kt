package com.adbdeck.feature.deviceinfo.parser

/**
 * Выжимка из `dumpsys battery`.
 */
internal data class BatteryHighlights(
    val level: Int?,
    val statusCode: Int?,
    val pluggedCode: Int?,
    val healthCode: Int?,
    val temperatureTenthsC: Int?,
    val voltageMv: Int?,
    val technology: String?,
    val acPowered: Boolean?,
    val usbPowered: Boolean?,
    val wirelessPowered: Boolean?,
    val present: Boolean?,
    val currentNowRaw: String?,
    val currentRaw: String?,
    val chargeCounterUah: Long?,
    val capacityRaw: String?,
    val estimatedCapacityRaw: String?,
    val maxChargingCurrentRaw: String?,
    val maxChargingVoltageRaw: String?,
)

/**
 * Разбирает `dumpsys battery` в структурированную модель.
 */
internal fun parseBatteryHighlights(output: String): BatteryHighlights {
    val values = parseColonKeyValueLines(output)
    val valuesLowercase = values.entries.associate { (key, value) ->
        key.trim().lowercase() to value
    }

    fun rawValue(vararg keys: String): String? {
        return keys.firstNotNullOfOrNull { key ->
            valuesLowercase[key.trim().lowercase()]
                ?.trim()
                ?.takeIf { it.isNotEmpty() }
        }
    }

    fun parseBool(raw: String?): Boolean? = raw?.let { value ->
        when (value.lowercase()) {
            "true", "1" -> true
            "false", "0" -> false
            else -> null
        }
    }

    return BatteryHighlights(
        level = parseLongFlexible(rawValue("level"))?.toInt(),
        statusCode = parseLongFlexible(rawValue("status"))?.toInt(),
        pluggedCode = parseLongFlexible(rawValue("plugged"))?.toInt(),
        healthCode = parseLongFlexible(rawValue("health"))?.toInt(),
        temperatureTenthsC = parseLongFlexible(rawValue("temperature"))?.toInt(),
        voltageMv = parseLongFlexible(rawValue("voltage"))?.toInt(),
        technology = rawValue("technology"),
        acPowered = parseBool(rawValue("AC powered", "ac powered")),
        usbPowered = parseBool(rawValue("USB powered", "usb powered")),
        wirelessPowered = parseBool(rawValue("Wireless powered", "wireless powered")),
        present = parseBool(rawValue("present")),
        currentNowRaw = rawValue("current now", "current_now", "current_now_ua"),
        currentRaw = rawValue("current"),
        chargeCounterUah = parseLongFlexible(rawValue("charge counter", "charge_counter")),
        capacityRaw = rawValue("capacity", "battery capacity"),
        estimatedCapacityRaw = rawValue("estimated capacity", "capacity estimate"),
        maxChargingCurrentRaw = rawValue("max charging current", "max_charging_current"),
        maxChargingVoltageRaw = rawValue("max charging voltage", "max_charging_voltage"),
    )
}

private fun parseLongFlexible(raw: String?): Long? {
    val value = raw?.trim()?.takeIf { it.isNotEmpty() } ?: return null
    val digits = Regex("""-?\d+""")
        .find(value.replace(",", ""))
        ?.value
        ?: return null
    return digits.toLongOrNull()
}

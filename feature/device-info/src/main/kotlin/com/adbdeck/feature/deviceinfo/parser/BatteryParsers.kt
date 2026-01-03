package com.adbdeck.feature.deviceinfo.parser

/**
 * Выжимка из `dumpsys battery`.
 */
internal data class BatteryHighlights(
    val level: Int?,
    val statusCode: Int?,
    val healthCode: Int?,
    val temperatureTenthsC: Int?,
    val voltageMv: Int?,
    val technology: String?,
    val acPowered: Boolean?,
    val usbPowered: Boolean?,
    val wirelessPowered: Boolean?,
)

/**
 * Разбирает `dumpsys battery` в структурированную модель.
 */
internal fun parseBatteryHighlights(output: String): BatteryHighlights {
    val values = parseColonKeyValueLines(output)

    fun parseBool(key: String): Boolean? = values[key]?.let { value ->
        when (value.lowercase()) {
            "true", "1" -> true
            "false", "0" -> false
            else -> null
        }
    }

    return BatteryHighlights(
        level = values["level"]?.toIntOrNull(),
        statusCode = values["status"]?.toIntOrNull(),
        healthCode = values["health"]?.toIntOrNull(),
        temperatureTenthsC = values["temperature"]?.toIntOrNull(),
        voltageMv = values["voltage"]?.toIntOrNull(),
        technology = values["technology"]?.takeIf { it.isNotBlank() },
        acPowered = parseBool("AC powered"),
        usbPowered = parseBool("USB powered"),
        wirelessPowered = parseBool("Wireless powered"),
    )
}

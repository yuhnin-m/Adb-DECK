package com.adbdeck.feature.deviceinfo.parser

/**
 * Выжимка summary-данных из `dumpsys batterystats`.
 */
internal data class BatteryStatsHighlights(
    val timeOnBattery: String?,
    val screenOnTime: String?,
    val wifiOnTime: String?,
    val mobileRadioActiveTime: String?,
    val wakelocksSummary: String?,
    val topAppsByPower: List<BatteryStatsTopAppPower>,
)

/**
 * Строка топ-потребителя из секции `Estimated power use (mAh)`.
 *
 * @param uidToken UID из batterystats (например `u0a301` или `1000`).
 * @param powerMahRaw Сырые mAh из дампа.
 */
internal data class BatteryStatsTopAppPower(
    val uidToken: String,
    val powerMahRaw: String,
)

/**
 * Парсит highlights из `dumpsys batterystats`.
 *
 * Важно: функция не возвращает "простыню" полного дампа,
 * а извлекает только компактные сводные поля.
 */
internal fun parseBatteryStatsHighlights(output: String): BatteryStatsHighlights {
    val lines = output.lineSequence()
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .toList()

    val timeOnBattery = findValueAfterColon(lines) { line ->
        line.startsWith("Time on battery:", ignoreCase = true)
    }

    val screenOnTime = findValueAfterColon(lines) { line ->
        line.startsWith("Screen on:", ignoreCase = true) ||
            line.startsWith("Estimated screen on time:", ignoreCase = true)
    }

    val wifiOnTime = findValueAfterColon(lines) { line ->
        line.startsWith("WiFi on:", ignoreCase = true) ||
            line.startsWith("Wifi on:", ignoreCase = true) ||
            line.startsWith("Wi-Fi on:", ignoreCase = true) ||
            line.startsWith("Wifi kernel active time:", ignoreCase = true)
    }

    val mobileRadioActiveTime = findValueAfterColon(lines) { line ->
        line.startsWith("Mobile radio active time:", ignoreCase = true) ||
            line.startsWith("Mobile radio active:", ignoreCase = true)
    }

    val wakelocksSummary = buildWakelocksSummary(lines)
    val topAppsByPower = parseTopAppsByPower(lines)

    return BatteryStatsHighlights(
        timeOnBattery = timeOnBattery,
        screenOnTime = screenOnTime,
        wifiOnTime = wifiOnTime,
        mobileRadioActiveTime = mobileRadioActiveTime,
        wakelocksSummary = wakelocksSummary,
        topAppsByPower = topAppsByPower,
    )
}

private fun findValueAfterColon(
    lines: List<String>,
    predicate: (String) -> Boolean,
): String? {
    val line = lines.firstOrNull(predicate) ?: return null
    return line.substringAfter(':', missingDelimiterValue = "")
        .trim()
        .takeIf { it.isNotEmpty() }
}

private fun buildWakelocksSummary(lines: List<String>): String? {
    val full = findValueAfterColon(lines) { line ->
        line.startsWith("Total full wakelock time:", ignoreCase = true)
    }
    val partial = findValueAfterColon(lines) { line ->
        line.startsWith("Total partial wakelock time:", ignoreCase = true)
    }
    val wifiMulticast = findValueAfterColon(lines) { line ->
        line.startsWith("Total WiFi Multicast wakelock time:", ignoreCase = true)
    }

    val parts = buildList {
        full?.let { add("full=$it") }
        partial?.let { add("partial=$it") }
        wifiMulticast?.let { add("wifi_multicast=$it") }
    }

    return parts.takeIf { it.isNotEmpty() }?.joinToString(separator = " | ")
}

private fun parseTopAppsByPower(lines: List<String>): List<BatteryStatsTopAppPower> {
    val startIndex = lines.indexOfFirst { it.contains("Estimated power use (mAh):", ignoreCase = true) }
    if (startIndex < 0) return emptyList()

    return lines.asSequence()
        .drop(startIndex + 1)
        .mapNotNull(::parseUidPowerLine)
        .filterNot { row -> row.uidToken == "0" || row.uidToken.startsWith("-") }
        .take(5)
        .map { row ->
            BatteryStatsTopAppPower(
                uidToken = row.uidToken,
                powerMahRaw = row.powerMahRaw,
            )
        }
        .toList()
}

private fun parseUidPowerLine(line: String): UidPowerRow? {
    val match = UID_POWER_LINE_REGEX.matchEntire(line) ?: return null
    val uid = match.groupValues.getOrElse(1) { "" }.trim()
    val value = match.groupValues.getOrElse(2) { "" }.trim()
    if (uid.isEmpty() || value.isEmpty()) return null
    return UidPowerRow(uidToken = uid, powerMahRaw = value)
}

private data class UidPowerRow(
    val uidToken: String,
    val powerMahRaw: String,
)

private val UID_POWER_LINE_REGEX = Regex(
    pattern = """^UID\s+([^:]+):\s*([0-9]+(?:\.[0-9]+)?)\b.*$""",
    option = RegexOption.IGNORE_CASE,
)

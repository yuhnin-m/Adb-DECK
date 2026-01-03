package com.adbdeck.feature.deviceinfo.parser

/**
 * Выжимка сетевых параметров устройства.
 */
internal data class NetworkHighlights(
    val ipAddresses: List<String>,
    val defaultRoute: String?,
    val wifiState: String?,
    val wifiSsid: String?,
    val wifiBssid: String?,
    val wifiRssi: String?,
    val connectivity: String?,
)

private val IPV4_CIDR_REGEX = Regex("""\binet\s+([0-9]{1,3}(?:\.[0-9]{1,3}){3}/\d+)""")

/**
 * Парсит `ip addr` / `ip route` / `dumpsys wifi` / `dumpsys connectivity`.
 */
internal fun parseNetworkHighlights(
    ipAddrOutput: String,
    ipRouteOutput: String,
    dumpsysWifiOutput: String,
    dumpsysConnectivityOutput: String,
): NetworkHighlights {
    val addresses = IPV4_CIDR_REGEX.findAll(ipAddrOutput)
        .map { it.groupValues[1] }
        .filterNot { it.startsWith("127.") || it.startsWith("169.254.") }
        .distinct()
        .toList()

    val defaultRoute = findFirstMatchingLine(ipRouteOutput) { line ->
        line.startsWith("default ")
    }?.let(::normalizeForUi)

    val wifiState = findFirstMatchingLine(dumpsysWifiOutput) { line ->
        line.contains("Wi-Fi is", ignoreCase = true) ||
            line.contains("wifi state", ignoreCase = true)
    }?.let(::normalizeForUi)

    val wifiSsid = dumpsysWifiOutput.lineSequence()
        .map { it.trim() }
        .firstNotNullOfOrNull(::extractWifiSsidValue)
        ?.let(::normalizeForUi)

    val wifiBssid = dumpsysWifiOutput.lineSequence()
        .map { it.trim() }
        .firstNotNullOfOrNull(::extractWifiBssidValue)
        ?.let(::normalizeForUi)

    val wifiRssi = dumpsysWifiOutput.lineSequence()
        .map { it.trim() }
        .firstNotNullOfOrNull { line ->
            extractAfterPrefix(line, "RSSI:")
                ?: Regex("""RSSI\s*[:=]\s*(-?\d+)""", RegexOption.IGNORE_CASE)
                    .find(line)
                    ?.groupValues
                    ?.getOrNull(1)
                    ?.let { "$it dBm" }
        }
        ?.let(::normalizeForUi)

    val connectivity = findFirstMatchingLine(dumpsysConnectivityOutput) { line ->
        line.contains("Active default network", ignoreCase = true) ||
            line.contains("Default network", ignoreCase = true) ||
            line.contains("mActiveDefaultNetwork", ignoreCase = true)
    }?.let(::normalizeForUi)

    return NetworkHighlights(
        ipAddresses = addresses,
        defaultRoute = defaultRoute,
        wifiState = wifiState,
        wifiSsid = wifiSsid,
        wifiBssid = wifiBssid,
        wifiRssi = wifiRssi,
        connectivity = connectivity,
    )
}

private fun extractWifiSsidValue(line: String): String? {
    val match = Regex("""\bSSID\s*[:=]\s*(".*?"|[^,]+)""", RegexOption.IGNORE_CASE)
        .find(line)
        ?.groupValues
        ?.getOrNull(1)
        ?: return null

    return match
        .removeSurrounding("\"")
        .trim()
        .takeIf { it.isNotEmpty() }
}

private fun extractWifiBssidValue(line: String): String? {
    val match = Regex(
        pattern = """\bBSSID\s*[:=]\s*([0-9A-Fa-f]{2}(?::[0-9A-Fa-f]{2}){5}|[^,]+)""",
        option = RegexOption.IGNORE_CASE,
    ).find(line)?.groupValues?.getOrNull(1) ?: return null

    return match.trim().takeIf { it.isNotEmpty() }
}

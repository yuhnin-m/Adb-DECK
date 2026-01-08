package com.adbdeck.feature.deviceinfo.parser

import kotlin.math.min

/**
 * Выжимка параметров IMS / RCS из `dumpsys phone`.
 */
internal data class ImsRcsHighlights(
    val defaultDataSubId: String?,
    val imsServices: List<String>,
    val mmTelState: String?,
    val rcsState: String?,
    val capabilities: List<String>,
    val slotToSubIdMap: String?,
)

/**
 * Парсит ключевые IMS/RCS highlights из `dumpsys phone`.
 *
 * Используется best-effort стратегия:
 * - ищем основные ключи через key=value/key: value;
 * - блок ImsResolver разбираем отдельно и достаем component names;
 * - при нестандартной структуре возвращаем частичный результат.
 */
internal fun parseImsRcsHighlights(output: String): ImsRcsHighlights {
    val normalized = output.replace('\u0000', ' ')
    val lines = normalized.lineSequence().toList()

    return ImsRcsHighlights(
        defaultDataSubId = extractFieldValue(normalized, "mDefaultDataSubId"),
        imsServices = parseImsResolverServices(lines),
        mmTelState = parseFeatureState(lines, "MmTelFeatureListeners"),
        rcsState = parseFeatureState(lines, "RcsFeatureListeners"),
        capabilities = parseCapabilities(normalized),
        slotToSubIdMap = extractFieldValue(normalized, "slotToSubIdMap"),
    )
}

private fun parseImsResolverServices(lines: List<String>): List<String> {
    if (lines.isEmpty()) return emptyList()

    val startIndex = lines.indexOfFirst { it.contains("ImsResolver", ignoreCase = true) }
    val scopedLines = if (startIndex >= 0) {
        val endExclusive = min(lines.size, startIndex + IMS_RESOLVER_SCAN_LINES)
        lines.subList(startIndex, endExclusive)
    } else {
        lines
    }

    return scopedLines.asSequence()
        .filter { line ->
            line.contains("ims", ignoreCase = true) || line.contains("rcs", ignoreCase = true)
        }
        .flatMap { line ->
            COMPONENT_NAME_REGEX.findAll(line).map { match -> match.value.trim().trimEnd(',') }
        }
        .filter { component ->
            component.contains("ims", ignoreCase = true) || component.contains("rcs", ignoreCase = true)
        }
        .distinct()
        .take(MAX_COMPONENTS)
        .toList()
}

private fun parseFeatureState(
    lines: List<String>,
    marker: String,
): String? {
    val index = lines.indexOfFirst { it.contains(marker, ignoreCase = true) }
    if (index < 0) return null

    val endExclusive = min(lines.size, index + FEATURE_STATE_SCAN_LINES)
    val window = lines.subList(index, endExclusive).joinToString(" ")

    val state = when {
        window.contains("UNAVAILABLE", ignoreCase = true) -> "UNAVAILABLE"
        window.contains("READY", ignoreCase = true) -> "READY"
        else -> null
    }

    val reason = REASON_REGEX.find(window)
        ?.groupValues
        ?.getOrNull(1)
        ?.trim()
        ?.takeIf { it.isNotEmpty() }

    return when {
        state != null && reason != null -> "$state ($reason)"
        state != null -> state
        else -> normalizeForUi(lines[index], maxLength = 220)
    }
}

private fun parseCapabilities(output: String): List<String> {
    val capabilities = CAPABILITIES_REGEX.findAll(output)
        .flatMap { match ->
            match.groupValues
                .getOrElse(1) { "" }
                .split(',')
                .asSequence()
                .map { it.trim() }
                .filter { it.isNotEmpty() }
        }
        .map { token ->
            token
                .replace(Regex("\\s+"), " ")
                .trim()
        }
        .distinct()
        .toList()

    if (capabilities.isNotEmpty()) return capabilities

    return output.lineSequence()
        .map { it.trim() }
        .filter { it.contains("capabilities", ignoreCase = true) }
        .map { normalizeForUi(it, maxLength = 220) }
        .distinct()
        .take(3)
        .toList()
}

private fun extractFieldValue(
    output: String,
    vararg keys: String,
): String? {
    keys.forEach { key ->
        val match = Regex(
            pattern = """(?:^|[^A-Za-z0-9_.-])${Regex.escape(key)}\s*[=:]\s*([^\n\r]+)""",
            option = RegexOption.MULTILINE,
        ).find(output) ?: return@forEach

        val cleaned = trimToFieldBoundary(match.groupValues[1])
        if (cleaned.isNotEmpty()) return cleaned
    }

    return null
}

private fun trimToFieldBoundary(raw: String): String {
    val start = raw.trim()
    val nextKey = NEXT_KEY_PATTERN.find(start)
    val truncated = if (nextKey != null) {
        start.substring(0, nextKey.range.first)
    } else {
        start
    }

    return truncated.trim().trimEnd(',', ';')
}

private const val IMS_RESOLVER_SCAN_LINES = 280
private const val FEATURE_STATE_SCAN_LINES = 8
private const val MAX_COMPONENTS = 6

private val NEXT_KEY_PATTERN = Regex("""(?:(?:,\s*)|\s+)[A-Za-z][A-Za-z0-9_.-]*\s*[=:]""")
private val REASON_REGEX = Regex("""reason(?:Code)?\s*[=:]\s*([^\],}]+)""", RegexOption.IGNORE_CASE)
private val CAPABILITIES_REGEX = Regex("""capabilities\s*=\s*\{([^}]*)}""", RegexOption.IGNORE_CASE)
private val COMPONENT_NAME_REGEX = Regex("""[A-Za-z_][A-Za-z0-9_]*(?:\.[A-Za-z_][A-Za-z0-9_]*){1,}(?:/[A-Za-z0-9_.$]+)?""")

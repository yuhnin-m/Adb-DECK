package com.adbdeck.feature.deviceinfo.parser

/**
 * Разбирает блок строк формата `key: value` в карту.
 */
internal fun parseColonKeyValueLines(output: String): Map<String, String> {
    val map = LinkedHashMap<String, String>()
    output.lineSequence().forEach { line ->
        val idx = line.indexOf(':')
        if (idx <= 0) return@forEach

        val key = line.substring(0, idx).trim()
        val value = line.substring(idx + 1).trim()
        if (key.isNotEmpty() && value.isNotEmpty()) {
            map[key] = value
        }
    }
    return map
}

/**
 * Возвращает первую непустую строку, подходящую под [predicate].
 */
internal fun findFirstMatchingLine(
    output: String,
    predicate: (String) -> Boolean,
): String? {
    return output.lineSequence()
        .map { it.trim() }
        .firstOrNull { it.isNotEmpty() && predicate(it) }
}

/**
 * Нормализует пробелы и опционально обрезает значение до заданной длины.
 */
internal fun normalizeForUi(
    raw: String,
    maxLength: Int? = null,
): String {
    val normalized = raw
        .replace('\u0000', ' ')
        .replace(Regex("\\s+"), " ")
        .trim()

    val limit = maxLength ?: return normalized
    if (limit <= 0 || normalized.length <= limit) {
        return normalized
    }

    return normalized.take(limit - 1) + "…"
}

/**
 * Возвращает строку после `prefix` или null, если префикс не найден.
 */
internal fun extractAfterPrefix(
    line: String,
    prefix: String,
): String? {
    if (!line.startsWith(prefix, ignoreCase = true)) return null
    return line.substringAfter(prefix).trim().takeIf { it.isNotEmpty() }
}

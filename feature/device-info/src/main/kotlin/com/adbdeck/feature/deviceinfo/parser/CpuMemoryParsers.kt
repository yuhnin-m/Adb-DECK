package com.adbdeck.feature.deviceinfo.parser

/**
 * Выжимка из `/proc/cpuinfo`.
 */
internal data class CpuHighlights(
    val model: String?,
    val cores: Int?,
    val features: String?,
)

/**
 * Выжимка из `dumpsys meminfo`.
 */
internal data class RamHighlights(
    val total: String?,
    val used: String?,
    val free: String?,
    val lost: String?,
)

/**
 * Парсит `/proc/cpuinfo` в компактный набор полей.
 */
internal fun parseCpuHighlights(output: String): CpuHighlights {
    val lines = output.lineSequence().map { it.trim() }.toList()

    val model = lines.firstNotNullOfOrNull { line ->
        extractAfterPrefix(line, "Hardware:")
            ?: extractAfterPrefix(line, "model name:")
            ?: extractAfterPrefix(line, "Processor:")
            ?: extractAfterPrefix(line, "cpu model:")
    }?.let(::normalizeForUi)

    val features = lines.firstNotNullOfOrNull { line ->
        extractAfterPrefix(line, "Features:")
            ?: extractAfterPrefix(line, "flags:")
    }?.let(::normalizeForUi)

    val cores = lines.count { line ->
        line.startsWith("processor", ignoreCase = true) && line.contains(':')
    }.takeIf { it > 0 }

    return CpuHighlights(
        model = model,
        cores = cores,
        features = features,
    )
}

/**
 * Парсит `dumpsys meminfo` в summary-значения RAM.
 */
internal fun parseRamHighlights(output: String): RamHighlights {
    fun findMemValue(prefix: String): String? {
        return output.lineSequence()
            .map { it.trim() }
            .firstNotNullOfOrNull { line ->
                extractAfterPrefix(line, prefix)
                    ?.substringBefore('(')
                    ?.trim()
                    ?.takeIf { it.isNotEmpty() }
            }
    }

    return RamHighlights(
        total = findMemValue("Total RAM:"),
        used = findMemValue("Used RAM:"),
        free = findMemValue("Free RAM:"),
        lost = findMemValue("Lost RAM:"),
    )
}

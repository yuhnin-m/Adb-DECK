package com.adbdeck.feature.deviceinfo.parser

/**
 * Сводка load average из `/proc/loadavg`.
 */
internal data class CpuLoadAverage(
    val oneMinute: String,
    val fiveMinutes: String,
    val fifteenMinutes: String,
)

/**
 * Парсит `/proc/loadavg` и возвращает 1m/5m/15m значения.
 */
internal fun parseCpuLoadAverage(output: String): CpuLoadAverage? {
    val line = output.lineSequence()
        .map { it.trim() }
        .firstOrNull { it.isNotEmpty() }
        ?: return null

    val parts = line.split(Regex("\\s+"))
    if (parts.size < 3) return null

    return CpuLoadAverage(
        oneMinute = parts[0],
        fiveMinutes = parts[1],
        fifteenMinutes = parts[2],
    )
}

/**
 * Разбирает диапазоны CPU вида `0-7` / `0-3,8-11` / `0 1 2`.
 *
 * Возвращает отсортированный список индексов CPU.
 */
internal fun parseCpuIndices(raw: String?): List<Int> {
    if (raw.isNullOrBlank()) return emptyList()

    val result = linkedSetOf<Int>()
    raw.split(',', ' ', '\t', '\n')
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .forEach { token ->
            if ('-' in token) {
                val bounds = token.split('-', limit = 2)
                val start = bounds.getOrNull(0)?.toIntOrNull() ?: return@forEach
                val end = bounds.getOrNull(1)?.toIntOrNull() ?: return@forEach
                if (end < start) return@forEach
                for (index in start..end) {
                    result += index
                }
            } else {
                token.toIntOrNull()?.let(result::add)
            }
        }

    return result.toList().sorted()
}

/**
 * Разбирает вывод `ls /sys/devices/system/cpu/cpufreq/` и возвращает policy-имена.
 */
internal fun parseCpuPolicyNames(output: String): List<String> {
    return output.lineSequence()
        .map { it.trim() }
        .filter { it.matches(POLICY_NAME_REGEX) }
        .distinct()
        .sortedBy { parsePolicyIndex(it) ?: Int.MAX_VALUE }
        .toList()
}

/**
 * Разбирает значение online-файла CPU (`0` / `1` / `true` / `false`).
 */
internal fun parseCpuOnlineState(raw: String?): Boolean? {
    val value = raw?.trim()?.lowercase() ?: return null
    return when (value) {
        "1", "true", "y", "yes" -> true
        "0", "false", "n", "no" -> false
        else -> null
    }
}

/**
 * Извлекает индекс policy (`policy0` -> `0`).
 */
internal fun parsePolicyIndex(policyName: String): Int? {
    return policyName.removePrefix("policy").toIntOrNull()
}

private val POLICY_NAME_REGEX = Regex("""^policy\d+$""")


package com.adbdeck.feature.deviceinfo.service

import com.adbdeck.feature.deviceinfo.parser.normalizeForUi
import com.adbdeck.feature.deviceinfo.parser.parseCpuIndices
import com.adbdeck.feature.deviceinfo.parser.parseCpuOnlineState
import java.util.Locale

internal data class CpuFrequencySummary(
    val governor: String?,
    val minMax: String?,
    val current: String?,
    val onlineCores: String?,
)

/**
 * Загрузка и форматирование cpufreq данных (policy/per-core fallback).
 */
internal class CpuFrequencyLoader(
    private val runShell: suspend (List<String>) -> String?,
) {

    private data class CpuPolicyFrequency(
        val policyName: String,
        val governor: String?,
        val minKHz: Long?,
        val maxKHz: Long?,
        val currentKHz: Long?,
        val relatedCpus: List<Int>,
    )

    private data class CpuCoreFrequency(
        val cpuIndex: Int,
        val minKHz: Long?,
        val maxKHz: Long?,
        val currentKHz: Long?,
    )

    suspend fun loadSummary(
        policyNames: List<String>,
        presentCpuIndices: List<Int>,
        coresCountFallback: Int?,
    ): CpuFrequencySummary {
        if (policyNames.isNotEmpty()) {
            val policies = policyNames.map { policyName ->
                val basePath = "/sys/devices/system/cpu/cpufreq/$policyName"
                CpuPolicyFrequency(
                    policyName = policyName,
                    governor = runShell(listOf("cat", "$basePath/scaling_governor")),
                    minKHz = parseLongFlexible(runShell(listOf("cat", "$basePath/scaling_min_freq"))),
                    maxKHz = parseLongFlexible(runShell(listOf("cat", "$basePath/scaling_max_freq"))),
                    currentKHz = parseLongFlexible(runShell(listOf("cat", "$basePath/scaling_cur_freq"))),
                    relatedCpus = parseCpuIndices(runShell(listOf("cat", "$basePath/related_cpus"))),
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
            val min = parseLongFlexible(runShell(listOf("cat", "$basePath/scaling_min_freq")))
            val max = parseLongFlexible(runShell(listOf("cat", "$basePath/scaling_max_freq")))
            val current = parseLongFlexible(runShell(listOf("cat", "$basePath/scaling_cur_freq")))
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

        val onlineValue = formatOnlineCoresValue(cpuIndices = fallbackCpuIndices)

        return CpuFrequencySummary(
            governor = null,
            minMax = minMaxValue,
            current = currentValue,
            onlineCores = onlineValue,
        )
    }

    private suspend fun formatOnlineCoresValue(cpuIndices: List<Int>): String? {
        if (cpuIndices.isEmpty()) return null

        val online = cpuIndices.sorted().filter { cpuIndex ->
            if (cpuIndex == 0) {
                true
            } else {
                val rawState = runShell(listOf("cat", "/sys/devices/system/cpu/cpu$cpuIndex/online"))
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
        val numeric = NUMERIC_REGEX
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

    private companion object {
        private val NUMERIC_REGEX = Regex("""-?\d+""")
    }
}

package com.adbdeck.feature.deviceinfo.parser

/**
 * Выжимка из `dumpsys display`.
 */
internal data class DisplayHighlights(
    val baseInfo: String?,
    val state: String?,
)

/**
 * Парсит вывод `wm size`.
 */
internal fun parseWmSize(output: String): String? {
    return output.lineSequence()
        .map { it.trim() }
        .firstOrNull { it.startsWith("Physical size:") || it.startsWith("Override size:") }
        ?.substringAfter(':')
        ?.trim()
        ?.takeIf { it.isNotEmpty() }
}

/**
 * Парсит вывод `wm density`.
 */
internal fun parseWmDensity(output: String): String? {
    return output.lineSequence()
        .map { it.trim() }
        .firstOrNull { it.startsWith("Physical density:") || it.startsWith("Override density:") }
        ?.substringAfter(':')
        ?.trim()
        ?.takeIf { it.isNotEmpty() }
}

/**
 * Парсит полезные строки из `dumpsys display`.
 */
internal fun parseDisplayHighlights(output: String): DisplayHighlights {
    val baseInfo = findFirstMatchingLine(output) { line ->
        line.contains("mBaseDisplayInfo", ignoreCase = true) ||
            line.contains("DisplayDeviceInfo", ignoreCase = true)
    }?.let(::normalizeForUi)

    val state = findFirstMatchingLine(output) { line ->
        line.contains("mCurrentState", ignoreCase = true) ||
            line.contains("mDisplayState", ignoreCase = true) ||
            line.contains("mPowerState", ignoreCase = true)
    }?.let(::normalizeForUi)

    return DisplayHighlights(baseInfo = baseInfo, state = state)
}

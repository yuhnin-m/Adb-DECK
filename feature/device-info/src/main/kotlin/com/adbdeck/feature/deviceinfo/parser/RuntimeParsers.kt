package com.adbdeck.feature.deviceinfo.parser

/**
 * Выжимка runtime-состояния из `dumpsys activity` и `dumpsys window`.
 */
internal data class RuntimeHighlights(
    val topResumedActivity: String?,
    val currentFocus: String?,
    val focusedApp: String?,
)

/**
 * Разбирает нужные строки из runtime-дампов.
 */
internal fun parseRuntimeHighlights(
    dumpsysActivityOutput: String,
    dumpsysWindowOutput: String,
): RuntimeHighlights {
    val topResumedActivity = findFirstMatchingLine(dumpsysActivityOutput) { line ->
        line.contains("topResumedActivity", ignoreCase = true) ||
            line.contains("mResumedActivity", ignoreCase = true) ||
            line.contains("ResumedActivity", ignoreCase = true)
    }?.let(::normalizeForUi)

    val currentFocus = findFirstMatchingLine(dumpsysWindowOutput) { line ->
        line.contains("mCurrentFocus", ignoreCase = true)
    }?.let(::normalizeForUi)

    val focusedApp = findFirstMatchingLine(dumpsysWindowOutput) { line ->
        line.contains("mFocusedApp", ignoreCase = true)
    }?.let(::normalizeForUi)

    return RuntimeHighlights(
        topResumedActivity = topResumedActivity,
        currentFocus = currentFocus,
        focusedApp = focusedApp,
    )
}

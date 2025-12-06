package com.adbdeck.core.adb.api.screen

/**
 * Параметры создания screenshot.
 *
 * @param format Формат выходного файла.
 * @param jpegQualityPercent Качество JPEG (1..100), используется только для [ScreenshotFormat.JPEG].
 */
data class ScreenshotOptions(
    val format: ScreenshotFormat = ScreenshotFormat.PNG,
    val jpegQualityPercent: Int = 92,
)

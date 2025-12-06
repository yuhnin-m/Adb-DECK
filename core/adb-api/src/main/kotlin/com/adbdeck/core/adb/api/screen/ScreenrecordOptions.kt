package com.adbdeck.core.adb.api.screen

/**
 * Параметры запуска записи экрана.
 *
 * @param bitRateMbps Битрейт в Mbps.
 * @param videoSize Разрешение в формате `WIDTHxHEIGHT` (например `1280x720`), либо `null` для native.
 */
data class ScreenrecordOptions(
    val bitRateMbps: Int = 8,
    val videoSize: String? = null,
)

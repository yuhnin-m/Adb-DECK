package com.adbdeck.core.adb.api.scrcpy

/**
 * Параметры запуска scrcpy-сессии.
 *
 * @param deviceId Идентификатор активного устройства (`adb -s <id>`).
 * @param maxSize Параметр `--max-size` (px) или `null` для режима без ограничения.
 * @param maxFps Параметр `--max-fps`.
 * @param bitrateMbps Параметр `--video-bit-rate` в Мбит/с или `null`.
 * @param allowInput Разрешить ввод (иначе `--no-control`).
 * @param turnScreenOff Параметр `--turn-screen-off`.
 * @param showTouches Параметр `--show-touches`.
 * @param stayAwake Параметр `--stay-awake`.
 * @param fullscreen Параметр `--fullscreen`.
 * @param alwaysOnTop Параметр `--always-on-top`.
 * @param borderless Параметр `--window-borderless`.
 * @param windowWidth Параметр `--window-width` или `null`.
 * @param windowHeight Параметр `--window-height` или `null`.
 * @param videoCodec Параметр `--video-codec`.
 * @param keyboardMode Параметр `--keyboard`.
 * @param mouseMode Параметр `--mouse`.
 */
data class ScrcpyLaunchRequest(
    val deviceId: String,
    val maxSize: Int? = null,
    val maxFps: Int = 60,
    val bitrateMbps: Int? = null,
    val allowInput: Boolean = true,
    val turnScreenOff: Boolean = false,
    val showTouches: Boolean = false,
    val stayAwake: Boolean = false,
    val fullscreen: Boolean = false,
    val alwaysOnTop: Boolean = false,
    val borderless: Boolean = false,
    val windowWidth: Int? = null,
    val windowHeight: Int? = null,
    val videoCodec: String = "h264",
    val keyboardMode: String = "sdk",
    val mouseMode: String = "sdk",
)

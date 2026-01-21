package com.adbdeck.feature.screentools

import com.adbdeck.core.adb.api.screen.ScreenrecordSession
import com.adbdeck.core.adb.api.screen.ScreenshotFormat

/**
 * Вкладка экрана Screen Tools.
 */
enum class ScreenToolsTab {
    /** Инструменты скриншотов. */
    SCREENSHOT,

    /** Инструменты записи экрана. */
    SCREENRECORD,
}

/**
 * Профили качества screenshot.
 */
enum class ScreenshotQualityPreset(
    val format: ScreenshotFormat,
    val jpegQualityPercent: Int,
    val extension: String,
) {
    /** PNG без потерь. */
    LOSSLESS_PNG(
        format = ScreenshotFormat.PNG,
        jpegQualityPercent = 100,
        extension = "png",
    ),

    /** JPEG максимального качества. */
    JPEG_BEST(
        format = ScreenshotFormat.JPEG,
        jpegQualityPercent = 95,
        extension = "jpg",
    ),

    /** JPEG высокого качества. */
    JPEG_HIGH(
        format = ScreenshotFormat.JPEG,
        jpegQualityPercent = 90,
        extension = "jpg",
    ),

    /** JPEG среднего качества. */
    JPEG_MEDIUM(
        format = ScreenshotFormat.JPEG,
        jpegQualityPercent = 80,
        extension = "jpg",
    ),

    /** JPEG низкого качества (максимальная экономия размера). */
    JPEG_LOW(
        format = ScreenshotFormat.JPEG,
        jpegQualityPercent = 65,
        extension = "jpg",
    ),

    /** JPEG минимального качества для очень маленького размера файла. */
    JPEG_TINY(
        format = ScreenshotFormat.JPEG,
        jpegQualityPercent = 45,
        extension = "jpg",
    ),
}

/**
 * Профили качества записи экрана.
 */
enum class ScreenrecordQualityPreset(
    val bitRateMbps: Int,
    val videoSize: String?,
) {
    /** Максимальное качество и исходное разрешение устройства. */
    ULTRA_NATIVE(
        bitRateMbps = 20,
        videoSize = null,
    ),

    /** Высокое качество с ограничением до 1080p. */
    HIGH_1080P(
        bitRateMbps = 12,
        videoSize = "1920x1080",
    ),

    /** Сбалансированный режим 720p. */
    MEDIUM_720P(
        bitRateMbps = 8,
        videoSize = "1280x720",
    ),

    /** Умеренное качество 540p. */
    BALANCED_540P(
        bitRateMbps = 6,
        videoSize = "960x540",
    ),

    /** Базовый режим 480p. */
    LOW_480P(
        bitRateMbps = 4,
        videoSize = "854x480",
    ),

    /** Минимальный размер файла с низким разрешением. */
    ECO_360P(
        bitRateMbps = 3,
        videoSize = "640x360",
    ),
}

/**
 * Состояние статус-блока операции.
 *
 * @param message Текст статуса.
 * @param isError `true`, если статус отражает ошибку.
 * @param progress Прогресс операции от `0f..1f`.
 * `null` означает indeterminate режим.
 */
data class ScreenToolsStatus(
    val message: String,
    val isError: Boolean = false,
    val progress: Float? = null,
)

/**
 * Состояние раздела Screenshot.
 */
data class ScreenshotSectionState(
    val outputDirectory: String,
    val quality: ScreenshotQualityPreset = ScreenshotQualityPreset.LOSSLESS_PNG,
    val isCapturing: Boolean = false,
    val lastFilePath: String? = null,
    val status: ScreenToolsStatus = ScreenToolsStatus(message = ""),
)

/**
 * Состояние раздела Screenrecord.
 */
enum class RecordingPhase {
    /** Запись не выполняется. */
    IDLE,

    /** Идёт запуск записи. */
    STARTING,

    /** Запись активна. */
    RECORDING,

    /** Идёт остановка и сохранение. */
    STOPPING,
}

/**
 * Состояние раздела Screenrecord.
 */
data class ScreenrecordSectionState(
    val outputDirectory: String,
    val quality: ScreenrecordQualityPreset = ScreenrecordQualityPreset.MEDIUM_720P,
    val phase: RecordingPhase = RecordingPhase.IDLE,
    val activeSession: ScreenrecordSession? = null,
    val recordingDeviceId: String? = null,
    val elapsedSeconds: Long = 0L,
    val currentLocalTargetPath: String? = null,
    val lastFilePath: String? = null,
    val status: ScreenToolsStatus = ScreenToolsStatus(message = ""),
) {
    /** Совместимость call-site'ов: идёт старт записи. */
    val isStarting: Boolean get() = phase == RecordingPhase.STARTING

    /** Совместимость call-site'ов: запись активна. */
    val isRecording: Boolean get() = phase == RecordingPhase.RECORDING

    /** Совместимость call-site'ов: идёт остановка записи. */
    val isStopping: Boolean get() = phase == RecordingPhase.STOPPING
}

/**
 * Краткоживущее сообщение для пользователя.
 */
data class ScreenToolsFeedback(
    val message: String,
    val isError: Boolean,
)

/**
 * Полное состояние feature Screen Tools.
 */
data class ScreenToolsState(
    val selectedTab: ScreenToolsTab = ScreenToolsTab.SCREENSHOT,
    val activeDeviceId: String? = null,
    val deviceMessage: String = "",
    val screenshot: ScreenshotSectionState,
    val screenrecord: ScreenrecordSectionState,
    val feedback: ScreenToolsFeedback? = null,
) {
    /** Признак, что в данный момент есть валидное активное устройство. */
    val isDeviceReady: Boolean get() = activeDeviceId != null
}

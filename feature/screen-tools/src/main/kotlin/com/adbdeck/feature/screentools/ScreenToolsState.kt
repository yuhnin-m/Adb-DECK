package com.adbdeck.feature.screentools

import com.adbdeck.core.adb.api.ScreenshotFormat

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
    val title: String,
    val format: ScreenshotFormat,
    val jpegQualityPercent: Int,
    val extension: String,
) {
    /** PNG без потерь. */
    LOSSLESS_PNG(
        title = "PNG (без потерь)",
        format = ScreenshotFormat.PNG,
        jpegQualityPercent = 100,
        extension = "png",
    ),

    /** JPEG максимального качества. */
    JPEG_BEST(
        title = "JPEG Best (95%)",
        format = ScreenshotFormat.JPEG,
        jpegQualityPercent = 95,
        extension = "jpg",
    ),

    /** JPEG высокого качества. */
    JPEG_HIGH(
        title = "JPEG High (90%)",
        format = ScreenshotFormat.JPEG,
        jpegQualityPercent = 90,
        extension = "jpg",
    ),

    /** JPEG среднего качества. */
    JPEG_MEDIUM(
        title = "JPEG Medium (80%)",
        format = ScreenshotFormat.JPEG,
        jpegQualityPercent = 80,
        extension = "jpg",
    ),

    /** JPEG низкого качества (максимальная экономия размера). */
    JPEG_LOW(
        title = "JPEG Low (65%)",
        format = ScreenshotFormat.JPEG,
        jpegQualityPercent = 65,
        extension = "jpg",
    ),

    /** JPEG минимального качества для очень маленького размера файла. */
    JPEG_TINY(
        title = "JPEG Tiny (45%)",
        format = ScreenshotFormat.JPEG,
        jpegQualityPercent = 45,
        extension = "jpg",
    ),
}

/**
 * Профили качества записи экрана.
 */
enum class ScreenrecordQualityPreset(
    val title: String,
    val bitRateMbps: Int,
    val videoSize: String?,
) {
    /** Максимальное качество и исходное разрешение устройства. */
    ULTRA_NATIVE(
        title = "Ultra (native)",
        bitRateMbps = 20,
        videoSize = null,
    ),

    /** Высокое качество с ограничением до 1080p. */
    HIGH_1080P(
        title = "High (1080p)",
        bitRateMbps = 12,
        videoSize = "1920x1080",
    ),

    /** Сбалансированный режим 720p. */
    MEDIUM_720P(
        title = "Medium (720p)",
        bitRateMbps = 8,
        videoSize = "1280x720",
    ),

    /** Умеренное качество 540p. */
    BALANCED_540P(
        title = "Balanced (540p)",
        bitRateMbps = 6,
        videoSize = "960x540",
    ),

    /** Базовый режим 480p. */
    LOW_480P(
        title = "Low (480p)",
        bitRateMbps = 4,
        videoSize = "854x480",
    ),

    /** Минимальный размер файла с низким разрешением. */
    ECO_360P(
        title = "Eco (360p)",
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
    val status: ScreenToolsStatus = ScreenToolsStatus(message = "Готов к созданию скриншота"),
)

/**
 * Состояние раздела Screenrecord.
 */
data class ScreenrecordSectionState(
    val outputDirectory: String,
    val quality: ScreenrecordQualityPreset = ScreenrecordQualityPreset.MEDIUM_720P,
    val isStarting: Boolean = false,
    val isRecording: Boolean = false,
    val isStopping: Boolean = false,
    val recordingDeviceId: String? = null,
    val elapsedSeconds: Long = 0L,
    val currentLocalTargetPath: String? = null,
    val lastFilePath: String? = null,
    val status: ScreenToolsStatus = ScreenToolsStatus(message = "Запись не запущена"),
)

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
    val deviceMessage: String = "Активное устройство не выбрано",
    val screenshot: ScreenshotSectionState,
    val screenrecord: ScreenrecordSectionState,
    val feedback: ScreenToolsFeedback? = null,
) {
    /** Признак, что в данный момент есть валидное активное устройство. */
    val isDeviceReady: Boolean get() = activeDeviceId != null
}

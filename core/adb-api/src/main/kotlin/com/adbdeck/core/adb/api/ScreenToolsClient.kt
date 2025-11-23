package com.adbdeck.core.adb.api

/**
 * Формат сохранения screenshot.
 */
enum class ScreenshotFormat {
    /** PNG без потерь. */
    PNG,

    /** JPEG с настраиваемым сжатием. */
    JPEG,
}

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

/**
 * Сессия активной записи экрана Android-устройства.
 *
 * @param sessionId Внутренний идентификатор сессии внутри реализации клиента.
 * @param deviceId  Идентификатор устройства, на котором запущена запись.
 * @param remotePath Временный путь видео-файла на устройстве.
 * @param remotePid PID процесса `screenrecord` на устройстве.
 * @param startedAtEpochMillis Момент запуска записи (epoch millis).
 */
data class ScreenrecordSession(
    val sessionId: String,
    val deviceId: String,
    val remotePath: String,
    val remotePid: String,
    val startedAtEpochMillis: Long,
)

/**
 * Контракт ADB-клиента для инструментов экрана (screenshot + screenrecord).
 */
interface ScreenToolsClient {

    /**
     * Сделать screenshot устройства и сохранить файл в [localOutputPath] на хосте.
     */
    suspend fun takeScreenshot(
        deviceId: String,
        localOutputPath: String,
        adbPath: String = "adb",
        options: ScreenshotOptions = ScreenshotOptions(),
    ): Result<Unit>

    /**
     * Запустить screenrecord на устройстве в [remoteOutputPath].
     *
     * Возвращает [ScreenrecordSession], которую затем нужно остановить
     * через [stopScreenrecord].
     */
    suspend fun startScreenrecord(
        deviceId: String,
        remoteOutputPath: String,
        adbPath: String = "adb",
        options: ScreenrecordOptions = ScreenrecordOptions(),
    ): Result<ScreenrecordSession>

    /**
     * Остановить ранее запущенную запись экрана по [sessionId].
     */
    suspend fun stopScreenrecord(
        sessionId: String,
    ): Result<Unit>
}

package com.adbdeck.core.adb.api.screen

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
 * Параметры установки APK на устройство.
 *
 * @param reinstall `-r`: переустановка поверх существующего пакета.
 * @param allowDowngrade `-d`: разрешить downgrade версии приложения.
 * @param grantRuntimePermissions `-g`: автоматически выдать runtime-permissions.
 */
data class ApkInstallOptions(
    val reinstall: Boolean = true,
    val allowDowngrade: Boolean = false,
    val grantRuntimePermissions: Boolean = false,
)

/**
 * Прогресс установки APK.
 *
 * @param progress Значение `0f..1f`, либо `null`, если доступен только текстовый статус.
 * @param message Текущий статус/строка из вывода adb.
 */
data class ApkInstallProgress(
    val progress: Float?,
    val message: String,
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

    /**
     * Установить APK на устройство через `adb install`.
     *
     * @param onProgress Колбек статуса/прогресса установки.
     */
    suspend fun installApk(
        deviceId: String,
        localApkPath: String,
        adbPath: String = "adb",
        options: ApkInstallOptions = ApkInstallOptions(),
        onProgress: (ApkInstallProgress) -> Unit = {},
    ): Result<Unit>
}

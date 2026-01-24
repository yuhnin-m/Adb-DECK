package com.adbdeck.core.adb.api.screen

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

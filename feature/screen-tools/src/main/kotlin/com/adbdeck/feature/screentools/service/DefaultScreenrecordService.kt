package com.adbdeck.feature.screentools.service

import com.adbdeck.core.adb.api.DeviceFileClient
import com.adbdeck.core.adb.api.ScreenrecordOptions
import com.adbdeck.core.adb.api.ScreenToolsClient
import com.adbdeck.core.adb.api.ScreenrecordSession
import com.adbdeck.core.utils.runCatchingPreserveCancellation

/**
 * Реализация [ScreenrecordService] поверх [ScreenToolsClient] и [DeviceFileClient].
 */
class DefaultScreenrecordService(
    private val screenToolsClient: ScreenToolsClient,
    private val deviceFileClient: DeviceFileClient,
) : ScreenrecordService {

    override suspend fun start(
        deviceId: String,
        remoteOutputPath: String,
        adbPath: String,
        options: ScreenrecordOptions,
    ): Result<ScreenrecordSession> =
        screenToolsClient.startScreenrecord(
            deviceId = deviceId,
            remoteOutputPath = remoteOutputPath,
            adbPath = adbPath,
            options = options,
        )

    override suspend fun stopAndSave(
        session: ScreenrecordSession,
        localOutputPath: String,
        adbPath: String,
        onProgress: (progress: Float?, message: String) -> Unit,
    ): Result<Unit> = runCatchingPreserveCancellation {
        onProgress(0.1f, "Останавливаем запись на устройстве…")
        screenToolsClient.stopScreenrecord(session.sessionId).getOrThrow()

        onProgress(0.55f, "Копируем видео на компьютер…")
        val pullResult = deviceFileClient.pull(
            deviceId = session.deviceId,
            remotePath = session.remotePath,
            localPath = localOutputPath,
            adbPath = adbPath,
        )

        onProgress(0.85f, "Удаляем временный файл на устройстве…")
        val cleanupResult = deviceFileClient.delete(
            deviceId = session.deviceId,
            path = session.remotePath,
            recursive = false,
            adbPath = adbPath,
        )

        pullResult.getOrThrow()
        cleanupResult.getOrThrow()

        onProgress(1f, "Запись сохранена")
    }

    override suspend fun abort(
        session: ScreenrecordSession,
        adbPath: String,
    ): Result<Unit> = runCatchingPreserveCancellation {
        // Остановку и удаление выполняем best-effort, чтобы не оставлять хвосты на устройстве.
        val stopResult = screenToolsClient.stopScreenrecord(session.sessionId)
        val cleanupResult = deviceFileClient.delete(
            deviceId = session.deviceId,
            path = session.remotePath,
            recursive = false,
            adbPath = adbPath,
        )

        val stopError = stopResult.exceptionOrNull()
        val cleanupError = cleanupResult.exceptionOrNull()
        if (stopError != null && cleanupError != null) {
            error(
                buildString {
                    append("Не удалось корректно прервать запись. ")
                    append("stop: ")
                    append(stopError.message ?: stopError::class.simpleName)
                    append("; cleanup: ")
                    append(cleanupError.message ?: cleanupError::class.simpleName)
                }
            )
        }
    }
}

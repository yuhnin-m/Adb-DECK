package com.adbdeck.feature.screentools.service

import adbdeck.feature.screen_tools.generated.resources.Res
import adbdeck.feature.screen_tools.generated.resources.screen_tools_service_abort_failed
import adbdeck.feature.screen_tools.generated.resources.screen_tools_service_progress_cleanup_device
import adbdeck.feature.screen_tools.generated.resources.screen_tools_service_progress_cleanup_failed_but_saved
import adbdeck.feature.screen_tools.generated.resources.screen_tools_service_progress_copy_host
import adbdeck.feature.screen_tools.generated.resources.screen_tools_service_stop_stage_copy_failed
import adbdeck.feature.screen_tools.generated.resources.screen_tools_service_stop_stage_copy_failed_with_reason
import adbdeck.feature.screen_tools.generated.resources.screen_tools_service_stop_stage_stop_failed
import adbdeck.feature.screen_tools.generated.resources.screen_tools_service_stop_stage_stop_failed_with_reason
import adbdeck.feature.screen_tools.generated.resources.screen_tools_service_progress_saved
import adbdeck.feature.screen_tools.generated.resources.screen_tools_service_progress_stop_device
import com.adbdeck.core.adb.api.files.DeviceFileClient
import com.adbdeck.core.adb.api.screen.ScreenrecordOptions
import com.adbdeck.core.adb.api.screen.ScreenToolsClient
import com.adbdeck.core.adb.api.screen.ScreenrecordSession
import com.adbdeck.core.utils.runCatchingPreserveCancellation
import org.jetbrains.compose.resources.getString

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
        onProgress(0.1f, getString(Res.string.screen_tools_service_progress_stop_device))
        val stopResult = screenToolsClient.stopScreenrecord(session.sessionId)
        if (stopResult.isFailure) {
            val reason = stopResult.exceptionOrNull()?.message?.trim().orEmpty()
            val message = if (reason.isNotBlank()) {
                getString(Res.string.screen_tools_service_stop_stage_stop_failed_with_reason, reason)
            } else {
                getString(Res.string.screen_tools_service_stop_stage_stop_failed)
            }
            throw ScreenrecordStopError.StopOnDevice(
                message = message,
                cause = stopResult.exceptionOrNull(),
            )
        }

        onProgress(0.55f, getString(Res.string.screen_tools_service_progress_copy_host))
        val pullResult = deviceFileClient.pull(
            deviceId = session.deviceId,
            remotePath = session.remotePath,
            localPath = localOutputPath,
            adbPath = adbPath,
        )
        if (pullResult.isFailure) {
            val reason = pullResult.exceptionOrNull()?.message?.trim().orEmpty()
            val message = if (reason.isNotBlank()) {
                getString(Res.string.screen_tools_service_stop_stage_copy_failed_with_reason, reason)
            } else {
                getString(Res.string.screen_tools_service_stop_stage_copy_failed)
            }
            throw ScreenrecordStopError.CopyToHost(
                message = message,
                cause = pullResult.exceptionOrNull(),
            )
        }

        // Удаляем временный файл только после успешного pull, чтобы
        // не потерять запись при ошибке копирования.
        onProgress(0.85f, getString(Res.string.screen_tools_service_progress_cleanup_device))
        val cleanupResult = deviceFileClient.delete(
            deviceId = session.deviceId,
            path = session.remotePath,
            recursive = false,
            adbPath = adbPath,
        )

        if (cleanupResult.isFailure) {
            onProgress(
                0.95f,
                getString(Res.string.screen_tools_service_progress_cleanup_failed_but_saved),
            )
        }

        onProgress(1f, getString(Res.string.screen_tools_service_progress_saved))
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
                getString(
                    Res.string.screen_tools_service_abort_failed,
                    stopError.message ?: stopError::class.simpleName.orEmpty(),
                    cleanupError.message ?: cleanupError::class.simpleName.orEmpty(),
                )
            )
        }
    }
}

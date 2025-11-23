package com.adbdeck.feature.screentools.service

import com.adbdeck.core.adb.api.ScreenrecordSession
import com.adbdeck.core.adb.api.ScreenrecordOptions

/**
 * Сервис запуска/остановки записи экрана устройства.
 */
interface ScreenrecordService {

    /**
     * Запустить запись экрана устройства.
     */
    suspend fun start(
        deviceId: String,
        remoteOutputPath: String,
        adbPath: String,
        options: ScreenrecordOptions,
    ): Result<ScreenrecordSession>

    /**
     * Остановить запись и сохранить видео на хосте.
     *
     * @param onProgress Колбек прогресса этапов сохранения.
     */
    suspend fun stopAndSave(
        session: ScreenrecordSession,
        localOutputPath: String,
        adbPath: String,
        onProgress: (progress: Float?, message: String) -> Unit,
    ): Result<Unit>

    /**
     * Прервать запись без сохранения на хост.
     *
     * Используется как best-effort cleanup при смене устройства.
     */
    suspend fun abort(
        session: ScreenrecordSession,
        adbPath: String,
    ): Result<Unit>
}

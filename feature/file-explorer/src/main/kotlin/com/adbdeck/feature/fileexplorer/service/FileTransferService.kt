package com.adbdeck.feature.fileexplorer.service

/**
 * Сервис переноса файлов между локальной и device-файловой системой.
 */
interface FileTransferService {

    /**
     * Перенос с хоста на устройство (`push`).
     *
     * @param overwrite Если `true`, существующий target будет удалён перед копированием.
     */
    suspend fun push(
        deviceId: String,
        localSourcePath: String,
        remoteTargetPath: String,
        adbPath: String,
        overwrite: Boolean,
    ): Result<Unit>

    /**
     * Перенос с устройства на хост (`pull`).
     *
     * @param overwrite Если `true`, существующий target будет удалён перед копированием.
     */
    suspend fun pull(
        deviceId: String,
        remoteSourcePath: String,
        localTargetPath: String,
        adbPath: String,
        overwrite: Boolean,
    ): Result<Unit>
}

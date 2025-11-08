package com.adbdeck.feature.fileexplorer.service

import com.adbdeck.core.utils.runCatchingPreserveCancellation

/**
 * Реализация [FileTransferService].
 *
 * Логика overwrite выполняется на уровне сервиса:
 * - при `push` удаляется существующий target на устройстве;
 * - при `pull` удаляется существующий локальный target.
 */
class DefaultFileTransferService(
    private val localFileService: LocalFileService,
    private val deviceFileService: DeviceFileService,
) : FileTransferService {

    override suspend fun push(
        deviceId: String,
        localSourcePath: String,
        remoteTargetPath: String,
        adbPath: String,
        overwrite: Boolean,
    ): Result<Unit> = runCatchingPreserveCancellation {
        if (overwrite) {
            val targetExists = deviceFileService.exists(deviceId, remoteTargetPath, adbPath)
                .getOrElse { throw it }
            if (targetExists) {
                deviceFileService.delete(deviceId, remoteTargetPath, adbPath)
                    .getOrElse { throw it }
            }
        }

        deviceFileService.push(
            deviceId = deviceId,
            localPath = localSourcePath,
            remotePath = remoteTargetPath,
            adbPath = adbPath,
        ).getOrElse { throw it }
    }.map { Unit }

    override suspend fun pull(
        deviceId: String,
        remoteSourcePath: String,
        localTargetPath: String,
        adbPath: String,
        overwrite: Boolean,
    ): Result<Unit> = runCatchingPreserveCancellation {
        if (overwrite) {
            val targetExists = localFileService.exists(localTargetPath).getOrElse { throw it }
            if (targetExists) {
                localFileService.delete(localTargetPath).getOrElse { throw it }
            }
        }

        deviceFileService.pull(
            deviceId = deviceId,
            remotePath = remoteSourcePath,
            localPath = localTargetPath,
            adbPath = adbPath,
        ).getOrElse { throw it }
    }.map { Unit }
}

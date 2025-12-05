package com.adbdeck.feature.fileexplorer.service

import com.adbdeck.core.adb.api.files.DeviceFileClient
import com.adbdeck.core.adb.api.files.DeviceFileType
import com.adbdeck.feature.fileexplorer.ExplorerFileItem
import com.adbdeck.feature.fileexplorer.ExplorerFileType

/**
 * Реализация [DeviceFileService], адаптирующая [DeviceFileClient] к модели feature.
 */
class DefaultDeviceFileService(
    private val deviceFileClient: DeviceFileClient,
) : DeviceFileService {

    override fun defaultPath(): String = "/sdcard"

    override suspend fun listDirectory(
        deviceId: String,
        path: String,
        adbPath: String,
    ): Result<List<ExplorerFileItem>> =
        deviceFileClient.listDirectory(deviceId, path, adbPath)
            .map { entries ->
                entries.map { entry ->
                    ExplorerFileItem(
                        name = entry.name,
                        fullPath = entry.fullPath,
                        type = when (entry.type) {
                            DeviceFileType.DIRECTORY -> ExplorerFileType.DIRECTORY
                            DeviceFileType.FILE -> ExplorerFileType.FILE
                            DeviceFileType.SYMLINK -> ExplorerFileType.SYMLINK
                            DeviceFileType.OTHER -> ExplorerFileType.OTHER
                        },
                        sizeBytes = entry.sizeBytes,
                        modifiedEpochMillis = entry.modifiedEpochSeconds?.times(1000L),
                    )
                }
            }

    override suspend fun exists(
        deviceId: String,
        path: String,
        adbPath: String,
    ): Result<Boolean> = deviceFileClient.exists(deviceId, path, adbPath)

    override suspend fun canAccessDirectory(
        deviceId: String,
        path: String,
        adbPath: String,
    ): Result<Boolean> = deviceFileClient.canAccessDirectory(deviceId, path, adbPath)

    override suspend fun createDirectory(
        deviceId: String,
        path: String,
        adbPath: String,
    ): Result<Unit> = deviceFileClient.createDirectory(deviceId, path, adbPath)

    override suspend fun delete(
        deviceId: String,
        path: String,
        adbPath: String,
    ): Result<Unit> = deviceFileClient.delete(deviceId, path, recursive = true, adbPath = adbPath)

    override suspend fun rename(
        deviceId: String,
        sourcePath: String,
        newName: String,
        adbPath: String,
    ): Result<String> {
        val targetPath = resolveChildPath(parentPath(sourcePath) ?: "/", newName)
        return deviceFileClient.rename(deviceId, sourcePath, targetPath, adbPath)
            .map { targetPath }
    }

    override suspend fun push(
        deviceId: String,
        localPath: String,
        remotePath: String,
        adbPath: String,
    ): Result<Unit> = deviceFileClient.push(deviceId, localPath, remotePath, adbPath)

    override suspend fun pull(
        deviceId: String,
        remotePath: String,
        localPath: String,
        adbPath: String,
    ): Result<Unit> = deviceFileClient.pull(deviceId, remotePath, localPath, adbPath)

    override fun parentPath(path: String): String? {
        val normalized = path.ifBlank { "/" }.trimEnd('/').ifBlank { "/" }
        if (normalized == "/") return null
        val idx = normalized.lastIndexOf('/')
        return if (idx <= 0) "/" else normalized.substring(0, idx)
    }

    override fun resolveChildPath(parentPath: String, name: String): String {
        val normalized = parentPath.ifBlank { "/" }.trimEnd('/').ifBlank { "/" }
        return if (normalized == "/") "/$name" else "$normalized/$name"
    }
}

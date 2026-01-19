package com.adbdeck.feature.fileexplorer.service

import com.adbdeck.core.adb.api.files.DeviceFileClient
import com.adbdeck.core.adb.api.files.DeviceFileType
import com.adbdeck.core.adb.api.monitoring.SystemMonitorClient
import com.adbdeck.core.utils.runCatchingPreserveCancellation
import com.adbdeck.feature.fileexplorer.ExplorerFileItem
import com.adbdeck.feature.fileexplorer.ExplorerFileType
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

/**
 * Реализация [DeviceFileService], адаптирующая [DeviceFileClient] к модели feature.
 */
class DefaultDeviceFileService(
    private val deviceFileClient: DeviceFileClient,
    private val systemMonitorClient: SystemMonitorClient,
) : DeviceFileService {

    override fun defaultPath(): String = "/sdcard"

    override fun defaultRoots(): List<String> = listOf(
        defaultPath(),
        "/storage/emulated/0",
        "/storage/self/primary",
        "/data/local/tmp",
    ).flatMap(::toBrowsablePaths)
        .map(::normalizePath)
        .distinct()

    override fun normalizePath(path: String): String {
        val normalized = path.trim().ifBlank { "/" }
            .let { value -> if (value.startsWith("/")) value else "/$value" }
            .replace(Regex("/{2,}"), "/")
            .trimEnd('/')
        return normalized.ifBlank { "/" }
    }

    override fun preferredStartPath(roots: List<String>): String {
        val normalizedRoots = roots.map(::normalizePath).distinct()
        val preferredByPriority = listOf(
            normalizePath(defaultPath()),
            "/sdcard",
            "/storage/emulated/0",
            "/storage/self/primary",
            "/data/local/tmp",
        )

        return preferredByPriority.firstOrNull { candidate -> normalizedRoots.any { it == candidate } }
            ?: normalizedRoots.firstOrNull()
            ?: normalizePath(defaultPath())
    }

    override suspend fun resolveAccessibleRoots(
        deviceId: String,
        adbPath: String,
    ): Result<List<String>> = runCatchingPreserveCancellation {
        val rootsFromStorage = systemMonitorClient.getStorageInfo(deviceId = deviceId, adbPath = adbPath)
            .getOrDefault(emptyList())
            .asSequence()
            .filter { it.isRelevant }
            .flatMap { partition ->
                toBrowsablePaths(partition.mountPoint).asSequence()
            }
            .toList()

        val mergedRoots = (rootsFromStorage + defaultRoots())
            .map(::normalizePath)
            .distinct()

        val accessibleRoots = probeAccessibleRootsParallel(
            deviceId = deviceId,
            adbPath = adbPath,
            roots = mergedRoots,
        )

        val fallbackAccessibleRoots = if (accessibleRoots.isEmpty()) {
            probeAccessibleRootsParallel(
                deviceId = deviceId,
                adbPath = adbPath,
                roots = FALLBACK_PROBE_ROOTS,
            )
        } else {
            emptyList()
        }

        (accessibleRoots + fallbackAccessibleRoots)
            .map(::normalizePath)
            .distinct()
    }

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

    /**
     * Параллельно проверяет доступность root-кандидатов для shell-пользователя.
     *
     * По каждому пути выполняется лёгкий probe через `canAccessDirectory`
     * вместо полного list'инга, чтобы ускорить initial загрузку.
     */
    private suspend fun probeAccessibleRootsParallel(
        deviceId: String,
        adbPath: String,
        roots: List<String>,
    ): List<String> = coroutineScope {
        roots
            .map(::normalizePath)
            .distinct()
            .map { root ->
                async {
                    val canOpen = runCatchingPreserveCancellation {
                        deviceFileClient.canAccessDirectory(
                            deviceId = deviceId,
                            directoryPath = root,
                            adbPath = adbPath,
                        ).getOrDefault(false)
                    }.getOrDefault(false)

                    root to canOpen
                }
            }
            .awaitAll()
            .filter { (_, canOpen) -> canOpen }
            .map { (root, _) -> root }
    }

    /**
     * Преобразует mount-point в реально просматриваемые пути для файлового менеджера.
     *
     * `df` часто возвращает системные точки (`/storage/emulated`), тогда как
     * пользовательские файлы доступны по `/storage/emulated/0` или `/sdcard`.
     */
    private fun toBrowsablePaths(rawPath: String): List<String> {
        val path = normalizePath(rawPath)
        val candidates = when (path) {
            "/" -> emptyList()
            "/storage/emulated" -> listOf("/storage/emulated/0", "/sdcard")
            "/storage/self" -> listOf("/storage/self/primary", "/sdcard")
            "/mnt/shell/emulated" -> listOf("/storage/emulated/0", "/sdcard")
            else -> listOf(path)
        }
        return candidates.map(::normalizePath).distinct()
    }

    private companion object {
        val FALLBACK_PROBE_ROOTS: List<String> = listOf(
            "/sdcard",
            "/storage/self/primary",
            "/storage/emulated/0",
            "/data/local/tmp",
            "/",
        )
    }
}

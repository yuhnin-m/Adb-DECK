package com.adbdeck.feature.fileexplorer.service

import com.adbdeck.feature.fileexplorer.ExplorerFileItem
import com.adbdeck.feature.fileexplorer.ExplorerFileType
import com.adbdeck.core.utils.runCatchingPreserveCancellation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes

/**
 * Реализация [LocalFileService] через JVM NIO API.
 */
class DefaultLocalFileService : LocalFileService {

    override fun defaultPath(): String =
        System.getProperty("user.home")?.ifBlank { null }
            ?: Paths.get("").toAbsolutePath().toString()

    override suspend fun listDirectory(path: String): Result<List<ExplorerFileItem>> =
        runCatchingPreserveCancellation {
            withContext(Dispatchers.IO) {
                val directory = Paths.get(path)
                if (!Files.exists(directory)) {
                    error("Локальный путь не найден: $path")
                }
                if (!Files.isDirectory(directory)) {
                    error("Путь не является директорией: $path")
                }

                Files.newDirectoryStream(directory).use { stream ->
                    stream
                        .map { entry -> toExplorerItem(entry) }
                        .sortedWith(compareByDescending<ExplorerFileItem> { it.isDirectory }.thenBy { it.name.lowercase() })
                        .toList()
                }
            }
        }

    override suspend fun exists(path: String): Result<Boolean> =
        runCatchingPreserveCancellation {
            withContext(Dispatchers.IO) {
                Files.exists(Paths.get(path))
            }
        }

    override suspend fun createDirectory(path: String): Result<Unit> =
        runCatchingPreserveCancellation {
            withContext(Dispatchers.IO) {
                Files.createDirectory(Paths.get(path))
            }
        }.map { Unit }

    override suspend fun delete(path: String): Result<Unit> =
        runCatchingPreserveCancellation {
            withContext(Dispatchers.IO) {
                val target = Paths.get(path)
                if (!Files.exists(target)) return@withContext

                Files.walkFileTree(target, object : SimpleFileVisitor<Path>() {
                    override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
                        Files.deleteIfExists(file)
                        return FileVisitResult.CONTINUE
                    }

                    override fun postVisitDirectory(dir: Path, exc: java.io.IOException?): FileVisitResult {
                        Files.deleteIfExists(dir)
                        return FileVisitResult.CONTINUE
                    }
                })
            }
        }.map { Unit }

    override suspend fun rename(sourcePath: String, newName: String): Result<String> =
        runCatchingPreserveCancellation {
            withContext(Dispatchers.IO) {
                val source = Paths.get(sourcePath)
                val target = source.resolveSibling(newName)
                Files.move(source, target)
                target.toString()
            }
        }

    override fun parentPath(path: String): String? {
        val current = Paths.get(path)
        return current.parent?.toString()
    }

    override fun resolveChildPath(parentPath: String, name: String): String =
        Paths.get(parentPath).resolve(name).toString()

    /** Преобразует локальный путь в унифицированную модель UI. */
    private fun toExplorerItem(path: Path): ExplorerFileItem {
        val attrs = Files.readAttributes(path, BasicFileAttributes::class.java)
        val type = when {
            attrs.isDirectory -> ExplorerFileType.DIRECTORY
            attrs.isRegularFile -> ExplorerFileType.FILE
            attrs.isSymbolicLink -> ExplorerFileType.SYMLINK
            else -> ExplorerFileType.OTHER
        }

        return ExplorerFileItem(
            name = path.fileName?.toString() ?: path.toString(),
            fullPath = path.toAbsolutePath().toString(),
            type = type,
            sizeBytes = if (attrs.isRegularFile) attrs.size() else null,
            modifiedEpochMillis = attrs.lastModifiedTime()?.toMillis(),
        )
    }
}

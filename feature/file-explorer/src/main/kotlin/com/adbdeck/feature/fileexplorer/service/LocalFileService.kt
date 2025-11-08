package com.adbdeck.feature.fileexplorer.service

import com.adbdeck.feature.fileexplorer.ExplorerFileItem

/**
 * Сервис работы с локальной файловой системой хоста.
 */
interface LocalFileService {

    /** Путь стартовой директории для локальной панели. */
    fun defaultPath(): String

    /** Получить список элементов директории [path]. */
    suspend fun listDirectory(path: String): Result<List<ExplorerFileItem>>

    /** Проверить существование [path]. */
    suspend fun exists(path: String): Result<Boolean>

    /** Создать директорию по пути [path]. */
    suspend fun createDirectory(path: String): Result<Unit>

    /** Удалить путь [path] (файл или директорию рекурсивно). */
    suspend fun delete(path: String): Result<Unit>

    /** Переименовать [sourcePath] в [newName]. */
    suspend fun rename(sourcePath: String, newName: String): Result<String>

    /** Вернуть родительский путь для [path], либо `null`, если выше идти нельзя. */
    fun parentPath(path: String): String?

    /** Построить путь дочернего элемента [name] внутри [parentPath]. */
    fun resolveChildPath(parentPath: String, name: String): String
}

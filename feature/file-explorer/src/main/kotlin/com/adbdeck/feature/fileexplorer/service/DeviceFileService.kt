package com.adbdeck.feature.fileexplorer.service

import com.adbdeck.feature.fileexplorer.ExplorerFileItem

/**
 * Сервис файловых операций на Android-устройстве.
 */
interface DeviceFileService {

    /** Стартовый путь для device-панели. */
    fun defaultPath(): String

    /** Получить список элементов директории [path] на устройстве. */
    suspend fun listDirectory(
        deviceId: String,
        path: String,
        adbPath: String,
    ): Result<List<ExplorerFileItem>>

    /** Проверить существование [path] на устройстве. */
    suspend fun exists(
        deviceId: String,
        path: String,
        adbPath: String,
    ): Result<Boolean>

    /** Создать директорию [path] на устройстве. */
    suspend fun createDirectory(
        deviceId: String,
        path: String,
        adbPath: String,
    ): Result<Unit>

    /** Удалить [path] на устройстве. */
    suspend fun delete(
        deviceId: String,
        path: String,
        adbPath: String,
    ): Result<Unit>

    /** Переименовать [sourcePath] в [newName] на устройстве. */
    suspend fun rename(
        deviceId: String,
        sourcePath: String,
        newName: String,
        adbPath: String,
    ): Result<String>

    /** Выполнить `adb push`. */
    suspend fun push(
        deviceId: String,
        localPath: String,
        remotePath: String,
        adbPath: String,
    ): Result<Unit>

    /** Выполнить `adb pull`. */
    suspend fun pull(
        deviceId: String,
        remotePath: String,
        localPath: String,
        adbPath: String,
    ): Result<Unit>

    /** Вернуть родительский путь для [path]. */
    fun parentPath(path: String): String?

    /** Построить дочерний путь [name] внутри [parentPath]. */
    fun resolveChildPath(parentPath: String, name: String): String
}

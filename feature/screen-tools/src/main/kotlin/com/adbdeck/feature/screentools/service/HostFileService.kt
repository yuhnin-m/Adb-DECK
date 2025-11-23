package com.adbdeck.feature.screentools.service

/**
 * Сервис host-side операций с файлами (desktop environment).
 */
interface HostFileService {

    /** Дефолтная директория скриншотов. */
    fun defaultScreenshotDirectory(): String

    /** Дефолтная директория screenrecord-видео. */
    fun defaultScreenrecordDirectory(): String

    /** Убедиться, что директория [path] существует. */
    suspend fun ensureDirectory(path: String): Result<Unit>

    /** Открыть файл [path] системным приложением. */
    suspend fun openFile(path: String): Result<Unit>

    /** Открыть директорию [path] в файловом менеджере ОС. */
    suspend fun openFolder(path: String): Result<Unit>

    /** Скопировать изображение из [path] в системный буфер обмена. */
    suspend fun copyImageToClipboard(path: String): Result<Unit>

    /** Проверить, что [path] существует и является файлом. */
    fun isFile(path: String?): Boolean

    /** Проверить, что [path] существует и является директорией. */
    fun isDirectory(path: String): Boolean
}

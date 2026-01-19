package com.adbdeck.feature.fileexplorer.service

import com.adbdeck.feature.fileexplorer.ExplorerFileItem

/**
 * Сервис файловых операций на Android-устройстве.
 */
interface DeviceFileService {

    /** Стартовый путь для device-панели. */
    fun defaultPath(): String

    /**
     * Базовый набор root-путей для устройства.
     *
     * Возвращается даже если авто-детект недоступен, чтобы UI всегда имел
     * предсказуемую точку входа в файловую систему.
     */
    fun defaultRoots(): List<String>

    /** Нормализовать device-путь в абсолютный формат без хвостового `/`. */
    fun normalizePath(path: String): String

    /** Выбрать предпочтительный стартовый путь из списка [roots]. */
    fun preferredStartPath(roots: List<String>): String

    /**
     * Найти доступные root-пути для shell-пользователя на устройстве.
     *
     * Реализация использует best-effort стратегию:
     * - пробует получить mount-point'ы из `df`/storage-источников;
     * - применяет fallback-список корней;
     * - проверяет доступ лёгким probe (`canAccessDirectory`).
     */
    suspend fun resolveAccessibleRoots(
        deviceId: String,
        adbPath: String,
    ): Result<List<String>>

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

    /**
     * Лёгкая проверка, можно ли открыть директорию [path] для shell-пользователя.
     *
     * Используется для фильтрации root-категорий без полного листинга каталога.
     */
    suspend fun canAccessDirectory(
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

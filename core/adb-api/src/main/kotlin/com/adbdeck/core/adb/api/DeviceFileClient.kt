package com.adbdeck.core.adb.api

/**
 * Контракт ADB-клиента для работы с файловой системой Android-устройства.
 *
 * Все операции выполняются для конкретного `deviceId` и возвращают [Result].
 * Реализация обычно использует команды `adb shell`, `adb push`, `adb pull`.
 */
interface DeviceFileClient {

    /**
     * Получить список элементов директории [directoryPath] на устройстве.
     */
    suspend fun listDirectory(
        deviceId: String,
        directoryPath: String,
        adbPath: String = "adb",
    ): Result<List<DeviceFileEntry>>

    /**
     * Проверить существование пути [path] на устройстве.
     */
    suspend fun exists(
        deviceId: String,
        path: String,
        adbPath: String = "adb",
    ): Result<Boolean>

    /**
     * Быстрая проверка доступности директории [directoryPath] для shell-пользователя.
     *
     * В отличие от [listDirectory], не читает содержимое каталога полностью.
     */
    suspend fun canAccessDirectory(
        deviceId: String,
        directoryPath: String,
        adbPath: String = "adb",
    ): Result<Boolean>

    /**
     * Создать директорию по пути [directoryPath].
     *
     * Поведение аналогично `mkdir -p`.
     */
    suspend fun createDirectory(
        deviceId: String,
        directoryPath: String,
        adbPath: String = "adb",
    ): Result<Unit>

    /**
     * Удалить файл или директорию [path] на устройстве.
     *
     * Если [recursive] == `true`, используется рекурсивное удаление.
     */
    suspend fun delete(
        deviceId: String,
        path: String,
        recursive: Boolean = true,
        adbPath: String = "adb",
    ): Result<Unit>

    /**
     * Переименовать или переместить [sourcePath] в [targetPath] на устройстве.
     */
    suspend fun rename(
        deviceId: String,
        sourcePath: String,
        targetPath: String,
        adbPath: String = "adb",
    ): Result<Unit>

    /**
     * Скопировать локальный файл/директорию [localPath] на устройство в [remotePath].
     *
     * Делегирует в `adb push`.
     */
    suspend fun push(
        deviceId: String,
        localPath: String,
        remotePath: String,
        adbPath: String = "adb",
    ): Result<Unit>

    /**
     * Скопировать файл/директорию [remotePath] с устройства на хост в [localPath].
     *
     * Делегирует в `adb pull`.
     */
    suspend fun pull(
        deviceId: String,
        remotePath: String,
        localPath: String,
        adbPath: String = "adb",
    ): Result<Unit>
}

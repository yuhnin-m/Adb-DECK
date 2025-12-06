package com.adbdeck.core.adb.api.files

/**
 * Тип объекта файловой системы Android-устройства.
 */
enum class DeviceFileType {
    /** Директория. */
    DIRECTORY,

    /** Обычный файл. */
    FILE,

    /** Символическая ссылка. */
    SYMLINK,

    /** Прочие типы (socket, block device и т.д.). */
    OTHER,
}

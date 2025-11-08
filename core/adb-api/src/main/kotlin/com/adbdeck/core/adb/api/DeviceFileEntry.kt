package com.adbdeck.core.adb.api

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

/**
 * Элемент файловой системы Android-устройства.
 *
 * @param name Название элемента без родительского пути.
 * @param fullPath Абсолютный путь на устройстве.
 * @param type Тип элемента.
 * @param sizeBytes Размер в байтах (доступен обычно только для файлов).
 * @param modifiedEpochSeconds Время последней модификации в epoch seconds, если удалось получить.
 */
data class DeviceFileEntry(
    val name: String,
    val fullPath: String,
    val type: DeviceFileType,
    val sizeBytes: Long? = null,
    val modifiedEpochSeconds: Long? = null,
) {
    /** `true`, если элемент является директорией. */
    val isDirectory: Boolean get() = type == DeviceFileType.DIRECTORY
}

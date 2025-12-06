package com.adbdeck.core.adb.api.monitoring.storage

/**
 * Категория раздела файловой системы для цветовой маркировки в UI.
 */
enum class StorageCategory {
    /** Системный раздел (/, /system, /vendor). */
    SYSTEM,

    /** Пользовательские данные (/data). */
    DATA,

    /** Внешнее хранилище, SD-карта (/sdcard, /storage/emulated/0). */
    EXTERNAL,

    /** Прочие разделы (tmpfs, /apex и т. д.). */
    OTHER,
}

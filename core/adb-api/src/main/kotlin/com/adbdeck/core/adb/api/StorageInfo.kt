package com.adbdeck.core.adb.api

/**
 * Информация об одном разделе / точке монтирования файловой системы.
 *
 * Получается из вывода `adb shell df` или `adb shell df -k`.
 *
 * @param filesystem  Имя файловой системы / блочного устройства (например `/dev/block/sda1`).
 * @param totalKb     Общий размер раздела в KB.
 * @param usedKb      Используемое пространство в KB.
 * @param freeKb      Свободное пространство в KB.
 * @param usedPercent Процент использования (0–100), парсится из вывода df.
 * @param mountPoint  Точка монтирования (например `/data`, `/sdcard`, `/system`).
 */
data class StoragePartition(
    val filesystem: String,
    val totalKb: Long,
    val usedKb: Long,
    val freeKb: Long,
    val usedPercent: Int,
    val mountPoint: String,
) {
    /**
     * `true` если раздел представляет реальное хранилище.
     *
     * Фильтрует псевдо-файловые системы (/proc, /sys, /dev) и пустые tmpfs-разделы,
     * которые не интересны с точки зрения анализа хранилища.
     */
    val isRelevant: Boolean
        get() = totalKb > 0L &&
            mountPoint.isNotBlank() &&
            !isExcluded

    /**
     * Категория раздела для визуального отображения.
     */
    val category: StorageCategory
        get() = when {
            mountPoint == "/" -> StorageCategory.SYSTEM
            mountPoint.startsWith("/system") ||
                mountPoint.startsWith("/product") ||
                mountPoint.startsWith("/vendor") -> StorageCategory.SYSTEM
            mountPoint.startsWith("/data") -> StorageCategory.DATA
            mountPoint.startsWith("/sdcard") ||
                mountPoint.startsWith("/storage/emulated") ||
                mountPoint.startsWith("/mnt/sdcard") -> StorageCategory.EXTERNAL
            else -> StorageCategory.OTHER
        }

    private val isExcluded: Boolean
        get() = mountPoint.startsWith("/proc") ||
            mountPoint.startsWith("/sys") ||
            mountPoint.startsWith("/dev/pts") ||
            (filesystem == "tmpfs" && totalKb < 512L) ||
            filesystem == "none" ||
            filesystem == "cgroup" ||
            filesystem == "pstore" ||
            filesystem == "selinuxfs"
}

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

/**
 * Агрегированная сводка по всему пользовательскому хранилищу устройства.
 *
 * Вычисляется из [StoragePartition.isRelevant] разделов в [DefaultSystemMonitorClient].
 *
 * @param totalKb  Суммарный размер всех релевантных разделов в KB.
 * @param usedKb   Суммарно использовано в KB.
 * @param freeKb   Суммарно свободно в KB.
 */
data class StorageSummary(
    val totalKb: Long,
    val usedKb: Long,
    val freeKb: Long,
) {
    /** Процент использования (0–100). Безопасен при totalKb == 0. */
    val usedPercent: Int
        get() = if (totalKb > 0L)
            ((usedKb.toFloat() / totalKb) * 100).toInt().coerceIn(0, 100)
        else 0
}

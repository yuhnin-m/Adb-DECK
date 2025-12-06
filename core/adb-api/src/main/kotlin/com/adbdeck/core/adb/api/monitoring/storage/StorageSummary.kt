package com.adbdeck.core.adb.api.monitoring.storage

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

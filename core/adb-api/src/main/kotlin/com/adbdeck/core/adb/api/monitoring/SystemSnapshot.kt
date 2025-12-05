package com.adbdeck.core.adb.api.monitoring

/**
 * Точка истории системных метрик (CPU + RAM) для отображения на временно́м графике.
 *
 * ## Управление памятью
 *
 * Снимки хранятся в ограниченном кольцевом буфере [ArrayDeque] внутри компонента.
 * Максимальный размер ограничен [HISTORY_LIMIT] — при превышении удаляется самая
 * старая точка ([ArrayDeque.removeFirst]).
 *
 * ## Производительность
 *
 * - Одна точка ≈ 40 байт (3 Float + 1 Long + timestamp Long).
 * - 120 точек × 40 байт = ~5 KB — пренебрежимо мало.
 * - Обновление state происходит не чаще раза в [POLL_INTERVAL_MS] (3 сек).
 * - Для UI используется неизменяемый `List<SystemSnapshot>` из `state`,
 *   что позволяет Compose эффективно пропускать ненужные recomposition.
 *
 * @param timestamp      Время снятия метрики (мс от эпохи, [System.currentTimeMillis]).
 * @param cpuPercent     Суммарное использование CPU системой (0–100).
 *                       На многоядерных устройствах нормируется к 100%.
 * @param usedRamKb      Используемая RAM в KB.
 * @param totalRamKb     Общий объём RAM в KB.
 */
data class SystemSnapshot(
    val timestamp: Long = System.currentTimeMillis(),
    val cpuPercent: Float,
    val usedRamKb: Long,
    val totalRamKb: Long,
) {
    /**
     * Процент использования RAM (0–100).
     * Безопасно при [totalRamKb] == 0: возвращает 0f.
     */
    val ramPercent: Float
        get() = if (totalRamKb > 0L)
            (usedRamKb.toFloat() / totalRamKb * 100f).coerceIn(0f, 100f)
        else 0f

    companion object {
        /** Максимальное количество точек в буфере истории графика (~6 минут при 3-сек. поллинге). */
        const val HISTORY_LIMIT = 120

        /** Интервал опроса метрик (мс). Не менять без обновления [HISTORY_LIMIT]. */
        const val POLL_INTERVAL_MS = 3_000L
    }
}

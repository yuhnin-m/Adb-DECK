package com.adbdeck.feature.systemmonitor.storage

import com.adbdeck.core.adb.api.monitoring.StoragePartition
import com.adbdeck.core.adb.api.monitoring.StorageSummary

// ── Состояние экрана хранилища ────────────────────────────────────────────────

/**
 * Состояние загрузки информации о хранилище.
 */
sealed class StorageListState {

    /** Устройство не выбрано или недоступно. */
    data object NoDevice : StorageListState()

    /** Идёт загрузка информации. */
    data object Loading : StorageListState()

    /**
     * Данные успешно загружены.
     *
     * @param partitions Все разделы файловой системы (включая нерелевантные).
     * @param summary    Агрегированная сводка по релевантным разделам.
     */
    data class Success(
        val partitions: List<StoragePartition>,
        val summary: StorageSummary,
    ) : StorageListState()

    /**
     * Ошибка загрузки.
     *
     * @param message Человекочитаемое описание ошибки.
     */
    data class Error(val message: String) : StorageListState()
}

// ── Корневое состояние экрана ─────────────────────────────────────────────────

/**
 * Полное состояние вкладки «Storage» в System Monitor.
 *
 * @param listState Состояние загрузки данных о разделах файловой системы.
 */
data class StorageState(
    val listState: StorageListState = StorageListState.NoDevice,
)

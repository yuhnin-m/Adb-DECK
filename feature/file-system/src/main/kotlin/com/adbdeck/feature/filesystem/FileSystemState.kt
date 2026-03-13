package com.adbdeck.feature.filesystem

import com.adbdeck.core.adb.api.monitoring.storage.StoragePartition
import com.adbdeck.core.adb.api.monitoring.storage.StorageSummary

// ── Состояние экрана файловых систем ──────────────────────────────────────────

/**
 * Состояние загрузки информации о файловых системах.
 */
sealed class FileSystemListState {

    /** Устройство не выбрано или недоступно. */
    data object NoDevice : FileSystemListState()

    /** Идёт загрузка информации. */
    data object Loading : FileSystemListState()

    /**
     * Данные успешно загружены.
     *
     * @param partitions Все разделы файловой системы (включая нерелевантные).
     * @param summary    Агрегированная сводка по релевантным разделам.
     */
    data class Success(
        val partitions: List<StoragePartition>,
        val summary: StorageSummary,
    ) : FileSystemListState()

    /**
     * Ошибка загрузки.
     *
     * @param message Человекочитаемое описание ошибки.
     */
    data class Error(val message: String) : FileSystemListState()
}

// ── Корневое состояние экрана ─────────────────────────────────────────────────

/**
 * Полное состояние экрана «File System».
 *
 * @param listState Состояние загрузки данных о разделах файловой системы.
 */
data class FileSystemState(
    val listState: FileSystemListState = FileSystemListState.NoDevice,
)

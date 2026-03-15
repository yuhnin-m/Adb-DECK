package com.adbdeck.feature.filesystem

import com.adbdeck.core.adb.api.monitoring.storage.StoragePartition
import com.adbdeck.core.adb.api.monitoring.storage.StorageSummary

// ── Состояние экрана файловых систем ──────────────────────────────────────────

/**
 * Раздел файловой системы с дополнительной UI-информацией.
 *
 * @param partition Исходные данные раздела из `df`.
 * @param openPath Путь, который можно открыть в File Explorer, либо `null`, если раздел недоступен.
 */
data class FileSystemPartitionItem(
    val partition: StoragePartition,
    val openPath: String? = null,
)

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
        val partitions: List<FileSystemPartitionItem>,
        val summary: StorageSummary,
    ) : FileSystemListState()

    /**
     * Ошибка загрузки.
     *
     * @param message Человекочитаемое описание ошибки.
     */
    data class Error(val message: String) : FileSystemListState()
}

/**
 * Сводка по категориям контента из `dumpsys diskstats`.
 *
 * Значения хранятся в KB. Любое поле может быть `null`, если на устройстве его нет.
 */
data class ContentAnalysis(
    val appSizeKb: Long? = null,
    val appDataSizeKb: Long? = null,
    val appCacheSizeKb: Long? = null,
    val photosSizeKb: Long? = null,
    val videosSizeKb: Long? = null,
    val audioSizeKb: Long? = null,
    val downloadsSizeKb: Long? = null,
    val otherSizeKb: Long? = null,
    val systemSizeKb: Long? = null,
    val dataFreeKb: Long? = null,
    val dataTotalKb: Long? = null,
)

/**
 * Состояние загрузки блока «Анализ контента».
 */
sealed class ContentAnalysisState {
    data object Idle : ContentAnalysisState()
    data object Loading : ContentAnalysisState()
    data class Success(val analysis: ContentAnalysis) : ContentAnalysisState()
    data class Error(val message: String) : ContentAnalysisState()
}

/**
 * Опции очистки временных файлов.
 */
enum class CleanupOption {
    TEMP,
    DOWNLOADS,
    APP_CACHE,
}

/**
 * Состояния выполнения cleanup-процесса.
 */
enum class CleanupStatus {
    IDLE,
    RUNNING,
    SUCCESS,
    ERROR,
}

/**
 * Состояние child-окна очистки временных файлов.
 */
data class CleanupState(
    val isDialogOpen: Boolean = false,
    val isConfirmDialogOpen: Boolean = false,
    val selectedOptions: Set<CleanupOption> = setOf(
        CleanupOption.TEMP,
        CleanupOption.DOWNLOADS,
        CleanupOption.APP_CACHE,
    ),
    val running: Boolean = false,
    val status: CleanupStatus = CleanupStatus.IDLE,
    val progress: Float = 0f,
    val log: String = "",
)

// ── Корневое состояние экрана ─────────────────────────────────────────────────

/**
 * Полное состояние экрана «File System».
 *
 * @param listState Состояние загрузки данных о разделах файловой системы.
 * @param contentAnalysis Состояние блока «Анализ контента».
 * @param cleanup Состояние окна очистки временных файлов.
 */
data class FileSystemState(
    val listState: FileSystemListState = FileSystemListState.NoDevice,
    val contentAnalysis: ContentAnalysisState = ContentAnalysisState.Idle,
    val cleanup: CleanupState = CleanupState(),
)

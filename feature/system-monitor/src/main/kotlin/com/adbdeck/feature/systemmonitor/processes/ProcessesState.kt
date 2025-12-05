package com.adbdeck.feature.systemmonitor.processes

import com.adbdeck.core.adb.api.monitoring.ProcessInfo
import com.adbdeck.core.adb.api.monitoring.ProcessDetails
import com.adbdeck.core.adb.api.monitoring.SystemSnapshot

// ── Состояние списка процессов ────────────────────────────────────────────────

/**
 * Состояние загрузки списка процессов.
 */
sealed class ProcessListState {

    /** Устройство не выбрано или недоступно. */
    data object NoDevice : ProcessListState()

    /** Идёт загрузка / обновление списка. */
    data object Loading : ProcessListState()

    /**
     * Список успешно загружен.
     *
     * @param processes Все процессы без фильтрации, в порядке получения от ADB.
     */
    data class Success(val processes: List<ProcessInfo>) : ProcessListState()

    /**
     * Ошибка загрузки.
     *
     * @param message Человекочитаемое описание ошибки.
     */
    data class Error(val message: String) : ProcessListState()
}

// ── Состояние панели деталей процесса ─────────────────────────────────────────

/**
 * Состояние загрузки детальной информации о выбранном процессе.
 */
sealed class ProcessDetailState {

    /** Процесс не выбран. */
    data object Idle : ProcessDetailState()

    /** Загружаем детали. */
    data object Loading : ProcessDetailState()

    /**
     * Детали загружены.
     *
     * @param details Расширенная информация о процессе.
     */
    data class Success(val details: ProcessDetails) : ProcessDetailState()

    /**
     * Ошибка загрузки деталей.
     *
     * @param message Описание ошибки.
     */
    data class Error(val message: String) : ProcessDetailState()
}

// ── Вспомогательные типы ──────────────────────────────────────────────────────

/**
 * Поле сортировки списка процессов.
 */
enum class ProcessSortField {
    /** По имени процесса (A→Z). */
    NAME,

    /** По PID (возрастание). */
    PID,

    /** По RSS-памяти (убывание — самые прожорливые сверху). */
    MEMORY,

    /** По CPU% (убывание). */
    CPU,
}

/**
 * Краткосрочная обратная связь после ADB-действия над процессом.
 *
 * Автоматически убирается через 3 секунды (управляется компонентом).
 *
 * @param message Текст для отображения.
 * @param isError `true` → красный баннер (операция провалилась).
 */
data class ProcessActionFeedback(
    val message: String,
    val isError: Boolean,
)

// ── Корневое состояние экрана ─────────────────────────────────────────────────

/**
 * Полное состояние вкладки «Processes» в System Monitor.
 *
 * @param listState         Состояние загрузки списка процессов.
 * @param filteredProcesses Отфильтрованный / отсортированный список для отображения.
 * @param searchQuery       Текстовый фильтр (по имени пакета / процесса).
 * @param sortField         Поле сортировки.
 * @param isMonitoring      `true` пока активен периодический опрос.
 *                          Доступен через [com.adbdeck.feature.systemmonitor.SystemMonitorComponent.isProcessMonitoring]
 *                          для отображения badge в Sidebar.
 * @param activeDeviceId    ID устройства, с которого идёт мониторинг.
 * @param selectedProcess   Выбранный процесс (для панели деталей).
 * @param detailState       Состояние загрузки деталей выбранного процесса.
 * @param actionFeedback    Краткосрочное сообщение о результате последнего действия.
 * @param isActionRunning   Флаг выполняющегося ADB-действия (блокирует кнопки).
 * @param history           История системных метрик для графиков (ограничена [SystemSnapshot.HISTORY_LIMIT]).
 */
data class ProcessesState(
    val listState: ProcessListState = ProcessListState.NoDevice,
    val filteredProcesses: List<ProcessInfo> = emptyList(),
    val searchQuery: String = "",
    val sortField: ProcessSortField = ProcessSortField.CPU,
    val isMonitoring: Boolean = false,
    val activeDeviceId: String? = null,
    val selectedProcess: ProcessInfo? = null,
    val detailState: ProcessDetailState = ProcessDetailState.Idle,
    val actionFeedback: ProcessActionFeedback? = null,
    val isActionRunning: Boolean = false,
    val history: List<SystemSnapshot> = emptyList(),
)

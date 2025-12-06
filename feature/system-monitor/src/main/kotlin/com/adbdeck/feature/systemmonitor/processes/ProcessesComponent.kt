package com.adbdeck.feature.systemmonitor.processes

import com.adbdeck.core.adb.api.monitoring.process.ProcessInfo
import kotlinx.coroutines.flow.StateFlow

/**
 * Контракт компонента вкладки «Processes» в System Monitor.
 *
 * ## Жизненный цикл мониторинга
 *
 * Мониторинг продолжается в фоне при переходе на другую вкладку или другой
 * top-level экран (аналогично Logcat LIVE). При смене активного устройства
 * мониторинг автоматически останавливается и требует явного перезапуска.
 *
 * ## Потокобезопасность
 *
 * Все методы могут вызываться из UI-потока (Main dispatcher). Внутри компонента
 * они маршрутизируются на coroutineScope (Dispatchers.IO для ADB, Main для state).
 */
interface ProcessesComponent {

    /** Реактивное состояние вкладки. */
    val state: StateFlow<ProcessesState>

    // ── Управление мониторингом ──────────────────────────────────────────────

    /**
     * Запустить периодический мониторинг процессов.
     *
     * Запускает фоновый цикл опроса с интервалом [com.adbdeck.core.adb.api.monitoring.SystemSnapshot.POLL_INTERVAL_MS].
     * При отсутствии активного устройства — показывает ошибку.
     * Если мониторинг уже запущен — вызов игнорируется.
     */
    fun onStartMonitoring()

    /**
     * Остановить периодический мониторинг.
     *
     * Отменяет фоновый цикл опроса. Список процессов остаётся видимым (последнее состояние).
     */
    fun onStopMonitoring()

    /**
     * Однократно обновить список процессов (без запуска непрерывного мониторинга).
     */
    fun onRefresh()

    // ── Фильтрация и сортировка ──────────────────────────────────────────────

    /**
     * Изменить текстовый фильтр.
     *
     * @param query Строка поиска по имени процесса / пакета. Пустая строка — сбросить фильтр.
     */
    fun onSearchChanged(query: String)

    /**
     * Изменить поле сортировки.
     *
     * @param field [ProcessSortField.CPU], [ProcessSortField.MEMORY], [ProcessSortField.NAME], [ProcessSortField.PID].
     */
    fun onSortFieldChanged(field: ProcessSortField)

    // ── Выбор и детали ────────────────────────────────────────────────────────

    /**
     * Выбрать процесс и загрузить его детали в панель справа.
     *
     * @param process Процесс для отображения деталей.
     */
    fun onSelectProcess(process: ProcessInfo)

    /** Снять выделение (скрыть панель деталей). */
    fun onClearSelection()

    // ── Действия ─────────────────────────────────────────────────────────────

    /**
     * Принудительно завершить выбранный процесс (`kill -9 <pid>`).
     *
     * @param process Процесс для завершения.
     */
    fun onKillProcess(process: ProcessInfo)

    /**
     * Force-stop Android-приложения (`am force-stop <package>`).
     *
     * Доступно только если [ProcessInfo.looksLikePackage] == `true`.
     *
     * @param process Процесс с именем пакета.
     */
    fun onForceStopApp(process: ProcessInfo)

    /**
     * Открыть пакет процесса на экране Packages с раскрытой карточкой деталей.
     *
     * @param process Процесс-приложение, для которого нужно открыть пакет.
     */
    fun onOpenPackageDetails(process: ProcessInfo)

    /** Скрыть текущий баннер обратной связи ([ProcessesState.actionFeedback]). */
    fun onDismissFeedback()
}

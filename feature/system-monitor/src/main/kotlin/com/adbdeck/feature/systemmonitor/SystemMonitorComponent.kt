package com.adbdeck.feature.systemmonitor

import com.adbdeck.feature.systemmonitor.processes.ProcessesComponent
import kotlinx.coroutines.flow.StateFlow

/**
 * Контракт корневого компонента экрана System Monitor.
 *
 * Агрегирует компонент мониторинга процессов:
 * - [processesComponent] — экран «Processes» (Task Manager)
 *
 * ## Sidebar badge
 *
 * [isProcessMonitoring] транслирует флаг мониторинга из [processesComponent]
 * в виде отдельного StateFlow. Это позволяет [com.adbdeck.app.ui.Sidebar] подписаться
 * на badge «MON» без необходимости знать о внутренней структуре вкладок.
 */
interface SystemMonitorComponent {

    /**
     * `true` пока активен периодический мониторинг процессов.
     *
     * Используется [com.adbdeck.app.ui.Sidebar] для отображения badge «MON».
     */
    val isProcessMonitoring: StateFlow<Boolean>

    /** Дочерний компонент вкладки «Processes». */
    val processesComponent: ProcessesComponent
}

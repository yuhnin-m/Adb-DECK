package com.adbdeck.feature.systemmonitor

import com.adbdeck.feature.systemmonitor.processes.ProcessesComponent
import com.adbdeck.feature.systemmonitor.storage.StorageComponent
import kotlinx.coroutines.flow.StateFlow

/**
 * Вкладки экрана System Monitor.
 */
enum class SystemMonitorTab {
    /** Диспетчер задач — список процессов с CPU/RAM-метриками. */
    PROCESSES,

    /** Информация о хранилище устройства (df). */
    STORAGE,
}

/**
 * Контракт корневого компонента экрана System Monitor.
 *
 * Агрегирует два дочерних компонента:
 * - [processesComponent] — вкладка «Processes» (Task Manager)
 * - [storageComponent]   — вкладка «Storage» (df-информация)
 *
 * ## Sidebar badge
 *
 * [isProcessMonitoring] транслирует флаг мониторинга из [processesComponent]
 * в виде отдельного StateFlow. Это позволяет [com.adbdeck.app.ui.Sidebar] подписаться
 * на badge «MON» без необходимости знать о внутренней структуре вкладок.
 *
 * ## Навигация по вкладкам
 *
 * Переключение вкладок — чисто UI-состояние: [activeTab] / [onTabSelected].
 * Оба sub-компонента остаются живыми при смене вкладки (coroutineScope не останавливается).
 */
interface SystemMonitorComponent {

    /** Активная вкладка (для TabRow в UI). */
    val activeTab: StateFlow<SystemMonitorTab>

    /**
     * `true` пока активен периодический мониторинг процессов.
     *
     * Используется [com.adbdeck.app.ui.Sidebar] для отображения badge «MON».
     */
    val isProcessMonitoring: StateFlow<Boolean>

    /** Дочерний компонент вкладки «Processes». */
    val processesComponent: ProcessesComponent

    /** Дочерний компонент вкладки «Storage». */
    val storageComponent: StorageComponent

    /**
     * Переключить активную вкладку.
     *
     * @param tab Целевая вкладка.
     */
    fun onTabSelected(tab: SystemMonitorTab)
}

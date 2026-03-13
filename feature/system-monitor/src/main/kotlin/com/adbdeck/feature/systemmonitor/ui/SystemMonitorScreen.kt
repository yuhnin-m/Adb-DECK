package com.adbdeck.feature.systemmonitor.ui

import androidx.compose.runtime.Composable
import com.adbdeck.feature.systemmonitor.SystemMonitorComponent

/**
 * Корневой composable экрана System Monitor.
 *
 * Экран состоит из одного раздела мониторинга процессов.
 *
 * @param component Корневой компонент экрана.
 */
@Composable
fun SystemMonitorScreen(component: SystemMonitorComponent) {
    ProcessesScreen(component = component.processesComponent)
}

package com.adbdeck.feature.systemmonitor.ui

import adbdeck.feature.system_monitor.generated.resources.Res
import adbdeck.feature.system_monitor.generated.resources.system_monitor_tab_processes
import adbdeck.feature.system_monitor.generated.resources.system_monitor_tab_storage
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.adbdeck.feature.systemmonitor.SystemMonitorComponent
import com.adbdeck.feature.systemmonitor.SystemMonitorTab
import org.jetbrains.compose.resources.stringResource

/**
 * Корневой composable экрана System Monitor.
 *
 * Отображает [TabRow] с двумя вкладками и переключает контент
 * между [ProcessesScreen] и [StorageScreen].
 *
 * Оба sub-компонента остаются живыми при переключении вкладки
 * (мониторинг процессов продолжается в фоне).
 *
 * @param component Корневой компонент экрана.
 */
@Composable
fun SystemMonitorScreen(component: SystemMonitorComponent) {
    val activeTab by component.activeTab.collectAsState()
    val processesTabLabel = stringResource(Res.string.system_monitor_tab_processes)
    val storageTabLabel = stringResource(Res.string.system_monitor_tab_storage)

    Column(modifier = Modifier.fillMaxSize()) {
        // ── Tab selector ─────────────────────────────────────────────
        TabRow(selectedTabIndex = activeTab.ordinal) {
            SystemMonitorTab.entries.forEach { tab ->
                Tab(
                    selected = activeTab == tab,
                    onClick = { component.onTabSelected(tab) },
                    text = {
                        Text(
                            text = when (tab) {
                                SystemMonitorTab.PROCESSES -> processesTabLabel
                                SystemMonitorTab.STORAGE   -> storageTabLabel
                            }
                        )
                    }
                )
            }
        }
        HorizontalDivider()

        // ── Контент активной вкладки ──────────────────────────────────
        when (activeTab) {
            SystemMonitorTab.PROCESSES ->
                ProcessesScreen(component = component.processesComponent)

            SystemMonitorTab.STORAGE ->
                StorageScreen(component = component.storageComponent)
        }
    }
}

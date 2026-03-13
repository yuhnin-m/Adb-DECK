package com.adbdeck.feature.systemmonitor.ui

import androidx.compose.runtime.Composable
import com.adbdeck.core.adb.api.monitoring.SystemSnapshot
import com.adbdeck.core.adb.api.monitoring.process.*
import com.adbdeck.core.designsystem.AdbDeckTheme
import com.adbdeck.feature.systemmonitor.SystemMonitorComponent
import com.adbdeck.feature.systemmonitor.processes.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.jetbrains.compose.ui.tooling.preview.Preview

// ── Preview-стабы ─────────────────────────────────────────────────────────────

private val sampleProcesses = listOf(
    ProcessInfo(pid = 1234, user = "u0_a100", name = "com.example.app",
        packageName = "com.example.app", state = ProcessState.RUNNING,
        cpuPercent = 12.5f, memPercent = 3.2f, rssKb = 45_678L, vszKb = 234_000L),
    ProcessInfo(pid = 5678, user = "system", name = "system_server",
        state = ProcessState.SLEEPING, cpuPercent = 2.1f, rssKb = 120_000L, vszKb = 980_000L),
    ProcessInfo(pid = 9012, user = "root", name = "kworker/0:1",
        state = ProcessState.IDLE, cpuPercent = 0f, rssKb = 0L, vszKb = 0L),
    ProcessInfo(pid = 3456, user = "u0_a200", name = "com.google.android.gms",
        packageName = "com.google.android.gms", state = ProcessState.SLEEPING,
        cpuPercent = 5.0f, memPercent = 8.0f, rssKb = 180_000L, vszKb = 1_200_000L),
)

private val sampleHistory = (1..30).map { i ->
    SystemSnapshot(
        cpuPercent = 20f + 15f * kotlin.math.sin(i * 0.4f),
        usedRamKb  = 2_000_000L + (i * 10_000L),
        totalRamKb = 6_000_000L,
    )
}

// ── ProcessesComponent stub ───────────────────────────────────────────────────

private class PreviewProcessesComponent : ProcessesComponent {
    override val state: StateFlow<ProcessesState> = MutableStateFlow(
        ProcessesState(
            listState        = ProcessListState.Success(sampleProcesses),
            filteredProcesses = sampleProcesses,
            sortField        = ProcessSortField.CPU,
            isMonitoring     = true,
            history          = sampleHistory,
        )
    )
    override fun onStartMonitoring() = Unit
    override fun onStopMonitoring()  = Unit
    override fun onRefresh()         = Unit
    override fun onSearchChanged(query: String) = Unit
    override fun onSortFieldChanged(field: ProcessSortField) = Unit
    override fun onSelectProcess(process: ProcessInfo) = Unit
    override fun onClearSelection()  = Unit
    override fun onKillProcess(process: ProcessInfo) = Unit
    override fun onForceStopApp(process: ProcessInfo) = Unit
    override fun onOpenPackageDetails(process: ProcessInfo) = Unit
    override fun onDismissFeedback() = Unit
}

// ── SystemMonitorComponent stub ───────────────────────────────────────────────

private class PreviewSystemMonitorComponent : SystemMonitorComponent {
    override val isProcessMonitoring: StateFlow<Boolean> = MutableStateFlow(true)
    override val processesComponent: ProcessesComponent = PreviewProcessesComponent()
}

// ── Previews ─────────────────────────────────────────────────────────────────

@Preview
@Composable
private fun SystemMonitorScreenPreview() {
    AdbDeckTheme(isDarkTheme = false) {
        SystemMonitorScreen(component = PreviewSystemMonitorComponent())
    }
}

@Preview
@Composable
private fun SystemMonitorScreenDarkPreview() {
    AdbDeckTheme(isDarkTheme = true) {
        SystemMonitorScreen(component = PreviewSystemMonitorComponent())
    }
}

@Preview
@Composable
private fun ProcessesScreenPreview() {
    AdbDeckTheme(isDarkTheme = false) {
        ProcessesScreen(component = PreviewProcessesComponent())
    }
}

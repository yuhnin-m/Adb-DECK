package com.adbdeck.feature.systemmonitor.ui

import org.jetbrains.compose.ui.tooling.preview.Preview
import androidx.compose.runtime.Composable
import com.adbdeck.core.adb.api.*
import com.adbdeck.core.designsystem.AdbDeckTheme
import com.adbdeck.feature.systemmonitor.SystemMonitorComponent
import com.adbdeck.feature.systemmonitor.SystemMonitorTab
import com.adbdeck.feature.systemmonitor.processes.*
import com.adbdeck.feature.systemmonitor.storage.StorageComponent
import com.adbdeck.feature.systemmonitor.storage.StorageListState
import com.adbdeck.feature.systemmonitor.storage.StorageState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

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

private val samplePartitions = listOf(
    StoragePartition(
        filesystem = "/dev/block/sda1", totalKb = 52_428_800L,
        usedKb = 10_485_760L, freeKb = 41_943_040L, usedPercent = 20,
        mountPoint = "/system"
    ),
    StoragePartition(
        filesystem = "/dev/block/sda2", totalKb = 104_857_600L,
        usedKb = 73_400_320L, freeKb = 31_457_280L, usedPercent = 70,
        mountPoint = "/data"
    ),
    StoragePartition(
        filesystem = "/dev/fuse", totalKb = 52_428_800L,
        usedKb = 20_971_520L, freeKb = 31_457_280L, usedPercent = 40,
        mountPoint = "/sdcard"
    ),
)

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

// ── StorageComponent stub ─────────────────────────────────────────────────────

private class PreviewStorageComponent : StorageComponent {
    override val state: StateFlow<StorageState> = MutableStateFlow(
        StorageState(
            listState = StorageListState.Success(
                partitions = samplePartitions,
                summary    = StorageSummary(
                    totalKb = samplePartitions.sumOf { it.totalKb },
                    usedKb  = samplePartitions.sumOf { it.usedKb },
                    freeKb  = samplePartitions.sumOf { it.freeKb },
                ),
            )
        )
    )
    override fun onRefresh() = Unit
}

// ── SystemMonitorComponent stub ───────────────────────────────────────────────

private class PreviewSystemMonitorComponent(
    override val activeTab: StateFlow<SystemMonitorTab> = MutableStateFlow(SystemMonitorTab.PROCESSES),
) : SystemMonitorComponent {
    override val isProcessMonitoring: StateFlow<Boolean> = MutableStateFlow(true)
    override val processesComponent: ProcessesComponent = PreviewProcessesComponent()
    override val storageComponent: StorageComponent     = PreviewStorageComponent()
    override fun onTabSelected(tab: SystemMonitorTab)   = Unit
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
private fun StorageTabPreview() {
    AdbDeckTheme(isDarkTheme = false) {
        SystemMonitorScreen(
            component = PreviewSystemMonitorComponent(
                activeTab = MutableStateFlow(SystemMonitorTab.STORAGE)
            )
        )
    }
}

@Preview
@Composable
private fun ProcessesScreenPreview() {
    AdbDeckTheme(isDarkTheme = false) {
        ProcessesScreen(component = PreviewProcessesComponent())
    }
}

@Preview
@Composable
private fun StorageScreenPreview() {
    AdbDeckTheme(isDarkTheme = false) {
        StorageScreen(component = PreviewStorageComponent())
    }
}

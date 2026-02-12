package com.adbdeck.feature.systemmonitor.processes

import com.adbdeck.core.adb.api.device.DeviceManager
import com.adbdeck.core.adb.api.monitoring.SystemMonitorClient
import com.adbdeck.core.adb.api.monitoring.SystemSnapshot
import com.adbdeck.core.adb.api.packages.PackageClient
import com.adbdeck.core.settings.SettingsRepository
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.lifecycle.coroutines.coroutineScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex

/**
 * Реализация [ProcessesComponent].
 *
 * Файл содержит только wiring и состояние. Поведение разбито на extension-файлы:
 * - monitoring/snapshot
 * - filters
 * - details
 * - actions/feedback
 */
class DefaultProcessesComponent(
    componentContext: ComponentContext,
    internal val deviceManager: DeviceManager,
    internal val systemMonitorClient: SystemMonitorClient,
    internal val packageClient: PackageClient,
    internal val settingsRepository: SettingsRepository,
    internal val openPackageDetails: (String) -> Unit = {},
) : ProcessesComponent, ComponentContext by componentContext {

    internal val scope: CoroutineScope = coroutineScope()

    internal val _state = MutableStateFlow(ProcessesState())
    override val state: StateFlow<ProcessesState> = _state.asStateFlow()

    internal var monitoringJob: Job? = null
    internal var snapshotJob: Job? = null
    internal var detailJob: Job? = null
    internal var actionJob: Job? = null
    internal var feedbackJob: Job? = null

    internal val historyBuffer = ArrayDeque<SystemSnapshot>(SystemSnapshot.HISTORY_LIMIT + 1)
    internal val snapshotFetchMutex = Mutex()

    init {
        observeSelectedDevice()
    }

    override fun onStartMonitoring() {
        handleStartMonitoring()
    }

    override fun onStopMonitoring() {
        handleStopMonitoring()
    }

    override fun onRefresh() {
        handleRefresh()
    }

    override fun onSearchChanged(query: String) {
        handleSearchChanged(query)
    }

    override fun onSortFieldChanged(field: ProcessSortField) {
        handleSortFieldChanged(field)
    }

    override fun onSelectProcess(process: com.adbdeck.core.adb.api.monitoring.process.ProcessInfo) {
        handleSelectProcess(process)
    }

    override fun onClearSelection() {
        handleClearSelection()
    }

    override fun onKillProcess(process: com.adbdeck.core.adb.api.monitoring.process.ProcessInfo) {
        handleKillProcess(process)
    }

    override fun onForceStopApp(process: com.adbdeck.core.adb.api.monitoring.process.ProcessInfo) {
        handleForceStopApp(process)
    }

    override fun onOpenPackageDetails(process: com.adbdeck.core.adb.api.monitoring.process.ProcessInfo) {
        handleOpenPackageDetails(process)
    }

    override fun onDismissFeedback() {
        handleDismissFeedback()
    }
}

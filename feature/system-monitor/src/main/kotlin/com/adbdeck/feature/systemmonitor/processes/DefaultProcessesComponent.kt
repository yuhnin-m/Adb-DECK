package com.adbdeck.feature.systemmonitor.processes

import com.adbdeck.core.adb.api.device.AdbDevice
import com.adbdeck.core.adb.api.device.DeviceManager
import com.adbdeck.core.adb.api.device.DeviceState
import com.adbdeck.core.adb.api.packages.PackageClient
import com.adbdeck.core.adb.api.monitoring.ProcessInfo
import com.adbdeck.core.adb.api.monitoring.SystemMonitorClient
import com.adbdeck.core.adb.api.monitoring.SystemSnapshot
import com.adbdeck.core.settings.SettingsRepository
import com.adbdeck.core.utils.runCatchingPreserveCancellation
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.lifecycle.coroutines.coroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Реализация [ProcessesComponent].
 *
 * Ключевые принципы:
 * - любой ответ ADB применяется только если запрос ещё актуален для выбранного deviceId
 * - при ошибке получения snapshot или пустом наборе процессов мониторинг останавливается
 *   и состояние сбрасывается в placeholder/error
 * - действия (kill/force-stop) сериализованы через единый runAction
 */
class DefaultProcessesComponent(
    componentContext: ComponentContext,
    private val deviceManager: DeviceManager,
    private val systemMonitorClient: SystemMonitorClient,
    private val packageClient: PackageClient,
    private val settingsRepository: SettingsRepository,
    private val openPackageDetails: (String) -> Unit = {},
) : ProcessesComponent, ComponentContext by componentContext {

    private val scope = coroutineScope()

    private val _state = MutableStateFlow(ProcessesState())
    override val state: StateFlow<ProcessesState> = _state.asStateFlow()

    private var monitoringJob: Job? = null
    private var snapshotJob: Job? = null
    private var detailJob: Job? = null
    private var actionJob: Job? = null
    private var feedbackJob: Job? = null

    private val historyBuffer = ArrayDeque<SystemSnapshot>(SystemSnapshot.HISTORY_LIMIT + 1)

    init {
        scope.launch {
            deviceManager.selectedDeviceFlow.collect { device ->
                handleDeviceChange(device)
            }
        }
    }

    // ── Управление мониторингом ───────────────────────────────────────────────

    override fun onStartMonitoring() {
        val device = deviceManager.selectedDeviceFlow.value
        if (device == null) {
            resetToNoDevice()
            showFeedback("Нет активного устройства", isError = true)
            return
        }
        if (device.state != DeviceState.DEVICE) {
            resetToDeviceError(device)
            showFeedback("Устройство недоступно (${device.state.rawValue})", isError = true)
            return
        }
        if (monitoringJob?.isActive == true) return

        _state.update {
            it.copy(
                isMonitoring = true,
                activeDeviceId = device.deviceId,
                listState = if (it.listState is ProcessListState.NoDevice) ProcessListState.Loading else it.listState,
            )
        }

        monitoringJob = scope.launch {
            while (isActive) {
                val ok = fetchSnapshot(device.deviceId, stopMonitoringOnFailure = true)
                if (!ok) break
                delay(SystemSnapshot.POLL_INTERVAL_MS)
            }
        }
    }

    override fun onStopMonitoring() {
        monitoringJob?.cancel()
        monitoringJob = null
        _state.update { it.copy(isMonitoring = false) }
    }

    override fun onRefresh() {
        val device = deviceManager.selectedDeviceFlow.value
        if (device == null) {
            resetToNoDevice()
            return
        }
        if (device.state != DeviceState.DEVICE) {
            resetToDeviceError(device)
            return
        }

        _state.update {
            it.copy(
                activeDeviceId = device.deviceId,
                listState = ProcessListState.Loading,
            )
        }
        launchSnapshot(device.deviceId)
    }

    private fun handleDeviceChange(device: AdbDevice?) {
        when {
            device == null -> {
                cancelAllJobs()
                resetToNoDevice()
            }

            device.state != DeviceState.DEVICE -> {
                cancelAllJobs()
                resetToDeviceError(device)
            }

            else -> {
                val current = _state.value
                val deviceChanged = current.activeDeviceId != device.deviceId
                if (deviceChanged) {
                    cancelAllJobs()
                    historyBuffer.clear()
                    _state.update {
                        ProcessesState(
                            listState = ProcessListState.Loading,
                            activeDeviceId = device.deviceId,
                            isMonitoring = false,
                        )
                    }
                    launchSnapshot(device.deviceId)
                } else if (current.listState is ProcessListState.NoDevice || current.listState is ProcessListState.Error) {
                    _state.update { it.copy(listState = ProcessListState.Loading) }
                    launchSnapshot(device.deviceId)
                }
            }
        }
    }

    private fun resetToNoDevice() {
        historyBuffer.clear()
        _state.update {
            ProcessesState(
                listState = ProcessListState.NoDevice,
            )
        }
    }

    private fun resetToDeviceError(device: AdbDevice) {
        historyBuffer.clear()
        _state.update {
            ProcessesState(
                listState = ProcessListState.Error("Устройство недоступно (${device.state.rawValue})"),
                activeDeviceId = device.deviceId,
                isMonitoring = false,
            )
        }
    }

    private fun cancelAllJobs() {
        monitoringJob?.cancel()
        monitoringJob = null

        snapshotJob?.cancel()
        snapshotJob = null

        detailJob?.cancel()
        detailJob = null

        actionJob?.cancel()
        actionJob = null

        feedbackJob?.cancel()
        feedbackJob = null
    }

    private fun launchSnapshot(deviceId: String) {
        snapshotJob?.cancel()
        snapshotJob = scope.launch {
            fetchSnapshot(deviceId, stopMonitoringOnFailure = true)
        }
    }

    // ── Загрузка snapshot ─────────────────────────────────────────────────────

    private suspend fun fetchSnapshot(
        deviceId: String,
        stopMonitoringOnFailure: Boolean,
    ): Boolean {
        if (!isRequestStillValid(deviceId)) return false

        val adbPath = settingsRepository.getSettings().adbPath.ifBlank { "adb" }
        val result = runCatchingPreserveCancellation {
            systemMonitorClient.getProcessSnapshot(deviceId, adbPath)
        }.getOrElse { e -> Result.failure(e) }

        if (!isRequestStillValid(deviceId)) return false

        return result.fold(
            onSuccess = { snapshot ->
                if (snapshot.processes.isEmpty()) {
                    failSnapshot(
                        deviceId = deviceId,
                        message = "ADB не вернул список процессов",
                        stopMonitoringOnFailure = stopMonitoringOnFailure,
                    )
                    false
                } else {
                    applySnapshot(snapshot)
                    true
                }
            },
            onFailure = { e ->
                failSnapshot(
                    deviceId = deviceId,
                    message = e.message ?: "Ошибка получения процессов",
                    stopMonitoringOnFailure = stopMonitoringOnFailure,
                )
                false
            },
        )
    }

    private fun applySnapshot(snapshot: com.adbdeck.core.adb.api.monitoring.ProcessSnapshot) {
        val historyPoint = SystemSnapshot(
            cpuPercent = snapshot.systemCpuPercent,
            usedRamKb = snapshot.usedRamKb,
            totalRamKb = snapshot.totalRamKb,
        )
        historyBuffer.addLast(historyPoint)
        while (historyBuffer.size > SystemSnapshot.HISTORY_LIMIT) {
            historyBuffer.removeFirst()
        }
        val historySnapshot = historyBuffer.toList()

        _state.update { current ->
            val selectedPid = current.selectedProcess?.pid
            val updatedSelected = selectedPid?.let { pid ->
                snapshot.processes.firstOrNull { it.pid == pid }
            }
            val hasSelected = updatedSelected != null

            val filtered = applyFilters(
                processes = snapshot.processes,
                query = current.searchQuery,
                sortField = current.sortField,
            )

            current.copy(
                listState = ProcessListState.Success(snapshot.processes),
                filteredProcesses = filtered,
                selectedProcess = updatedSelected,
                detailState = if (hasSelected) current.detailState else ProcessDetailState.Idle,
                history = historySnapshot,
            )
        }
    }

    private fun failSnapshot(
        deviceId: String,
        message: String,
        stopMonitoringOnFailure: Boolean,
    ) {
        if (!isRequestStillValid(deviceId)) return

        if (stopMonitoringOnFailure) {
            monitoringJob?.cancel()
            monitoringJob = null
            detailJob?.cancel()
            detailJob = null
            actionJob?.cancel()
            actionJob = null
            historyBuffer.clear()

            _state.update {
                ProcessesState(
                    listState = ProcessListState.Error(message),
                    activeDeviceId = deviceId,
                    isMonitoring = false,
                )
            }
        } else {
            _state.update {
                it.copy(
                    listState = ProcessListState.Error(message),
                    filteredProcesses = emptyList(),
                    selectedProcess = null,
                    detailState = ProcessDetailState.Idle,
                    history = emptyList(),
                )
            }
        }
    }

    private fun isRequestStillValid(deviceId: String): Boolean {
        val selected = deviceManager.selectedDeviceFlow.value
        return selected != null &&
            selected.state == DeviceState.DEVICE &&
            selected.deviceId == deviceId &&
            _state.value.activeDeviceId == deviceId
    }

    // ── Фильтрация ────────────────────────────────────────────────────────────

    override fun onSearchChanged(query: String) {
        _state.update { current ->
            val processes = (current.listState as? ProcessListState.Success)?.processes ?: emptyList()
            current.copy(
                searchQuery = query,
                filteredProcesses = applyFilters(processes, query, current.sortField),
            )
        }
    }

    override fun onSortFieldChanged(field: ProcessSortField) {
        _state.update { current ->
            val processes = (current.listState as? ProcessListState.Success)?.processes ?: emptyList()
            current.copy(
                sortField = field,
                filteredProcesses = applyFilters(processes, current.searchQuery, field),
            )
        }
    }

    private fun applyFilters(
        processes: List<ProcessInfo>,
        query: String,
        sortField: ProcessSortField,
    ): List<ProcessInfo> {
        val q = query.trim()
        val filtered = if (q.isEmpty()) {
            processes
        } else {
            processes.filter { p ->
                p.name.contains(q, ignoreCase = true) ||
                    p.packageName.contains(q, ignoreCase = true) ||
                    p.user.contains(q, ignoreCase = true) ||
                    p.pid.toString() == q
            }
        }

        return when (sortField) {
            ProcessSortField.CPU -> filtered.sortedByDescending { it.cpuPercent }
            ProcessSortField.MEMORY -> filtered.sortedByDescending { it.rssKb }
            ProcessSortField.NAME -> filtered.sortedBy { it.displayName.lowercase() }
            ProcessSortField.PID -> filtered.sortedBy { it.pid }
        }
    }

    // ── Детали ────────────────────────────────────────────────────────────────

    override fun onSelectProcess(process: ProcessInfo) {
        detailJob?.cancel()
        _state.update { it.copy(selectedProcess = process, detailState = ProcessDetailState.Loading) }

        val selected = deviceManager.selectedDeviceFlow.value
        if (selected == null || selected.state != DeviceState.DEVICE) {
            _state.update { it.copy(detailState = ProcessDetailState.Error("Нет активного устройства")) }
            return
        }

        val deviceId = selected.deviceId
        val adbPath = settingsRepository.getSettings().adbPath.ifBlank { "adb" }

        detailJob = scope.launch {
            val result = systemMonitorClient.getProcessDetails(deviceId, process.pid, adbPath)
            if (!isRequestStillValid(deviceId)) return@launch

            _state.update { current ->
                if (current.selectedProcess?.pid != process.pid) {
                    current
                } else {
                    current.copy(
                        detailState = result.fold(
                            onSuccess = { ProcessDetailState.Success(it) },
                            onFailure = { ProcessDetailState.Error(it.message ?: "Ошибка загрузки деталей") },
                        ),
                    )
                }
            }
        }
    }

    override fun onClearSelection() {
        detailJob?.cancel()
        detailJob = null
        _state.update { it.copy(selectedProcess = null, detailState = ProcessDetailState.Idle) }
    }

    // ── Действия ─────────────────────────────────────────────────────────────

    override fun onKillProcess(process: ProcessInfo) {
        runAction(successMessage = "kill -9 ${process.pid} выполнен") { deviceId, adbPath ->
            systemMonitorClient.killProcess(deviceId, process.pid, adbPath)
        }
    }

    override fun onForceStopApp(process: ProcessInfo) {
        if (!process.looksLikePackage) {
            showFeedback("Процесс '${process.name}' не является Android-приложением", isError = true)
            return
        }

        val packageName = process.packageName.ifEmpty { process.name }
        runAction(successMessage = "Force-stop выполнен: $packageName") { deviceId, adbPath ->
            packageClient.forceStop(deviceId, packageName, adbPath)
        }
    }

    override fun onOpenPackageDetails(process: ProcessInfo) {
        if (!process.looksLikePackage) {
            showFeedback("Для этого процесса нет Android package", isError = true)
            return
        }
        val packageName = process.packageName.ifEmpty { process.name }
        openPackageDetails(packageName)
    }

    private fun runAction(
        successMessage: String,
        block: suspend (deviceId: String, adbPath: String) -> Result<Unit>?,
    ) {
        val selected = deviceManager.selectedDeviceFlow.value
        if (selected == null || selected.state != DeviceState.DEVICE) {
            showFeedback("Нет активного устройства", isError = true)
            return
        }

        val deviceId = selected.deviceId
        val adbPath = settingsRepository.getSettings().adbPath.ifBlank { "adb" }

        var shouldStart = false
        _state.update { current ->
            if (current.isActionRunning) {
                current
            } else {
                shouldStart = true
                current.copy(isActionRunning = true)
            }
        }
        if (!shouldStart) return

        actionJob?.cancel()
        actionJob = scope.launch {
            try {
                val result = block(deviceId, adbPath) ?: return@launch
                if (!isRequestStillValid(deviceId)) return@launch

                result
                    .onSuccess {
                        showFeedback(successMessage, isError = false)
                        launchSnapshot(deviceId)
                    }
                    .onFailure { e ->
                        showFeedback(e.message ?: "Ошибка выполнения действия", isError = true)
                    }
            } finally {
                _state.update { it.copy(isActionRunning = false) }
                actionJob = null
            }
        }
    }

    override fun onDismissFeedback() {
        feedbackJob?.cancel()
        feedbackJob = null
        _state.update { it.copy(actionFeedback = null) }
    }

    private fun showFeedback(message: String, isError: Boolean) {
        feedbackJob?.cancel()
        _state.update { it.copy(actionFeedback = ProcessActionFeedback(message, isError)) }
        feedbackJob = scope.launch {
            delay(3_000L)
            _state.update { current ->
                if (current.actionFeedback?.message == message) {
                    current.copy(actionFeedback = null)
                } else {
                    current
                }
            }
        }
    }
}

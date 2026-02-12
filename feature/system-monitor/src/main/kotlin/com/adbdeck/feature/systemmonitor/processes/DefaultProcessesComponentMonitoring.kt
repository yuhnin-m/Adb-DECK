package com.adbdeck.feature.systemmonitor.processes

import adbdeck.feature.system_monitor.generated.resources.Res
import adbdeck.feature.system_monitor.generated.resources.system_monitor_error_device_unavailable
import adbdeck.feature.system_monitor.generated.resources.system_monitor_error_no_active_device
import adbdeck.feature.system_monitor.generated.resources.system_monitor_error_process_fetch_failed
import adbdeck.feature.system_monitor.generated.resources.system_monitor_error_process_list_empty
import adbdeck.feature.system_monitor.generated.resources.system_monitor_feedback_monitoring_stopped
import com.adbdeck.core.adb.api.device.AdbDevice
import com.adbdeck.core.adb.api.device.DeviceState
import com.adbdeck.core.adb.api.monitoring.SystemSnapshot
import com.adbdeck.core.adb.api.monitoring.process.ProcessSnapshot
import com.adbdeck.core.utils.runCatchingPreserveCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.withLock
import org.jetbrains.compose.resources.getString

internal fun DefaultProcessesComponent.observeSelectedDevice() {
    scope.launch {
        deviceManager.selectedDeviceFlow.collect { device ->
            handleDeviceChange(device)
        }
    }
}

internal fun DefaultProcessesComponent.handleStartMonitoring() {
    val device = deviceManager.selectedDeviceFlow.value
    if (device == null) {
        resetToNoDevice()
        showFeedbackResource(
            messageRes = Res.string.system_monitor_error_no_active_device,
            isError = true,
        )
        return
    }
    if (device.state != DeviceState.DEVICE) {
        resetToDeviceError(device)
        showFeedbackResource(
            messageRes = Res.string.system_monitor_error_device_unavailable,
            isError = true,
            device.state.rawValue,
        )
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

internal fun DefaultProcessesComponent.handleStopMonitoring() {
    monitoringJob?.cancel()
    monitoringJob = null
    _state.update { it.copy(isMonitoring = false) }
}

internal fun DefaultProcessesComponent.handleRefresh() {
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
    launchSnapshot(device.deviceId, stopMonitoringOnFailure = false)
}

internal fun DefaultProcessesComponent.handleDeviceChange(device: AdbDevice?) {
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
                launchSnapshot(device.deviceId, stopMonitoringOnFailure = false)
            } else if (current.listState is ProcessListState.NoDevice || current.listState is ProcessListState.Error) {
                _state.update { it.copy(listState = ProcessListState.Loading) }
                launchSnapshot(device.deviceId, stopMonitoringOnFailure = false)
            }
        }
    }
}

internal fun DefaultProcessesComponent.resetToNoDevice() {
    historyBuffer.clear()
    _state.update {
        ProcessesState(
            listState = ProcessListState.NoDevice,
        )
    }
}

internal fun DefaultProcessesComponent.resetToDeviceError(device: AdbDevice) {
    scope.launch {
        val message = getString(
            Res.string.system_monitor_error_device_unavailable,
            device.state.rawValue,
        )
        val selected = deviceManager.selectedDeviceFlow.value
        if (selected == null || selected.deviceId != device.deviceId || selected.state == DeviceState.DEVICE) {
            return@launch
        }
        historyBuffer.clear()
        _state.update {
            ProcessesState(
                listState = ProcessListState.Error(message),
                activeDeviceId = device.deviceId,
                isMonitoring = false,
            )
        }
    }
}

internal fun DefaultProcessesComponent.cancelAllJobs() {
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

internal fun DefaultProcessesComponent.launchSnapshot(
    deviceId: String,
    stopMonitoringOnFailure: Boolean,
) {
    snapshotJob?.cancel()
    snapshotJob = scope.launch {
        fetchSnapshot(deviceId, stopMonitoringOnFailure = stopMonitoringOnFailure)
    }
}

internal suspend fun DefaultProcessesComponent.fetchSnapshot(
    deviceId: String,
    stopMonitoringOnFailure: Boolean,
): Boolean = snapshotFetchMutex.withLock {
    if (!isRequestStillValid(deviceId)) return@withLock false

    val adbPath = settingsRepository.getSettings().adbPath.ifBlank { "adb" }
    val result = runCatchingPreserveCancellation {
        systemMonitorClient.getProcessSnapshot(deviceId, adbPath)
    }.getOrElse { e -> Result.failure(e) }

    if (!isRequestStillValid(deviceId)) return@withLock false

    result.fold(
        onSuccess = { snapshot ->
            if (snapshot.processes.isEmpty()) {
                val message = getString(Res.string.system_monitor_error_process_list_empty)
                failSnapshot(
                    deviceId = deviceId,
                    message = message,
                    stopMonitoringOnFailure = stopMonitoringOnFailure,
                )
                false
            } else {
                applySnapshot(snapshot)
                true
            }
        },
        onFailure = { e ->
            val fallbackMessage = getString(Res.string.system_monitor_error_process_fetch_failed)
            failSnapshot(
                deviceId = deviceId,
                message = e.message ?: fallbackMessage,
                stopMonitoringOnFailure = stopMonitoringOnFailure,
            )
            false
        },
    )
}

internal fun DefaultProcessesComponent.applySnapshot(snapshot: ProcessSnapshot) {
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

internal fun DefaultProcessesComponent.failSnapshot(
    deviceId: String,
    message: String,
    stopMonitoringOnFailure: Boolean,
) {
    if (!isRequestStillValid(deviceId)) return
    val hasCachedData = _state.value.listState is ProcessListState.Success

    if (stopMonitoringOnFailure) {
        monitoringJob?.cancel()
        monitoringJob = null
        if (hasCachedData) {
            _state.update { it.copy(isMonitoring = false) }
            showFeedbackResource(
                messageRes = Res.string.system_monitor_feedback_monitoring_stopped,
                isError = true,
                message,
            )
            return
        }

        detailJob?.cancel()
        detailJob = null
        actionJob?.cancel()
        actionJob = null
        historyBuffer.clear()
    } else if (hasCachedData) {
        showFeedback(message, isError = true)
        return
    }

    _state.update {
        it.copy(
            listState = ProcessListState.Error(message),
            filteredProcesses = emptyList(),
            selectedProcess = null,
            detailState = ProcessDetailState.Idle,
            history = emptyList(),
            isMonitoring = false,
        )
    }
}

internal fun DefaultProcessesComponent.isRequestStillValid(deviceId: String): Boolean {
    val selected = deviceManager.selectedDeviceFlow.value
    return selected != null &&
        selected.state == DeviceState.DEVICE &&
        selected.deviceId == deviceId &&
        _state.value.activeDeviceId == deviceId
}

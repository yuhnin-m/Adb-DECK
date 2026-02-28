package com.adbdeck.feature.systemmonitor.processes

import adbdeck.feature.system_monitor.generated.resources.Res
import adbdeck.feature.system_monitor.generated.resources.system_monitor_error_no_active_device
import adbdeck.feature.system_monitor.generated.resources.system_monitor_error_process_details_failed
import com.adbdeck.core.adb.api.device.DeviceState
import com.adbdeck.core.adb.api.monitoring.process.ProcessInfo
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString

internal fun DefaultProcessesComponent.handleSelectProcess(process: ProcessInfo) {
    detailJob?.cancel()
    _state.update { it.copy(selectedProcess = process, detailState = ProcessDetailState.Loading) }

    val selected = deviceManager.selectedDeviceFlow.value
    if (selected == null || selected.state != DeviceState.DEVICE) {
        val requestPid = process.pid
        scope.launch {
            val message = getString(Res.string.system_monitor_error_no_active_device)
            _state.update { current ->
                if (current.selectedProcess?.pid == requestPid) {
                    current.copy(detailState = ProcessDetailState.Error(message))
                } else {
                    current
                }
            }
        }
        return
    }

    val deviceId = selected.deviceId
    val adbPath = settingsRepository.resolvedAdbPath()

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
                        onFailure = {
                            val fallback = getString(Res.string.system_monitor_error_process_details_failed)
                            ProcessDetailState.Error(it.message ?: fallback)
                        },
                    ),
                )
            }
        }
    }
}

internal fun DefaultProcessesComponent.handleClearSelection() {
    detailJob?.cancel()
    detailJob = null
    _state.update { it.copy(selectedProcess = null, detailState = ProcessDetailState.Idle) }
}

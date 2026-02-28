package com.adbdeck.feature.systemmonitor.storage

import adbdeck.feature.system_monitor.generated.resources.Res
import adbdeck.feature.system_monitor.generated.resources.system_monitor_error_device_unavailable
import adbdeck.feature.system_monitor.generated.resources.system_monitor_error_storage_data_empty
import adbdeck.feature.system_monitor.generated.resources.system_monitor_error_storage_fetch_failed
import adbdeck.feature.system_monitor.generated.resources.system_monitor_error_storage_no_relevant_partitions
import com.adbdeck.core.adb.api.device.AdbDevice
import com.adbdeck.core.adb.api.device.DeviceManager
import com.adbdeck.core.adb.api.device.DeviceState
import com.adbdeck.core.adb.api.monitoring.storage.StorageSummary
import com.adbdeck.core.adb.api.monitoring.SystemMonitorClient
import com.adbdeck.core.settings.SettingsRepository
import com.adbdeck.core.utils.runCatchingPreserveCancellation
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.lifecycle.coroutines.coroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString

/**
 * Реализация [StorageComponent] с защитой от stale-ответов при смене устройства.
 */
class DefaultStorageComponent(
    componentContext: ComponentContext,
    private val deviceManager: DeviceManager,
    private val systemMonitorClient: SystemMonitorClient,
    private val settingsRepository: SettingsRepository,
) : StorageComponent, ComponentContext by componentContext {

    private val scope = coroutineScope()

    private val _state = MutableStateFlow(StorageState())
    override val state: StateFlow<StorageState> = _state.asStateFlow()

    private var fetchJob: Job? = null
    private var activeDeviceId: String? = null

    init {
        scope.launch {
            deviceManager.selectedDeviceFlow.collect { device ->
                handleDeviceChange(device)
            }
        }
    }

    override fun onRefresh() {
        val device = deviceManager.selectedDeviceFlow.value
        if (device == null) {
            resetNoDevice()
            return
        }
        if (device.state != DeviceState.DEVICE) {
            resetDeviceError(device)
            return
        }

        activeDeviceId = device.deviceId
        _state.update { it.copy(listState = StorageListState.Loading) }
        launchFetch(device.deviceId)
    }

    private fun handleDeviceChange(device: AdbDevice?) {
        when {
            device == null -> {
                fetchJob?.cancel()
                fetchJob = null
                resetNoDevice()
            }

            device.state != DeviceState.DEVICE -> {
                fetchJob?.cancel()
                fetchJob = null
                resetDeviceError(device)
            }

            else -> {
                val isChanged = activeDeviceId != device.deviceId
                val needsReload = _state.value.listState !is StorageListState.Success
                activeDeviceId = device.deviceId

                if (isChanged || needsReload) {
                    _state.update { it.copy(listState = StorageListState.Loading) }
                    launchFetch(device.deviceId)
                }
            }
        }
    }

    private fun launchFetch(deviceId: String) {
        fetchJob?.cancel()
        fetchJob = scope.launch {
            fetchStorage(deviceId)
        }
    }

    private suspend fun fetchStorage(deviceId: String) {
        if (!isRequestStillValid(deviceId)) return

        val adbPath = settingsRepository.resolvedAdbPath()
        val result = runCatchingPreserveCancellation {
            systemMonitorClient.getStorageInfo(deviceId, adbPath)
        }.getOrElse { e -> Result.failure(e) }

        if (!isRequestStillValid(deviceId)) return

        result.fold(
            onSuccess = { partitions ->
                if (partitions.isEmpty()) {
                    val message = getString(Res.string.system_monitor_error_storage_data_empty)
                    _state.update {
                        StorageState(listState = StorageListState.Error(message))
                    }
                    return@fold
                }

                val relevant = partitions.filter { it.isRelevant }
                if (relevant.isEmpty()) {
                    val message = getString(Res.string.system_monitor_error_storage_no_relevant_partitions)
                    _state.update {
                        StorageState(listState = StorageListState.Error(message))
                    }
                    return@fold
                }

                val summary = StorageSummary(
                    totalKb = relevant.sumOf { it.totalKb },
                    usedKb = relevant.sumOf { it.usedKb },
                    freeKb = relevant.sumOf { it.freeKb },
                )

                _state.update {
                    it.copy(listState = StorageListState.Success(partitions, summary))
                }
            },
            onFailure = { e ->
                val fallback = getString(Res.string.system_monitor_error_storage_fetch_failed)
                _state.update {
                    StorageState(
                        listState = StorageListState.Error(
                            e.message ?: fallback,
                        ),
                    )
                }
            },
        )
    }

    private fun resetNoDevice() {
        activeDeviceId = null
        _state.update { StorageState(listState = StorageListState.NoDevice) }
    }

    private fun resetDeviceError(device: AdbDevice) {
        scope.launch {
            val message = getString(
                Res.string.system_monitor_error_device_unavailable,
                device.state.rawValue,
            )
            val selected = deviceManager.selectedDeviceFlow.value
            if (selected == null || selected.deviceId != device.deviceId || selected.state == DeviceState.DEVICE) {
                return@launch
            }
            activeDeviceId = device.deviceId
            _state.update {
                StorageState(listState = StorageListState.Error(message))
            }
        }
    }

    private fun isRequestStillValid(deviceId: String): Boolean {
        val selected = deviceManager.selectedDeviceFlow.value
        return selected != null &&
            selected.deviceId == deviceId &&
            selected.state == DeviceState.DEVICE &&
            activeDeviceId == deviceId
    }
}

package com.adbdeck.feature.devices

import com.adbdeck.core.adb.api.DeviceManager
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.lifecycle.coroutines.coroutineScope
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Реализация [DevicesComponent].
 *
 * При инициализации автоматически загружает список устройств.
 *
 * @param componentContext Контекст Decompose-компонента.
 * @param deviceManager Единый менеджер устройств (общий с DeviceBar).
 */
class DefaultDevicesComponent(
    componentContext: ComponentContext,
    private val deviceManager: DeviceManager,
) : DevicesComponent, ComponentContext by componentContext {

    private val scope = coroutineScope()

    private val _state = MutableStateFlow<DevicesState>(DevicesState.Loading)
    override val state: StateFlow<DevicesState> = _state.asStateFlow()

    init {
        // Состояние экрана синхронизировано с общим DeviceManager.
        scope.launch {
            combine(
                deviceManager.devicesFlow,
                deviceManager.isConnecting,
                deviceManager.errorFlow,
            ) { devices, isConnecting, error ->
                when {
                    isConnecting && devices.isEmpty() -> DevicesState.Loading
                    error != null && devices.isEmpty() -> DevicesState.Error(error)
                    devices.isEmpty() -> DevicesState.Empty
                    else -> DevicesState.Success(devices)
                }
            }.collect { _state.value = it }
        }

        // Первый refresh только если список еще не загружен.
        loadDevices()
    }

    override fun onRefresh() {
        loadDevices()
    }

    private fun loadDevices() {
        if (deviceManager.isConnecting.value) return
        scope.launch {
            deviceManager.clearError()
            deviceManager.refresh()
        }
    }
}

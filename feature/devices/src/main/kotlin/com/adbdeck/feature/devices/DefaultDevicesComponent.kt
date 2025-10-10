package com.adbdeck.feature.devices

import com.adbdeck.core.adb.api.AdbClient
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.lifecycle.coroutines.coroutineScope
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
 * @param adbClient ADB-клиент для получения списка устройств.
 */
class DefaultDevicesComponent(
    componentContext: ComponentContext,
    private val adbClient: AdbClient,
) : DevicesComponent, ComponentContext by componentContext {

    private val scope = coroutineScope()

    private val _state = MutableStateFlow<DevicesState>(DevicesState.Loading)
    override val state: StateFlow<DevicesState> = _state.asStateFlow()

    init {
        // Загружаем устройства при создании компонента
        loadDevices()
    }

    override fun onRefresh() {
        loadDevices()
    }

    private fun loadDevices() {
        scope.launch {
            _state.value = DevicesState.Loading
            adbClient.getDevices()
                .onSuccess { devices ->
                    _state.value = if (devices.isEmpty()) {
                        DevicesState.Empty
                    } else {
                        DevicesState.Success(devices)
                    }
                }
                .onFailure { error ->
                    _state.value = DevicesState.Error(
                        error.message ?: "Неизвестная ошибка"
                    )
                }
        }
    }
}

package com.adbdeck.app.devicemanager

import com.adbdeck.core.adb.api.device.AdbDevice
import com.adbdeck.core.adb.api.device.DeviceEndpoint
import com.adbdeck.core.adb.api.device.DeviceManager
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.lifecycle.coroutines.coroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Реализация [DeviceSelectorComponent].
 *
 * Делегирует все операции [DeviceManager]-у, запуская suspend-функции
 * в coroutine scope, привязанном к Decompose lifecycle [ComponentContext].
 *
 * При инициализации автоматически выполняет первый refresh устройств.
 *
 * @param componentContext Decompose-контекст жизненного цикла.
 * @param deviceManager Singleton-менеджер устройств.
 */
class DefaultDeviceSelectorComponent(
    componentContext: ComponentContext,
    private val deviceManager: DeviceManager,
) : DeviceSelectorComponent, ComponentContext by componentContext {

    private val scope = coroutineScope()

    // Делегируем StateFlow напрямую из DeviceManager
    override val devices: StateFlow<List<AdbDevice>> = deviceManager.devicesFlow
    override val selectedDevice: StateFlow<AdbDevice?> = deviceManager.selectedDeviceFlow
    override val isConnecting: StateFlow<Boolean> = deviceManager.isConnecting
    override val error: StateFlow<String?> = deviceManager.errorFlow
    override val savedEndpoints: StateFlow<List<DeviceEndpoint>> = deviceManager.savedEndpointsFlow

    init {
        // Первоначальная загрузка списка устройств при старте приложения
        onRefresh()
    }

    override fun onRefresh() {
        scope.launch { deviceManager.refresh() }
    }

    override fun onConnect(host: String, port: Int) {
        scope.launch {
            val result = deviceManager.connect(host, port)
            // Сохраняем endpoint только при успешном подключении
            if (result.isSuccess) {
                deviceManager.saveEndpoint(DeviceEndpoint(host = host, port = port))
            }
        }
    }

    override fun onDisconnect() {
        val device = selectedDevice.value ?: return
        scope.launch { deviceManager.disconnect(device.deviceId) }
    }

    override fun onSelectDevice(device: AdbDevice) {
        deviceManager.selectDevice(device)
    }

    override fun onConnectSaved(endpoint: DeviceEndpoint) {
        scope.launch { deviceManager.connect(endpoint.host, endpoint.port) }
    }

    override fun onSwitchToTcpIp(serialId: String, port: Int) {
        scope.launch { deviceManager.switchToTcpIp(serialId, port) }
    }

    override fun onRemoveEndpoint(endpoint: DeviceEndpoint) {
        scope.launch { deviceManager.removeEndpoint(endpoint) }
    }

    override fun onClearError() {
        deviceManager.clearError()
    }
}

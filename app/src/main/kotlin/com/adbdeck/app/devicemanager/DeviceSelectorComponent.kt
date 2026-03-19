package com.adbdeck.app.devicemanager

import com.adbdeck.core.adb.api.device.AdbDevice
import com.adbdeck.core.adb.api.device.DeviceEndpoint
import kotlinx.coroutines.flow.StateFlow

/**
 * Компонент выбора и управления устройствами для UI (DeviceBar).
 *
 * Тонкая обертка над [DeviceManager], адаптированная для Compose-слоя:
 * - Экспонирует потоки для наблюдения из composable-функций
 * - Предоставляет синхронные callback-и для пользовательских действий
 *   (suspend-вызовы выполняются во внутреннем CoroutineScope)
 *
 * Живет на весь lifecycle приложения (создается в AppComponent).
 */
interface DeviceSelectorComponent {

    /** Список всех видимых ADB-устройств. */
    val devices: StateFlow<List<AdbDevice>>

    /** Текущее выбранное (активное) устройство. */
    val selectedDevice: StateFlow<AdbDevice?>

    /** `true` пока выполняется adb-операция. */
    val isConnecting: StateFlow<Boolean>

    /** Последнее сообщение об ошибке или `null`. */
    val error: StateFlow<String?>

    /** Сохраненные endpoint-ы для быстрого переподключения. */
    val savedEndpoints: StateFlow<List<DeviceEndpoint>>

    /** Обновить список устройств (`adb devices`). */
    fun onRefresh()

    /**
     * Подключиться к устройству по IP-адресу (`adb connect host:port`).
     * После успешного подключения endpoint автоматически сохраняется.
     */
    fun onConnect(host: String, port: Int = 5555)

    /** Отключить текущее выбранное устройство (`adb disconnect`). */
    fun onDisconnect()

    /** Выбрать активное устройство из списка. */
    fun onSelectDevice(device: AdbDevice)

    /** Подключиться к сохраненному endpoint-у. */
    fun onConnectSaved(endpoint: DeviceEndpoint)

    /**
     * Переключить USB-устройство в TCP/IP режим (`adb -s serial tcpip port`).
     * После этого можно подключиться по IP.
     */
    fun onSwitchToTcpIp(serialId: String, port: Int = 5555)

    /** Удалить endpoint из сохраненных. */
    fun onRemoveEndpoint(endpoint: DeviceEndpoint)

    /** Сбросить текущую ошибку. */
    fun onClearError()
}

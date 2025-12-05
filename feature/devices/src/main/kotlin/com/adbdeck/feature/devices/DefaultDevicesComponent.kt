package com.adbdeck.feature.devices

import com.adbdeck.core.adb.api.device.AdbDevice
import com.adbdeck.core.adb.api.device.DeviceControlClient
import com.adbdeck.core.adb.api.device.DeviceInfoClient
import com.adbdeck.core.adb.api.device.DeviceInfoLoadState
import com.adbdeck.core.adb.api.device.DeviceManager
import com.adbdeck.core.adb.api.device.DeviceTransportType
import com.adbdeck.core.adb.api.device.RebootMode
import com.adbdeck.core.settings.SettingsRepository
import com.adbdeck.core.utils.runCatchingPreserveCancellation
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.lifecycle.coroutines.coroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Реализация [DevicesComponent].
 *
 * ## Жизненный цикл данных
 *
 * 1. В `init` подписывается на `DeviceManager.devicesFlow` + `isConnecting` + `errorFlow`.
 * 2. При появлении новых устройств в списке автоматически запускает загрузку
 *    расширенной информации ([DeviceInfoClient]) для каждого устройства,
 *    у которого она ещё не загружена.
 * 3. Результаты кешируются в `_state.deviceInfos`. При `onRefreshDeviceInfo`
 *    принудительно перезагружает данные.
 *
 * ## Потокобезопасность
 *
 * Все мутации `_state` через `_state.update {}` (атомарно, Main dispatcher).
 * I/O-операции выполняются в `coroutineScope()` (Dispatchers.Main → Essenty).
 *
 * @param componentContext     Decompose-контекст.
 * @param deviceManager        Синглтон менеджер устройств.
 * @param deviceInfoClient     ADB-клиент расширенной информации об устройстве.
 * @param deviceControlClient  ADB-клиент управления устройством (перезагрузка).
 * @param settingsRepository   Репозиторий настроек (путь к adb).
 * @param onNavigateToLogcat   Callback перехода на экран Logcat.
 * @param onNavigateToPackages Callback перехода на экран Packages.
 * @param onNavigateToSystemMonitor Callback перехода на System Monitor.
 */
class DefaultDevicesComponent(
    componentContext: ComponentContext,
    private val deviceManager: DeviceManager,
    private val deviceInfoClient: DeviceInfoClient,
    private val deviceControlClient: DeviceControlClient,
    private val settingsRepository: SettingsRepository,
    private val onNavigateToLogcat: () -> Unit,
    private val onNavigateToPackages: () -> Unit,
    private val onNavigateToSystemMonitor: () -> Unit,
) : DevicesComponent, ComponentContext by componentContext {

    private val scope = coroutineScope()

    private val _state = MutableStateFlow(DevicesState())
    override val state: StateFlow<DevicesState> = _state.asStateFlow()

    /** Job автосброса feedback-баннера. */
    private var feedbackJob: Job? = null

    init {
        // Подписка на DeviceManager — синхронизируем список и выбранное устройство
        scope.launch {
            combine(
                deviceManager.devicesFlow,
                deviceManager.selectedDeviceFlow,
                deviceManager.isConnecting,
                deviceManager.errorFlow,
            ) { devices, selected, isConnecting, error ->
                val listState: DeviceListState = when {
                    isConnecting && devices.isEmpty() -> DeviceListState.Loading
                    error != null && devices.isEmpty() -> DeviceListState.Error(error)
                    devices.isEmpty() -> DeviceListState.Empty
                    else              -> DeviceListState.Success(devices)
                }
                Pair(listState, Triple(devices, selected?.deviceId, error))
            }.collect { (listState, extra) ->
                val (devices, selectedId, _) = extra
                _state.update { current ->
                    current.copy(
                        listState = listState,
                        selectedDeviceId = selectedId,
                    )
                }
                // Для каждого нового устройства инициируем загрузку DeviceInfo
                if (listState is DeviceListState.Success) {
                    fetchMissingDeviceInfos(devices)
                }
            }
        }

        // Первоначальная загрузка списка
        refreshDeviceList()
    }

    // ── Список устройств ──────────────────────────────────────────────────────

    override fun onRefresh() {
        refreshDeviceList()
    }

    private fun refreshDeviceList() {
        if (deviceManager.isConnecting.value) return
        scope.launch {
            deviceManager.clearError()
            deviceManager.refresh()
        }
    }

    // ── Выбор устройства ──────────────────────────────────────────────────────

    override fun onSelectDevice(device: AdbDevice) {
        deviceManager.selectDevice(device)
    }

    // ── Панель деталей ────────────────────────────────────────────────────────

    override fun onOpenDetails(device: AdbDevice) {
        _state.update { it.copy(detailsDeviceId = device.deviceId) }
        // Если DeviceInfo ещё не загружен — грузим
        val current = _state.value.deviceInfos[device.deviceId]
        if (current == null || current is DeviceInfoLoadState.Failed) {
            loadDeviceInfo(device.deviceId)
        }
    }

    override fun onCloseDetails() {
        _state.update { it.copy(detailsDeviceId = null) }
    }

    override fun onRefreshDeviceInfo(device: AdbDevice) {
        loadDeviceInfo(device.deviceId)
    }

    // ── Навигация ─────────────────────────────────────────────────────────────

    override fun onNavigateToLogcat() = onNavigateToLogcat.invoke()
    override fun onNavigateToPackages() = onNavigateToPackages.invoke()
    override fun onNavigateToSystemMonitor() = onNavigateToSystemMonitor.invoke()

    // ── Действия с подтверждением ─────────────────────────────────────────────

    override fun onRequestReboot(device: AdbDevice) {
        _state.update {
            it.copy(
                pendingAction = PendingDeviceAction(
                    device  = device,
                    type    = PendingDeviceActionType.REBOOT,
                    title   = "Перезагрузить устройство?",
                    message = "Устройство «${device.deviceId}» будет перезагружено. " +
                              "Соединение ADB временно прервётся.",
                )
            )
        }
    }

    override fun onRequestRebootRecovery(device: AdbDevice) {
        _state.update {
            it.copy(
                pendingAction = PendingDeviceAction(
                    device  = device,
                    type    = PendingDeviceActionType.REBOOT_RECOVERY,
                    title   = "Перезагрузить в Recovery?",
                    message = "Устройство «${device.deviceId}» перезагрузится в режим Recovery.",
                )
            )
        }
    }

    override fun onRequestRebootBootloader(device: AdbDevice) {
        _state.update {
            it.copy(
                pendingAction = PendingDeviceAction(
                    device  = device,
                    type    = PendingDeviceActionType.REBOOT_BOOTLOADER,
                    title   = "Перезагрузить в Bootloader?",
                    message = "Устройство «${device.deviceId}» перезагрузится в режим Bootloader (Fastboot).",
                )
            )
        }
    }

    override fun onRequestDisconnect(device: AdbDevice) {
        _state.update {
            it.copy(
                pendingAction = PendingDeviceAction(
                    device  = device,
                    type    = PendingDeviceActionType.DISCONNECT,
                    title   = "Отключить устройство?",
                    message = "Wi-Fi-соединение с «${device.deviceId}» будет разорвано.",
                )
            )
        }
    }

    override fun onConfirmAction() {
        val action = _state.value.pendingAction ?: return
        _state.update { it.copy(pendingAction = null, isActionRunning = true) }

        scope.launch {
            val adbPath = settingsRepository.getSettings().adbPath.ifBlank { "adb" }
            val result = when (action.type) {
                PendingDeviceActionType.REBOOT ->
                    runCatchingPreserveCancellation {
                        deviceControlClient.reboot(action.device.deviceId, RebootMode.NORMAL, adbPath)
                    }.getOrElse { e -> Result.failure(e) }

                PendingDeviceActionType.REBOOT_RECOVERY ->
                    runCatchingPreserveCancellation {
                        deviceControlClient.reboot(action.device.deviceId, RebootMode.RECOVERY, adbPath)
                    }.getOrElse { e -> Result.failure(e) }

                PendingDeviceActionType.REBOOT_BOOTLOADER ->
                    runCatchingPreserveCancellation {
                        deviceControlClient.reboot(action.device.deviceId, RebootMode.BOOTLOADER, adbPath)
                    }.getOrElse { e -> Result.failure(e) }

                PendingDeviceActionType.DISCONNECT ->
                    runCatchingPreserveCancellation {
                        deviceManager.disconnect(action.device.deviceId)
                    }.getOrElse { e -> Result.failure(e) }
            }

            _state.update { it.copy(isActionRunning = false) }

            result.fold(
                onSuccess = {
                    val label = when (action.type) {
                        PendingDeviceActionType.REBOOT             -> "Перезагрузка запущена"
                        PendingDeviceActionType.REBOOT_RECOVERY    -> "Перезагрузка в Recovery запущена"
                        PendingDeviceActionType.REBOOT_BOOTLOADER  -> "Перезагрузка в Bootloader запущена"
                        PendingDeviceActionType.DISCONNECT         -> "Устройство отключено"
                    }
                    showFeedback(label, isError = false)
                    // Обновляем список после disconnect
                    if (action.type == PendingDeviceActionType.DISCONNECT) {
                        refreshDeviceList()
                    }
                },
                onFailure = { e ->
                    showFeedback("Ошибка: ${e.message}", isError = true)
                },
            )
        }
    }

    override fun onCancelAction() {
        _state.update { it.copy(pendingAction = null) }
    }

    // ── Feedback ─────────────────────────────────────────────────────────────

    override fun onDismissFeedback() {
        feedbackJob?.cancel()
        _state.update { it.copy(actionFeedback = null) }
    }

    // ── Вспомогательные функции ───────────────────────────────────────────────

    /**
     * Запускает загрузку [DeviceInfoClient] для всех устройств, которых ещё нет в кеше.
     */
    private fun fetchMissingDeviceInfos(devices: List<AdbDevice>) {
        val currentInfos = _state.value.deviceInfos
        devices
            .filter { it.deviceId !in currentInfos }
            .forEach { device -> loadDeviceInfo(device.deviceId) }
    }

    /**
     * Асинхронно загружает расширенную информацию об устройстве.
     *
     * Устанавливает [DeviceInfoLoadState.Loading] до запроса,
     * [DeviceInfoLoadState.Loaded] или [DeviceInfoLoadState.Failed] по результату.
     */
    private fun loadDeviceInfo(deviceId: String) {
        val adbPath = settingsRepository.getSettings().adbPath.ifBlank { "adb" }

        // Помечаем как загружающееся
        _state.update { state ->
            state.copy(deviceInfos = state.deviceInfos + (deviceId to DeviceInfoLoadState.Loading))
        }

        scope.launch {
            val result = runCatchingPreserveCancellation {
                deviceInfoClient.fetchDeviceInfo(deviceId, adbPath)
            }.getOrElse { e -> Result.failure(e) }

            val newState = result.fold(
                onSuccess = { DeviceInfoLoadState.Loaded(it) },
                onFailure = { DeviceInfoLoadState.Failed(it.message ?: "Неизвестная ошибка") },
            )

            _state.update { state ->
                state.copy(deviceInfos = state.deviceInfos + (deviceId to newState))
            }
        }
    }

    /**
     * Показывает feedback-баннер и планирует его автосброс через 3 секунды.
     */
    private fun showFeedback(message: String, isError: Boolean) {
        feedbackJob?.cancel()
        _state.update { it.copy(actionFeedback = DeviceActionFeedback(message, isError)) }
        feedbackJob = scope.launch {
            delay(3_000L)
            _state.update { it.copy(actionFeedback = null) }
        }
    }

    /**
     * Определяет тип транспорта устройства по его deviceId.
     *
     * Используется для показа иконки и фильтрации доступных действий (disconnect только для Wi-Fi).
     */
    private fun AdbDevice.transportType(): DeviceTransportType = when {
        deviceId.startsWith("emulator-") -> DeviceTransportType.EMULATOR
        deviceId.contains(':')           -> DeviceTransportType.WIFI
        else                             -> DeviceTransportType.USB
    }
}

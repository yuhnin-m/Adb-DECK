package com.adbdeck.feature.devices

import com.adbdeck.core.adb.api.device.AdbDevice
import com.adbdeck.core.adb.api.device.SavedWifiDevice
import kotlinx.coroutines.flow.StateFlow

/**
 * Контракт компонента экрана устройств.
 *
 * ## Архитектура
 *
 * Компонент координирует несколько источников данных:
 * - [com.adbdeck.core.adb.api.device.DeviceManager] — список устройств, выбор, подключение
 * - [com.adbdeck.core.adb.api.device.DeviceInfoClient] — расширенная информация (загружается лениво)
 * - [com.adbdeck.core.adb.api.device.DeviceControlClient] — перезагрузка
 *
 * Все навигационные действия (перейти в Logcat / Packages / System Monitor)
 * выполняются через callbacks, переданные из [com.adbdeck.app.navigation.DefaultRootComponent].
 *
 * ## Потокобезопасность
 *
 * Все методы могут вызываться из UI-потока (Main dispatcher). Тяжёлые I/O-операции
 * делегируются на корутины компонента.
 */
interface DevicesComponent {

    /** Реактивное состояние экрана. */
    val state: StateFlow<DevicesState>

    // ── Список устройств ──────────────────────────────────────────────────────

    /** Обновить список устройств через `adb devices`. */
    fun onRefresh()

    // ── История Wi-Fi ───────────────────────────────────────────────────────

    /**
     * Подключиться к устройству из истории Wi-Fi (`adb connect host:port`).
     */
    fun onConnectHistoryDevice(device: SavedWifiDevice)

    /**
     * Удалить устройство из локальной истории Wi-Fi.
     */
    fun onRemoveHistoryDevice(device: SavedWifiDevice)

    // ── Выбор устройства ──────────────────────────────────────────────────────

    /**
     * Установить [device] как активное (выбранное) устройство.
     *
     * @param device Устройство для активации.
     */
    fun onSelectDevice(device: AdbDevice)

    // ── Панель деталей ────────────────────────────────────────────────────────

    /**
     * Открыть панель деталей для [device].
     *
     * Если информация о нём ещё не загружена — инициирует асинхронную загрузку.
     *
     * @param device Устройство для отображения деталей.
     */
    fun onOpenDetails(device: AdbDevice)

    /** Закрыть панель деталей. */
    fun onCloseDetails()

    /**
     * Перезагрузить расширенную информацию об устройстве (из ADB shell).
     *
     * @param device Устройство для обновления информации.
     */
    fun onRefreshDeviceInfo(device: AdbDevice)

    // ── Навигация ─────────────────────────────────────────────────────────────

    /** Перейти на экран Logcat (для выбранного устройства). */
    fun onNavigateToLogcat()

    /** Перейти на экран Packages. */
    fun onNavigateToPackages()

    /** Перейти на экран System Monitor. */
    fun onNavigateToSystemMonitor()

    // ── Действия (с подтверждением) ───────────────────────────────────────────

    /**
     * Запросить обычную перезагрузку устройства.
     *
     * Показывает диалог подтверждения; реальная перезагрузка происходит
     * только после [onConfirmAction].
     */
    fun onRequestReboot(device: AdbDevice)

    /**
     * Запросить перезагрузку в Recovery Mode.
     *
     * @param device Целевое устройство.
     */
    fun onRequestRebootRecovery(device: AdbDevice)

    /**
     * Запросить перезагрузку в Bootloader / Fastboot.
     *
     * @param device Целевое устройство.
     */
    fun onRequestRebootBootloader(device: AdbDevice)

    /**
     * Запросить отключение Wi-Fi-устройства (`adb disconnect`).
     *
     * @param device Wi-Fi-устройство (с IP-адресом в deviceId).
     */
    fun onRequestDisconnect(device: AdbDevice)

    /**
     * Подтвердить ожидающее действие ([DevicesState.pendingAction]).
     *
     * Выполняет соответствующую ADB-команду.
     */
    fun onConfirmAction()

    /** Отменить ожидающее действие, скрыть диалог. */
    fun onCancelAction()

    // ── Feedback ─────────────────────────────────────────────────────────────

    /** Скрыть текущее уведомление о результате действия. */
    fun onDismissFeedback()
}

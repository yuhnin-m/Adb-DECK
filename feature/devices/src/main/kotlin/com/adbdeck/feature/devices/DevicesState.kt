package com.adbdeck.feature.devices

import com.adbdeck.core.adb.api.device.AdbDevice
import com.adbdeck.core.adb.api.device.DeviceInfoLoadState
import com.adbdeck.core.adb.api.device.SavedWifiDevice

// ── Состояние списка устройств ────────────────────────────────────────────────

/**
 * Состояние загрузки списка ADB-устройств.
 */
sealed class DeviceListState {
    /** Список загружается (первый запрос или ручной refresh). */
    data object Loading : DeviceListState()

    /** Список загружен, но устройств нет. */
    data object Empty : DeviceListState()

    /**
     * Список успешно получен.
     *
     * @param devices Список подключенных устройств.
     */
    data class Success(val devices: List<AdbDevice>) : DeviceListState()

    /**
     * Ошибка при получении списка.
     *
     * @param message Описание ошибки.
     */
    data class Error(val message: String) : DeviceListState()
}

// ── Действия, требующие подтверждения ─────────────────────────────────────────

/**
 * Тип потенциально опасного действия над устройством, требующего подтверждения.
 */
enum class PendingDeviceActionType {
    /** Обычная перезагрузка устройства. */
    REBOOT,

    /** Перезагрузка в Recovery. */
    REBOOT_RECOVERY,

    /** Перезагрузка в Bootloader / Fastboot. */
    REBOOT_BOOTLOADER,

    /** Отключение Wi-Fi-устройства (`adb disconnect`). */
    DISCONNECT,
}

/**
 * Незавершённое действие, ожидающее подтверждения пользователем.
 *
 * @param device  Целевое устройство.
 * @param type    Тип действия.
 */
data class PendingDeviceAction(
    val device: AdbDevice,
    val type: PendingDeviceActionType,
)

// ── Обратная связь по действиям ───────────────────────────────────────────────

/**
 * Краткосрочное уведомление о результате действия над устройством.
 *
 * Автоматически убирается через 3 секунды (управляется компонентом).
 */
sealed interface DeviceActionFeedback {
    /**
     * Успешное завершение действия над устройством.
     *
     * @param actionType Тип выполненного действия.
     */
    data class ActionSuccess(
        val actionType: PendingDeviceActionType,
    ) : DeviceActionFeedback

    /**
     * Ошибка выполнения действия.
     *
     * @param details Текст детали ошибки (может быть `null`).
     */
    data class ActionError(
        val details: String?,
    ) : DeviceActionFeedback
}

// ── Главное состояние экрана ──────────────────────────────────────────────────

/**
 * Полное состояние экрана Devices.
 *
 * @param listState          Состояние загрузки списка устройств.
 * @param selectedDeviceId   ID активного (выбранного) устройства.
 * @param deviceInfos        Расширенная информация об устройствах по deviceId.
 *                           Загружается асинхронно после появления устройства в списке.
 * @param detailsDeviceId    ID устройства, открытого в панели деталей. `null` = панель скрыта.
 * @param wifiHistory        История ранее подключенных Wi-Fi-устройств.
 * @param actionFeedback     Краткосрочное уведомление о результате последнего действия.
 * @param pendingAction      Действие, ожидающее подтверждения в диалоге.
 * @param isActionRunning    Флаг выполнения текущего действия (блокирует кнопки).
 */
data class DevicesState(
    val listState: DeviceListState = DeviceListState.Loading,
    val selectedDeviceId: String? = null,
    val deviceInfos: Map<String, DeviceInfoLoadState> = emptyMap(),
    val detailsDeviceId: String? = null,
    val wifiHistory: List<SavedWifiDevice> = emptyList(),
    val actionFeedback: DeviceActionFeedback? = null,
    val pendingAction: PendingDeviceAction? = null,
    val isActionRunning: Boolean = false,
)

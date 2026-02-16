package com.adbdeck.feature.dashboard

/**
 * Типизированный статус проверки доступности ADB.
 */
sealed class DashboardAdbCheckState {
    data object Idle : DashboardAdbCheckState()
    data object Checking : DashboardAdbCheckState()
    data class Available(val version: String) : DashboardAdbCheckState()
    data class NotAvailable(val reason: String) : DashboardAdbCheckState()
}

/**
 * Состояние экрана Dashboard.
 *
 * @param adbCheckState     Статус последней проверки доступности adb.
 * @param isRefreshingDevices Флаг выполняющегося обновления списка устройств.
 * @param deviceCount       Количество устройств в актуальном [com.adbdeck.core.adb.api.device.DeviceManager.devicesFlow].
 * @param refreshError      Сообщение ошибки последнего обновления устройств.
 */
data class DashboardState(
    val adbCheckState: DashboardAdbCheckState = DashboardAdbCheckState.Idle,
    val isRefreshingDevices: Boolean = false,
    val deviceCount: Int = 0,
    val refreshError: String? = null,
)

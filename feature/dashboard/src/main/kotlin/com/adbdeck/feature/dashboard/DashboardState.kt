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
 * Состояние ADB server для Dashboard-блока.
 */
enum class DashboardAdbServerState {
    RUNNING,
    STOPPED,
    UNKNOWN,
    ERROR,
}

/**
 * Активное действие управления ADB server.
 */
enum class DashboardAdbServerAction {
    REFRESH,
    START,
    STOP,
    RESTART,
}

/**
 * UI-состояние секции ADB server на Dashboard.
 */
data class DashboardAdbServerUiState(
    val adbPath: String = "adb",
    val isAdbFound: Boolean = false,
    val adbVersion: String? = null,
    val serverState: DashboardAdbServerState = DashboardAdbServerState.UNKNOWN,
    val serverMessage: String? = null,
    val activeAction: DashboardAdbServerAction? = null,
    val actionError: String? = null,
) {
    val isBusy: Boolean get() = activeAction != null
}

/**
 * Состояние экрана Dashboard.
 *
 * @param adbCheckState     Статус последней проверки доступности adb.
 * @param isRefreshingDevices Флаг выполняющегося обновления списка устройств.
 * @param deviceCount       Количество устройств в актуальном [com.adbdeck.core.adb.api.device.DeviceManager.devicesFlow].
 * @param refreshError      Сообщение ошибки последнего обновления устройств.
 * @param adbServer         Состояние секции ADB server.
 */
data class DashboardState(
    val adbCheckState: DashboardAdbCheckState = DashboardAdbCheckState.Idle,
    val isRefreshingDevices: Boolean = false,
    val deviceCount: Int = 0,
    val refreshError: String? = null,
    val adbServer: DashboardAdbServerUiState = DashboardAdbServerUiState(),
)

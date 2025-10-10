package com.adbdeck.feature.dashboard

/**
 * Состояние экрана Dashboard.
 *
 * @param adbStatusText     Текст с результатом последней проверки adb (пустой — проверка не запускалась).
 * @param isCheckingAdb     Флаг выполняющейся проверки доступности adb.
 * @param isRefreshingDevices Флаг выполняющегося обновления списка устройств.
 * @param deviceCount       Количество устройств после последнего обновления (null — не загружалось).
 */
data class DashboardState(
    val adbStatusText: String = "",
    val isCheckingAdb: Boolean = false,
    val isRefreshingDevices: Boolean = false,
    val deviceCount: Int? = null,
)

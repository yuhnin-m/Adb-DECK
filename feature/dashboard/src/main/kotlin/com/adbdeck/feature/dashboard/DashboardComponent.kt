package com.adbdeck.feature.dashboard

import kotlinx.coroutines.flow.StateFlow

/**
 * Контракт компонента экрана Dashboard.
 *
 * Содержит только публичное состояние и обработчики действий пользователя.
 * Реализация живет в [DefaultDashboardComponent].
 */
interface DashboardComponent {

    /** Текущее состояние экрана. */
    val state: StateFlow<DashboardState>

    /** Переход на экран устройств. */
    fun onOpenDevices()

    /** Переход на экран logcat. */
    fun onOpenLogcat()

    /** Переход на экран настроек. */
    fun onOpenSettings()

    /** Обновить список устройств и отобразить их количество на плитке. */
    fun onRefreshDevices()

    /** Проверить доступность adb и отобразить результат. */
    fun onCheckAdb()

    /** Скрыть баннер результата проверки adb. */
    fun onDismissAdbCheck()

    /** Скрыть баннер ошибки обновления устройств. */
    fun onDismissRefreshError()
}

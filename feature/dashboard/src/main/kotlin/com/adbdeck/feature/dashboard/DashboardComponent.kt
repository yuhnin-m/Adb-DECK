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

    /** Переход на экран информации об устройстве. */
    fun onOpenDeviceInfo()

    /** Переход на экран быстрых тумблеров. */
    fun onOpenQuickToggles()

    /** Переход на экран logcat. */
    fun onOpenLogcat()

    /** Переход на экран пакетов. */
    fun onOpenPackages()

    /** Переход на экран установки APK. */
    fun onOpenApkInstall()

    /** Переход на экран Deep Links / Intents. */
    fun onOpenDeepLinks()

    /** Переход на экран уведомлений. */
    fun onOpenNotifications()

    /** Переход на экран Screen Tools. */
    fun onOpenScreenTools()

    /** Переход на экран Scrcpy (Mirror screen). */
    fun onOpenScrcpy()

    /** Переход на экран File Explorer. */
    fun onOpenFileExplorer()

    /** Переход на экран Contacts. */
    fun onOpenContacts()

    /** Переход на экран System Monitor. */
    fun onOpenSystemMonitor()

    /** Переход на экран настроек. */
    fun onOpenSettings()

    /** Обновить список устройств и отобразить их количество на плитке. */
    fun onRefreshDevices()

    /** Проверить доступность adb и отобразить результат. */
    fun onCheckAdb()

    /** Обновить секцию ADB server status (путь, доступность, версия, статус сервера). */
    fun onRefreshAdbServerStatus()

    /** Запустить ADB server. */
    fun onStartAdbServer()

    /** Остановить ADB server. */
    fun onStopAdbServer()

    /** Перезапустить ADB server. */
    fun onRestartAdbServer()

    /** Скрыть баннер результата проверки adb. */
    fun onDismissAdbCheck()

    /** Скрыть баннер ошибки обновления устройств. */
    fun onDismissRefreshError()

    /** Скрыть баннер ошибки действий ADB server. */
    fun onDismissAdbServerError()
}

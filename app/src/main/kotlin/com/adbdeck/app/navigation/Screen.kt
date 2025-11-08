package com.adbdeck.app.navigation

/**
 * Конфигурации экранов для Decompose child stack.
 *
 * Каждый объект — уникальная конфигурация навигации.
 * При добавлении нового экрана достаточно добавить новый data object сюда
 * и обработать его в [DefaultRootComponent.createChild].
 */
sealed interface Screen {

    /** Стартовый экран с кнопками быстрых действий. */
    data object Dashboard : Screen

    /** Экран списка подключенных устройств. */
    data object Devices : Screen

    /** Экран просмотра logcat. */
    data object Logcat : Screen

    /** Экран настроек приложения. */
    data object Settings : Screen

    /** Экран установленных пакетов / приложений. */
    data object Packages : Screen

    /** Экран мониторинга системы (процессы + хранилище). */
    data object SystemMonitor : Screen

    /** Двухпанельный файловый менеджер (Local + Device). */
    data object FileExplorer : Screen
}

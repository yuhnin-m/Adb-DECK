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

    /** Экран мониторинга процессов устройства. */
    data object SystemMonitor : Screen

    /** Экран информации о файловых системах устройства. */
    data object FileSystem : Screen

    /** Двухпанельный файловый менеджер (Local + Device). */
    data object FileExplorer : Screen

    /** Экран управления контактами Android-устройства. */
    data object Contacts : Screen

    /** Экран инструментов экрана (Screenshot / Screenrecord). */
    data object ScreenTools : Screen

    /** Экран установки APK на устройство. */
    data object ApkInstall : Screen

    /** Экран запуска Deep Link и Intent через ADB. */
    data object DeepLinks : Screen

    /** Экран просмотра и анализа уведомлений Android-устройства. */
    data object Notifications : Screen

    /** Экран подробной информации об устройстве и системе. */
    data object DeviceInfo : Screen

    /** Экран быстрых QA-тумблеров (Wi-Fi/Data/Bluetooth и др.). */
    data object QuickToggles : Screen

    /** Экран управления scrcpy (зеркалирование экрана устройства). */
    data object Scrcpy : Screen
}

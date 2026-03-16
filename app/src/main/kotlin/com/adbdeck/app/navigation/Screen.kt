package com.adbdeck.app.navigation

/**
 * Конфигурации экранов для Decompose child stack.
 *
 * Экраны без навигационных аргументов объявлены как `data object`.
 * Экраны, которые могут получать аргументы при открытии, объявлены как `data class`
 * с [equals]/[hashCode] по типу — это позволяет [com.arkivanov.decompose.router.stack.bringToFront]
 * корректно находить уже существующий компонент в стеке и переиспользовать его
 * без пересоздания, независимо от значения аргументов.
 *
 * При добавлении нового экрана достаточно добавить новый тип сюда
 * и обработать его в [DefaultRootComponent.createChild].
 */
sealed interface Screen {

    /** Стартовый экран с кнопками быстрых действий. */
    data object Dashboard : Screen

    /** Экран списка подключенных устройств. */
    data object Devices : Screen

    /**
     * Экран просмотра logcat.
     *
     * @param packageFilter Имя пакета для предустановленного фильтра.
     *                      Применяется только при первом создании компонента.
     */
    data class Logcat(val packageFilter: String? = null) : Screen {
        override fun equals(other: Any?) = other is Logcat
        override fun hashCode() = javaClass.hashCode()
    }

    /** Экран настроек приложения. */
    data object Settings : Screen

    /**
     * Экран установленных пакетов / приложений.
     *
     * @param packageToReveal Имя пакета, который нужно выделить и прокрутить к нему список.
     *                        Применяется только при первом создании компонента.
     */
    data class Packages(val packageToReveal: String? = null) : Screen {
        override fun equals(other: Any?) = other is Packages
        override fun hashCode() = javaClass.hashCode()
    }

    /** Экран мониторинга процессов устройства. */
    data object SystemMonitor : Screen

    /** Экран информации о файловых системах устройства. */
    data object FileSystem : Screen

    /**
     * Двухпанельный файловый менеджер (Local + Device).
     *
     * @param initialPath Путь на устройстве, к которому нужно перейти при открытии.
     * Применяется только при первом создании компонента.
     */
    data class FileExplorer(val initialPath: String? = null) : Screen {
        override fun equals(other: Any?) = other is FileExplorer
        override fun hashCode() = javaClass.hashCode()
    }

    /** Экран управления контактами Android-устройства. */
    data object Contacts : Screen

    /** Экран инструментов экрана (Screenshot / Screenrecord). */
    data object ScreenTools : Screen

    /** Экран установки APK на устройство. */
    data object ApkInstall : Screen

    /**
     * Экран запуска Deep Link и Intent через ADB.
     *
     * @param prefillUri URI для предзаполнения поля Deep Link.
     *                   Применяется только при первом создании компонента.
     */
    data class DeepLinks(val prefillUri: String? = null) : Screen {
        override fun equals(other: Any?) = other is DeepLinks
        override fun hashCode() = javaClass.hashCode()
    }

    /** Экран просмотра и анализа уведомлений Android-устройства. */
    data object Notifications : Screen

    /** Экран подробной информации об устройстве и системе. */
    data object DeviceInfo : Screen

    /** Экран быстрых QA-тумблеров (Wi-Fi/Data/Bluetooth и др.). */
    data object QuickToggles : Screen

    /** Экран управления scrcpy (зеркалирование экрана устройства). */
    data object Scrcpy : Screen
}

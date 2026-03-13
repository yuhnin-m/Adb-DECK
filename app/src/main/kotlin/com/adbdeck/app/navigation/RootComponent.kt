package com.adbdeck.app.navigation

import com.adbdeck.feature.contacts.ContactsComponent
import com.adbdeck.feature.notifications.NotificationsComponent
import com.adbdeck.feature.dashboard.DashboardComponent
import com.adbdeck.feature.scrcpy.ScrcpyComponent
import com.adbdeck.feature.devices.DevicesComponent
import com.adbdeck.feature.apkinstall.ApkInstallComponent
import com.adbdeck.feature.deviceinfo.DeviceInfoComponent
import com.adbdeck.feature.filesystem.FileSystemComponent
import com.adbdeck.feature.fileexplorer.FileExplorerComponent
import com.adbdeck.feature.logcat.LogcatComponent
import com.adbdeck.feature.packages.PackagesComponent
import com.adbdeck.feature.deeplinks.DeepLinksComponent
import com.adbdeck.feature.screentools.ScreenToolsComponent
import com.adbdeck.feature.settings.SettingsComponent
import com.adbdeck.feature.systemmonitor.SystemMonitorComponent
import com.adbdeck.feature.quicktoggles.QuickTogglesComponent
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.value.Value

/**
 * Корневой компонент навигации.
 *
 * Управляет стеком дочерних компонентов через Decompose [ChildStack].
 * Desktop-навигация: переключение разделов через боковую панель,
 * без привычного mobile-стека "назад-вперед".
 */
interface RootComponent {

    /** Текущий стек дочерних компонентов (наблюдаемый Value из Decompose). */
    val childStack: Value<ChildStack<*, Child>>

    /**
     * Перейти на экран [screen], выдвинув его наверх стека.
     *
     * @param screen Конфигурация целевого экрана.
     */
    fun navigate(screen: Screen)

    /**
     * Возможные дочерние компоненты root stack.
     *
     * Каждый Child оборачивает конкретный компонент экрана.
     */
    sealed class Child {
        /** Дочерний компонент Dashboard. */
        class Dashboard(val component: DashboardComponent) : Child()

        /** Дочерний компонент Devices. */
        class Devices(val component: DevicesComponent) : Child()

        /** Дочерний компонент Logcat. */
        class Logcat(val component: LogcatComponent) : Child()

        /** Дочерний компонент Settings. */
        class Settings(val component: SettingsComponent) : Child()

        /** Дочерний компонент Packages. */
        class Packages(val component: PackagesComponent) : Child()

        /** Дочерний компонент System Monitor. */
        class SystemMonitor(val component: SystemMonitorComponent) : Child()

        /** Дочерний компонент File System. */
        class FileSystem(val component: FileSystemComponent) : Child()

        /** Дочерний компонент File Explorer. */
        class FileExplorer(val component: FileExplorerComponent) : Child()

        /** Дочерний компонент Contacts. */
        class Contacts(val component: ContactsComponent) : Child()

        /** Дочерний компонент Screen Tools. */
        class ScreenTools(val component: ScreenToolsComponent) : Child()

        /** Дочерний компонент APK Install. */
        class ApkInstall(val component: ApkInstallComponent) : Child()

        /** Дочерний компонент Deep Links / Intents. */
        class DeepLinks(val component: DeepLinksComponent) : Child()

        /** Дочерний компонент Notifications. */
        class Notifications(val component: NotificationsComponent) : Child()

        /** Дочерний компонент Device Info. */
        class DeviceInfo(val component: DeviceInfoComponent) : Child()

        /** Дочерний компонент Quick Toggles. */
        class QuickToggles(val component: QuickTogglesComponent) : Child()

        /** Дочерний компонент Scrcpy. */
        class Scrcpy(val component: ScrcpyComponent) : Child()
    }
}

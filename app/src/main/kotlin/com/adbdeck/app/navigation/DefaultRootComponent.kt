package com.adbdeck.app.navigation

import com.adbdeck.core.adb.api.adb.AdbClient
import com.adbdeck.core.adb.api.adb.BundletoolClient
import com.adbdeck.core.adb.api.apkinstall.ApkInstallClient
import com.adbdeck.core.adb.api.contacts.ContactsClient
import com.adbdeck.core.adb.api.device.DeviceControlClient
import com.adbdeck.core.adb.api.intents.IntentLaunchClient
import com.adbdeck.core.adb.api.notifications.NotificationsClient
import com.adbdeck.core.adb.api.files.DeviceFileClient
import com.adbdeck.core.adb.api.device.DeviceInfoClient
import com.adbdeck.core.adb.api.device.DeviceManager
import com.adbdeck.core.adb.api.logcat.LogcatStreamer
import com.adbdeck.core.adb.api.packages.PackageClient
import com.adbdeck.core.adb.api.screen.ScreenToolsClient
import com.adbdeck.core.adb.api.scrcpy.ScrcpyClient
import com.adbdeck.core.adb.api.monitoring.SystemMonitorClient
import com.adbdeck.core.settings.SettingsRepository
import com.adbdeck.feature.contacts.DefaultContactsComponent
import com.adbdeck.feature.dashboard.DefaultDashboardComponent
import com.adbdeck.feature.scrcpy.DefaultScrcpyComponent
import com.adbdeck.feature.deeplinks.DefaultDeepLinksComponent
import com.adbdeck.feature.deviceinfo.DefaultDeviceInfoComponent
import com.adbdeck.feature.notifications.DefaultNotificationsComponent
import com.adbdeck.feature.devices.DefaultDevicesComponent
import com.adbdeck.feature.apkinstall.createApkInstallComponent
import com.adbdeck.feature.filesystem.DefaultFileSystemComponent
import com.adbdeck.feature.fileexplorer.createFileExplorerComponent
import com.adbdeck.feature.logcat.DefaultLogcatComponent
import com.adbdeck.feature.packages.DefaultPackagesComponent
import com.adbdeck.feature.quicktoggles.DefaultQuickTogglesComponent
import com.adbdeck.feature.quicktoggles.service.DefaultQuickTogglesService
import com.adbdeck.feature.screentools.createScreenToolsComponent
import com.adbdeck.feature.settings.DefaultSettingsComponent
import com.adbdeck.feature.deviceinfo.service.DefaultDeviceInfoService
import com.adbdeck.feature.systemmonitor.DefaultSystemMonitorComponent
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.bringToFront
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.value.Value

/**
 * Реализация [RootComponent].
 *
 * Создает child stack через Decompose [StackNavigation].
 * Каждый экран представлен своим [Screen] конфигом — при переключении
 * Decompose сохраняет жизненный цикл компонентов в стеке.
 *
 * @param componentContext     Контекст Decompose root-компонента.
 * @param adbClient            ADB-клиент, передается в нужные дочерние компоненты.
 * @param settingsRepository   Репозиторий настроек.
 * @param deviceManager        Singleton менеджер устройств — передается в LogcatComponent.
 * @param logcatStreamer        Singleton streamer logcat — передается в LogcatComponent.
 * @param packageClient        ADB-клиент для работы с пакетами — передается в PackagesComponent.
 * @param systemMonitorClient  ADB-клиент мониторинга процессов и хранилища.
 * @param deviceInfoClient     ADB-клиент расширенной информации об устройстве.
 * @param deviceControlClient  ADB-клиент управления устройством (перезагрузка, disconnect).
 * @param deviceFileClient     ADB-клиент файловых операций на устройстве.
 * @param contactsClient       ADB-клиент для работы с контактами.
 * @param screenToolsClient    ADB-клиент для screenshot/screenrecord.
 * @param apkInstallClient     ADB-клиент установки APK.
 * @param bundletoolClient     Клиент проверки bundletool для Settings.
 */
class DefaultRootComponent(
    componentContext: ComponentContext,
    private val adbClient: AdbClient,
    private val settingsRepository: SettingsRepository,
    private val deviceManager: DeviceManager,
    private val logcatStreamer: LogcatStreamer,
    private val packageClient: PackageClient,
    private val systemMonitorClient: SystemMonitorClient,
    private val deviceInfoClient: DeviceInfoClient,
    private val deviceControlClient: DeviceControlClient,
    private val deviceFileClient: DeviceFileClient,
    private val contactsClient: ContactsClient,
    private val screenToolsClient: ScreenToolsClient,
    private val scrcpyClient: ScrcpyClient,
    private val apkInstallClient: ApkInstallClient,
    private val bundletoolClient: BundletoolClient,
    private val intentLaunchClient: IntentLaunchClient,
    private val notificationsClient: NotificationsClient,
) : RootComponent, ComponentContext by componentContext {

    private val navigation = StackNavigation<Screen>()

    override val childStack: Value<ChildStack<*, RootComponent.Child>> = childStack(
        source = navigation,
        serializer = null, // восстановление стейта не требуется для десктопа ^_^
        initialConfiguration = Screen.Dashboard,
        handleBackButton = false, // и кнопки назад нет
        childFactory = ::createChild,
    )

    override fun navigate(screen: Screen) {
        // bringToFront переиспользует уже созданный компонент, если он есть в стеке
        navigation.bringToFront(screen)
    }

    /**
     * Перейти в Packages и выделить пакет по имени.
     *
     * Если компонент Packages уже есть в стеке — вызывает [onRevealPackage] напрямую
     * и поднимает экран наверх. Иначе открывает экран с аргументом [packageToReveal]
     * в конфиге — он будет прочитан в [createChild] при создании компонента.
     */
    private fun openPackageFromSystemMonitor(packageName: String) {
        val normalized = packageName.trim()
        if (normalized.isEmpty()) return

        val existing = childStack.value.items
            .asSequence()
            .map { it.instance }
            .filterIsInstance<RootComponent.Child.Packages>()
            .map { it.component }
            .firstOrNull()

        if (existing != null) {
            existing.onRevealPackage(normalized)
            navigation.bringToFront(Screen.Packages())
        } else {
            navigation.bringToFront(Screen.Packages(packageToReveal = normalized))
        }
    }

    /**
     * Перейти в Logcat и предзаполнить фильтр пакета.
     *
     * Если компонент Logcat уже есть в стеке — фильтр применяется напрямую.
     * Иначе открывает экран с аргументом [packageFilter] в конфиге.
     */
    private fun openPackageInLogcat(packageName: String) {
        val normalized = packageName.trim()
        if (normalized.isEmpty()) return

        val existing = childStack.value.items
            .asSequence()
            .map { it.instance }
            .filterIsInstance<RootComponent.Child.Logcat>()
            .map { it.component }
            .firstOrNull()

        if (existing != null) {
            existing.onPackageFilterChanged(normalized)
            navigation.bringToFront(Screen.Logcat())
        } else {
            navigation.bringToFront(Screen.Logcat(packageFilter = normalized))
        }
    }

    /**
     * Перейти в Deep Links и предзаполнить URI из уведомления.
     *
     * Если компонент DeepLinks уже есть в стеке — URI передаётся напрямую.
     * Иначе открывает экран с аргументом [prefillUri] в конфиге.
     */
    private fun openDeepLinkFromNotifications(uri: String) {
        val existing = childStack.value.items
            .asSequence()
            .map { it.instance }
            .filterIsInstance<RootComponent.Child.DeepLinks>()
            .map { it.component }
            .firstOrNull()

        if (existing != null) {
            existing.prefillDeepLinkUri(uri)
            navigation.bringToFront(Screen.DeepLinks())
        } else {
            navigation.bringToFront(Screen.DeepLinks(prefillUri = uri))
        }
    }

    /**
     * Перейти в File Explorer и открыть путь на устройстве.
     *
     * Если компонент FileExplorer уже есть в стеке — путь применяется напрямую.
     * Иначе открывает экран с аргументом [initialPath] в конфиге.
     */
    private fun openPathInFileExplorer(path: String) {
        val normalized = path.trim()
        if (normalized.isEmpty()) return

        val existing = childStack.value.items
            .asSequence()
            .map { it.instance }
            .filterIsInstance<RootComponent.Child.FileExplorer>()
            .map { it.component }
            .firstOrNull()

        if (existing != null) {
            existing.onSelectDeviceRoot(normalized)
            navigation.bringToFront(Screen.FileExplorer())
        } else {
            navigation.bringToFront(Screen.FileExplorer(initialPath = normalized))
        }
    }

    /**
     * Фабрика дочерних компонентов — вызывается Decompose при создании нового Child.
     *
     * @param screen           Конфигурация запрошенного экрана.
     * @param componentContext Контекст нового дочернего компонента.
     */
    private fun createChild(
        screen: Screen,
        componentContext: ComponentContext,
    ): RootComponent.Child = when (screen) {
        is Screen.Dashboard -> RootComponent.Child.Dashboard(
            DefaultDashboardComponent(
                componentContext = componentContext,
                adbClient = adbClient,
                deviceManager = deviceManager,
                settingsRepository = settingsRepository,
                onNavigateToDevices = { navigate(Screen.Devices) },
                onNavigateToDeviceInfo = { navigate(Screen.DeviceInfo) },
                onNavigateToQuickToggles = { navigate(Screen.QuickToggles) },
                onNavigateToLogcat = { navigate(Screen.Logcat()) },
                onNavigateToPackages = { navigate(Screen.Packages()) },
                onNavigateToApkInstall = { navigate(Screen.ApkInstall) },
                onNavigateToDeepLinks = { navigate(Screen.DeepLinks()) },
                onNavigateToNotifications = { navigate(Screen.Notifications) },
                onNavigateToScreenTools = { navigate(Screen.ScreenTools) },
                onNavigateToScrcpy = { navigate(Screen.Scrcpy) },
                onNavigateToFileExplorer = { navigate(Screen.FileExplorer()) },
                onNavigateToFileSystem = { navigate(Screen.FileSystem) },
                onNavigateToContacts = { navigate(Screen.Contacts) },
                onNavigateToSystemMonitor = { navigate(Screen.SystemMonitor) },
                onNavigateToSettings = { navigate(Screen.Settings) },
            )
        )

        is Screen.Devices -> RootComponent.Child.Devices(
            DefaultDevicesComponent(
                componentContext          = componentContext,
                deviceManager             = deviceManager,
                deviceInfoClient          = deviceInfoClient,
                deviceControlClient       = deviceControlClient,
                settingsRepository        = settingsRepository,
                onNavigateToLogcat        = { navigate(Screen.Logcat()) },
                onNavigateToPackages      = { navigate(Screen.Packages()) },
                onNavigateToSystemMonitor = { navigate(Screen.SystemMonitor) },
            )
        )

        is Screen.Logcat -> RootComponent.Child.Logcat(
            DefaultLogcatComponent(
                componentContext = componentContext,
                deviceManager = deviceManager,
                logcatStreamer = logcatStreamer,
                systemMonitorClient = systemMonitorClient,
                packageClient = packageClient,
                settingsRepository = settingsRepository,
            ).also { component ->
                // Аргумент из конфига применяется только при первом создании компонента
                screen.packageFilter?.also(component::onPackageFilterChanged)
            }
        )

        is Screen.Settings -> RootComponent.Child.Settings(
            DefaultSettingsComponent(
                componentContext = componentContext,
                adbClient = adbClient,
                bundletoolClient = bundletoolClient,
                scrcpyClient = scrcpyClient,
                settingsRepository = settingsRepository,
            )
        )

        is Screen.Packages -> RootComponent.Child.Packages(
            DefaultPackagesComponent(
                componentContext = componentContext,
                deviceManager = deviceManager,
                packageClient = packageClient,
                settingsRepository = settingsRepository,
                openPackageInLogcat = ::openPackageInLogcat,
                // Аргумент из конфига применяется только при первом создании компонента
                initialPackageToReveal = screen.packageToReveal,
            )
        )

        is Screen.SystemMonitor -> RootComponent.Child.SystemMonitor(
            DefaultSystemMonitorComponent(
                componentContext = componentContext,
                deviceManager = deviceManager,
                systemMonitorClient = systemMonitorClient,
                packageClient = packageClient,
                settingsRepository = settingsRepository,
                openPackageDetails = ::openPackageFromSystemMonitor,
            )
        )

        is Screen.FileSystem -> RootComponent.Child.FileSystem(
            DefaultFileSystemComponent(
                componentContext = componentContext,
                deviceManager = deviceManager,
                systemMonitorClient = systemMonitorClient,
                deviceFileClient = deviceFileClient,
                settingsRepository = settingsRepository,
                openInFileExplorer = ::openPathInFileExplorer,
            )
        )

        is Screen.FileExplorer -> RootComponent.Child.FileExplorer(
            createFileExplorerComponent(
                componentContext = componentContext,
                deviceManager = deviceManager,
                settingsRepository = settingsRepository,
                systemMonitorClient = systemMonitorClient,
                deviceFileClient = deviceFileClient,
            ).also { component ->
                // Аргумент из конфига применяется только при первом создании компонента
                screen.initialPath?.also(component::onSelectDeviceRoot)
            }
        )

        is Screen.Contacts -> RootComponent.Child.Contacts(
            DefaultContactsComponent(
                componentContext   = componentContext,
                deviceManager      = deviceManager,
                contactsClient     = contactsClient,
                settingsRepository = settingsRepository,
            )
        )

        is Screen.ScreenTools -> RootComponent.Child.ScreenTools(
            createScreenToolsComponent(
                componentContext = componentContext,
                deviceManager = deviceManager,
                settingsRepository = settingsRepository,
                screenToolsClient = screenToolsClient,
                deviceFileClient = deviceFileClient,
            )
        )

        is Screen.ApkInstall -> RootComponent.Child.ApkInstall(
            createApkInstallComponent(
                componentContext = componentContext,
                deviceManager = deviceManager,
                settingsRepository = settingsRepository,
                apkInstallClient = apkInstallClient,
            )
        )

        is Screen.DeepLinks -> RootComponent.Child.DeepLinks(
            DefaultDeepLinksComponent(
                componentContext   = componentContext,
                deviceManager      = deviceManager,
                packageClient      = packageClient,
                intentLaunchClient = intentLaunchClient,
                settingsRepository = settingsRepository,
                // Аргумент из конфига применяется только при первом создании компонента
                initialDeepLinkUri = screen.prefillUri,
            )
        )

        is Screen.Notifications -> RootComponent.Child.Notifications(
            DefaultNotificationsComponent(
                componentContext    = componentContext,
                deviceManager       = deviceManager,
                notificationsClient = notificationsClient,
                settingsRepository  = settingsRepository,
                onOpenInPackages    = ::openPackageFromSystemMonitor,
                onOpenInDeepLinks   = ::openDeepLinkFromNotifications,
            )
        )

        is Screen.DeviceInfo -> RootComponent.Child.DeviceInfo(
            DefaultDeviceInfoComponent(
                componentContext = componentContext,
                deviceManager = deviceManager,
                settingsRepository = settingsRepository,
                deviceInfoService = DefaultDeviceInfoService(deviceInfoClient),
            )
        )

        is Screen.QuickToggles -> RootComponent.Child.QuickToggles(
            DefaultQuickTogglesComponent(
                componentContext = componentContext,
                deviceManager = deviceManager,
                settingsRepository = settingsRepository,
                quickTogglesService = DefaultQuickTogglesService(deviceInfoClient),
            )
        )

        is Screen.Scrcpy -> RootComponent.Child.Scrcpy(
            DefaultScrcpyComponent(
                componentContext = componentContext,
                deviceManager = deviceManager,
                settingsRepository = settingsRepository,
                scrcpyClient = scrcpyClient,
                onOpenSettings = { navigate(Screen.Settings) },
            )
        )
    }
}

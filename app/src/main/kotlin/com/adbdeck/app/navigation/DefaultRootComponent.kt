package com.adbdeck.app.navigation

import com.adbdeck.core.adb.api.AdbClient
import com.adbdeck.core.adb.api.ContactsClient
import com.adbdeck.core.adb.api.DeviceControlClient
import com.adbdeck.core.adb.api.DeviceFileClient
import com.adbdeck.core.adb.api.DeviceInfoClient
import com.adbdeck.core.adb.api.DeviceManager
import com.adbdeck.core.adb.api.LogcatStreamer
import com.adbdeck.core.adb.api.PackageClient
import com.adbdeck.core.adb.api.ScreenToolsClient
import com.adbdeck.core.adb.api.SystemMonitorClient
import com.adbdeck.core.settings.SettingsRepository
import com.adbdeck.feature.contacts.DefaultContactsComponent
import com.adbdeck.feature.dashboard.DefaultDashboardComponent
import com.adbdeck.feature.devices.DefaultDevicesComponent
import com.adbdeck.feature.apkinstall.DefaultApkInstallComponent
import com.adbdeck.feature.apkinstall.service.DefaultApkInstallService
import com.adbdeck.feature.fileexplorer.DefaultFileExplorerComponent
import com.adbdeck.feature.logcat.DefaultLogcatComponent
import com.adbdeck.feature.packages.DefaultPackagesComponent
import com.adbdeck.feature.screentools.DefaultScreenToolsComponent
import com.adbdeck.feature.screentools.service.DefaultHostFileService
import com.adbdeck.feature.screentools.service.DefaultScreenrecordService
import com.adbdeck.feature.screentools.service.DefaultScreenshotService
import com.adbdeck.feature.settings.DefaultSettingsComponent
import com.adbdeck.feature.fileexplorer.service.DefaultDeviceFileService
import com.adbdeck.feature.fileexplorer.service.DefaultFileTransferService
import com.adbdeck.feature.fileexplorer.service.DefaultLocalFileService
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
) : RootComponent, ComponentContext by componentContext {

    private val navigation = StackNavigation<Screen>()
    private var pendingPackageToReveal: String? = null

    override val childStack: Value<ChildStack<*, RootComponent.Child>> = childStack(
        source = navigation,
        serializer = null, // state restoration не требуется для desktop
        initialConfiguration = Screen.Dashboard,
        handleBackButton = false, // desktop-UX: нет мобильной кнопки "назад"
        childFactory = ::createChild,
    )

    override fun navigate(screen: Screen) {
        // bringToFront переиспользует уже созданный компонент, если он есть в стеке
        navigation.bringToFront(screen)
    }

    private fun openPackageFromSystemMonitor(packageName: String) {
        val normalized = packageName.trim()
        if (normalized.isEmpty()) return

        val existingPackages = childStack.value.items
            .asSequence()
            .map { it.instance }
            .filterIsInstance<RootComponent.Child.Packages>()
            .map { it.component }
            .firstOrNull()

        if (existingPackages != null) {
            existingPackages.onRevealPackage(normalized)
            navigate(Screen.Packages)
        } else {
            pendingPackageToReveal = normalized
            navigate(Screen.Packages)
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
                onNavigateToDevices = { navigate(Screen.Devices) },
                onNavigateToLogcat = { navigate(Screen.Logcat) },
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
                onNavigateToLogcat        = { navigate(Screen.Logcat) },
                onNavigateToPackages      = { navigate(Screen.Packages) },
                onNavigateToSystemMonitor = { navigate(Screen.SystemMonitor) },
            )
        )

        is Screen.Logcat -> RootComponent.Child.Logcat(
            DefaultLogcatComponent(
                componentContext = componentContext,
                deviceManager = deviceManager,
                logcatStreamer = logcatStreamer,
                settingsRepository = settingsRepository,
            )
        )

        is Screen.Settings -> RootComponent.Child.Settings(
            DefaultSettingsComponent(
                componentContext = componentContext,
                adbClient = adbClient,
                settingsRepository = settingsRepository,
            )
        )

        is Screen.Packages -> RootComponent.Child.Packages(
            DefaultPackagesComponent(
                componentContext = componentContext,
                deviceManager = deviceManager,
                packageClient = packageClient,
                settingsRepository = settingsRepository,
                initialPackageToReveal = pendingPackageToReveal.also { pendingPackageToReveal = null },
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

        is Screen.FileExplorer -> RootComponent.Child.FileExplorer(
            run {
                val localFileService = DefaultLocalFileService()
                val deviceFileService = DefaultDeviceFileService(deviceFileClient)
                val transferService = DefaultFileTransferService(
                    localFileService = localFileService,
                    deviceFileService = deviceFileService,
                )

                DefaultFileExplorerComponent(
                    componentContext = componentContext,
                    deviceManager = deviceManager,
                    settingsRepository = settingsRepository,
                    systemMonitorClient = systemMonitorClient,
                    localFileService = localFileService,
                    deviceFileService = deviceFileService,
                    fileTransferService = transferService,
                )
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
            run {
                val screenshotService = DefaultScreenshotService(screenToolsClient)
                val screenrecordService = DefaultScreenrecordService(
                    screenToolsClient = screenToolsClient,
                    deviceFileClient = deviceFileClient,
                )
                val hostFileService = DefaultHostFileService()

                DefaultScreenToolsComponent(
                    componentContext = componentContext,
                    deviceManager = deviceManager,
                    settingsRepository = settingsRepository,
                    screenshotService = screenshotService,
                    screenrecordService = screenrecordService,
                    hostFileService = hostFileService,
                )
            }
        )

        is Screen.ApkInstall -> RootComponent.Child.ApkInstall(
            run {
                val apkInstallService = DefaultApkInstallService(screenToolsClient)
                DefaultApkInstallComponent(
                    componentContext = componentContext,
                    deviceManager = deviceManager,
                    settingsRepository = settingsRepository,
                    apkInstallService = apkInstallService,
                )
            }
        )
    }
}

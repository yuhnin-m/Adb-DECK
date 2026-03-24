package com.adbdeck.app.navigation

import com.adbdeck.core.adb.api.adb.AdbClient
import com.adbdeck.core.adb.api.adb.BundletoolClient
import com.adbdeck.core.adb.api.apkinstall.ApkInstallClient
import com.adbdeck.core.adb.api.contacts.ContactsClient
import com.adbdeck.core.adb.api.device.DeviceControlClient
import com.adbdeck.core.adb.api.device.DeviceInfoClient
import com.adbdeck.core.adb.api.device.DeviceManager
import com.adbdeck.core.adb.api.files.DeviceFileClient
import com.adbdeck.core.adb.api.intents.IntentLaunchClient
import com.adbdeck.core.adb.api.logcat.LogcatStreamer
import com.adbdeck.core.adb.api.monitoring.SystemMonitorClient
import com.adbdeck.core.adb.api.notifications.NotificationsClient
import com.adbdeck.core.adb.api.packages.PackageClient
import com.adbdeck.core.adb.api.screen.ScreenToolsClient
import com.adbdeck.core.adb.api.scrcpy.ScrcpyClient
import com.adbdeck.core.settings.SettingsRepository
import com.adbdeck.feature.apkinstall.createApkInstallComponent
import com.adbdeck.feature.contacts.DefaultContactsComponent
import com.adbdeck.feature.dashboard.DashboardAppUpdateBanner
import com.adbdeck.feature.dashboard.DefaultDashboardComponent
import com.adbdeck.feature.deeplinks.DefaultDeepLinksComponent
import com.adbdeck.feature.deviceinfo.DefaultDeviceInfoComponent
import com.adbdeck.feature.deviceinfo.service.DefaultDeviceInfoService
import com.adbdeck.feature.devices.DefaultDevicesComponent
import com.adbdeck.feature.fileexplorer.createFileExplorerComponent
import com.adbdeck.feature.filesystem.DefaultFileSystemComponent
import com.adbdeck.feature.logcat.DefaultLogcatComponent
import com.adbdeck.feature.notifications.DefaultNotificationsComponent
import com.adbdeck.feature.packages.DefaultPackagesComponent
import com.adbdeck.feature.quicktoggles.DefaultQuickTogglesComponent
import com.adbdeck.feature.quicktoggles.service.DefaultQuickTogglesService
import com.adbdeck.feature.screentools.createScreenToolsComponent
import com.adbdeck.feature.scrcpy.DefaultScrcpyComponent
import com.adbdeck.feature.settings.DefaultSettingsComponent
import com.adbdeck.feature.systemmonitor.DefaultSystemMonitorComponent
import com.arkivanov.decompose.ComponentContext
import kotlinx.coroutines.flow.Flow

/**
 * Реализация [RootChildFactory], которая знает как собрать каждый feature-child.
 */
class DefaultRootChildFactory(
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
) : RootChildFactory {

    override fun createChild(
        screen: Screen,
        componentContext: ComponentContext,
        navigate: (Screen) -> Unit,
        openPackageFromSystemMonitor: (String) -> Unit,
        openPackageInLogcat: (String) -> Unit,
        openDeepLinkFromNotifications: (String) -> Unit,
        openPathInFileExplorer: (String) -> Unit,
        dashboardAppUpdateFlow: Flow<DashboardAppUpdateBanner?>,
        checkForAppUpdates: suspend () -> Result<Boolean>,
    ): RootComponent.Child = when (screen) {
        is Screen.Dashboard -> RootComponent.Child.Dashboard(
            DefaultDashboardComponent(
                componentContext = componentContext,
                adbClient = adbClient,
                deviceManager = deviceManager,
                settingsRepository = settingsRepository,
                availableAppUpdateFlow = dashboardAppUpdateFlow,
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
                componentContext = componentContext,
                deviceManager = deviceManager,
                deviceInfoClient = deviceInfoClient,
                deviceControlClient = deviceControlClient,
                settingsRepository = settingsRepository,
                onNavigateToLogcat = { navigate(Screen.Logcat()) },
                onNavigateToPackages = { navigate(Screen.Packages()) },
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
                checkForAppUpdates = checkForAppUpdates,
            )
        )

        is Screen.Packages -> RootComponent.Child.Packages(
            DefaultPackagesComponent(
                componentContext = componentContext,
                deviceManager = deviceManager,
                packageClient = packageClient,
                settingsRepository = settingsRepository,
                openPackageInLogcat = openPackageInLogcat,
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
                openPackageDetails = openPackageFromSystemMonitor,
            )
        )

        is Screen.FileSystem -> RootComponent.Child.FileSystem(
            DefaultFileSystemComponent(
                componentContext = componentContext,
                deviceManager = deviceManager,
                systemMonitorClient = systemMonitorClient,
                deviceFileClient = deviceFileClient,
                settingsRepository = settingsRepository,
                openInFileExplorer = openPathInFileExplorer,
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
                componentContext = componentContext,
                deviceManager = deviceManager,
                contactsClient = contactsClient,
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
                componentContext = componentContext,
                deviceManager = deviceManager,
                packageClient = packageClient,
                intentLaunchClient = intentLaunchClient,
                settingsRepository = settingsRepository,
                // Аргумент из конфига применяется только при первом создании компонента
                initialDeepLinkUri = screen.prefillUri,
            )
        )

        is Screen.Notifications -> RootComponent.Child.Notifications(
            DefaultNotificationsComponent(
                componentContext = componentContext,
                deviceManager = deviceManager,
                notificationsClient = notificationsClient,
                settingsRepository = settingsRepository,
                onOpenInPackages = openPackageFromSystemMonitor,
                onOpenInDeepLinks = openDeepLinkFromNotifications,
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

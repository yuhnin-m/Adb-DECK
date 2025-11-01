package com.adbdeck.app.navigation

import com.adbdeck.core.adb.api.AdbClient
import com.adbdeck.core.adb.api.DeviceControlClient
import com.adbdeck.core.adb.api.DeviceInfoClient
import com.adbdeck.core.adb.api.DeviceManager
import com.adbdeck.core.adb.api.LogcatStreamer
import com.adbdeck.core.adb.api.PackageClient
import com.adbdeck.core.adb.api.SystemMonitorClient
import com.adbdeck.core.settings.SettingsRepository
import com.adbdeck.feature.dashboard.DefaultDashboardComponent
import com.adbdeck.feature.devices.DefaultDevicesComponent
import com.adbdeck.feature.logcat.DefaultLogcatComponent
import com.adbdeck.feature.packages.DefaultPackagesComponent
import com.adbdeck.feature.settings.DefaultSettingsComponent
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
    }
}

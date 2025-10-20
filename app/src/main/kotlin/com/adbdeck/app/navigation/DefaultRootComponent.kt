package com.adbdeck.app.navigation

import com.adbdeck.core.adb.api.AdbClient
import com.adbdeck.core.adb.api.DeviceManager
import com.adbdeck.core.adb.api.LogcatStreamer
import com.adbdeck.core.settings.SettingsRepository
import com.adbdeck.feature.dashboard.DefaultDashboardComponent
import com.adbdeck.feature.devices.DefaultDevicesComponent
import com.adbdeck.feature.logcat.DefaultLogcatComponent
import com.adbdeck.feature.settings.DefaultSettingsComponent
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
 * @param componentContext   Контекст Decompose root-компонента.
 * @param adbClient          ADB-клиент, передается в нужные дочерние компоненты.
 * @param settingsRepository Репозиторий настроек.
 * @param deviceManager      Singleton менеджер устройств — передается в LogcatComponent.
 * @param logcatStreamer      Singleton streamer logcat — передается в LogcatComponent.
 */
class DefaultRootComponent(
    componentContext: ComponentContext,
    private val adbClient: AdbClient,
    private val settingsRepository: SettingsRepository,
    private val deviceManager: DeviceManager,
    private val logcatStreamer: LogcatStreamer,
) : RootComponent, ComponentContext by componentContext {

    private val navigation = StackNavigation<Screen>()

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
                componentContext = componentContext,
                deviceManager = deviceManager,
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
    }
}

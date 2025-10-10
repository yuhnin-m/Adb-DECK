package com.adbdeck.app.navigation

import com.adbdeck.feature.dashboard.DashboardComponent
import com.adbdeck.feature.devices.DevicesComponent
import com.adbdeck.feature.logcat.LogcatComponent
import com.adbdeck.feature.settings.SettingsComponent
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.value.Value

/**
 * Корневой компонент навигации.
 *
 * Управляет стеком дочерних компонентов через Decompose [ChildStack].
 * Desktop-навигация: переключение разделов через боковую панель,
 * без привычного mobile-стека "назад-вперёд".
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
    }
}

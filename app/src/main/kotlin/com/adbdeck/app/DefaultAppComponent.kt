package com.adbdeck.app

import com.adbdeck.app.devicemanager.DeviceSelectorComponent
import com.adbdeck.app.navigation.RootComponent

/**
 * Реализация [AppComponent].
 *
 * Тонкий контейнер уже собранных верхнеуровневых компонентов приложения.
 */
class DefaultAppComponent(
    override val rootComponent: RootComponent,
    override val deviceSelectorComponent: DeviceSelectorComponent,
) : AppComponent

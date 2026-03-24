package com.adbdeck.app

import com.adbdeck.app.devicemanager.DeviceSelectorComponent
import com.adbdeck.app.navigation.RootComponent
import com.adbdeck.feature.update.AppUpdateComponent

/**
 * Верхнеуровневый компонент приложения.
 *
 * Держит глобальные компоненты, которые должны жить в одном Decompose lifecycle:
 * - [rootComponent] — навигация между экранами.
 * - [deviceSelectorComponent] — глобальный селектор устройства в шапке.
 * - [appUpdateComponent] — управление диалогом обновления приложения.
 */
interface AppComponent {
    val rootComponent: RootComponent
    val deviceSelectorComponent: DeviceSelectorComponent
    val appUpdateComponent: AppUpdateComponent
}

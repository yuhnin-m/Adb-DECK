package com.adbdeck.app.navigation

import com.arkivanov.decompose.ComponentContext

/**
 * Фабрика дочерних компонентов root-навигации.
 *
 * Выносит связывание feature-зависимостей из [DefaultRootComponent], чтобы root
 * отвечал только за навигацию и переиспользование инстансов в стеке.
 */
interface RootChildFactory {
    fun createChild(
        screen: Screen,
        componentContext: ComponentContext,
        navigate: (Screen) -> Unit,
        openPackageFromSystemMonitor: (String) -> Unit,
        openPackageInLogcat: (String) -> Unit,
        openDeepLinkFromNotifications: (String) -> Unit,
        openPathInFileExplorer: (String) -> Unit,
    ): RootComponent.Child
}


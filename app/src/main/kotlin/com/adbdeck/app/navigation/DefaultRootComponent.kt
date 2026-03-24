package com.adbdeck.app.navigation

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.bringToFront
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.value.Value
import com.adbdeck.core.utils.runCatchingPreserveCancellation
import com.adbdeck.feature.dashboard.DashboardAppUpdateBanner
import com.adbdeck.feature.update.AppUpdateComponent
import com.adbdeck.feature.update.AppUpdatePhase
import com.adbdeck.feature.update.AppUpdateUiState
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/**
 * Реализация [RootComponent].
 *
 * Создает child stack через Decompose [StackNavigation].
 * Каждый экран представлен своим [Screen] конфигом — при переключении
 * Decompose сохраняет жизненный цикл компонентов в стеке.
 *
 * @param componentContext Контекст Decompose root-компонента.
 * @param rootChildFactory Фабрика дочерних компонентов root-stack.
 */
class DefaultRootComponent(
    componentContext: ComponentContext,
    private val rootChildFactory: RootChildFactory,
    private val appUpdateComponent: AppUpdateComponent,
) : RootComponent, ComponentContext by componentContext {

    private val navigation = StackNavigation<Screen>()
    private val dashboardAppUpdateFlow = appUpdateComponent.state
        .map { it.toDashboardAppUpdateBanner() }
        .distinctUntilChanged()

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

    private suspend fun checkForAppUpdates(): Result<Boolean> = runCatchingPreserveCancellation {
        appUpdateComponent.checkForUpdatesNow()
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
    ): RootComponent.Child =
        rootChildFactory.createChild(
            screen = screen,
            componentContext = componentContext,
            navigate = ::navigate,
            openPackageFromSystemMonitor = ::openPackageFromSystemMonitor,
            openPackageInLogcat = ::openPackageInLogcat,
            openDeepLinkFromNotifications = ::openDeepLinkFromNotifications,
            openPathInFileExplorer = ::openPathInFileExplorer,
            openAppUpdate = appUpdateComponent::onInstallUpdateNow,
            dashboardAppUpdateFlow = dashboardAppUpdateFlow,
            checkForAppUpdates = ::checkForAppUpdates,
        )
}

private fun AppUpdateUiState.toDashboardAppUpdateBanner(): DashboardAppUpdateBanner? {
    if (phase != AppUpdatePhase.AVAILABLE) return null
    val version = targetVersion?.trim().orEmpty()
    if (version.isBlank()) return null
    return DashboardAppUpdateBanner(version = version)
}

package com.adbdeck.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.adbdeck.app.devicemanager.DeviceSelectorComponent
import com.adbdeck.app.navigation.RootComponent
import com.adbdeck.app.navigation.Screen
import com.adbdeck.feature.dashboard.ui.DashboardScreen
import com.adbdeck.feature.devices.ui.DevicesScreen
import com.adbdeck.feature.logcat.ui.LogcatScreen
import com.adbdeck.feature.settings.ui.SettingsScreen
import com.arkivanov.decompose.extensions.compose.subscribeAsState

/**
 * Корневой composable главного окна ADB Deck.
 *
 * Собирает три слоя desktop layout:
 * - [Sidebar] слева — навигация между разделами
 * - Основной контент справа:
 *   - [TopBar] с заголовком раздела и [DeviceBar] (выбор устройства)
 *   - ContentArea — активный экран
 *   - [StatusBar]
 *
 * @param rootComponent          Корневой навигационный компонент.
 * @param isDarkTheme            Текущий режим темы (для Sidebar).
 * @param onToggleTheme          Callback переключения темы.
 * @param deviceSelectorComponent Компонент выбора устройства для TopBar.
 */
@Composable
fun AppContent(
    rootComponent: RootComponent,
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit,
    deviceSelectorComponent: DeviceSelectorComponent,
) {
    // subscribeAsState() возвращает Compose State<ChildStack>
    val childStack by rootComponent.childStack.subscribeAsState()
    val activeChild = childStack.active.instance

    // Определяем активный экран по типу дочернего компонента
    val activeScreen: Screen = when (activeChild) {
        is RootComponent.Child.Dashboard -> Screen.Dashboard
        is RootComponent.Child.Devices -> Screen.Devices
        is RootComponent.Child.Logcat -> Screen.Logcat
        is RootComponent.Child.Settings -> Screen.Settings
        else -> Screen.Dashboard
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            // ── Sidebar ─────────────────────────────────────────
            Sidebar(
                activeScreen = activeScreen,
                onNavigate = rootComponent::navigate,
                isDarkTheme = isDarkTheme,
                onToggleTheme = onToggleTheme,
            )

            // ── Основная область ──────────────────────────────────
            Column(modifier = Modifier.fillMaxSize()) {
                // TopBar с DeviceBar в trailing-слоте
                TopBar(
                    title = activeScreen.title(),
                    trailingContent = {
                        DeviceBar(component = deviceSelectorComponent)
                    },
                )

                Box(modifier = Modifier.weight(1f)) {
                    // Рендерим активный экран напрямую по типу активного Child
                    when (val instance = activeChild) {
                        is RootComponent.Child.Dashboard -> DashboardScreen(instance.component)
                        is RootComponent.Child.Devices -> DevicesScreen(instance.component)
                        is RootComponent.Child.Logcat -> LogcatScreen(instance.component)
                        is RootComponent.Child.Settings -> SettingsScreen(instance.component)
                    }
                }

                StatusBar()
            }
        }
    }
}

/**
 * Возвращает заголовок TopBar для текущего экрана.
 */
private fun Screen.title(): String = when (this) {
    is Screen.Dashboard -> "Dashboard"
    is Screen.Devices -> "Devices"
    is Screen.Logcat -> "Logcat"
    is Screen.Settings -> "Settings"
}

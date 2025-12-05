package com.adbdeck.feature.dashboard

import com.adbdeck.core.adb.api.adb.AdbCheckResult
import com.adbdeck.core.adb.api.adb.AdbClient
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.lifecycle.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Реализация [DashboardComponent].
 *
 * Lifecycle компонента привязан к [ComponentContext], поэтому
 * coroutineScope автоматически отменяется при уничтожении компонента.
 *
 * @param componentContext Контекст Decompose-компонента.
 * @param adbClient ADB-клиент для проверки и получения устройств.
 * @param onNavigateToDevices Callback навигации на экран устройств.
 * @param onNavigateToLogcat  Callback навигации на экран logcat.
 * @param onNavigateToSettings Callback навигации на экран настроек.
 */
class DefaultDashboardComponent(
    componentContext: ComponentContext,
    private val adbClient: AdbClient,
    private val onNavigateToDevices: () -> Unit,
    private val onNavigateToLogcat: () -> Unit,
    private val onNavigateToSettings: () -> Unit,
) : DashboardComponent, ComponentContext by componentContext {

    private val scope = coroutineScope()

    private val _state = MutableStateFlow(DashboardState())
    override val state: StateFlow<DashboardState> = _state.asStateFlow()

    override fun onOpenDevices() = onNavigateToDevices()

    override fun onOpenLogcat() = onNavigateToLogcat()

    override fun onOpenSettings() = onNavigateToSettings()

    override fun onRefreshDevices() {
        if (_state.value.isRefreshingDevices) return
        scope.launch {
            _state.update { it.copy(isRefreshingDevices = true) }
            val result = adbClient.getDevices()
            _state.update {
                it.copy(
                    isRefreshingDevices = false,
                    deviceCount = result.getOrNull()?.size,
                )
            }
        }
    }

    override fun onCheckAdb() {
        if (_state.value.isCheckingAdb) return
        scope.launch {
            _state.update { it.copy(isCheckingAdb = true, adbStatusText = "") }
            val result = adbClient.checkAvailability()
            val statusText = when (result) {
                is AdbCheckResult.Available -> "✓ adb доступен: ${result.version}"
                is AdbCheckResult.NotAvailable -> "✗ adb недоступен: ${result.reason}"
            }
            _state.update { it.copy(isCheckingAdb = false, adbStatusText = statusText) }
        }
    }
}

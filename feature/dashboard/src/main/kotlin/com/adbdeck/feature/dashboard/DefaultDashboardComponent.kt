package com.adbdeck.feature.dashboard

import com.adbdeck.core.adb.api.adb.AdbCheckResult
import com.adbdeck.core.adb.api.adb.AdbClient
import com.adbdeck.core.adb.api.device.DeviceManager
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.lifecycle.coroutines.coroutineScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Реализация [DashboardComponent].
 *
 * Lifecycle компонента привязан к [ComponentContext], поэтому
 * coroutineScope автоматически отменяется при уничтожении компонента.
 *
 * @param componentContext Контекст Decompose-компонента.
 * @param adbClient ADB-клиент для проверки доступности.
 * @param deviceManager Менеджер устройств — единый источник списка устройств в приложении.
 * @param onNavigateToDevices Callback навигации на экран устройств.
 * @param onNavigateToLogcat  Callback навигации на экран logcat.
 * @param onNavigateToSettings Callback навигации на экран настроек.
 */
class DefaultDashboardComponent(
    componentContext: ComponentContext,
    private val adbClient: AdbClient,
    private val deviceManager: DeviceManager,
    private val onNavigateToDevices: () -> Unit,
    private val onNavigateToLogcat: () -> Unit,
    private val onNavigateToSettings: () -> Unit,
) : DashboardComponent, ComponentContext by componentContext {

    private val scope = coroutineScope()
    private var refreshJob: Job? = null
    private var adbCheckJob: Job? = null

    private val _state = MutableStateFlow(
        DashboardState(
            deviceCount = deviceManager.devicesFlow.value.size,
        )
    )
    override val state: StateFlow<DashboardState> = _state.asStateFlow()

    init {
        observeDevices()
    }

    override fun onOpenDevices() = onNavigateToDevices()

    override fun onOpenLogcat() = onNavigateToLogcat()

    override fun onOpenSettings() = onNavigateToSettings()

    override fun onRefreshDevices() {
        if (refreshJob?.isActive == true) return
        refreshJob = scope.launch {
            _state.update { it.copy(isRefreshingDevices = true, refreshError = null) }
            try {
                deviceManager.clearError()
                deviceManager.refresh()
                _state.update { current ->
                    current.copy(
                        isRefreshingDevices = false,
                        refreshError = deviceManager.errorFlow.value,
                    )
                }
            } catch (e: CancellationException) {
                _state.update { it.copy(isRefreshingDevices = false) }
                throw e
            } catch (e: Exception) {
                _state.update { current ->
                    current.copy(
                        isRefreshingDevices = false,
                        refreshError = e.message,
                    )
                }
            } finally {
                refreshJob = null
            }
        }
    }

    override fun onCheckAdb() {
        if (adbCheckJob?.isActive == true) return
        adbCheckJob = scope.launch {
            _state.update { it.copy(adbCheckState = DashboardAdbCheckState.Checking) }
            try {
                val result = adbClient.checkAvailability()
                val nextState = when (result) {
                    is AdbCheckResult.Available -> DashboardAdbCheckState.Available(result.version)
                    is AdbCheckResult.NotAvailable -> DashboardAdbCheckState.NotAvailable(result.reason)
                }
                _state.update { it.copy(adbCheckState = nextState) }
            } catch (e: CancellationException) {
                _state.update { it.copy(adbCheckState = DashboardAdbCheckState.Idle) }
                throw e
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        adbCheckState = DashboardAdbCheckState.NotAvailable(e.message.orEmpty()),
                    )
                }
            } finally {
                adbCheckJob = null
            }
        }
    }

    override fun onDismissAdbCheck() {
        _state.update { it.copy(adbCheckState = DashboardAdbCheckState.Idle) }
    }

    override fun onDismissRefreshError() {
        _state.update { it.copy(refreshError = null) }
    }

    private fun observeDevices() {
        scope.launch {
            deviceManager.devicesFlow.collect { devices ->
                _state.update { current ->
                    if (current.deviceCount == devices.size) {
                        current
                    } else {
                        current.copy(deviceCount = devices.size)
                    }
                }
            }
        }
    }
}

package com.adbdeck.feature.systemmonitor

import com.adbdeck.core.adb.api.device.DeviceManager
import com.adbdeck.core.adb.api.packages.PackageClient
import com.adbdeck.core.adb.api.monitoring.SystemMonitorClient
import com.adbdeck.core.settings.SettingsRepository
import com.adbdeck.feature.systemmonitor.processes.DefaultProcessesComponent
import com.adbdeck.feature.systemmonitor.processes.ProcessesComponent
import com.adbdeck.feature.systemmonitor.storage.DefaultStorageComponent
import com.adbdeck.feature.systemmonitor.storage.StorageComponent
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.lifecycle.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * Реализация [SystemMonitorComponent].
 *
 * ## Управление дочерними компонентами
 *
 * Оба sub-компонента создаются в конструкторе и разделяют одинаковый [ComponentContext].
 * Это допустимо: каждый sub-компонент получает отдельный [coroutineScope] из Essenty
 * и не конфликтует с другим.
 *
 * Примечание: в полноценном Decompose-приложении sub-компоненты можно создать через
 * `childContext(key)`. Здесь для простоты передаём родительский `componentContext` напрямую,
 * что корректно для статических (не навигационных) дочерних компонентов.
 *
 * ## Sidebar badge
 *
 * [isProcessMonitoring] — это `stateIn`-поток, транслирующий `state.isMonitoring`
 * из [processesComponent]. Используется в [com.adbdeck.app.ui.AppContent] для
 * передачи в Sidebar без прямой зависимости от ProcessesComponent.
 *
 * @param componentContext    Decompose-контекст.
 * @param deviceManager       Менеджер устройств (передаётся в оба sub-компонента).
 * @param systemMonitorClient ADB-клиент мониторинга (передаётся в оба sub-компонента).
 * @param settingsRepository  Репозиторий настроек (путь к adb).
 */
class DefaultSystemMonitorComponent(
    componentContext: ComponentContext,
    deviceManager: DeviceManager,
    systemMonitorClient: SystemMonitorClient,
    packageClient: PackageClient,
    settingsRepository: SettingsRepository,
    openPackageDetails: (String) -> Unit = {},
) : SystemMonitorComponent, ComponentContext by componentContext {

    private val scope = coroutineScope()

    private val _activeTab = MutableStateFlow(SystemMonitorTab.PROCESSES)
    override val activeTab: StateFlow<SystemMonitorTab> = _activeTab.asStateFlow()

    override val processesComponent: ProcessesComponent = DefaultProcessesComponent(
        componentContext = componentContext,
        deviceManager = deviceManager,
        systemMonitorClient = systemMonitorClient,
        packageClient = packageClient,
        settingsRepository = settingsRepository,
        openPackageDetails = openPackageDetails,
    )

    override val storageComponent: StorageComponent = DefaultStorageComponent(
        componentContext = componentContext,
        deviceManager = deviceManager,
        systemMonitorClient = systemMonitorClient,
        settingsRepository = settingsRepository,
    )

    /**
     * Транслирует `isMonitoring` из ProcessesComponent в виде отдельного StateFlow
     * для подписки в Sidebar (badge «MON»).
     */
    override val isProcessMonitoring: StateFlow<Boolean> =
        processesComponent.state
            .map { it.isMonitoring }
            .stateIn(
                scope = scope,
                started = SharingStarted.WhileSubscribed(5_000L),
                initialValue = false,
            )

    override fun onTabSelected(tab: SystemMonitorTab) {
        _activeTab.value = tab
    }
}

package com.adbdeck.feature.systemmonitor

import com.adbdeck.core.adb.api.device.DeviceManager
import com.adbdeck.core.adb.api.packages.PackageClient
import com.adbdeck.core.adb.api.monitoring.SystemMonitorClient
import com.adbdeck.core.settings.SettingsRepository
import com.adbdeck.feature.systemmonitor.processes.DefaultProcessesComponent
import com.adbdeck.feature.systemmonitor.processes.ProcessesComponent
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.lifecycle.coroutines.coroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * Реализация [SystemMonitorComponent].
 *
 * ## Управление дочерними компонентами
 *
 * Компонент процессов создаётся в конструкторе и разделяет одинаковый [ComponentContext].
 * Это допустимо: sub-компонент получает отдельный [coroutineScope] из Essenty.
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
 * @param deviceManager       Менеджер устройств.
 * @param systemMonitorClient ADB-клиент мониторинга.
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

    override val processesComponent: ProcessesComponent = DefaultProcessesComponent(
        componentContext = componentContext,
        deviceManager = deviceManager,
        systemMonitorClient = systemMonitorClient,
        packageClient = packageClient,
        settingsRepository = settingsRepository,
        openPackageDetails = openPackageDetails,
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
}

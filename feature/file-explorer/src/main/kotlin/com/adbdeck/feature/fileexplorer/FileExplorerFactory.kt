package com.adbdeck.feature.fileexplorer

import com.adbdeck.core.adb.api.device.DeviceManager
import com.adbdeck.core.adb.api.files.DeviceFileClient
import com.adbdeck.core.adb.api.monitoring.SystemMonitorClient
import com.adbdeck.core.settings.SettingsRepository
import com.adbdeck.feature.fileexplorer.service.DefaultDeviceFileService
import com.adbdeck.feature.fileexplorer.service.DefaultFileTransferService
import com.adbdeck.feature.fileexplorer.service.DefaultLocalFileService
import com.arkivanov.decompose.ComponentContext

/**
 * Фабрика создания компонента File Explorer с внутренней сборкой service-слоя.
 *
 * Нужна чтобы не дублировать инициализацию сервисов в root-навигации приложения.
 */
fun createFileExplorerComponent(
    componentContext: ComponentContext,
    deviceManager: DeviceManager,
    settingsRepository: SettingsRepository,
    systemMonitorClient: SystemMonitorClient,
    deviceFileClient: DeviceFileClient,
): FileExplorerComponent {
    val localFileService = DefaultLocalFileService()
    val deviceFileService = DefaultDeviceFileService(
        deviceFileClient = deviceFileClient,
        systemMonitorClient = systemMonitorClient,
    )
    val transferService = DefaultFileTransferService(
        localFileService = localFileService,
        deviceFileService = deviceFileService,
    )

    return DefaultFileExplorerComponent(
        componentContext = componentContext,
        deviceManager = deviceManager,
        settingsRepository = settingsRepository,
        localFileService = localFileService,
        deviceFileService = deviceFileService,
        fileTransferService = transferService,
    )
}

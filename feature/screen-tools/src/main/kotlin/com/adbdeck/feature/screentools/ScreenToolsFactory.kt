package com.adbdeck.feature.screentools

import com.adbdeck.core.adb.api.device.DeviceManager
import com.adbdeck.core.adb.api.files.DeviceFileClient
import com.adbdeck.core.adb.api.screen.ScreenToolsClient
import com.adbdeck.core.settings.SettingsRepository
import com.adbdeck.feature.screentools.service.DefaultHostFileService
import com.adbdeck.feature.screentools.service.DefaultScreenrecordService
import com.adbdeck.feature.screentools.service.DefaultScreenshotService
import com.arkivanov.decompose.ComponentContext

/**
 * Фабрика создания компонента Screen Tools с внутренней сборкой service-слоя.
 *
 * Нужна чтобы не дублировать wiring сервисов в root-навигации приложения.
 */
fun createScreenToolsComponent(
    componentContext: ComponentContext,
    deviceManager: DeviceManager,
    settingsRepository: SettingsRepository,
    screenToolsClient: ScreenToolsClient,
    deviceFileClient: DeviceFileClient,
): ScreenToolsComponent {
    val screenshotService = DefaultScreenshotService(screenToolsClient)
    val screenrecordService = DefaultScreenrecordService(
        screenToolsClient = screenToolsClient,
        deviceFileClient = deviceFileClient,
    )
    val hostFileService = DefaultHostFileService()

    return DefaultScreenToolsComponent(
        componentContext = componentContext,
        deviceManager = deviceManager,
        settingsRepository = settingsRepository,
        screenshotService = screenshotService,
        screenrecordService = screenrecordService,
        hostFileService = hostFileService,
    )
}

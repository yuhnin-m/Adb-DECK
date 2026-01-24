package com.adbdeck.feature.apkinstall

import com.adbdeck.core.adb.api.apkinstall.ApkInstallClient
import com.adbdeck.core.adb.api.device.DeviceManager
import com.adbdeck.core.settings.SettingsRepository
import com.adbdeck.feature.apkinstall.service.DefaultApkInstallHostFileService
import com.adbdeck.feature.apkinstall.service.DefaultApkInstallService
import com.arkivanov.decompose.ComponentContext

/**
 * Фабрика создания компонента APK Install с внутренней сборкой service-слоя.
 */
fun createApkInstallComponent(
    componentContext: ComponentContext,
    deviceManager: DeviceManager,
    settingsRepository: SettingsRepository,
    apkInstallClient: ApkInstallClient,
): ApkInstallComponent {
    val apkInstallService = DefaultApkInstallService(apkInstallClient)
    val hostFileService = DefaultApkInstallHostFileService()

    return DefaultApkInstallComponent(
        componentContext = componentContext,
        deviceManager = deviceManager,
        settingsRepository = settingsRepository,
        apkInstallService = apkInstallService,
        hostFileService = hostFileService,
    )
}

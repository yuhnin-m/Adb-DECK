package com.adbdeck.feature.apkinstall.service

import com.adbdeck.core.adb.api.screen.ApkInstallOptions
import com.adbdeck.core.adb.api.screen.ScreenToolsClient

/**
 * Реализация [ApkInstallService] поверх [ScreenToolsClient].
 */
class DefaultApkInstallService(
    private val screenToolsClient: ScreenToolsClient,
) : ApkInstallService {

    override suspend fun install(
        deviceId: String,
        localApkPath: String,
        adbPath: String,
        options: ApkInstallOptions,
        onProgress: (progress: Float?, message: String) -> Unit,
    ): Result<Unit> =
        screenToolsClient.installApk(
            deviceId = deviceId,
            localApkPath = localApkPath,
            adbPath = adbPath,
            options = options,
            onProgress = { progress ->
                onProgress(progress.progress, progress.message)
            },
        )
}

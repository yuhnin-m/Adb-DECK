package com.adbdeck.feature.apkinstall.service

import com.adbdeck.core.adb.api.apkinstall.ApkInstallClient
import com.adbdeck.core.adb.api.apkinstall.ApkInstallOptions

/**
 * Реализация [ApkInstallService] поверх [ApkInstallClient].
 */
class DefaultApkInstallService(
    private val apkInstallClient: ApkInstallClient,
) : ApkInstallService {

    override suspend fun install(
        deviceId: String,
        localApkPath: String,
        adbPath: String,
        options: ApkInstallOptions,
        onProgress: (progress: Float?, message: String) -> Unit,
    ): Result<Unit> =
        apkInstallClient.installApk(
            deviceId = deviceId,
            localApkPath = localApkPath,
            adbPath = adbPath,
            options = options,
            onProgress = { progress ->
                onProgress(progress.progress, progress.message)
            },
        )
}

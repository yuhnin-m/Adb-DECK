package com.adbdeck.feature.apkinstall.service

import com.adbdeck.core.adb.api.apkinstall.ApkInstallClient
import com.adbdeck.core.adb.api.apkinstall.ApkInstallOptions
import com.adbdeck.core.adb.api.apkinstall.ApkInstallProgress

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
        onProgress: (ApkInstallProgress) -> Unit,
    ): Result<Unit> =
        apkInstallClient.installApk(
            deviceId = deviceId,
            localApkPath = localApkPath,
            adbPath = adbPath,
            options = options,
            onProgress = onProgress,
        )
}

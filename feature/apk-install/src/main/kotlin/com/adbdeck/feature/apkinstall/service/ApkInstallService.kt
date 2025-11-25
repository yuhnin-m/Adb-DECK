package com.adbdeck.feature.apkinstall.service

import com.adbdeck.core.adb.api.ApkInstallOptions

/**
 * Сервис установки APK на активное устройство.
 */
interface ApkInstallService {

    /**
     * Устанавливает APK-файл на устройство.
     *
     * @param onProgress Колбек прогресса и статуса установки.
     */
    suspend fun install(
        deviceId: String,
        localApkPath: String,
        adbPath: String,
        options: ApkInstallOptions = ApkInstallOptions(),
        onProgress: (progress: Float?, message: String) -> Unit,
    ): Result<Unit>
}

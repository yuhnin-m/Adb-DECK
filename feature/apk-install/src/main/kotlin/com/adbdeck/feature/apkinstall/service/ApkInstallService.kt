package com.adbdeck.feature.apkinstall.service

import com.adbdeck.core.adb.api.apkinstall.ApkInstallOptions

/**
 * Сервис установки пакета на активное устройство.
 */
interface ApkInstallService {

    /**
     * Устанавливает пакет на устройство.
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

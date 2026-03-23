package com.adbdeck.feature.apkinstall.service

import com.adbdeck.core.adb.api.apkinstall.ApkInstallOptions
import com.adbdeck.core.adb.api.apkinstall.ApkInstallProgress

/**
 * Сервис установки пакета на активное устройство.
 */
interface ApkInstallService {

    /**
     * Устанавливает пакет на устройство.
     *
     * @param onProgress Колбек типизированного прогресса установки.
     */
    suspend fun install(
        deviceId: String,
        localApkPath: String,
        adbPath: String,
        options: ApkInstallOptions = ApkInstallOptions(),
        onProgress: (ApkInstallProgress) -> Unit,
    ): Result<Unit>
}

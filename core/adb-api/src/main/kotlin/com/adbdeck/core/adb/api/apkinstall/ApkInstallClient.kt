package com.adbdeck.core.adb.api.apkinstall

/**
 * Контракт клиента установки APK через ADB.
 */
interface ApkInstallClient {

    /**
     * Установить APK на устройство через `adb install`.
     *
     * @param deviceId Серийный номер / адрес устройства.
     * @param localApkPath Абсолютный путь к APK на хосте.
     * @param adbPath Путь к исполняемому файлу `adb`.
     * @param options Параметры установки (`-r`, `-d`, `-g`).
     * @param onProgress Колбек для статуса/прогресса установки.
     */
    suspend fun installApk(
        deviceId: String,
        localApkPath: String,
        adbPath: String = "adb",
        options: ApkInstallOptions = ApkInstallOptions(),
        onProgress: (ApkInstallProgress) -> Unit = {},
    ): Result<Unit>
}

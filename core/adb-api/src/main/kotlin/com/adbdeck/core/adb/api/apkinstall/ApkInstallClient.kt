package com.adbdeck.core.adb.api.apkinstall

/**
 * Контракт клиента установки пакетов через ADB.
 */
interface ApkInstallClient {

    /**
     * Установить пакет на устройство.
     *
     * Поддерживаемые источники (best-effort):
     * - `.apk` через `adb install`
     * - split APK (`.apks`, `.xapk`, директория c `.apk`) через `adb install-multiple`
     * - `.aab` через bundletool (`build-apks` + `install-apks`)
     *
     * @param deviceId Серийный номер / адрес устройства.
     * @param localApkPath Абсолютный путь к install target на хосте.
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

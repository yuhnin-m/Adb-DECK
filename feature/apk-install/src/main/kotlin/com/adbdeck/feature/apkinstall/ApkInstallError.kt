package com.adbdeck.feature.apkinstall

/**
 * Типизированные ошибки feature APK Install.
 */
sealed interface ApkInstallError {
    data object InstallAlreadyRunning : ApkInstallError
    data object ActiveDeviceMissing : ApkInstallError
    data object ApkPathMissing : ApkInstallError
    data class ApkFileNotFound(val path: String) : ApkInstallError
    data object InvalidApkExtension : ApkInstallError
    data object ApkPathAccessFailed : ApkInstallError
    data class InstallFailed(val details: String?) : ApkInstallError
    data object InstallInterruptedByDeviceChange : ApkInstallError
    data class PickerFailed(val details: String?) : ApkInstallError
}

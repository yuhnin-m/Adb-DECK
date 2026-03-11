package com.adbdeck.feature.apkinstall

/**
 * Состояние статус-блока установки APK.
 *
 * @param message Текст текущего статуса.
 * @param isError `true`, если статус содержит ошибку.
 * @param progress Прогресс `0f..1f`, либо `null` для indeterminate.
 */
data class ApkInstallStatus(
    val message: String,
    val isError: Boolean = false,
    val progress: Float? = null,
)

/**
 * Краткоживущее уведомление для пользователя.
 */
data class ApkInstallFeedback(
    val message: String,
    val isError: Boolean,
)

/**
 * Полное состояние экрана установки APK.
 */
data class ApkInstallState(
    val activeDeviceId: String? = null,
    val deviceMessage: String = "",
    val apkPath: String = "",
    val allowTestOnlyInstall: Boolean = false,
    val isInstalling: Boolean = false,
    val installingDeviceId: String? = null,
    val status: ApkInstallStatus = ApkInstallStatus(message = ""),
    val logLines: List<String> = emptyList(),
    val lastInstalledApkPath: String? = null,
    val feedback: ApkInstallFeedback? = null,
) {
    /** Признак доступности текущего active device для операций. */
    val isDeviceReady: Boolean get() = activeDeviceId != null
}

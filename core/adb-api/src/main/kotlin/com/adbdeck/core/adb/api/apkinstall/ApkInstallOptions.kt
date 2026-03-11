package com.adbdeck.core.adb.api.apkinstall

/**
 * Параметры установки APK на устройство.
 *
 * @param reinstall `-r`: переустановка поверх существующего пакета.
 * @param allowDowngrade `-d`: разрешить downgrade версии приложения.
 * @param grantRuntimePermissions `-g`: автоматически выдать runtime-permissions.
 * @param allowTestOnly `-t`: разрешить установку test-only APK.
 * @param bundletoolPath Путь к bundletool (бинарник или `.jar`) для AAB/APKS сценариев.
 */
data class ApkInstallOptions(
    val reinstall: Boolean = true,
    val allowDowngrade: Boolean = false,
    val grantRuntimePermissions: Boolean = false,
    val allowTestOnly: Boolean = false,
    val bundletoolPath: String? = null,
)

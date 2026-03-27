package com.adbdeck.feature.update.install

import java.nio.file.Path

/**
 * Инсталлер-заглушка: принудительно отключает in-app установку обновления.
 *
 * Используется для browser-only сценария, когда приложение открывает ссылку
 * на релиз вместо самостоятельной установки пакета.
 */
class BrowserOnlyAppUpdateInstaller : AppUpdateInstaller {

    override fun canInstallInApp(downloadUrl: String): Boolean = false

    override suspend fun preflightInstall(downloadUrl: String) {
        throw AppUpdatePreflightException(
            reason = AppUpdatePreflightFailureReason.UNSUPPORTED_PLATFORM_OR_ASSET,
        )
    }

    override suspend fun installFromDownloadedPackage(packageFile: Path) {
        throw AppUpdatePreflightException(
            reason = AppUpdatePreflightFailureReason.UNSUPPORTED_PLATFORM_OR_ASSET,
        )
    }
}

package com.adbdeck.feature.update

import adbdeck.feature.update.generated.resources.Res
import adbdeck.feature.update.generated.resources.app_update_details_cancelled
import adbdeck.feature.update.generated.resources.app_update_details_download_start
import adbdeck.feature.update.generated.resources.app_update_details_installing
import adbdeck.feature.update.generated.resources.app_update_details_restarting
import adbdeck.feature.update.generated.resources.app_update_details_verifying
import adbdeck.feature.update.generated.resources.app_update_error_checksum_mismatch
import adbdeck.feature.update.generated.resources.app_update_error_install_failed
import adbdeck.feature.update.generated.resources.app_update_error_preflight_failed
import adbdeck.feature.update.generated.resources.app_update_error_preflight_unknown_reason
import com.adbdeck.core.utils.runCatchingPreserveCancellation
import com.adbdeck.feature.update.download.AppUpdateDownloader
import com.adbdeck.feature.update.download.Sha512ChecksumVerifier
import com.adbdeck.feature.update.install.AppUpdateInstaller
import com.adbdeck.feature.update.logging.AppUpdateLogger
import com.adbdeck.feature.update.logging.NoOpAppUpdateLogger
import com.adbdeck.feature.update.provider.AppUpdateCheckResult
import com.adbdeck.feature.update.provider.AppUpdateProvider
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.lifecycle.coroutines.coroutineScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import java.awt.Desktop
import java.net.URI
import kotlin.system.exitProcess

/**
 * Базовая реализация [AppUpdateComponent].
 *
 * Поддерживаемые сценарии:
 * - проверка обновлений через [appUpdateProvider];
 * - in-app установка обновления (скачивание ZIP + запуск platform installer);
 * - fallback в браузер, если in-app установка недоступна.
 *
 * Mock-режим обновления через env-переменные:
 * - `ADBDECK_UPDATE_MOCK_VERSION`
 * - `ADBDECK_UPDATE_MOCK_CHANGELOG`
 * - `ADBDECK_UPDATE_MOCK_URL`
 */
class DefaultAppUpdateComponent(
    componentContext: ComponentContext,
    private val appUpdateProvider: AppUpdateProvider,
    private val currentVersion: String,
    private val appUpdateDownloader: AppUpdateDownloader,
    private val appUpdateInstaller: AppUpdateInstaller,
    private val appUpdateLogger: AppUpdateLogger = NoOpAppUpdateLogger,
    private val terminateApplicationForUpdate: () -> Unit = { exitProcess(0) },
) : AppUpdateComponent, ComponentContext by componentContext {

    private val scope = coroutineScope()

    private val _state = MutableStateFlow(AppUpdateUiState.Hidden)
    override val state: StateFlow<AppUpdateUiState> = _state.asStateFlow()

    private var started: Boolean = false
    private var installJob: Job? = null
    private var installCancelledByUser: Boolean = false
    private var availableUpdate: AvailableUpdate? = null

    override fun onStart() {
        if (started) return
        started = true

        scope.launch {
            runCatchingPreserveCancellation { performUpdateCheck(failSilently = true, isManual = false) }
                .onFailure { error ->
                    appUpdateLogger.error(
                        message = "Unexpected error during startup update check.",
                        throwable = error,
                    )
                }
        }
    }

    override suspend fun checkForUpdatesNow(): Boolean {
        return when (performUpdateCheck(failSilently = false, isManual = true)) {
            UpdateCheckOutcome.AVAILABLE -> true
            UpdateCheckOutcome.UP_TO_DATE -> false
            UpdateCheckOutcome.FAILED -> {
                appUpdateLogger.error("Manual update check finished with FAILED result.")
                error("Updates check failed")
            }
        }
    }

    override fun onInstallUpdateNow() {
        if (installJob?.isActive == true) return

        val update = availableUpdate ?: run {
            appUpdateLogger.warn("Install update requested but no update is currently available.")
            return
        }

        val targetUrl = update.downloadUrl ?: run {
            appUpdateLogger.warn("Install update requested but download URL is missing.")
            return
        }

        if (!appUpdateInstaller.canInstallInApp(targetUrl)) {
            appUpdateLogger.info("In-app install is not supported for current platform/asset. URL: $targetUrl")
            openInBrowser(targetUrl)
            _state.value = AppUpdateUiState.Hidden
            return
        }

        installCancelledByUser = false
        installJob = scope.launch {
            runInAppInstall(update, targetUrl)
        }
    }

    override fun onOpenUpdateDialog() {
        val update = availableUpdate ?: return
        if (installJob?.isActive == true) {
            _state.update { it.copy(visible = true) }
            return
        }
        _state.value = buildAvailableState(
            update = update,
            visible = true,
            details = null,
        )
    }

    override fun onCancelUpdate() {
        val activeInstall = installJob
        if (activeInstall?.isActive == true && _state.value.phase == AppUpdatePhase.DOWNLOADING) {
            installCancelledByUser = true
            activeInstall.cancel()
            return
        }

        if (_state.value.canDismiss) {
            onDismissUpdate()
        }
    }

    override fun onDismissUpdate() {
        _state.update { it.copy(visible = false) }
    }

    private suspend fun runInAppInstall(
        update: AvailableUpdate,
        downloadUrl: String,
    ) {
        val preflightResult = runCatchingPreserveCancellation {
            appUpdateInstaller.preflightInstall(downloadUrl)
        }
        if (preflightResult.isFailure) {
            val error = preflightResult.exceptionOrNull()
            appUpdateLogger.error("In-app update preflight failed.", error)
            val reason = error
                ?.message
                ?.trim()
                .orEmpty()
                .ifBlank { getString(Res.string.app_update_error_preflight_unknown_reason) }
            _state.value = buildErrorState(
                update = update,
                details = getString(Res.string.app_update_error_preflight_failed, reason),
            )
            return
        }

        _state.value = AppUpdateUiState(
            visible = true,
            blocking = true,
            phase = AppUpdatePhase.DOWNLOADING,
            currentVersion = currentVersion,
            targetVersion = update.version,
            progress = 0f,
            details = getString(Res.string.app_update_details_download_start),
            changelog = update.changelog,
            canInstallNow = false,
            canCancel = true,
            canDismiss = false,
        )

        try {
            val downloadedPackage = appUpdateDownloader.download(
                url = downloadUrl,
                targetVersion = update.version,
            ) { progress ->
                _state.update { current ->
                    if (current.phase != AppUpdatePhase.DOWNLOADING) {
                        current
                    } else {
                        current.copy(progress = progress ?: current.progress)
                    }
                }
            }

            val expectedSha512 = update.expectedSha512
                ?.trim()
                ?.takeIf { it.isNotEmpty() }
            if (expectedSha512 != null) {
                _state.update { current ->
                    current.copy(
                        progress = null,
                        details = getString(Res.string.app_update_details_verifying),
                        canCancel = false,
                    )
                }

                val checksumVerified = Sha512ChecksumVerifier.verify(
                    file = downloadedPackage.file,
                    expectedSha512Base64 = expectedSha512,
                )
                if (!checksumVerified) {
                    appUpdateLogger.error(
                        "Checksum mismatch for downloaded update package: ${downloadedPackage.file}",
                    )
                    _state.value = buildErrorState(
                        update = update,
                        details = getString(Res.string.app_update_error_checksum_mismatch),
                    )
                    return
                }
                appUpdateLogger.info("Downloaded update package checksum verified successfully.")
            }

            _state.update { current ->
                current.copy(
                    phase = AppUpdatePhase.INSTALLING,
                    progress = null,
                    details = getString(Res.string.app_update_details_installing),
                    canCancel = false,
                    canDismiss = false,
                    blocking = true,
                )
            }

            appUpdateInstaller.installFromDownloadedPackage(downloadedPackage.file)

            _state.update { current ->
                current.copy(
                    phase = AppUpdatePhase.RESTARTING,
                    details = getString(Res.string.app_update_details_restarting),
                    canCancel = false,
                    canDismiss = false,
                    canInstallNow = false,
                    blocking = true,
                )
            }

            delay(350)
            terminateApplicationForUpdate()
        } catch (cancellation: CancellationException) {
            if (installCancelledByUser) {
                _state.value = buildAvailableState(
                    update = update,
                    visible = true,
                    details = getString(Res.string.app_update_details_cancelled),
                )
            } else {
                throw cancellation
            }
        } catch (error: Throwable) {
            appUpdateLogger.error("In-app update installation failed.", error)
            _state.value = buildErrorState(
                update = update,
                details = getString(Res.string.app_update_error_install_failed),
            )
        } finally {
            installJob = null
            installCancelledByUser = false
        }
    }

    private fun tryShowMockFallback(): Boolean {
        val mockedVersion = readMockVersion() ?: return false
        if (mockedVersion == currentVersion) return false

        val mockedUpdate = AvailableUpdate(
            version = mockedVersion,
            changelog = readMockChangelog(),
            downloadUrl = readMockUrl(),
            expectedSha512 = null,
        )
        showAvailableUpdateDialog(mockedUpdate)
        return true
    }

    private suspend fun performUpdateCheck(failSilently: Boolean, isManual: Boolean): UpdateCheckOutcome {
        return when (val update = appUpdateProvider.checkForUpdate(currentVersion)) {
            is AppUpdateCheckResult.UpToDate -> UpdateCheckOutcome.UP_TO_DATE
            is AppUpdateCheckResult.UpdateAvailable -> {
                showAvailableUpdateDialog(
                    AvailableUpdate(
                        version = update.version,
                        changelog = update.changelog,
                        downloadUrl = update.downloadUrl,
                        expectedSha512 = update.expectedSha512,
                    )
                )
                UpdateCheckOutcome.AVAILABLE
            }

            is AppUpdateCheckResult.Failed -> {
                val reason = update.reason.ifBlank { "Failed to check updates." }
                val mockShown = tryShowMockFallback()
                if (mockShown) {
                    appUpdateLogger.warn(
                        message = buildErrorLogMessage(
                            reason = reason,
                            isManual = isManual,
                            mockFallbackUsed = true,
                        ),
                    )
                    UpdateCheckOutcome.AVAILABLE
                } else if (failSilently) {
                    appUpdateLogger.error(
                        message = buildErrorLogMessage(
                            reason = reason,
                            isManual = isManual,
                            mockFallbackUsed = false,
                        ),
                    )
                    UpdateCheckOutcome.FAILED
                } else {
                    appUpdateLogger.error(
                        message = buildErrorLogMessage(
                            reason = reason,
                            isManual = isManual,
                            mockFallbackUsed = false,
                        ),
                    )
                    throw IllegalStateException(reason)
                }
            }
        }
    }

    private fun showAvailableUpdateDialog(update: AvailableUpdate) {
        availableUpdate = update
        _state.value = buildAvailableState(
            update = update,
            visible = true,
            details = null,
        )
    }

    private fun buildAvailableState(
        update: AvailableUpdate,
        visible: Boolean,
        details: String?,
    ): AppUpdateUiState {
        return AppUpdateUiState(
            visible = visible,
            blocking = false,
            phase = AppUpdatePhase.AVAILABLE,
            currentVersion = currentVersion,
            targetVersion = update.version,
            progress = null,
            details = details,
            changelog = update.changelog,
            canInstallNow = update.downloadUrl != null,
            canCancel = false,
            canDismiss = true,
        )
    }

    private fun buildErrorState(
        update: AvailableUpdate,
        details: String,
    ): AppUpdateUiState {
        return AppUpdateUiState(
            visible = true,
            blocking = false,
            phase = AppUpdatePhase.ERROR,
            currentVersion = currentVersion,
            targetVersion = update.version,
            progress = null,
            details = details,
            changelog = update.changelog,
            canInstallNow = update.downloadUrl != null,
            canCancel = false,
            canDismiss = true,
        )
    }

    private fun readMockVersion(): String? {
        return System.getenv(MOCK_VERSION_ENV)
            ?.trim()
            ?.removePrefix("v")
            ?.takeIf { it.isNotBlank() }
    }

    private fun readMockChangelog(): String {
        return System.getenv(MOCK_CHANGELOG_ENV)
            ?.trim()
            .orEmpty()
    }

    private fun readMockUrl(): String? {
        val raw = System.getenv(MOCK_URL_ENV)?.trim().orEmpty()
        if (raw.isBlank()) return null
        val normalized = raw.lowercase()
        return if (normalized.startsWith("https://") || normalized.startsWith("http://")) {
            raw
        } else {
            null
        }
    }

    private fun openInBrowser(url: String) {
        if (!Desktop.isDesktopSupported()) {
            appUpdateLogger.warn("Desktop integration is not supported. Cannot open URL: $url")
            return
        }
        runCatching {
            val desktop = Desktop.getDesktop()
            if (!desktop.isSupported(Desktop.Action.BROWSE)) {
                appUpdateLogger.warn("Desktop browse action is not supported. Cannot open URL: $url")
                return@runCatching
            }
            desktop.browse(URI(url))
            appUpdateLogger.info("Opened update URL in browser: $url")
        }.onFailure { error ->
            appUpdateLogger.error("Failed to open update URL in browser: $url", error)
        }
    }

    private fun buildErrorLogMessage(
        reason: String,
        isManual: Boolean,
        mockFallbackUsed: Boolean,
    ): String {
        val source = if (isManual) "manual" else "startup"
        val fallback = if (mockFallbackUsed) " Mock fallback was applied." else ""
        return "App update check failed ($source): $reason.$fallback"
    }

    private data class AvailableUpdate(
        val version: String,
        val changelog: String,
        val downloadUrl: String?,
        val expectedSha512: String?,
    )

    private companion object {
        private const val MOCK_VERSION_ENV = "ADBDECK_UPDATE_MOCK_VERSION"
        private const val MOCK_CHANGELOG_ENV = "ADBDECK_UPDATE_MOCK_CHANGELOG"
        private const val MOCK_URL_ENV = "ADBDECK_UPDATE_MOCK_URL"
    }

    private enum class UpdateCheckOutcome {
        AVAILABLE,
        UP_TO_DATE,
        FAILED,
    }
}

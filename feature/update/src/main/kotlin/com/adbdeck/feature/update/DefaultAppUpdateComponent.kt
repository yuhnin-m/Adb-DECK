package com.adbdeck.feature.update

import com.adbdeck.feature.update.provider.AppUpdateCheckResult
import com.adbdeck.feature.update.provider.AppUpdateProvider
import com.adbdeck.feature.update.logging.AppUpdateLogger
import com.adbdeck.feature.update.logging.NoOpAppUpdateLogger
import com.adbdeck.core.utils.runCatchingPreserveCancellation
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.lifecycle.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.awt.Desktop
import java.net.URI

/**
 * Базовая реализация [AppUpdateComponent].
 *
 * На текущем этапе компонент поддерживает mock-режим обновления через env-переменные:
 * - `ADBDECK_UPDATE_MOCK_VERSION`
 * - `ADBDECK_UPDATE_MOCK_CHANGELOG`
 * - `ADBDECK_UPDATE_MOCK_URL`
 *
 * Приоритет проверки:
 * 1. Реальный провайдер [appUpdateProvider]
 * 2. Mock-режим через env (fallback для локальной проверки)
 */
class DefaultAppUpdateComponent(
    componentContext: ComponentContext,
    private val appUpdateProvider: AppUpdateProvider,
    private val currentVersion: String,
    private val appUpdateLogger: AppUpdateLogger = NoOpAppUpdateLogger,
) : AppUpdateComponent, ComponentContext by componentContext {

    private val scope = coroutineScope()

    private val _state = MutableStateFlow(AppUpdateUiState.Hidden)
    override val state: StateFlow<AppUpdateUiState> = _state.asStateFlow()

    private var started: Boolean = false
    private var downloadUrl: String? = null

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
        val targetUrl = downloadUrl ?: run {
            appUpdateLogger.warn("Install update requested but download URL is missing.")
            return
        }
        openInBrowser(targetUrl)
        _state.value = AppUpdateUiState.Hidden
    }

    override fun onCancelUpdate() {
        _state.value = AppUpdateUiState.Hidden
    }

    override fun onDismissUpdate() {
        _state.update { it.copy(visible = false, canDismiss = false, canCancel = false, canInstallNow = false) }
    }

    private fun tryShowMockFallback(): Boolean {
        val mockedVersion = readMockVersion() ?: return false
        if (mockedVersion == currentVersion) return false

        val mockedUrl = readMockUrl()
        downloadUrl = mockedUrl
        _state.value = AppUpdateUiState(
            visible = true,
            blocking = false,
            phase = AppUpdatePhase.AVAILABLE,
            currentVersion = currentVersion,
            targetVersion = mockedVersion,
            changelog = readMockChangelog(),
            canInstallNow = mockedUrl != null,
            canCancel = false,
            canDismiss = true,
        )
        return true
    }

    private suspend fun performUpdateCheck(failSilently: Boolean, isManual: Boolean): UpdateCheckOutcome {
        return when (val update = appUpdateProvider.checkForUpdate(currentVersion)) {
            is AppUpdateCheckResult.UpToDate -> UpdateCheckOutcome.UP_TO_DATE
            is AppUpdateCheckResult.UpdateAvailable -> {
                showAvailableUpdateDialog(update)
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

    private fun showAvailableUpdateDialog(update: AppUpdateCheckResult.UpdateAvailable) {
        downloadUrl = update.downloadUrl
        _state.value = AppUpdateUiState(
            visible = true,
            blocking = false,
            phase = AppUpdatePhase.AVAILABLE,
            currentVersion = currentVersion,
            targetVersion = update.version,
            changelog = update.changelog,
            canInstallNow = true,
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

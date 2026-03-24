package com.adbdeck.app.update

import com.adbdeck.app.APP_VERSION
import com.adbdeck.app.update.provider.AppUpdateCheckResult
import com.adbdeck.app.update.provider.AppUpdateProvider
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
            runCatchingPreserveCancellation {
                performUpdateCheck(failSilently = true)
            }
        }
    }

    override suspend fun checkForUpdatesNow(): Boolean {
        return when (performUpdateCheck(failSilently = false)) {
            UpdateCheckOutcome.AVAILABLE -> true
            UpdateCheckOutcome.UP_TO_DATE -> false
            UpdateCheckOutcome.FAILED -> error("Updates check failed")
        }
    }

    override fun onInstallUpdateNow() {
        val targetUrl = downloadUrl ?: return
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
        if (mockedVersion == APP_VERSION) return false

        val mockedUrl = readMockUrl()
        downloadUrl = mockedUrl
        _state.value = AppUpdateUiState(
            visible = true,
            blocking = false,
            phase = AppUpdatePhase.AVAILABLE,
            currentVersion = APP_VERSION,
            targetVersion = mockedVersion,
            changelog = readMockChangelog(),
            canInstallNow = mockedUrl != null,
            canCancel = false,
            canDismiss = true,
        )
        return true
    }

    private suspend fun performUpdateCheck(failSilently: Boolean): UpdateCheckOutcome {
        return when (val update = appUpdateProvider.checkForUpdate(APP_VERSION)) {
            is AppUpdateCheckResult.UpToDate -> UpdateCheckOutcome.UP_TO_DATE
            is AppUpdateCheckResult.UpdateAvailable -> {
                showAvailableUpdateDialog(update)
                UpdateCheckOutcome.AVAILABLE
            }
            is AppUpdateCheckResult.Failed -> {
                val mockShown = tryShowMockFallback()
                if (mockShown) {
                    UpdateCheckOutcome.AVAILABLE
                } else if (failSilently) {
                    UpdateCheckOutcome.FAILED
                } else {
                    throw IllegalStateException(
                        update.reason.ifBlank { "Failed to check updates." },
                    )
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
            currentVersion = APP_VERSION,
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
        if (!Desktop.isDesktopSupported()) return
        runCatching {
            val desktop = Desktop.getDesktop()
            if (desktop.isSupported(Desktop.Action.BROWSE)) {
                desktop.browse(URI(url))
            }
        }
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

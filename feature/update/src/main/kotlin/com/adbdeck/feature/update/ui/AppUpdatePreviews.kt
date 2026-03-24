package com.adbdeck.feature.update.ui

import adbdeck.feature.update.generated.resources.Res
import adbdeck.feature.update.generated.resources.app_update_details_installing
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.adbdeck.core.designsystem.AdbDeckTheme
import com.adbdeck.feature.update.AppUpdatePhase
import com.adbdeck.feature.update.AppUpdateUiState
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
private fun AppUpdatePreviewBody(
    isDarkTheme: Boolean,
    state: AppUpdateUiState,
) {
    AdbDeckTheme(isDarkTheme = isDarkTheme) {
        Surface(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.fillMaxSize()) {
                AppUpdateOverlay(
                    state = state,
                    onInstallNow = {},
                    onCancel = {},
                    onDismiss = {},
                )
            }
        }
    }
}

@Preview
@Composable
private fun AppUpdateAvailableLightPreview() {
    AppUpdatePreviewBody(
        isDarkTheme = false,
        state = AppUpdateUiState(
            visible = true,
            phase = AppUpdatePhase.AVAILABLE,
            currentVersion = "1.1.0",
            targetVersion = "1.2.0",
            canInstallNow = true,
            canDismiss = true,
        ),
    )
}

@Preview
@Composable
private fun AppUpdateInstallingDarkPreview() {
    AppUpdatePreviewBody(
        isDarkTheme = true,
        state = AppUpdateUiState(
            visible = true,
            blocking = true,
            phase = AppUpdatePhase.INSTALLING,
            currentVersion = "1.1.0",
            targetVersion = "1.2.0",
            details = stringResource(Res.string.app_update_details_installing),
            canInstallNow = false,
            canCancel = false,
            canDismiss = false,
        ),
    )
}

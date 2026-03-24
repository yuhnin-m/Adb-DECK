package com.adbdeck.app.ui

import adbdeck.app.generated.resources.Res
import adbdeck.app.generated.resources.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import com.adbdeck.app.update.AppUpdatePhase
import com.adbdeck.app.update.AppUpdateUiState
import com.adbdeck.core.designsystem.AdbCornerRadius
import com.adbdeck.core.designsystem.Dimensions
import com.adbdeck.core.ui.alertdialogs.AdbAlertDialog
import com.adbdeck.core.ui.alertdialogs.AdbAlertDialogAction
import com.adbdeck.core.ui.buttons.AdbButtonType
import org.jetbrains.compose.resources.stringResource

private const val AppUpdateDialogWidthFraction = 0.68f

/**
 * Диалог обновления приложения поверх основного UI.
 *
 * Используется для явного управления процессом обновления
 * (установка, отмена, просмотр changelog).
 */
@Composable
internal fun AppUpdateOverlay(
    state: AppUpdateUiState,
    onInstallNow: (() -> Unit)?,
    onCancel: (() -> Unit)?,
    onDismiss: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    if (!state.visible) return

    val phaseTitle = state.phase.toLocalizedTitle()
    val hasPassiveProgressState = !state.canInstallNow && !state.canCancel && !state.canDismiss

    val confirmAction = when {
        state.canInstallNow && onInstallNow != null -> AdbAlertDialogAction(
            text = stringResource(Res.string.app_update_action_install_now),
            onClick = onInstallNow,
            type = AdbButtonType.SUCCESS,
        )

        state.canDismiss && onDismiss != null -> AdbAlertDialogAction(
            text = stringResource(Res.string.app_update_action_hide),
            onClick = onDismiss,
            type = AdbButtonType.NEUTRAL,
        )

        else -> AdbAlertDialogAction(
            text = phaseTitle,
            onClick = {},
            enabled = false,
            loading = state.blocking && hasPassiveProgressState,
            type = AdbButtonType.NEUTRAL,
        )
    }

    val dismissAction = when {
        state.canCancel && onCancel != null -> AdbAlertDialogAction(
            text = stringResource(Res.string.app_update_action_cancel),
            onClick = onCancel,
            type = AdbButtonType.DANGER,
        )

        state.canInstallNow && state.canDismiss && onDismiss != null -> AdbAlertDialogAction(
            text = stringResource(Res.string.app_update_action_hide),
            onClick = onDismiss,
            type = AdbButtonType.NEUTRAL,
        )

        else -> null
    }

    val onDismissRequest = when {
        state.canDismiss && onDismiss != null -> onDismiss
        state.canCancel && onCancel != null -> onCancel
        else -> {
            {}
        }
    }

    AdbAlertDialog(
        onDismissRequest = onDismissRequest,
        title = stringResource(Res.string.app_update_title),
        modifier = modifier.fillMaxWidth(AppUpdateDialogWidthFraction),
        confirmAction = confirmAction,
        dismissAction = dismissAction,
        // Когда диалог "занят", разрешаем закрытие только если это явно допустимо.
        allowDismissWhenBusy = state.canDismiss,
        hideDismissWhenBusy = false,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = Dimensions.sidebarWidth * 2),
            verticalArrangement = Arrangement.spacedBy(Dimensions.paddingMedium),
        ) {
            Text(
                text = phaseTitle,
                style = MaterialTheme.typography.titleMedium,
                color = if (state.phase == AppUpdatePhase.ERROR) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.primary
                },
            )

            val currentVersion = state.currentVersion
            val targetVersion = state.targetVersion
            if (!currentVersion.isNullOrBlank() && !targetVersion.isNullOrBlank()) {
                Text(
                    text = stringResource(
                        Res.string.app_update_versions,
                        currentVersion,
                        targetVersion,
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            state.progress?.let { progress ->
                LinearProgressIndicator(
                    progress = { progress.coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            state.details
                ?.trim()
                ?.takeIf { it.isNotEmpty() }
                ?.let { details ->
                    Text(
                        text = details,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (state.phase == AppUpdatePhase.ERROR) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                    )
                }

            if (state.changelog.isNotBlank()) {
                Text(
                    text = stringResource(Res.string.app_update_changelog_title),
                    style = MaterialTheme.typography.titleSmall,
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = Dimensions.sidebarWidth * 0.4f, max = Dimensions.sidebarWidth)
                        .padding(top = Dimensions.paddingSmall),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(AdbCornerRadius.MEDIUM.value),
                            )
                            .padding(Dimensions.paddingMedium),
                    ) {
                        Text(
                            text = state.changelog,
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.verticalScroll(rememberScrollState()),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AppUpdatePhase.toLocalizedTitle(): String = when (this) {
    AppUpdatePhase.CHECKING -> stringResource(Res.string.app_update_phase_checking)
    AppUpdatePhase.AVAILABLE -> stringResource(Res.string.app_update_phase_available)
    AppUpdatePhase.DOWNLOADING -> stringResource(Res.string.app_update_phase_downloading)
    AppUpdatePhase.READY_TO_INSTALL -> stringResource(Res.string.app_update_phase_ready_to_install)
    AppUpdatePhase.INSTALLING -> stringResource(Res.string.app_update_phase_installing)
    AppUpdatePhase.RESTARTING -> stringResource(Res.string.app_update_phase_restarting)
    AppUpdatePhase.ERROR -> stringResource(Res.string.app_update_phase_error)
}

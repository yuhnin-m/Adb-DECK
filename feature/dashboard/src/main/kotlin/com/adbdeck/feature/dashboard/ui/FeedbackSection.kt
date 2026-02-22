package com.adbdeck.feature.dashboard.ui

import adbdeck.feature.dashboard.generated.resources.Res
import adbdeck.feature.dashboard.generated.resources.*
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.adbdeck.core.designsystem.Dimensions
import com.adbdeck.core.ui.AdbBanner
import com.adbdeck.core.ui.AdbBannerType
import com.adbdeck.feature.dashboard.DashboardAdbCheckState
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun FeedbackSection(
    state: FeedbackUiState,
    onDismissAdbCheck: () -> Unit,
    onDismissRefreshError: () -> Unit,
    onDismissAdbServerError: () -> Unit,
) {
    when (val adbState = state.adbCheckState) {
        DashboardAdbCheckState.Idle,
        DashboardAdbCheckState.Checking,
        -> Unit

        is DashboardAdbCheckState.Available -> {
            val message = stringResource(Res.string.dashboard_adb_available, adbState.version)
            AdbBanner(
                message = message,
                type = AdbBannerType.SUCCESS,
                onDismiss = onDismissAdbCheck,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        is DashboardAdbCheckState.NotAvailable -> {
            val reason = adbState.reason.ifBlank {
                stringResource(Res.string.dashboard_unknown_error)
            }
            val message = stringResource(Res.string.dashboard_adb_unavailable, reason)
            AdbBanner(
                message = message,
                type = AdbBannerType.ERROR,
                onDismiss = onDismissAdbCheck,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }

    state.refreshError?.takeIf { it.isNotBlank() }?.let { error ->
        val message = stringResource(Res.string.dashboard_refresh_failed, error)
        Spacer(modifier = Modifier.height(Dimensions.paddingXSmall))
        AdbBanner(
            message = message,
            type = AdbBannerType.ERROR,
            onDismiss = onDismissRefreshError,
            modifier = Modifier.fillMaxWidth(),
        )
    }

    state.adbServerError?.let { error ->
        val reason = error.ifBlank {
            stringResource(Res.string.dashboard_unknown_error)
        }
        val message = stringResource(Res.string.dashboard_adb_server_action_failed, reason)
        Spacer(modifier = Modifier.height(Dimensions.paddingXSmall))
        AdbBanner(
            message = message,
            type = AdbBannerType.ERROR,
            onDismiss = onDismissAdbServerError,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

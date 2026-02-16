package com.adbdeck.feature.dashboard.ui

import adbdeck.feature.dashboard.generated.resources.Res
import adbdeck.feature.dashboard.generated.resources.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.DevicesOther
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.adbdeck.core.designsystem.Dimensions
import com.adbdeck.core.i18n.AdbCommonStringRes
import com.adbdeck.core.ui.AdbBanner
import com.adbdeck.core.ui.AdbBannerType
import com.adbdeck.core.ui.actiontiles.AdbActionTile
import com.adbdeck.core.ui.sectioncards.AdbSectionCard
import com.adbdeck.feature.dashboard.DashboardAdbCheckState
import com.adbdeck.feature.dashboard.DashboardComponent
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import org.jetbrains.compose.resources.stringResource

@Composable
fun DashboardScreen(component: DashboardComponent) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(Dimensions.paddingLarge),
            verticalArrangement = Arrangement.spacedBy(Dimensions.paddingMedium),
        ) {
            HeaderSection()
            QuickActionsSectionHost(component = component)
            DiagnosticsSectionHost(component = component)
            FeedbackSectionHost(component = component)
        }
    }
}

private data class QuickActionsUiState(
    val deviceCount: Int,
)

private data class DiagnosticsUiState(
    val isRefreshingDevices: Boolean,
    val isCheckingAdb: Boolean,
)

private data class FeedbackUiState(
    val adbCheckState: DashboardAdbCheckState,
    val refreshError: String?,
)

@Composable
private fun HeaderSection() {
    Text(
        text = stringResource(Res.string.dashboard_title),
        style = MaterialTheme.typography.headlineLarge,
        color = MaterialTheme.colorScheme.onBackground,
    )
    Text(
        text = stringResource(Res.string.dashboard_subtitle),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun QuickActionsSectionHost(component: DashboardComponent) {
    val initialUi = remember(component) {
        QuickActionsUiState(deviceCount = component.state.value.deviceCount)
    }
    val uiState by remember(component) {
        component.state
            .map { QuickActionsUiState(deviceCount = it.deviceCount) }
            .distinctUntilChanged()
    }.collectAsState(initial = initialUi)

    QuickActionsSection(
        state = uiState,
        onOpenDevices = component::onOpenDevices,
        onOpenLogcat = component::onOpenLogcat,
        onOpenSettings = component::onOpenSettings,
    )
}

@Composable
private fun DiagnosticsSectionHost(component: DashboardComponent) {
    val initialUi = remember(component) {
        DiagnosticsUiState(
            isRefreshingDevices = component.state.value.isRefreshingDevices,
            isCheckingAdb = component.state.value.adbCheckState is DashboardAdbCheckState.Checking,
        )
    }
    val uiState by remember(component) {
        component.state
            .map {
                DiagnosticsUiState(
                    isRefreshingDevices = it.isRefreshingDevices,
                    isCheckingAdb = it.adbCheckState is DashboardAdbCheckState.Checking,
                )
            }
            .distinctUntilChanged()
    }.collectAsState(initial = initialUi)

    DiagnosticsSection(
        state = uiState,
        onRefreshDevices = component::onRefreshDevices,
        onCheckAdb = component::onCheckAdb,
    )
}

@Composable
private fun FeedbackSectionHost(component: DashboardComponent) {
    val initialUi = remember(component) {
        FeedbackUiState(
            adbCheckState = component.state.value.adbCheckState,
            refreshError = component.state.value.refreshError,
        )
    }
    val uiState by remember(component) {
        component.state
            .map {
                FeedbackUiState(
                    adbCheckState = it.adbCheckState,
                    refreshError = it.refreshError,
                )
            }
            .distinctUntilChanged()
    }.collectAsState(initial = initialUi)

    FeedbackSection(
        state = uiState,
        onDismissAdbCheck = component::onDismissAdbCheck,
        onDismissRefreshError = component::onDismissRefreshError,
    )
}

@Composable
private fun QuickActionsSection(
    state: QuickActionsUiState,
    onOpenDevices: () -> Unit,
    onOpenLogcat: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val sectionTitle = stringResource(Res.string.dashboard_section_quick_actions)
    val devicesTitle = stringResource(Res.string.dashboard_tile_devices_title)
    val devicesSubtitle = if (state.deviceCount > 0) {
        stringResource(Res.string.dashboard_tile_devices_subtitle_count, state.deviceCount)
    } else {
        stringResource(Res.string.dashboard_tile_devices_subtitle_default)
    }
    val logcatTitle = stringResource(Res.string.dashboard_tile_logcat_title)
    val logcatSubtitle = stringResource(Res.string.dashboard_tile_logcat_subtitle)
    val settingsTitle = stringResource(AdbCommonStringRes.actionSettings)
    val settingsSubtitle = stringResource(Res.string.dashboard_tile_settings_subtitle)

    AdbSectionCard(
        title = sectionTitle,
        titleUppercase = true,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Dimensions.paddingMedium),
            modifier = Modifier.fillMaxWidth(),
        ) {
            AdbActionTile(
                icon = Icons.Outlined.DevicesOther,
                title = devicesTitle,
                subtitle = devicesSubtitle,
                onClick = onOpenDevices,
                modifier = Modifier.weight(1f),
            )
            AdbActionTile(
                icon = Icons.Outlined.Terminal,
                title = logcatTitle,
                subtitle = logcatSubtitle,
                onClick = onOpenLogcat,
                modifier = Modifier.weight(1f),
            )
            AdbActionTile(
                icon = Icons.Outlined.Settings,
                title = settingsTitle,
                subtitle = settingsSubtitle,
                onClick = onOpenSettings,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun DiagnosticsSection(
    state: DiagnosticsUiState,
    onRefreshDevices: () -> Unit,
    onCheckAdb: () -> Unit,
) {
    val sectionTitle = stringResource(Res.string.dashboard_section_diagnostics)
    val refreshTitle = stringResource(Res.string.dashboard_tile_refresh_devices_title)
    val refreshSubtitle = if (state.isRefreshingDevices) {
        stringResource(Res.string.dashboard_tile_refresh_devices_subtitle_loading)
    } else {
        stringResource(Res.string.dashboard_tile_refresh_devices_subtitle_idle)
    }
    val checkTitle = stringResource(Res.string.dashboard_tile_check_adb_title)
    val checkSubtitle = if (state.isCheckingAdb) {
        stringResource(Res.string.dashboard_tile_check_adb_subtitle_loading)
    } else {
        stringResource(Res.string.dashboard_tile_check_adb_subtitle_idle)
    }

    AdbSectionCard(
        title = sectionTitle,
        titleUppercase = true,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Dimensions.paddingMedium),
            modifier = Modifier.fillMaxWidth(),
        ) {
            AdbActionTile(
                icon = Icons.Outlined.Refresh,
                title = refreshTitle,
                subtitle = refreshSubtitle,
                onClick = onRefreshDevices,
                isLoading = state.isRefreshingDevices,
                modifier = Modifier.weight(1f),
            )
            AdbActionTile(
                icon = Icons.Outlined.Checklist,
                title = checkTitle,
                subtitle = checkSubtitle,
                onClick = onCheckAdb,
                isLoading = state.isCheckingAdb,
                modifier = Modifier.weight(1f),
            )
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun FeedbackSection(
    state: FeedbackUiState,
    onDismissAdbCheck: () -> Unit,
    onDismissRefreshError: () -> Unit,
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
}

package com.adbdeck.feature.dashboard.ui

import adbdeck.feature.dashboard.generated.resources.Res
import adbdeck.feature.dashboard.generated.resources.*
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.adbdeck.core.designsystem.Dimensions
import com.adbdeck.feature.dashboard.DashboardAdbCheckState
import com.adbdeck.feature.dashboard.DashboardAdbServerAction
import com.adbdeck.feature.dashboard.DashboardAdbServerState
import com.adbdeck.feature.dashboard.DashboardComponent
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import org.jetbrains.compose.resources.stringResource

@Composable
fun DashboardScreen(component: DashboardComponent) {
    LaunchedEffect(component) {
        component.onRefreshAdbServerStatus()
    }
    val scrollState = rememberScrollState()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(Dimensions.paddingLarge)
                    .padding(end = Dimensions.paddingSmall + 8.dp),
                verticalArrangement = Arrangement.spacedBy(Dimensions.paddingMedium),
            ) {
                HeaderSection()
                AdbCoreSectionHost(component = component)
                QuickActionsSectionHost(component = component)
                FeedbackSectionHost(component = component)
            }

            VerticalScrollbar(
                adapter = rememberScrollbarAdapter(scrollState),
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .padding(vertical = Dimensions.paddingLarge)
                    .width(8.dp),
            )
        }
    }
}

internal data class QuickActionsUiState(
    val deviceCount: Int,
)

internal data class AdbCoreUiState(
    val isAdbFound: Boolean,
    val adbVersion: String?,
    val deviceCount: Int,
    val serverState: DashboardAdbServerState,
    val activeAction: DashboardAdbServerAction?,
)

internal data class FeedbackUiState(
    val adbCheckState: DashboardAdbCheckState,
    val refreshError: String?,
    val adbServerError: String?,
    val isTerminalLaunchFailed: Boolean,
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
private fun AdbCoreSectionHost(component: DashboardComponent) {
    val initialUi = remember(component) {
        AdbCoreUiState(
            isAdbFound = component.state.value.adbServer.isAdbFound,
            adbVersion = component.state.value.adbServer.adbVersion,
            deviceCount = component.state.value.deviceCount,
            serverState = component.state.value.adbServer.serverState,
            activeAction = component.state.value.adbServer.activeAction,
        )
    }
    val uiState by remember(component) {
        component.state
            .map { state ->
                AdbCoreUiState(
                    isAdbFound = state.adbServer.isAdbFound,
                    adbVersion = state.adbServer.adbVersion,
                    deviceCount = state.deviceCount,
                    serverState = state.adbServer.serverState,
                    activeAction = state.adbServer.activeAction,
                )
            }
            .distinctUntilChanged()
    }.collectAsState(initial = initialUi)

    val onRefreshStatus = remember(component) { { component.onRefreshAdbServerStatus() } }
    val onStartServer = remember(component) { { component.onStartAdbServer() } }
    val onStopServer = remember(component) { { component.onStopAdbServer() } }
    val onRestartServer = remember(component) { { component.onRestartAdbServer() } }
    val onOpenSettings = remember(component) { { component.onOpenSettings() } }

    AdbCoreSection(
        state = uiState,
        onRefreshStatus = onRefreshStatus,
        onStartServer = onStartServer,
        onStopServer = onStopServer,
        onRestartServer = onRestartServer,
        onOpenSettings = onOpenSettings,
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

    val onOpenDevices = remember(component) { { component.onOpenDevices() } }
    val onOpenDeviceInfo = remember(component) { { component.onOpenDeviceInfo() } }
    val onOpenQuickToggles = remember(component) { { component.onOpenQuickToggles() } }
    val onOpenPackages = remember(component) { { component.onOpenPackages() } }
    val onOpenApkInstall = remember(component) { { component.onOpenApkInstall() } }
    val onOpenDeepLinks = remember(component) { { component.onOpenDeepLinks() } }
    val onOpenNotifications = remember(component) { { component.onOpenNotifications() } }
    val onOpenScreenTools = remember(component) { { component.onOpenScreenTools() } }
    val onOpenScrcpy = remember(component) { { component.onOpenScrcpy() } }
    val onOpenFileExplorer = remember(component) { { component.onOpenFileExplorer() } }
    val onOpenFileSystem = remember(component) { { component.onOpenFileSystem() } }
    val onOpenContacts = remember(component) { { component.onOpenContacts() } }
    val onOpenSystemMonitor = remember(component) { { component.onOpenSystemMonitor() } }
    val onOpenLogcat = remember(component) { { component.onOpenLogcat() } }
    val onOpenAdbShell = remember(component) { { component.onOpenAdbShell() } }
    val onOpenRootAdbShell = remember(component) { { component.onOpenRootAdbShell() } }

    QuickActionsSection(
        state = uiState,
        onOpenDevices = onOpenDevices,
        onOpenDeviceInfo = onOpenDeviceInfo,
        onOpenQuickToggles = onOpenQuickToggles,
        onOpenPackages = onOpenPackages,
        onOpenApkInstall = onOpenApkInstall,
        onOpenDeepLinks = onOpenDeepLinks,
        onOpenNotifications = onOpenNotifications,
        onOpenScreenTools = onOpenScreenTools,
        onOpenScrcpy = onOpenScrcpy,
        onOpenFileExplorer = onOpenFileExplorer,
        onOpenFileSystem = onOpenFileSystem,
        onOpenContacts = onOpenContacts,
        onOpenSystemMonitor = onOpenSystemMonitor,
        onOpenLogcat = onOpenLogcat,
        onOpenAdbShell = onOpenAdbShell,
        onOpenRootAdbShell = onOpenRootAdbShell,
    )
}

@Composable
private fun FeedbackSectionHost(component: DashboardComponent) {
    val initialUi = remember(component) {
        FeedbackUiState(
            adbCheckState = component.state.value.adbCheckState,
            refreshError = component.state.value.refreshError,
            adbServerError = component.state.value.adbServer.actionError,
            isTerminalLaunchFailed = component.state.value.isTerminalLaunchFailed,
        )
    }
    val uiState by remember(component) {
        component.state
            .map {
                FeedbackUiState(
                    adbCheckState = it.adbCheckState,
                    refreshError = it.refreshError,
                    adbServerError = it.adbServer.actionError,
                    isTerminalLaunchFailed = it.isTerminalLaunchFailed,
                )
            }
            .distinctUntilChanged()
    }.collectAsState(initial = initialUi)

    val onDismissAdbCheck = remember(component) { { component.onDismissAdbCheck() } }
    val onDismissRefreshError = remember(component) { { component.onDismissRefreshError() } }
    val onDismissAdbServerError = remember(component) { { component.onDismissAdbServerError() } }
    val onDismissTerminalLaunchError = remember(component) { { component.onDismissTerminalLaunchError() } }

    FeedbackSection(
        state = uiState,
        onDismissAdbCheck = onDismissAdbCheck,
        onDismissRefreshError = onDismissRefreshError,
        onDismissAdbServerError = onDismissAdbServerError,
        onDismissTerminalLaunchError = onDismissTerminalLaunchError,
    )
}

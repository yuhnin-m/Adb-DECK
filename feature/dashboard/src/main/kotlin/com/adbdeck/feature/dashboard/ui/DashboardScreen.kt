package com.adbdeck.feature.dashboard.ui

import adbdeck.feature.dashboard.generated.resources.Res
import adbdeck.feature.dashboard.generated.resources.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Contacts
import androidx.compose.material.icons.outlined.DevicesOther
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Monitor
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material.icons.outlined.SystemUpdateAlt
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.adbdeck.core.designsystem.AdbTheme
import com.adbdeck.core.designsystem.Dimensions
import com.adbdeck.core.ui.AdbBanner
import com.adbdeck.core.ui.AdbBannerType
import com.adbdeck.core.ui.actiontiles.AdbActionTile
import com.adbdeck.core.ui.buttons.AdbButtonSize
import com.adbdeck.core.ui.buttons.AdbOutlinedButton
import com.adbdeck.core.ui.sectioncards.AdbSectionCard
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
            AdbCoreSectionHost(component = component)
            QuickActionsSectionHost(component = component)
            FeedbackSectionHost(component = component)
        }
    }
}

private data class QuickActionsUiState(
    val deviceCount: Int,
)

private data class DashboardNavItemUi(
    val icon: ImageVector,
    val title: String,
    val subtitle: String,
    val onClick: () -> Unit,
)

private data class AdbCoreUiState(
    val isAdbFound: Boolean,
    val adbVersion: String?,
    val deviceCount: Int,
    val serverState: DashboardAdbServerState,
    val activeAction: DashboardAdbServerAction?,
)

private data class FeedbackUiState(
    val adbCheckState: DashboardAdbCheckState,
    val refreshError: String?,
    val adbServerError: String?,
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

    AdbCoreSection(
        state = uiState,
        onRefreshStatus = component::onRefreshAdbServerStatus,
        onStartServer = component::onStartAdbServer,
        onStopServer = component::onStopAdbServer,
        onRestartServer = component::onRestartAdbServer,
        onOpenSettings = component::onOpenSettings,
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
        onOpenDeviceInfo = component::onOpenDeviceInfo,
        onOpenQuickToggles = component::onOpenQuickToggles,
        onOpenPackages = component::onOpenPackages,
        onOpenApkInstall = component::onOpenApkInstall,
        onOpenDeepLinks = component::onOpenDeepLinks,
        onOpenNotifications = component::onOpenNotifications,
        onOpenScreenTools = component::onOpenScreenTools,
        onOpenFileExplorer = component::onOpenFileExplorer,
        onOpenContacts = component::onOpenContacts,
        onOpenSystemMonitor = component::onOpenSystemMonitor,
        onOpenLogcat = component::onOpenLogcat,
    )
}

@Composable
private fun FeedbackSectionHost(component: DashboardComponent) {
    val initialUi = remember(component) {
        FeedbackUiState(
            adbCheckState = component.state.value.adbCheckState,
            refreshError = component.state.value.refreshError,
            adbServerError = component.state.value.adbServer.actionError,
        )
    }
    val uiState by remember(component) {
        component.state
            .map {
                FeedbackUiState(
                    adbCheckState = it.adbCheckState,
                    refreshError = it.refreshError,
                    adbServerError = it.adbServer.actionError,
                )
            }
            .distinctUntilChanged()
    }.collectAsState(initial = initialUi)

    FeedbackSection(
        state = uiState,
        onDismissAdbCheck = component::onDismissAdbCheck,
        onDismissRefreshError = component::onDismissRefreshError,
        onDismissAdbServerError = component::onDismissAdbServerError,
    )
}

@Composable
private fun AdbCoreSection(
    state: AdbCoreUiState,
    onRefreshStatus: () -> Unit,
    onStartServer: () -> Unit,
    onStopServer: () -> Unit,
    onRestartServer: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val sectionTitle = stringResource(Res.string.dashboard_section_adb_server_status)
    val refreshStatusDescription = stringResource(Res.string.dashboard_adb_server_refresh)
    val statusLabel = stringResource(Res.string.dashboard_adb_server_row_status)
    val versionLabel = stringResource(Res.string.dashboard_adb_server_row_version)
    val devicesLabel = stringResource(Res.string.dashboard_adb_server_row_devices)
    val versionValue = state.adbVersion?.ifBlank { "—" } ?: "—"

    val statusValue = when {
        state.isAdbFound && state.serverState == DashboardAdbServerState.RUNNING ->
            stringResource(Res.string.dashboard_adb_server_status_ready)

        state.isAdbFound && state.serverState == DashboardAdbServerState.STOPPED ->
            stringResource(Res.string.dashboard_adb_server_status_stopped)

        state.isAdbFound && state.serverState == DashboardAdbServerState.ERROR ->
            stringResource(Res.string.dashboard_adb_server_status_error)

        state.isAdbFound ->
            stringResource(Res.string.dashboard_adb_server_status_unknown)

        else ->
            stringResource(Res.string.dashboard_adb_server_status_not_found)
    }

    val statusColor = if (state.isAdbFound && state.serverState == DashboardAdbServerState.RUNNING) {
        AdbTheme.semanticColors.success
    } else {
        MaterialTheme.colorScheme.error
    }

    val isServerBusy = state.activeAction != null
    val isRunning = state.serverState == DashboardAdbServerState.RUNNING
    val canToggleServer = state.isAdbFound &&
        !isServerBusy &&
        (state.serverState == DashboardAdbServerState.RUNNING || state.serverState == DashboardAdbServerState.STOPPED)
    val canRestart = state.isAdbFound &&
        !isServerBusy &&
        isRunning

    val toggleTitle = if (isRunning) {
        stringResource(Res.string.dashboard_adb_server_action_stop_title)
    } else {
        stringResource(Res.string.dashboard_adb_server_action_start_title)
    }
    val toggleSubtitle = if (isRunning) {
        stringResource(Res.string.dashboard_adb_server_action_stop_subtitle)
    } else {
        stringResource(Res.string.dashboard_adb_server_action_start_subtitle)
    }
    val toggleIcon = if (isRunning) Icons.Outlined.Stop else Icons.Outlined.PlayArrow
    val toggleLoading = state.activeAction == DashboardAdbServerAction.START ||
        state.activeAction == DashboardAdbServerAction.STOP
    val toggleOnClick = if (isRunning) onStopServer else onStartServer

    val actionItems = listOf(
        AdbCoreActionCardUi(
            title = toggleTitle,
            subtitle = toggleSubtitle,
            icon = toggleIcon,
            enabled = canToggleServer,
            isLoading = toggleLoading,
            onClick = toggleOnClick,
            containerColor = if (isRunning) {
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.38f)
            } else {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.52f)
            },
        ),
        AdbCoreActionCardUi(
            title = stringResource(Res.string.dashboard_adb_server_action_restart_title),
            subtitle = stringResource(Res.string.dashboard_adb_server_action_restart_subtitle),
            icon = Icons.Outlined.RestartAlt,
            enabled = canRestart,
            isLoading = state.activeAction == DashboardAdbServerAction.RESTART,
            onClick = onRestartServer,
            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f),
        ),
        AdbCoreActionCardUi(
            title = stringResource(Res.string.dashboard_adb_server_action_open_settings_title),
            subtitle = stringResource(Res.string.dashboard_adb_server_action_open_settings_subtitle),
            icon = Icons.Outlined.Settings,
            enabled = !isServerBusy,
            isLoading = false,
            onClick = onOpenSettings,
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.52f),
        ),
    )

    AdbSectionCard(
        title = sectionTitle,
        titleUppercase = true,
        contentSpacing = Dimensions.paddingMedium,
        contentPadding = PaddingValues(
            horizontal = Dimensions.paddingLarge,
            vertical = Dimensions.paddingMedium,
        ),
        headerTrailing = {
            AdbOutlinedButton(
                onClick = onRefreshStatus,
                leadingIcon = Icons.Outlined.Refresh,
                contentDescription = refreshStatusDescription,
                enabled = !isServerBusy,
                loading = state.activeAction == DashboardAdbServerAction.REFRESH,
                size = AdbButtonSize.XSMALL,
            )
        },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(Dimensions.paddingMedium),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AdbCoreInfoTable(
                statusLabel = statusLabel,
                statusValue = statusValue,
                statusColor = statusColor,
                versionLabel = versionLabel,
                versionValue = versionValue,
                devicesLabel = devicesLabel,
                devicesValue = state.deviceCount.toString(),
                modifier = Modifier.widthIn(min = 280.dp, max = 360.dp),
            )
            Spacer(modifier = Modifier.width(Dimensions.paddingSmall))
            Surface(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(1.dp),
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
            ) {}
            Spacer(modifier = Modifier.width(Dimensions.paddingSmall))
            AdbCoreActionsRow(
                actions = actionItems,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

private data class AdbCoreActionCardUi(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val enabled: Boolean,
    val isLoading: Boolean,
    val onClick: () -> Unit,
    val containerColor: Color,
)

@Composable
private fun AdbCoreInfoTable(
    statusLabel: String,
    statusValue: String,
    statusColor: Color,
    versionLabel: String,
    versionValue: String,
    devicesLabel: String,
    devicesValue: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(Dimensions.paddingSmall),
    ) {
        AdbCoreInfoRow(
            label = statusLabel,
            value = statusValue,
            valueColor = statusColor,
            emphasizeValue = true,
        )
        AdbCoreInfoRow(
            label = versionLabel,
            value = versionValue,
        )
        AdbCoreInfoRow(
            label = devicesLabel,
            value = devicesValue,
        )
    }
}

@Composable
private fun AdbCoreInfoRow(
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
    emphasizeValue: Boolean = false,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(Dimensions.paddingSmall),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .width(136.dp)
                .fillMaxHeight(),
            contentAlignment = Alignment.CenterStart,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = value,
            style = if (emphasizeValue) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
            fontWeight = if (emphasizeValue) FontWeight.SemiBold else FontWeight.Medium,
            color = valueColor,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun AdbCoreActionsRow(
    actions: List<AdbCoreActionCardUi>,
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(Dimensions.paddingSmall),
        modifier = modifier.fillMaxWidth(),
    ) {
        actions.forEach { action ->
            AdbCoreActionCard(
                action = action,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun AdbCoreActionCard(
    action: AdbCoreActionCardUi,
    modifier: Modifier = Modifier,
) {
    val isEnabled = action.enabled && !action.isLoading
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val isPressed by interactionSource.collectIsPressedAsState()
    val animatedContainerColor = when {
        !isEnabled -> action.containerColor.copy(alpha = 0.62f)
        isPressed -> action.containerColor.copy(alpha = 0.9f)
        isHovered -> action.containerColor.copy(alpha = 0.96f)
        else -> action.containerColor
    }

    Surface(
        modifier = modifier
            .height(114.dp)
            .clickable(
                enabled = isEnabled,
                interactionSource = interactionSource,
                indication = null,
                onClick = action.onClick,
            ),
        shape = MaterialTheme.shapes.small,
        color = animatedContainerColor,
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
        ),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(Dimensions.paddingDefault),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            if (action.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(Dimensions.iconSizeCard),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary,
                )
            } else {
                Icon(
                    imageVector = action.icon,
                    contentDescription = null,
                    modifier = Modifier.size(Dimensions.iconSizeCard),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
            Column {
                Text(
                    text = action.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = action.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun QuickActionsSection(
    state: QuickActionsUiState,
    onOpenDevices: () -> Unit,
    onOpenDeviceInfo: () -> Unit,
    onOpenQuickToggles: () -> Unit,
    onOpenPackages: () -> Unit,
    onOpenApkInstall: () -> Unit,
    onOpenDeepLinks: () -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenScreenTools: () -> Unit,
    onOpenFileExplorer: () -> Unit,
    onOpenContacts: () -> Unit,
    onOpenSystemMonitor: () -> Unit,
    onOpenLogcat: () -> Unit,
) {
    val devicesSubtitle = if (state.deviceCount > 0) {
        stringResource(Res.string.dashboard_tile_devices_subtitle_count, state.deviceCount)
    } else {
        stringResource(Res.string.dashboard_tile_devices_subtitle_default)
    }

    val devicesSectionTitle = stringResource(Res.string.dashboard_section_devices)
    val appsSectionTitle = stringResource(Res.string.dashboard_section_apps)
    val deviceToolsSectionTitle = stringResource(Res.string.dashboard_section_device_tools)
    val debuggingSectionTitle = stringResource(Res.string.dashboard_section_debugging)

    DashboardNavSection(
        title = devicesSectionTitle,
        showDivider = true,
        items = listOf(
            DashboardNavItemUi(
                icon = Icons.Outlined.DevicesOther,
                title = stringResource(Res.string.dashboard_tile_devices_title),
                subtitle = devicesSubtitle,
                onClick = onOpenDevices,
            ),
            DashboardNavItemUi(
                icon = Icons.Outlined.Info,
                title = stringResource(Res.string.dashboard_tile_device_info_title),
                subtitle = stringResource(Res.string.dashboard_tile_device_info_subtitle),
                onClick = onOpenDeviceInfo,
            ),
            DashboardNavItemUi(
                icon = Icons.Outlined.Tune,
                title = stringResource(Res.string.dashboard_tile_quick_toggles_title),
                subtitle = stringResource(Res.string.dashboard_tile_quick_toggles_subtitle),
                onClick = onOpenQuickToggles,
            ),
        ),
    )

    DashboardNavSection(
        title = appsSectionTitle,
        showDivider = true,
        items = listOf(
            DashboardNavItemUi(
                icon = Icons.Outlined.Apps,
                title = stringResource(Res.string.dashboard_tile_packages_title),
                subtitle = stringResource(Res.string.dashboard_tile_packages_subtitle),
                onClick = onOpenPackages,
            ),
            DashboardNavItemUi(
                icon = Icons.Outlined.SystemUpdateAlt,
                title = stringResource(Res.string.dashboard_tile_apk_install_title),
                subtitle = stringResource(Res.string.dashboard_tile_apk_install_subtitle),
                onClick = onOpenApkInstall,
            ),
            DashboardNavItemUi(
                icon = Icons.Outlined.Link,
                title = stringResource(Res.string.dashboard_tile_deep_links_title),
                subtitle = stringResource(Res.string.dashboard_tile_deep_links_subtitle),
                onClick = onOpenDeepLinks,
            ),
            DashboardNavItemUi(
                icon = Icons.Outlined.Notifications,
                title = stringResource(Res.string.dashboard_tile_notifications_title),
                subtitle = stringResource(Res.string.dashboard_tile_notifications_subtitle),
                onClick = onOpenNotifications,
            ),
        ),
    )

    DashboardNavSection(
        title = deviceToolsSectionTitle,
        showDivider = true,
        items = listOf(
            DashboardNavItemUi(
                icon = Icons.Outlined.CameraAlt,
                title = stringResource(Res.string.dashboard_tile_screen_tools_title),
                subtitle = stringResource(Res.string.dashboard_tile_screen_tools_subtitle),
                onClick = onOpenScreenTools,
            ),
            DashboardNavItemUi(
                icon = Icons.Outlined.Folder,
                title = stringResource(Res.string.dashboard_tile_file_explorer_title),
                subtitle = stringResource(Res.string.dashboard_tile_file_explorer_subtitle),
                onClick = onOpenFileExplorer,
            ),
            DashboardNavItemUi(
                icon = Icons.Outlined.Contacts,
                title = stringResource(Res.string.dashboard_tile_contacts_title),
                subtitle = stringResource(Res.string.dashboard_tile_contacts_subtitle),
                onClick = onOpenContacts,
            ),
            DashboardNavItemUi(
                icon = Icons.Outlined.Monitor,
                title = stringResource(Res.string.dashboard_tile_system_monitor_title),
                subtitle = stringResource(Res.string.dashboard_tile_system_monitor_subtitle),
                onClick = onOpenSystemMonitor,
            ),
        ),
    )

    DashboardNavSection(
        title = debuggingSectionTitle,
        showDivider = false,
        items = listOf(
            DashboardNavItemUi(
                icon = Icons.Outlined.Terminal,
                title = stringResource(Res.string.dashboard_tile_logcat_title),
                subtitle = stringResource(Res.string.dashboard_tile_logcat_subtitle),
                onClick = onOpenLogcat,
            ),
            DashboardNavItemUi(
                icon = Icons.Outlined.Monitor,
                title = stringResource(Res.string.dashboard_tile_process_monitor_title),
                subtitle = stringResource(Res.string.dashboard_tile_process_monitor_subtitle),
                onClick = onOpenSystemMonitor,
            ),
            DashboardNavItemUi(
                icon = Icons.Outlined.Notifications,
                title = stringResource(Res.string.dashboard_tile_notification_monitor_title),
                subtitle = stringResource(Res.string.dashboard_tile_notification_monitor_subtitle),
                onClick = onOpenNotifications,
            ),
            DashboardNavItemUi(
                icon = Icons.Outlined.Restore,
                title = stringResource(Res.string.dashboard_tile_intent_history_title),
                subtitle = stringResource(Res.string.dashboard_tile_intent_history_subtitle),
                onClick = onOpenDeepLinks,
            ),
        ),
    )
}

@Composable
private fun DashboardNavSection(
    title: String,
    items: List<DashboardNavItemUi>,
    showDivider: Boolean,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Dimensions.paddingSmall),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(Dimensions.paddingMedium),
            modifier = Modifier.fillMaxWidth(),
        ) {
            items.forEach { item ->
                AdbActionTile(
                    icon = item.icon,
                    title = item.title,
                    subtitle = item.subtitle,
                    onClick = item.onClick,
                    height = 116.dp,
                    modifier = Modifier.weight(1f),
                )
            }
            repeat((4 - items.size).coerceAtLeast(0)) {
                Spacer(modifier = Modifier.weight(1f))
            }
        }

        if (showDivider) {
            HorizontalDivider(
                modifier = Modifier.padding(top = Dimensions.paddingXSmall),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.68f),
            )
        }
    }
}

@Composable
private fun FeedbackSection(
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

    state.adbServerError?.takeIf { it.isNotBlank() }?.let { error ->
        val message = stringResource(Res.string.dashboard_adb_server_action_failed, error)
        Spacer(modifier = Modifier.height(Dimensions.paddingXSmall))
        AdbBanner(
            message = message,
            type = AdbBannerType.ERROR,
            onDismiss = onDismissAdbServerError,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

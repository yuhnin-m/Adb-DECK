package com.adbdeck.feature.dashboard.ui

import adbdeck.feature.dashboard.generated.resources.Res
import adbdeck.feature.dashboard.generated.resources.*
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.adbdeck.core.ui.buttons.AdbButtonSize
import com.adbdeck.core.ui.buttons.AdbOutlinedButton
import com.adbdeck.core.ui.sectioncards.AdbSectionCard
import com.adbdeck.feature.dashboard.DashboardAdbServerAction
import com.adbdeck.feature.dashboard.DashboardAdbServerState
import org.jetbrains.compose.resources.stringResource

private val AdbCoreWideLayoutMinWidth = 900.dp
private val AdbCoreWideActionCardMaxWidth = 260.dp
private val AdbCoreCompactButtonMaxWidth = 300.dp
private val AdbCoreActionsTopSpacerHeight = AdbButtonSize.XSMALL.height + Dimensions.paddingXSmall
private const val AdbCoreEmptyValue = "—"

@Composable
internal fun AdbCoreSection(
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
    val versionValue = state.adbVersion?.ifBlank { AdbCoreEmptyValue } ?: AdbCoreEmptyValue

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

    val toggleContainerColor = if (isRunning) {
        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.38f)
    } else {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.52f)
    }
    val restartContainerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
    val settingsContainerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.52f)

    val compactRestartLabel = stringResource(Res.string.dashboard_adb_server_action_restart_title)
    val compactSettingsLabel = stringResource(Res.string.dashboard_adb_server_action_open_settings_title)
    val canOpenSettings = !isServerBusy
    val refreshLoading = state.activeAction == DashboardAdbServerAction.REFRESH
    val restartLoading = state.activeAction == DashboardAdbServerAction.RESTART

    val actionItems = listOf(
        AdbCoreActionCardUi(
            title = toggleTitle,
            subtitle = toggleSubtitle,
            icon = toggleIcon,
            enabled = canToggleServer,
            isLoading = toggleLoading,
            onClick = toggleOnClick,
            containerColor = toggleContainerColor,
        ),
        AdbCoreActionCardUi(
            title = stringResource(Res.string.dashboard_adb_server_action_restart_title),
            subtitle = stringResource(Res.string.dashboard_adb_server_action_restart_subtitle),
            icon = Icons.Outlined.RestartAlt,
            enabled = canRestart,
            isLoading = restartLoading,
            onClick = onRestartServer,
            containerColor = restartContainerColor,
        ),
        AdbCoreActionCardUi(
            title = stringResource(Res.string.dashboard_adb_server_action_open_settings_title),
            subtitle = stringResource(Res.string.dashboard_adb_server_action_open_settings_subtitle),
            icon = Icons.Outlined.Settings,
            enabled = canOpenSettings,
            isLoading = false,
            onClick = onOpenSettings,
            containerColor = settingsContainerColor,
        ),
    )

    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val isWideLayout = maxWidth >= AdbCoreWideLayoutMinWidth
        Crossfade(targetState = isWideLayout, label = "adbCoreLayoutMode") { wide ->
            if (wide) {
                AdbCoreWideLayout(
                    sectionTitle = sectionTitle,
                    refreshStatusDescription = refreshStatusDescription,
                    onRefreshStatus = onRefreshStatus,
                    canRefreshStatus = !isServerBusy,
                    refreshLoading = refreshLoading,
                    statusLabel = statusLabel,
                    statusValue = statusValue,
                    statusColor = statusColor,
                    versionLabel = versionLabel,
                    versionValue = versionValue,
                    devicesLabel = devicesLabel,
                    devicesValue = state.deviceCount.toString(),
                    actions = actionItems,
                )
            } else {
                AdbCoreCompactLayout(
                    sectionTitle = sectionTitle,
                    refreshStatusDescription = refreshStatusDescription,
                    onRefreshStatus = onRefreshStatus,
                    canRefreshStatus = !isServerBusy,
                    refreshLoading = refreshLoading,
                    statusLabel = statusLabel,
                    statusValue = statusValue,
                    statusColor = statusColor,
                    versionLabel = versionLabel,
                    versionValue = versionValue,
                    devicesLabel = devicesLabel,
                    devicesValue = state.deviceCount.toString(),
                    toggleLabel = toggleTitle,
                    toggleIcon = toggleIcon,
                    canToggleServer = canToggleServer,
                    toggleLoading = toggleLoading,
                    onToggleServer = toggleOnClick,
                    canRestart = canRestart,
                    restartLoading = restartLoading,
                    onRestartServer = onRestartServer,
                    canOpenSettings = canOpenSettings,
                    onOpenSettings = onOpenSettings,
                    restartLabel = compactRestartLabel,
                    settingsLabel = compactSettingsLabel,
                    toggleContainerColor = toggleContainerColor,
                    restartContainerColor = restartContainerColor,
                    settingsContainerColor = settingsContainerColor,
                )
            }
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
private fun AdbCoreWideLayout(
    sectionTitle: String,
    refreshStatusDescription: String,
    onRefreshStatus: () -> Unit,
    canRefreshStatus: Boolean,
    refreshLoading: Boolean,
    statusLabel: String,
    statusValue: String,
    statusColor: Color,
    versionLabel: String,
    versionValue: String,
    devicesLabel: String,
    devicesValue: String,
    actions: List<AdbCoreActionCardUi>,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(Dimensions.paddingMedium),
        verticalAlignment = Alignment.Top,
    ) {
        AdbCoreStatusCard(
            sectionTitle = sectionTitle,
            refreshStatusDescription = refreshStatusDescription,
            onRefreshStatus = onRefreshStatus,
            canRefreshStatus = canRefreshStatus,
            refreshLoading = refreshLoading,
            statusLabel = statusLabel,
            statusValue = statusValue,
            statusColor = statusColor,
            versionLabel = versionLabel,
            versionValue = versionValue,
            devicesLabel = devicesLabel,
            devicesValue = devicesValue,
            modifier = Modifier.widthIn(min = 300.dp, max = 360.dp),
        )
        AdbCoreActionsRow(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            actions = actions,
            topSpacerHeight = AdbCoreActionsTopSpacerHeight,
        )
    }
}

@Composable
private fun AdbCoreCompactLayout(
    sectionTitle: String,
    refreshStatusDescription: String,
    onRefreshStatus: () -> Unit,
    canRefreshStatus: Boolean,
    refreshLoading: Boolean,
    statusLabel: String,
    statusValue: String,
    statusColor: Color,
    versionLabel: String,
    versionValue: String,
    devicesLabel: String,
    devicesValue: String,
    toggleLabel: String,
    toggleIcon: ImageVector,
    canToggleServer: Boolean,
    toggleLoading: Boolean,
    onToggleServer: () -> Unit,
    canRestart: Boolean,
    restartLoading: Boolean,
    onRestartServer: () -> Unit,
    canOpenSettings: Boolean,
    onOpenSettings: () -> Unit,
    restartLabel: String,
    settingsLabel: String,
    toggleContainerColor: Color,
    restartContainerColor: Color,
    settingsContainerColor: Color,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(Dimensions.paddingMedium),
        verticalAlignment = Alignment.Top,
    ) {
        AdbCoreStatusCard(
            sectionTitle = sectionTitle,
            refreshStatusDescription = refreshStatusDescription,
            onRefreshStatus = onRefreshStatus,
            canRefreshStatus = canRefreshStatus,
            refreshLoading = refreshLoading,
            statusLabel = statusLabel,
            statusValue = statusValue,
            statusColor = statusColor,
            versionLabel = versionLabel,
            versionValue = versionValue,
            devicesLabel = devicesLabel,
            devicesValue = devicesValue,
            modifier = Modifier.weight(1f),
        )

        Column(
            modifier = Modifier
                .widthIn(max = AdbCoreCompactButtonMaxWidth)
                .fillMaxHeight(),
        ) {
            Spacer(modifier = Modifier.height(AdbCoreActionsTopSpacerHeight))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(
                    space = Dimensions.paddingXSmall,
                    alignment = Alignment.Bottom,
                ),
                horizontalAlignment = Alignment.Start,
            ) {
                AdbCoreCompactSoftButton(
                    onClick = onToggleServer,
                    text = toggleLabel,
                    leadingIcon = toggleIcon,
                    enabled = canToggleServer,
                    loading = toggleLoading,
                    containerColor = toggleContainerColor,
                    modifier = Modifier.widthIn(max = AdbCoreCompactButtonMaxWidth),
                )
                AdbCoreCompactSoftButton(
                    onClick = onRestartServer,
                    text = restartLabel,
                    leadingIcon = Icons.Outlined.RestartAlt,
                    enabled = canRestart,
                    loading = restartLoading,
                    containerColor = restartContainerColor,
                    modifier = Modifier.widthIn(max = AdbCoreCompactButtonMaxWidth),
                )
                AdbCoreCompactSoftButton(
                    onClick = onOpenSettings,
                    text = settingsLabel,
                    leadingIcon = Icons.Outlined.Settings,
                    enabled = canOpenSettings,
                    loading = false,
                    containerColor = settingsContainerColor,
                    modifier = Modifier.widthIn(max = AdbCoreCompactButtonMaxWidth),
                )
            }
        }
    }
}

@Composable
private fun AdbCoreStatusCard(
    sectionTitle: String,
    refreshStatusDescription: String,
    onRefreshStatus: () -> Unit,
    canRefreshStatus: Boolean,
    refreshLoading: Boolean,
    statusLabel: String,
    statusValue: String,
    statusColor: Color,
    versionLabel: String,
    versionValue: String,
    devicesLabel: String,
    devicesValue: String,
    modifier: Modifier = Modifier,
) {
    AdbSectionCard(
        title = sectionTitle,
        modifier = modifier,
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
                enabled = canRefreshStatus,
                loading = refreshLoading,
                size = AdbButtonSize.XSMALL,
            )
        },
    ) {
        AdbCoreInfoTable(
            statusLabel = statusLabel,
            statusValue = statusValue,
            statusColor = statusColor,
            versionLabel = versionLabel,
            versionValue = versionValue,
            devicesLabel = devicesLabel,
            devicesValue = devicesValue,
        )
    }
}

@Composable
private fun AdbCoreCompactSoftButton(
    onClick: () -> Unit,
    text: String,
    leadingIcon: ImageVector,
    enabled: Boolean,
    loading: Boolean,
    containerColor: Color,
    modifier: Modifier = Modifier,
) {
    val isEnabled = enabled && !loading
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val isPressed by interactionSource.collectIsPressedAsState()
    val background = resolveActionContainerColor(
        baseColor = containerColor,
        isEnabled = isEnabled,
        isHovered = isHovered,
        isPressed = isPressed,
    )
    val animatedBackground by animateColorAsState(
        targetValue = background,
        label = "adbCoreCompactButtonContainerColor",
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(AdbButtonSize.SMALL.height)
            .clickable(
                enabled = isEnabled,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
        shape = MaterialTheme.shapes.small,
        color = animatedBackground,
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
        ),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = Dimensions.paddingSmall),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(14.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary,
                )
            } else {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }

            Spacer(modifier = Modifier.width(Dimensions.paddingXSmall))

            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

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
    topSpacerHeight: androidx.compose.ui.unit.Dp = 0.dp,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight(),
    ) {
        Spacer(modifier = Modifier.height(topSpacerHeight))

        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(Dimensions.paddingSmall),
            modifier = Modifier
                .fillMaxWidth()
        ) {
            actions.forEach { action ->
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.TopStart,
                ) {
                    AdbCoreActionCard(
                        action = action,
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = AdbCoreWideActionCardMaxWidth),
                    )
                }
            }
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
    val animatedContainerColor = resolveActionContainerColor(
        baseColor = action.containerColor,
        isEnabled = isEnabled,
        isHovered = isHovered,
        isPressed = isPressed,
    )
    val animatedContainerColorState by animateColorAsState(
        targetValue = animatedContainerColor,
        label = "adbCoreWideCardContainerColor",
    )

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
        color = animatedContainerColorState,
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

/**
 * Keeps action surfaces in one palette for wide cards and compact buttons.
 * Hover/press use subtle alpha shifts to avoid overly bright states.
 */
private fun resolveActionContainerColor(
    baseColor: Color,
    isEnabled: Boolean,
    isHovered: Boolean,
    isPressed: Boolean,
): Color {
    val baseAlpha = baseColor.alpha
    return when {
        !isEnabled -> baseColor.copy(alpha = (baseAlpha * 0.75f).coerceIn(0f, 1f))
        isPressed -> baseColor.copy(alpha = (baseAlpha + 0.08f).coerceIn(0f, 1f))
        isHovered -> baseColor.copy(alpha = (baseAlpha + 0.04f).coerceIn(0f, 1f))
        else -> baseColor
    }
}

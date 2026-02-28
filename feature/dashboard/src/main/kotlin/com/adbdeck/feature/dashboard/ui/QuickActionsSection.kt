package com.adbdeck.feature.dashboard.ui

import adbdeck.feature.dashboard.generated.resources.Res
import adbdeck.feature.dashboard.generated.resources.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Cast
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Contacts
import androidx.compose.material.icons.outlined.DevicesOther
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Monitor
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.SystemUpdateAlt
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.adbdeck.core.designsystem.Dimensions
import com.adbdeck.core.ui.actiontiles.AdbActionTile
import org.jetbrains.compose.resources.stringResource

private val DashboardTileWidth = 232.dp

private data class DashboardNavItemUi(
    val icon: ImageVector,
    val title: String,
    val subtitle: String,
    val onClick: () -> Unit,
)

@Composable
internal fun QuickActionsSection(
    state: QuickActionsUiState,
    onOpenDevices: () -> Unit,
    onOpenDeviceInfo: () -> Unit,
    onOpenQuickToggles: () -> Unit,
    onOpenPackages: () -> Unit,
    onOpenApkInstall: () -> Unit,
    onOpenDeepLinks: () -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenScreenTools: () -> Unit,
    onOpenScrcpy: () -> Unit,
    onOpenFileExplorer: () -> Unit,
    onOpenContacts: () -> Unit,
    onOpenSystemMonitor: () -> Unit,
    onOpenLogcat: () -> Unit,
    onOpenAdbShell: () -> Unit,
    onOpenRootAdbShell: () -> Unit,
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

    val devicesTitle = stringResource(Res.string.dashboard_tile_devices_title)
    val deviceInfoTitle = stringResource(Res.string.dashboard_tile_device_info_title)
    val deviceInfoSubtitle = stringResource(Res.string.dashboard_tile_device_info_subtitle)
    val quickTogglesTitle = stringResource(Res.string.dashboard_tile_quick_toggles_title)
    val quickTogglesSubtitle = stringResource(Res.string.dashboard_tile_quick_toggles_subtitle)

    val packagesTitle = stringResource(Res.string.dashboard_tile_packages_title)
    val packagesSubtitle = stringResource(Res.string.dashboard_tile_packages_subtitle)
    val apkInstallTitle = stringResource(Res.string.dashboard_tile_apk_install_title)
    val apkInstallSubtitle = stringResource(Res.string.dashboard_tile_apk_install_subtitle)
    val deepLinksTitle = stringResource(Res.string.dashboard_tile_deep_links_title)
    val deepLinksSubtitle = stringResource(Res.string.dashboard_tile_deep_links_subtitle)
    val notificationsTitle = stringResource(Res.string.dashboard_tile_notifications_title)
    val notificationsSubtitle = stringResource(Res.string.dashboard_tile_notifications_subtitle)

    val screenToolsTitle = stringResource(Res.string.dashboard_tile_screen_tools_title)
    val screenToolsSubtitle = stringResource(Res.string.dashboard_tile_screen_tools_subtitle)
    val mirrorScreenTitle = stringResource(Res.string.dashboard_tile_mirror_screen_title)
    val mirrorScreenSubtitle = stringResource(Res.string.dashboard_tile_mirror_screen_subtitle)
    val fileExplorerTitle = stringResource(Res.string.dashboard_tile_file_explorer_title)
    val fileExplorerSubtitle = stringResource(Res.string.dashboard_tile_file_explorer_subtitle)
    val contactsTitle = stringResource(Res.string.dashboard_tile_contacts_title)
    val contactsSubtitle = stringResource(Res.string.dashboard_tile_contacts_subtitle)
    val systemMonitorTitle = stringResource(Res.string.dashboard_tile_system_monitor_title)
    val systemMonitorSubtitle = stringResource(Res.string.dashboard_tile_system_monitor_subtitle)
    val adbShellTitle = stringResource(Res.string.dashboard_tile_adb_shell_title)
    val adbShellSubtitle = stringResource(Res.string.dashboard_tile_adb_shell_subtitle)
    val rootAdbShellTitle = stringResource(Res.string.dashboard_tile_root_adb_shell_title)
    val rootAdbShellSubtitle = stringResource(Res.string.dashboard_tile_root_adb_shell_subtitle)

    val logcatTitle = stringResource(Res.string.dashboard_tile_logcat_title)
    val logcatSubtitle = stringResource(Res.string.dashboard_tile_logcat_subtitle)
    val processMonitorTitle = stringResource(Res.string.dashboard_tile_process_monitor_title)
    val processMonitorSubtitle = stringResource(Res.string.dashboard_tile_process_monitor_subtitle)
    val notificationMonitorTitle = stringResource(Res.string.dashboard_tile_notification_monitor_title)
    val notificationMonitorSubtitle = stringResource(Res.string.dashboard_tile_notification_monitor_subtitle)
    val intentHistoryTitle = stringResource(Res.string.dashboard_tile_intent_history_title)
    val intentHistorySubtitle = stringResource(Res.string.dashboard_tile_intent_history_subtitle)

    val devicesItems = remember(
        devicesTitle,
        devicesSubtitle,
        deviceInfoTitle,
        deviceInfoSubtitle,
        quickTogglesTitle,
        quickTogglesSubtitle,
        onOpenDevices,
        onOpenDeviceInfo,
        onOpenQuickToggles,
    ) {
        listOf(
            DashboardNavItemUi(
                icon = Icons.Outlined.DevicesOther,
                title = devicesTitle,
                subtitle = devicesSubtitle,
                onClick = onOpenDevices,
            ),
            DashboardNavItemUi(
                icon = Icons.Outlined.Info,
                title = deviceInfoTitle,
                subtitle = deviceInfoSubtitle,
                onClick = onOpenDeviceInfo,
            ),
            DashboardNavItemUi(
                icon = Icons.Outlined.Tune,
                title = quickTogglesTitle,
                subtitle = quickTogglesSubtitle,
                onClick = onOpenQuickToggles,
            ),
        )
    }

    val appsItems = remember(
        packagesTitle,
        packagesSubtitle,
        apkInstallTitle,
        apkInstallSubtitle,
        deepLinksTitle,
        deepLinksSubtitle,
        notificationsTitle,
        notificationsSubtitle,
        onOpenPackages,
        onOpenApkInstall,
        onOpenDeepLinks,
        onOpenNotifications,
    ) {
        listOf(
            DashboardNavItemUi(
                icon = Icons.Outlined.Apps,
                title = packagesTitle,
                subtitle = packagesSubtitle,
                onClick = onOpenPackages,
            ),
            DashboardNavItemUi(
                icon = Icons.Outlined.SystemUpdateAlt,
                title = apkInstallTitle,
                subtitle = apkInstallSubtitle,
                onClick = onOpenApkInstall,
            ),
            DashboardNavItemUi(
                icon = Icons.Outlined.Link,
                title = deepLinksTitle,
                subtitle = deepLinksSubtitle,
                onClick = onOpenDeepLinks,
            ),
            DashboardNavItemUi(
                icon = Icons.Outlined.Notifications,
                title = notificationsTitle,
                subtitle = notificationsSubtitle,
                onClick = onOpenNotifications,
            ),
        )
    }

    val deviceToolsItems = remember(
        screenToolsTitle,
        screenToolsSubtitle,
        mirrorScreenTitle,
        mirrorScreenSubtitle,
        fileExplorerTitle,
        fileExplorerSubtitle,
        contactsTitle,
        contactsSubtitle,
        systemMonitorTitle,
        systemMonitorSubtitle,
        adbShellTitle,
        adbShellSubtitle,
        rootAdbShellTitle,
        rootAdbShellSubtitle,
        onOpenScreenTools,
        onOpenScrcpy,
        onOpenFileExplorer,
        onOpenContacts,
        onOpenSystemMonitor,
        onOpenAdbShell,
        onOpenRootAdbShell,
    ) {
        listOf(
            DashboardNavItemUi(
                icon = Icons.Outlined.CameraAlt,
                title = screenToolsTitle,
                subtitle = screenToolsSubtitle,
                onClick = onOpenScreenTools,
            ),
            DashboardNavItemUi(
                icon = Icons.Outlined.Cast,
                title = mirrorScreenTitle,
                subtitle = mirrorScreenSubtitle,
                onClick = onOpenScrcpy,
            ),
            DashboardNavItemUi(
                icon = Icons.Outlined.Folder,
                title = fileExplorerTitle,
                subtitle = fileExplorerSubtitle,
                onClick = onOpenFileExplorer,
            ),
            DashboardNavItemUi(
                icon = Icons.Outlined.Contacts,
                title = contactsTitle,
                subtitle = contactsSubtitle,
                onClick = onOpenContacts,
            ),
            DashboardNavItemUi(
                icon = Icons.Outlined.Monitor,
                title = systemMonitorTitle,
                subtitle = systemMonitorSubtitle,
                onClick = onOpenSystemMonitor,
            ),
            DashboardNavItemUi(
                icon = Icons.Outlined.Terminal,
                title = adbShellTitle,
                subtitle = adbShellSubtitle,
                onClick = onOpenAdbShell,
            ),
            DashboardNavItemUi(
                icon = Icons.Outlined.Security,
                title = rootAdbShellTitle,
                subtitle = rootAdbShellSubtitle,
                onClick = onOpenRootAdbShell,
            ),
        )
    }

    val debuggingItems = remember(
        logcatTitle,
        logcatSubtitle,
        processMonitorTitle,
        processMonitorSubtitle,
        notificationMonitorTitle,
        notificationMonitorSubtitle,
        intentHistoryTitle,
        intentHistorySubtitle,
        onOpenLogcat,
        onOpenSystemMonitor,
        onOpenNotifications,
        onOpenDeepLinks,
    ) {
        listOf(
            DashboardNavItemUi(
                icon = Icons.Outlined.Terminal,
                title = logcatTitle,
                subtitle = logcatSubtitle,
                onClick = onOpenLogcat,
            ),
            DashboardNavItemUi(
                icon = Icons.Outlined.Monitor,
                title = processMonitorTitle,
                subtitle = processMonitorSubtitle,
                onClick = onOpenSystemMonitor,
            ),
            DashboardNavItemUi(
                icon = Icons.Outlined.Notifications,
                title = notificationMonitorTitle,
                subtitle = notificationMonitorSubtitle,
                onClick = onOpenNotifications,
            ),
            DashboardNavItemUi(
                icon = Icons.Outlined.Restore,
                title = intentHistoryTitle,
                subtitle = intentHistorySubtitle,
                onClick = onOpenDeepLinks,
            ),
        )
    }

    DashboardNavSection(
        title = devicesSectionTitle,
        showDivider = true,
        items = devicesItems,
    )

    DashboardNavSection(
        title = appsSectionTitle,
        showDivider = true,
        items = appsItems,
    )

    DashboardNavSection(
        title = deviceToolsSectionTitle,
        showDivider = true,
        items = deviceToolsItems,
    )

    DashboardNavSection(
        title = debuggingSectionTitle,
        showDivider = false,
        items = debuggingItems,
    )
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
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

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(Dimensions.paddingMedium),
            verticalArrangement = Arrangement.spacedBy(Dimensions.paddingMedium),
            modifier = Modifier.fillMaxWidth(),
        ) {
            items.forEach { item ->
                AdbActionTile(
                    icon = item.icon,
                    title = item.title,
                    subtitle = item.subtitle,
                    onClick = item.onClick,
                    height = 116.dp,
                    modifier = Modifier.width(DashboardTileWidth),
                )
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

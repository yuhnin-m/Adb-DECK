package com.adbdeck.feature.notifications.ui

import adbdeck.feature.notifications.generated.resources.Res
import adbdeck.feature.notifications.generated.resources.notifications_detail_action_copy_package
import adbdeck.feature.notifications.generated.resources.notifications_detail_action_copy_text
import adbdeck.feature.notifications.generated.resources.notifications_detail_action_copy_title
import adbdeck.feature.notifications.generated.resources.notifications_detail_action_export_json
import adbdeck.feature.notifications.generated.resources.notifications_detail_action_open_packages
import adbdeck.feature.notifications.generated.resources.notifications_detail_action_save
import adbdeck.feature.notifications.generated.resources.notifications_export_dialog_filter_description
import adbdeck.feature.notifications.generated.resources.notifications_export_dialog_title
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.BookmarkAdd
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material.icons.outlined.Title
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.adbdeck.core.adb.api.notifications.NotificationRecord
import com.adbdeck.core.designsystem.Dimensions
import com.adbdeck.core.ui.buttons.AdbButtonSize
import com.adbdeck.core.ui.buttons.AdbOutlinedButton
import com.adbdeck.feature.notifications.NotificationsComponent
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun NotificationActionsGrid(
    record: NotificationRecord,
    isSaved: Boolean,
    component: NotificationsComponent,
) {
    val exportDialogTitle = stringResource(Res.string.notifications_export_dialog_title)
    val exportDialogFilter = stringResource(Res.string.notifications_export_dialog_filter_description)

    Column(verticalArrangement = Arrangement.spacedBy(Dimensions.paddingXSmall)) {
        NotificationActionButton(
            icon = Icons.Outlined.ContentCopy,
            label = stringResource(Res.string.notifications_detail_action_copy_package),
            onClick = { component.onCopyPackageName(record) },
        )

        record.title?.takeIf { it.isNotBlank() }?.let {
            NotificationActionButton(
                icon = Icons.Outlined.Title,
                label = stringResource(Res.string.notifications_detail_action_copy_title),
                onClick = { component.onCopyTitle(record) },
            )
        }

        record.text?.takeIf { it.isNotBlank() }?.let {
            NotificationActionButton(
                icon = Icons.Outlined.TextFields,
                label = stringResource(Res.string.notifications_detail_action_copy_text),
                onClick = { component.onCopyText(record) },
            )
        }

        if (!isSaved) {
            NotificationActionButton(
                icon = Icons.Outlined.BookmarkAdd,
                label = stringResource(Res.string.notifications_detail_action_save),
                onClick = { component.onSaveNotification(record) },
            )
        }

        NotificationActionButton(
            icon = Icons.Outlined.Apps,
            label = stringResource(Res.string.notifications_detail_action_open_packages),
            onClick = { component.onOpenInPackages(record.packageName) },
        )

        NotificationActionButton(
            icon = Icons.Outlined.FileDownload,
            label = stringResource(Res.string.notifications_detail_action_export_json),
            onClick = {
                showNotificationSaveFileDialog(
                    defaultName = "${record.packageName}_notif.json",
                    extension = "json",
                    dialogTitle = exportDialogTitle,
                    filterDescription = exportDialogFilter,
                )?.let { selectedPath ->
                    component.onExportToJson(record, selectedPath)
                }
            },
        )
    }
}

@Composable
private fun NotificationActionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    AdbOutlinedButton(
        onClick = onClick,
        text = label,
        leadingIcon = icon,
        size = AdbButtonSize.SMALL,
        fullWidth = true,
    )
}

@Composable
internal fun NotificationDetailRow(
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Dimensions.paddingXSmall / 2),
        horizontalArrangement = Arrangement.Start,
    ) {
        Text(
            text = "$label:",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(Dimensions.sidebarWidth / 2),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(1f),
        )
    }
}

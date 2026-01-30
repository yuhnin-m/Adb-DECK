package com.adbdeck.feature.notifications.ui

import adbdeck.feature.notifications.generated.resources.Res
import adbdeck.feature.notifications.generated.resources.notifications_detail_action_delete_saved
import adbdeck.feature.notifications.generated.resources.notifications_saved_empty
import adbdeck.feature.notifications.generated.resources.notifications_saved_saved_at
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import com.adbdeck.core.designsystem.Dimensions
import com.adbdeck.core.ui.EmptyView
import com.adbdeck.core.ui.buttons.AdbButtonType
import com.adbdeck.core.ui.buttons.AdbPlainButton
import com.adbdeck.feature.notifications.NotificationsComponent
import com.adbdeck.feature.notifications.SavedNotification
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun NotificationSavedTab(
    saved: List<SavedNotification>,
    component: NotificationsComponent,
) {
    if (saved.isEmpty()) {
        EmptyView(message = stringResource(Res.string.notifications_saved_empty))
        return
    }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(saved, key = { it.id }) { item ->
            NotificationSavedRow(
                item = item,
                onSelect = { component.onSelectNotification(item.record) },
                onDelete = { component.onDeleteSaved(item.id) },
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        }
    }
}

@Composable
private fun NotificationSavedRow(
    item: SavedNotification,
    onSelect: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect)
            .padding(
                horizontal = Dimensions.paddingSmall,
                vertical = Dimensions.paddingXSmall,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.record.packageName,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontFamily = FontFamily.Monospace,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            item.record.title?.takeIf { it.isNotBlank() }?.let { title ->
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Text(
                text = stringResource(
                    Res.string.notifications_saved_saved_at,
                    formatNotificationFullTime(item.savedAt),
                ),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        AdbPlainButton(
            onClick = onDelete,
            leadingIcon = Icons.Outlined.DeleteOutline,
            type = AdbButtonType.DANGER,
            contentDescription = stringResource(Res.string.notifications_detail_action_delete_saved),
        )
    }
}

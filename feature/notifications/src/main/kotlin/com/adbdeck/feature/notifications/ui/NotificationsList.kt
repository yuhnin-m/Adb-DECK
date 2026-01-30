package com.adbdeck.feature.notifications.ui

import adbdeck.feature.notifications.generated.resources.Res
import adbdeck.feature.notifications.generated.resources.notifications_row_ongoing
import adbdeck.feature.notifications.generated.resources.notifications_row_saved_marker
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.adbdeck.core.designsystem.AdbCornerRadius
import com.adbdeck.core.adb.api.notifications.NotificationRecord
import com.adbdeck.core.designsystem.Dimensions
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun NotificationsList(
    notifications: List<NotificationRecord>,
    selectedKey: String?,
    savedKeys: Set<String>,
    onSelect: (NotificationRecord) -> Unit,
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(
            items = notifications,
            key = { it.key },
        ) { record ->
            NotificationRow(
                record = record,
                isSelected = record.key == selectedKey,
                isSaved = record.key in savedKeys,
                onClick = { onSelect(record) },
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        }
    }
}

@Composable
private fun NotificationRow(
    record: NotificationRecord,
    isSelected: Boolean,
    isSaved: Boolean,
    onClick: () -> Unit,
) {
    val backgroundColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
    } else {
        Color.Transparent
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(
                horizontal = Dimensions.paddingDefault,
                vertical = Dimensions.paddingSmall,
            ),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(Dimensions.paddingSmall),
    ) {
        ImportanceBadge(
            importance = record.importance,
            modifier = Modifier.padding(top = Dimensions.paddingXSmall / 2),
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = record.packageName,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontFamily = FontFamily.Monospace,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            record.title?.takeIf { it.isNotBlank() }?.let { title ->
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            record.text?.takeIf { it.isNotBlank() }?.let { contentText ->
                Text(
                    text = contentText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(Dimensions.paddingXSmall / 2),
        ) {
            if (record.isOngoing) {
                MiniChip(
                    text = stringResource(Res.string.notifications_row_ongoing),
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                )
            }

            if (isSaved) {
                MiniChip(
                    text = stringResource(Res.string.notifications_row_saved_marker),
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                )
            }

            record.postedAt?.let { timestamp ->
                Text(
                    text = formatNotificationShortTime(timestamp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** Цветной бейдж уровня важности уведомления. */
@Composable
private fun ImportanceBadge(
    importance: Int,
    modifier: Modifier = Modifier,
) {
    val (color, label) = when (importance) {
        5 -> MaterialTheme.colorScheme.error to "!!!"
        4 -> MaterialTheme.colorScheme.error.copy(alpha = 0.7f) to "!!"
        3 -> MaterialTheme.colorScheme.primary to "!"
        2 -> MaterialTheme.colorScheme.outline to "↓"
        1 -> MaterialTheme.colorScheme.outlineVariant to "—"
        else -> MaterialTheme.colorScheme.outlineVariant to "?"
    }

    Surface(
        modifier = modifier.size(Dimensions.iconSizeCard - (Dimensions.paddingXSmall / 2)),
        shape = RoundedCornerShape(AdbCornerRadius.SMALL.value),
        color = color.copy(alpha = 0.15f),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = color,
            )
        }
    }
}

/** Мини-чип для служебных отметок в строке списка. */
@Composable
private fun MiniChip(
    text: String,
    containerColor: Color,
) {
    Surface(shape = RoundedCornerShape(AdbCornerRadius.XLARGE.value), color = containerColor) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(
                horizontal = Dimensions.paddingSmall - (Dimensions.paddingXSmall / 2),
                vertical = Dimensions.paddingXSmall / 4,
            ),
        )
    }
}

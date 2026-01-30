package com.adbdeck.feature.notifications.ui

import adbdeck.feature.notifications.generated.resources.Res
import adbdeck.feature.notifications.generated.resources.notifications_detail_action_item
import adbdeck.feature.notifications.generated.resources.notifications_detail_action_open_deep_links
import adbdeck.feature.notifications.generated.resources.notifications_detail_actions_count
import adbdeck.feature.notifications.generated.resources.notifications_detail_actions_unavailable
import adbdeck.feature.notifications.generated.resources.notifications_detail_label_big_text
import adbdeck.feature.notifications.generated.resources.notifications_detail_label_category
import adbdeck.feature.notifications.generated.resources.notifications_detail_label_channel
import adbdeck.feature.notifications.generated.resources.notifications_detail_label_clearable
import adbdeck.feature.notifications.generated.resources.notifications_detail_label_date
import adbdeck.feature.notifications.generated.resources.notifications_detail_label_flags
import adbdeck.feature.notifications.generated.resources.notifications_detail_label_group
import adbdeck.feature.notifications.generated.resources.notifications_detail_label_id_tag
import adbdeck.feature.notifications.generated.resources.notifications_detail_label_importance
import adbdeck.feature.notifications.generated.resources.notifications_detail_label_key
import adbdeck.feature.notifications.generated.resources.notifications_detail_label_ongoing
import adbdeck.feature.notifications.generated.resources.notifications_detail_label_package
import adbdeck.feature.notifications.generated.resources.notifications_detail_label_sort_key
import adbdeck.feature.notifications.generated.resources.notifications_detail_label_sub_text
import adbdeck.feature.notifications.generated.resources.notifications_detail_label_summary
import adbdeck.feature.notifications.generated.resources.notifications_detail_label_text
import adbdeck.feature.notifications.generated.resources.notifications_detail_label_title
import adbdeck.feature.notifications.generated.resources.notifications_detail_section_actions
import adbdeck.feature.notifications.generated.resources.notifications_detail_section_component_actions
import adbdeck.feature.notifications.generated.resources.notifications_detail_section_visual
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import com.adbdeck.core.adb.api.notifications.NotificationRecord
import com.adbdeck.core.designsystem.Dimensions
import com.adbdeck.core.ui.buttons.AdbButtonSize
import com.adbdeck.core.ui.buttons.AdbPlainButton
import com.adbdeck.feature.notifications.NotificationsComponent
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun NotificationDetailsTab(
    record: NotificationRecord,
    isSaved: Boolean,
    component: NotificationsComponent,
) {
    val detectedUri = remember(record) { extractNotificationUri(record) }

    SelectionContainer {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(Dimensions.paddingSmall),
        ) {
            NotificationDetailRow(
                label = stringResource(Res.string.notifications_detail_label_package),
                value = record.packageName,
            )
            NotificationDetailRow(
                label = stringResource(Res.string.notifications_detail_label_id_tag),
                value = "${record.notificationId}${record.tag?.let { " / $it" } ?: ""}",
            )
            NotificationDetailRow(
                label = stringResource(Res.string.notifications_detail_label_key),
                value = record.key,
            )

            record.channelId?.let { value ->
                NotificationDetailRow(
                    label = stringResource(Res.string.notifications_detail_label_channel),
                    value = value,
                )
            }
            record.title?.let { value ->
                NotificationDetailRow(
                    label = stringResource(Res.string.notifications_detail_label_title),
                    value = value,
                )
            }
            record.text?.let { value ->
                NotificationDetailRow(
                    label = stringResource(Res.string.notifications_detail_label_text),
                    value = value,
                )
            }
            record.subText?.let { value ->
                NotificationDetailRow(
                    label = stringResource(Res.string.notifications_detail_label_sub_text),
                    value = value,
                )
            }
            record.bigText?.let { value ->
                NotificationDetailRow(
                    label = stringResource(Res.string.notifications_detail_label_big_text),
                    value = value,
                )
            }
            record.summaryText?.let { value ->
                NotificationDetailRow(
                    label = stringResource(Res.string.notifications_detail_label_summary),
                    value = value,
                )
            }
            record.category?.let { value ->
                NotificationDetailRow(
                    label = stringResource(Res.string.notifications_detail_label_category),
                    value = value,
                )
            }

            NotificationDetailRow(
                label = stringResource(Res.string.notifications_detail_label_importance),
                value = notificationImportanceLabel(record.importance),
            )
            NotificationDetailRow(
                label = stringResource(Res.string.notifications_detail_label_flags),
                value = "0x${record.flags.toString(16).uppercase().padStart(8, '0')}",
            )
            NotificationDetailRow(
                label = stringResource(Res.string.notifications_detail_label_ongoing),
                value = record.isOngoing.toString(),
            )
            NotificationDetailRow(
                label = stringResource(Res.string.notifications_detail_label_clearable),
                value = record.isClearable.toString(),
            )
            record.postedAt?.let { value ->
                NotificationDetailRow(
                    label = stringResource(Res.string.notifications_detail_label_date),
                    value = formatNotificationFullTime(value),
                )
            }
            record.group?.let { value ->
                NotificationDetailRow(
                    label = stringResource(Res.string.notifications_detail_label_group),
                    value = value,
                )
            }
            record.sortKey?.let { value ->
                NotificationDetailRow(
                    label = stringResource(Res.string.notifications_detail_label_sort_key),
                    value = value,
                )
            }

            if (record.actionsCount != null || record.actionTitles.isNotEmpty()) {
                HorizontalDivider(modifier = Modifier.padding(vertical = Dimensions.paddingSmall))
                Text(
                    text = stringResource(Res.string.notifications_detail_section_actions),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = Dimensions.paddingXSmall),
                )

                record.actionsCount?.let { count ->
                    NotificationDetailRow(
                        label = stringResource(Res.string.notifications_detail_actions_count),
                        value = count.toString(),
                    )
                }

                if (record.actionTitles.isNotEmpty()) {
                    record.actionTitles.forEachIndexed { index, actionTitle ->
                        NotificationDetailRow(
                            label = stringResource(
                                Res.string.notifications_detail_action_item,
                                index + 1,
                            ),
                            value = actionTitle,
                        )
                    }
                } else {
                    Text(
                        text = stringResource(Res.string.notifications_detail_actions_unavailable),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            if (record.imageParameters.isNotEmpty()) {
                HorizontalDivider(modifier = Modifier.padding(vertical = Dimensions.paddingSmall))
                Text(
                    text = stringResource(Res.string.notifications_detail_section_visual),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = Dimensions.paddingXSmall),
                )
                record.imageParameters.forEach { (key, value) ->
                    NotificationDetailRow(label = key, value = value)
                }
            }

            detectedUri?.let { uri ->
                HorizontalDivider(modifier = Modifier.padding(vertical = Dimensions.paddingSmall))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Dimensions.paddingXSmall),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Link,
                        contentDescription = null,
                        modifier = Modifier.size(Dimensions.iconSizeSmall),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = uri,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    AdbPlainButton(
                        onClick = { component.onOpenInDeepLinks(uri) },
                        text = stringResource(Res.string.notifications_detail_action_open_deep_links),
                        size = AdbButtonSize.SMALL,
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = Dimensions.paddingSmall))
            Text(
                text = stringResource(Res.string.notifications_detail_section_component_actions),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = Dimensions.paddingXSmall),
            )

            NotificationActionsGrid(
                record = record,
                isSaved = isSaved,
                component = component,
            )
        }
    }
}

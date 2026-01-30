package com.adbdeck.feature.notifications.ui

import adbdeck.feature.notifications.generated.resources.Res
import adbdeck.feature.notifications.generated.resources.notifications_detail_tab_details
import adbdeck.feature.notifications.generated.resources.notifications_detail_tab_dump
import adbdeck.feature.notifications.generated.resources.notifications_detail_tab_saved
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import com.adbdeck.core.adb.api.notifications.NotificationRecord
import com.adbdeck.core.designsystem.Dimensions
import com.adbdeck.core.i18n.AdbCommonStringRes
import com.adbdeck.core.ui.buttons.AdbButtonType
import com.adbdeck.core.ui.buttons.AdbPlainButton
import com.adbdeck.feature.notifications.NotificationsComponent
import com.adbdeck.feature.notifications.NotificationsTab
import com.adbdeck.feature.notifications.SavedNotification
import org.jetbrains.compose.resources.stringResource

/**
 * Правая панель деталей выбранного уведомления.
 */
@Composable
internal fun NotificationDetailPanel(
    modifier: Modifier,
    record: NotificationRecord,
    selectedTab: NotificationsTab,
    savedNotifications: List<SavedNotification>,
    isSaved: Boolean,
    component: NotificationsComponent,
) {
    Column(modifier = modifier.background(MaterialTheme.colorScheme.surface)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimensions.paddingSmall),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = record.packageName,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                record.title?.takeIf { it.isNotBlank() }?.let { title ->
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            AdbPlainButton(
                onClick = component::onCloseDetail,
                leadingIcon = Icons.Outlined.Close,
                type = AdbButtonType.NEUTRAL,
                contentDescription = stringResource(AdbCommonStringRes.actionClose),
            )
        }

        TabRow(selectedTabIndex = selectedTab.ordinal) {
            NotificationsTab.entries.forEach { tab ->
                Tab(
                    selected = selectedTab == tab,
                    onClick = { component.onSelectTab(tab) },
                    text = {
                        Text(
                            text = when (tab) {
                                NotificationsTab.DETAILS -> stringResource(Res.string.notifications_detail_tab_details)
                                NotificationsTab.RAW_DUMP -> stringResource(Res.string.notifications_detail_tab_dump)
                                NotificationsTab.SAVED -> stringResource(
                                    Res.string.notifications_detail_tab_saved,
                                    savedNotifications.size,
                                )
                            },
                            style = MaterialTheme.typography.labelSmall,
                        )
                    },
                )
            }
        }

        when (selectedTab) {
            NotificationsTab.DETAILS -> NotificationDetailsTab(
                record = record,
                isSaved = isSaved,
                component = component,
            )

            NotificationsTab.RAW_DUMP -> NotificationRawDumpTab(
                record = record,
                component = component,
            )

            NotificationsTab.SAVED -> NotificationSavedTab(
                saved = savedNotifications,
                component = component,
            )
        }
    }
}

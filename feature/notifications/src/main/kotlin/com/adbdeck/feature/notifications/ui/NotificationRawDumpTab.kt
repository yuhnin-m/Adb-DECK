package com.adbdeck.feature.notifications.ui

import adbdeck.feature.notifications.generated.resources.Res
import adbdeck.feature.notifications.generated.resources.notifications_detail_action_copy_dump
import adbdeck.feature.notifications.generated.resources.notifications_detail_raw_dump_unavailable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import com.adbdeck.core.adb.api.notifications.NotificationRecord
import com.adbdeck.core.designsystem.AdbCornerRadius
import com.adbdeck.core.designsystem.Dimensions
import com.adbdeck.core.ui.buttons.AdbButtonSize
import com.adbdeck.core.ui.buttons.AdbOutlinedButton
import com.adbdeck.feature.notifications.NotificationsComponent
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun NotificationRawDumpTab(
    record: NotificationRecord,
    component: NotificationsComponent,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimensions.paddingSmall),
            horizontalArrangement = Arrangement.End,
        ) {
            AdbOutlinedButton(
                onClick = { component.onCopyRawDump(record) },
                text = stringResource(Res.string.notifications_detail_action_copy_dump),
                leadingIcon = Icons.Outlined.ContentCopy,
                size = AdbButtonSize.SMALL,
            )
        }

        SelectionContainer {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(Dimensions.paddingSmall)
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(AdbCornerRadius.SMALL.value),
                    )
                    .padding(Dimensions.paddingSmall),
            ) {
                Text(
                    text = record.rawBlock.ifBlank {
                        stringResource(Res.string.notifications_detail_raw_dump_unavailable)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                )
            }
        }
    }
}

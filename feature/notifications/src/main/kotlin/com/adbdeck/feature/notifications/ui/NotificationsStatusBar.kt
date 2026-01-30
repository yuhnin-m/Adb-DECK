package com.adbdeck.feature.notifications.ui

import adbdeck.feature.notifications.generated.resources.Res
import adbdeck.feature.notifications.generated.resources.notifications_status_current
import adbdeck.feature.notifications.generated.resources.notifications_status_history
import adbdeck.feature.notifications.generated.resources.notifications_status_saved
import adbdeck.feature.notifications.generated.resources.notifications_status_shown
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import com.adbdeck.core.designsystem.Dimensions
import com.adbdeck.feature.notifications.NotificationsState
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun NotificationsStatusBar(state: NotificationsState) {
    val currentKeys = remember(state.currentNotifications) {
        state.currentNotifications.asSequence().map { it.key }.toHashSet()
    }
    val historyCount = remember(state.snapshotHistory, currentKeys) {
        state.snapshotHistory.count { historyItem -> historyItem.key !in currentKeys }
    }

    val displayedCount = state.displayedNotifications.size
    val currentCount = state.currentNotifications.size
    val savedCount = state.savedNotifications.size

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(Dimensions.statusBarHeight)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(horizontal = Dimensions.paddingDefault),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimensions.paddingSmall),
    ) {
        Text(
            text = stringResource(Res.string.notifications_status_shown, displayedCount),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        StatusBarDivider()
        Text(
            text = stringResource(Res.string.notifications_status_current, currentCount),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        StatusBarDivider()
        Text(
            text = stringResource(Res.string.notifications_status_history, historyCount),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        StatusBarDivider()
        Text(
            text = stringResource(Res.string.notifications_status_saved, savedCount),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        state.activeDeviceId?.let { deviceId ->
            StatusBarDivider()
            Text(
                text = deviceId,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontFamily = FontFamily.Monospace,
            )
        }
    }
}

@Composable
private fun StatusBarDivider() {
    Text(
        text = "│",
        color = MaterialTheme.colorScheme.outlineVariant,
    )
}

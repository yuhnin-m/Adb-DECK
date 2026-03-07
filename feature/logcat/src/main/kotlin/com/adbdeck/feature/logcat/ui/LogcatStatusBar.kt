package com.adbdeck.feature.logcat.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import adbdeck.feature.logcat.generated.resources.Res
import adbdeck.feature.logcat.generated.resources.*
import com.adbdeck.core.designsystem.Dimensions
import com.adbdeck.feature.logcat.LogcatError
import com.adbdeck.feature.logcat.LogcatState
import org.jetbrains.compose.resources.stringResource

/**
 * Строка состояния потока logcat.
 *
 * Показывает: индикатор (зеленый/серый), статус, устройство, счетчик строк, ошибку.
 */
@Composable
internal fun LogcatStatusBar(state: LogcatState) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(Dimensions.statusBarHeight)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(horizontal = Dimensions.paddingDefault),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimensions.paddingSmall),
    ) {
        // Индикатор состояния
        val dotColor = when {
            state.isRunning -> Color(0xFF4CAF50)
            state.error != null -> MaterialTheme.colorScheme.error
            else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)
        }
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(dotColor, CircleShape),
        )

        Text(
            text = when {
                state.isRunning -> stringResource(Res.string.logcat_status_streaming)
                state.error != null -> stringResource(Res.string.logcat_status_error)
                else -> stringResource(Res.string.logcat_status_stopped)
            },
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        state.activeDeviceId?.let { id ->
            Text("│", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
            Text(
                text = id,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Spacer(Modifier.weight(1f))

        if (!state.autoScroll) {
            Text(
                text = stringResource(Res.string.logcat_status_paused_indicator),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }

        if (state.entries.isNotEmpty()) {
            val hasFilter = state.levelFilter != null ||
                    state.searchQuery.isNotBlank() ||
                    state.tagFilter.isNotBlank() ||
                    state.packageFilter.isNotBlank()
            Text(
                text = if (hasFilter) {
                    stringResource(
                        Res.string.logcat_status_lines_filtered,
                        state.filteredEntries.size,
                        state.entries.size,
                    )
                } else {
                    stringResource(
                        Res.string.logcat_status_lines_total,
                        state.entries.size,
                    )
                },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (state.error != null) {
            Text(
                text = localizeLogcatError(state.error),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
        }
    }
}

/**
 * Переводит [LogcatError] в локализованную строку для отображения в UI.
 *
 * Доступна всем файлам модуля `feature/logcat`.
 */
@Composable
internal fun localizeLogcatError(error: LogcatError?): String {
    if (error == null) return ""

    return when (error) {
        LogcatError.NoActiveDevice -> stringResource(Res.string.logcat_error_no_active_device)
        is LogcatError.DeviceUnavailable -> stringResource(
            Res.string.logcat_error_device_unavailable,
            error.deviceId,
            error.deviceStateRaw,
        )
        is LogcatError.StreamFailure -> {
            val details = error.details.orEmpty().trim()
            if (details.isNotEmpty()) {
                stringResource(Res.string.logcat_error_stream_with_reason, details)
            } else {
                stringResource(Res.string.logcat_error_stream_generic)
            }
        }
    }
}

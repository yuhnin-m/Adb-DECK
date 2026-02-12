package com.adbdeck.feature.systemmonitor.ui

import adbdeck.feature.system_monitor.generated.resources.Res
import adbdeck.feature.system_monitor.generated.resources.system_monitor_list_action_kill_process
import adbdeck.feature.system_monitor.generated.resources.system_monitor_list_header_cpu
import adbdeck.feature.system_monitor.generated.resources.system_monitor_list_header_name
import adbdeck.feature.system_monitor.generated.resources.system_monitor_list_header_pid
import adbdeck.feature.system_monitor.generated.resources.system_monitor_list_header_rss
import adbdeck.feature.system_monitor.generated.resources.system_monitor_list_header_user
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.adbdeck.core.adb.api.monitoring.process.ProcessInfo
import com.adbdeck.core.adb.api.monitoring.process.ProcessState
import com.adbdeck.core.utils.formatKb
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun ProcessList(
    processes: List<ProcessInfo>,
    selectedProcess: ProcessInfo?,
    isActionRunning: Boolean,
    onSelectProcess: (ProcessInfo) -> Unit,
    onKillProcess: (ProcessInfo) -> Unit,
) {
    val listState = rememberLazyListState()

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
    ) {
        item(key = "header") {
            ProcessListHeader()
            HorizontalDivider()
        }
        items(processes, key = { it.pid }) { process ->
            ProcessRow(
                process = process,
                isSelected = process.pid == selectedProcess?.pid,
                isActionRunning = isActionRunning,
                onClick = { onSelectProcess(process) },
                onKill = { onKillProcess(process) },
            )
            HorizontalDivider(thickness = 0.5.dp)
        }
    }
}

@Composable
private fun ProcessListHeader() {
    val pidLabel = stringResource(Res.string.system_monitor_list_header_pid)
    val nameLabel = stringResource(Res.string.system_monitor_list_header_name)
    val userLabel = stringResource(Res.string.system_monitor_list_header_user)
    val cpuLabel = stringResource(Res.string.system_monitor_list_header_cpu)
    val rssLabel = stringResource(Res.string.system_monitor_list_header_rss)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(28.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(pidLabel, style = headerStyle(), modifier = Modifier.width(52.dp))
        Text(nameLabel, style = headerStyle(), modifier = Modifier.weight(1f))
        Text(userLabel, style = headerStyle(), modifier = Modifier.width(80.dp))
        Text(cpuLabel, style = headerStyle(), modifier = Modifier.width(54.dp))
        Text(rssLabel, style = headerStyle(), modifier = Modifier.width(70.dp))
        Spacer(Modifier.width(36.dp))
    }
}

@Composable
private fun headerStyle() = MaterialTheme.typography.labelSmall.copy(
    color = MaterialTheme.colorScheme.onSurfaceVariant,
)

@Composable
private fun ProcessRow(
    process: ProcessInfo,
    isSelected: Boolean,
    isActionRunning: Boolean,
    onClick: () -> Unit,
    onKill: () -> Unit,
) {
    val killProcessContentDescription = stringResource(Res.string.system_monitor_list_action_kill_process)
    val bgColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
    } else {
        Color.Transparent
    }

    val stateColor = when (process.state) {
        ProcessState.RUNNING -> MaterialTheme.colorScheme.primary
        ProcessState.ZOMBIE -> MaterialTheme.colorScheme.error
        ProcessState.STOPPED -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(36.dp)
            .background(bgColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = process.pid.toString(),
            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(52.dp),
        )
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = process.displayName,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
            ProcessStateChip(process.state, stateColor)
        }
        Text(
            text = process.user.take(12),
            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.width(80.dp),
        )
        Text(
            text = "%.1f".format(process.cpuPercent),
            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
            color = if (process.cpuPercent > 50f) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.width(54.dp),
        )
        Text(
            text = process.rssKb.formatKb(),
            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.width(70.dp),
        )
        IconButton(
            onClick = onKill,
            enabled = !isActionRunning,
            modifier = Modifier.size(28.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.Close,
                contentDescription = killProcessContentDescription,
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ProcessStateChip(state: ProcessState, color: Color) {
    if (state == ProcessState.SLEEPING || state == ProcessState.UNKNOWN) return
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = color.copy(alpha = 0.15f),
        modifier = Modifier.wrapContentSize(),
    ) {
        Text(
            text = state.symbol.toString(),
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = FontFamily.Monospace,
                color = color,
            ),
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
        )
    }
}

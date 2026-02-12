package com.adbdeck.feature.systemmonitor.ui

import adbdeck.feature.system_monitor.generated.resources.Res
import adbdeck.feature.system_monitor.generated.resources.system_monitor_sort_cpu
import adbdeck.feature.system_monitor.generated.resources.system_monitor_sort_memory
import adbdeck.feature.system_monitor.generated.resources.system_monitor_sort_name
import adbdeck.feature.system_monitor.generated.resources.system_monitor_sort_pid
import adbdeck.feature.system_monitor.generated.resources.system_monitor_toolbar_refresh_content_desc
import adbdeck.feature.system_monitor.generated.resources.system_monitor_toolbar_search_placeholder
import adbdeck.feature.system_monitor.generated.resources.system_monitor_toolbar_start_monitoring
import adbdeck.feature.system_monitor.generated.resources.system_monitor_toolbar_stop_monitoring
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.adbdeck.core.designsystem.AdbCornerRadius
import com.adbdeck.core.ui.buttons.AdbButtonSize
import com.adbdeck.core.ui.buttons.AdbButtonType
import com.adbdeck.core.ui.buttons.AdbFilledButton
import com.adbdeck.core.ui.buttons.AdbOutlinedButton
import com.adbdeck.core.ui.segmentedbuttons.AdbSegmentedButtonSize
import com.adbdeck.core.ui.segmentedbuttons.AdbSegmentedOption
import com.adbdeck.core.ui.segmentedbuttons.AdbSingleSegmentedButtons
import com.adbdeck.core.ui.textfields.AdbOutlinedTextField
import com.adbdeck.core.ui.textfields.AdbTextFieldSize
import com.adbdeck.feature.systemmonitor.processes.ProcessSortField
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun ProcessesToolbar(
    isMonitoring: Boolean,
    searchQuery: String,
    sortField: ProcessSortField,
    onStartMonitoring: () -> Unit,
    onStopMonitoring: () -> Unit,
    onRefresh: () -> Unit,
    onSearchChanged: (String) -> Unit,
    onSortFieldChanged: (ProcessSortField) -> Unit,
) {
    val stopLabel = stringResource(Res.string.system_monitor_toolbar_stop_monitoring)
    val monitorLabel = stringResource(Res.string.system_monitor_toolbar_start_monitoring)
    val refreshContentDescription = stringResource(Res.string.system_monitor_toolbar_refresh_content_desc)
    val searchPlaceholder = stringResource(Res.string.system_monitor_toolbar_search_placeholder)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = 12.dp),
    ) {
        Row(
            modifier = Modifier.align(Alignment.CenterStart),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (isMonitoring) {
                AdbFilledButton(
                    onClick = onStopMonitoring,
                    text = stopLabel,
                    leadingIcon = Icons.Outlined.Stop,
                    type = AdbButtonType.DANGER,
                    size = AdbButtonSize.MEDIUM,
                    cornerRadius = AdbCornerRadius.MEDIUM,
                )
            } else {
                AdbFilledButton(
                    onClick = onStartMonitoring,
                    text = monitorLabel,
                    leadingIcon = Icons.Outlined.PlayArrow,
                    size = AdbButtonSize.MEDIUM,
                    cornerRadius = AdbCornerRadius.MEDIUM,
                )
            }

            AdbOutlinedButton(
                onClick = onRefresh,
                text = null,
                leadingIcon = Icons.Outlined.Refresh,
                contentDescription = refreshContentDescription,
                size = AdbButtonSize.MEDIUM,
                cornerRadius = AdbCornerRadius.MEDIUM,
            )
        }

        SortButtons(
            modifier = Modifier.align(Alignment.Center),
            currentSort = sortField,
            onSortChanged = onSortFieldChanged,
        )

        AdbOutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChanged,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .width(220.dp),
            placeholder = searchPlaceholder,
            size = AdbTextFieldSize.MEDIUM,
            cornerRadius = AdbCornerRadius.MEDIUM,
            leadingIcon = Icons.Outlined.Search,
            trailingIcon = if (searchQuery.isNotEmpty()) Icons.Outlined.Clear else null,
            onTrailingIconClick = if (searchQuery.isNotEmpty()) {
                { onSearchChanged("") }
            } else {
                null
            },
            singleLine = true,
        )
    }
}

@Composable
private fun SortButtons(
    modifier: Modifier = Modifier,
    currentSort: ProcessSortField,
    onSortChanged: (ProcessSortField) -> Unit,
) {
    val cpuLabel = stringResource(Res.string.system_monitor_sort_cpu)
    val memoryLabel = stringResource(Res.string.system_monitor_sort_memory)
    val nameLabel = stringResource(Res.string.system_monitor_sort_name)
    val pidLabel = stringResource(Res.string.system_monitor_sort_pid)
    val options = remember(cpuLabel, memoryLabel, nameLabel, pidLabel) {
        listOf(
            AdbSegmentedOption(value = ProcessSortField.CPU, label = cpuLabel),
            AdbSegmentedOption(value = ProcessSortField.MEMORY, label = memoryLabel),
            AdbSegmentedOption(value = ProcessSortField.NAME, label = nameLabel),
            AdbSegmentedOption(value = ProcessSortField.PID, label = pidLabel),
        )
    }

    AdbSingleSegmentedButtons(
        options = options,
        selectedValue = currentSort,
        onValueSelected = onSortChanged,
        modifier = modifier,
        size = AdbSegmentedButtonSize.SMALL,
        cornerRadius = AdbCornerRadius.MEDIUM,
    )
}

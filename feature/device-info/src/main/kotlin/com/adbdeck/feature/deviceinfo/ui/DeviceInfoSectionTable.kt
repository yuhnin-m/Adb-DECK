package com.adbdeck.feature.deviceinfo.ui

import adbdeck.feature.device_info.generated.resources.Res
import adbdeck.feature.device_info.generated.resources.device_info_context_copy_all
import adbdeck.feature.device_info.generated.resources.device_info_context_copy_key
import adbdeck.feature.device_info.generated.resources.device_info_context_copy_section
import adbdeck.feature.device_info.generated.resources.device_info_context_copy_selected
import adbdeck.feature.device_info.generated.resources.device_info_context_copy_value
import adbdeck.feature.device_info.generated.resources.device_info_section_loading
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.adbdeck.core.designsystem.AdbTheme
import com.adbdeck.core.designsystem.Dimensions
import com.adbdeck.core.ui.sectioncards.AdbSectionCard
import com.adbdeck.feature.deviceinfo.DeviceInfoRow
import com.adbdeck.feature.deviceinfo.DeviceInfoSection
import com.adbdeck.feature.deviceinfo.DeviceInfoSectionLoadState
import org.jetbrains.compose.resources.stringResource

/**
 * Отрисовывает секцию Device Info в формате mini-table Key | Value.
 */
@Composable
internal fun DeviceInfoSectionTable(
    section: DeviceInfoSection,
    title: String,
    selectedRowIds: Set<String>,
    hasAnySelection: Boolean,
    onRowClick: (rowId: String) -> Unit,
    onCopySelected: () -> Unit,
    onCopyAll: () -> Unit,
    onCopySection: () -> Unit,
    onCopyKey: (String) -> Unit,
    onCopyValue: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    AdbSectionCard(
        title = title,
        titleUppercase = true,
        containerColor = AdbTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = AdbTheme.colorScheme.outline.copy(alpha = 0.3f),
        ),
        contentSpacing = Dimensions.paddingXSmall,
        modifier = modifier,
    ) {
        when (val loadState = section.state) {
            DeviceInfoSectionLoadState.Loading -> {
                Text(
                    text = stringResource(Res.string.device_info_section_loading),
                    style = MaterialTheme.typography.bodySmall,
                    color = AdbTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = Dimensions.paddingSmall),
                )
            }

            is DeviceInfoSectionLoadState.Error -> {
                Text(
                    text = loadState.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = AdbTheme.colorScheme.error,
                    modifier = Modifier.padding(vertical = Dimensions.paddingSmall),
                )
            }

            is DeviceInfoSectionLoadState.Success -> {
                loadState.rows.forEach { row ->
                    DeviceInfoRowItem(
                        row = row,
                        isSelected = row.id in selectedRowIds,
                        hasAnySelection = hasAnySelection,
                        onClick = { onRowClick(row.id) },
                        onCopySelected = onCopySelected,
                        onCopyAll = onCopyAll,
                        onCopySection = onCopySection,
                        onCopyKey = { onCopyKey(row.key) },
                        onCopyValue = { onCopyValue(row.value) },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun DeviceInfoRowItem(
    row: DeviceInfoRow,
    isSelected: Boolean,
    hasAnySelection: Boolean,
    onClick: () -> Unit,
    onCopySelected: () -> Unit,
    onCopyAll: () -> Unit,
    onCopySection: () -> Unit,
    onCopyKey: () -> Unit,
    onCopyValue: () -> Unit,
) {
    var isContextMenuExpanded by remember(row.id) { mutableStateOf(false) }
    val selectionColor = if (isSelected) {
        AdbTheme.colorScheme.primary.copy(alpha = 0.13f)
    } else {
        Color.Transparent
    }

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(selectionColor)
                .onPointerEvent(PointerEventType.Press) { event ->
                    if (event.buttons.isSecondaryPressed) {
                        onClick()
                        isContextMenuExpanded = true
                    }
                }
                .pointerInput(row.id) {
                    detectTapGestures(
                        onTap = { onClick() },
                        onLongPress = {
                            onClick()
                            isContextMenuExpanded = true
                        },
                    )
                }
                .padding(horizontal = Dimensions.paddingSmall, vertical = Dimensions.paddingXSmall),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimensions.paddingSmall),
        ) {
            Text(
                text = row.key,
                style = MaterialTheme.typography.bodySmall,
                color = AdbTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(0.36f),
            )
            Text(
                text = row.value,
                style = MaterialTheme.typography.bodySmall,
                color = AdbTheme.colorScheme.onSurface,
                modifier = Modifier.weight(0.64f),
            )

            DropdownMenu(
                expanded = isContextMenuExpanded,
                onDismissRequest = { isContextMenuExpanded = false },
            ) {
                DropdownMenuItem(
                    text = { Text(stringResource(Res.string.device_info_context_copy_selected)) },
                    enabled = hasAnySelection,
                    onClick = {
                        isContextMenuExpanded = false
                        onCopySelected()
                    },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(Res.string.device_info_context_copy_value)) },
                    onClick = {
                        isContextMenuExpanded = false
                        onCopyValue()
                    },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(Res.string.device_info_context_copy_key)) },
                    onClick = {
                        isContextMenuExpanded = false
                        onCopyKey()
                    },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(Res.string.device_info_context_copy_section)) },
                    onClick = {
                        isContextMenuExpanded = false
                        onCopySection()
                    },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(Res.string.device_info_context_copy_all)) },
                    onClick = {
                        isContextMenuExpanded = false
                        onCopyAll()
                    },
                )
            }
        }

        HorizontalDivider(color = AdbTheme.colorScheme.outline.copy(alpha = 0.12f))
    }
}

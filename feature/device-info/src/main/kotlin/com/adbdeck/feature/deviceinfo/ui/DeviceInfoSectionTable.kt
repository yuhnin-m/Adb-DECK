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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.DpOffset
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
                var contextMenuState by remember(section.kind) {
                    mutableStateOf<DeviceInfoContextMenuState?>(null)
                }

                loadState.rows.forEach { row ->
                    DeviceInfoRowItem(
                        row = row,
                        isSelected = row.id in selectedRowIds,
                        onClick = { onRowClick(row.id) },
                        onContextMenuRequested = { offset ->
                            contextMenuState = DeviceInfoContextMenuState(
                                row = row,
                                offset = offset,
                            )
                        },
                    )
                }

                DropdownMenu(
                    expanded = contextMenuState != null,
                    onDismissRequest = { contextMenuState = null },
                    offset = contextMenuState?.offset ?: DpOffset(0.dp, 0.dp),
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(Res.string.device_info_context_copy_selected)) },
                        enabled = hasAnySelection,
                        onClick = {
                            contextMenuState = null
                            onCopySelected()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(Res.string.device_info_context_copy_value)) },
                        onClick = {
                            val row = contextMenuState?.row ?: return@DropdownMenuItem
                            contextMenuState = null
                            onCopyValue(row.value)
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(Res.string.device_info_context_copy_key)) },
                        onClick = {
                            val row = contextMenuState?.row ?: return@DropdownMenuItem
                            contextMenuState = null
                            onCopyKey(row.key)
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(Res.string.device_info_context_copy_section)) },
                        onClick = {
                            contextMenuState = null
                            onCopySection()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(Res.string.device_info_context_copy_all)) },
                        onClick = {
                            contextMenuState = null
                            onCopyAll()
                        },
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
    onClick: () -> Unit,
    onContextMenuRequested: (DpOffset) -> Unit,
) {
    val density = LocalDensity.current
    var rowOffsetInParent by remember(row.id) { mutableStateOf(Offset.Zero) }
    val primaryColor = AdbTheme.colorScheme.primary
    val selectedColor = remember(primaryColor) {
        primaryColor.copy(alpha = 0.13f)
    }
    val selectionColor = if (isSelected) selectedColor else Color.Transparent

    fun Offset.toMenuOffset(): DpOffset = with(density) {
        DpOffset(x.toDp(), y.toDp())
    }

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(selectionColor)
                .onGloballyPositioned { coordinates ->
                    rowOffsetInParent = coordinates.positionInParent()
                }
                .onPointerEvent(PointerEventType.Press) { event ->
                    if (event.buttons.isSecondaryPressed) {
                        onClick()
                        val position = event.changes.firstOrNull()?.position ?: Offset.Zero
                        onContextMenuRequested((rowOffsetInParent + position).toMenuOffset())
                    }
                }
                .pointerInput(row.id) {
                    detectTapGestures(
                        onTap = { onClick() },
                        onLongPress = { pressOffset ->
                            onClick()
                            onContextMenuRequested((rowOffsetInParent + pressOffset).toMenuOffset())
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
        }

        HorizontalDivider(color = AdbTheme.colorScheme.outline.copy(alpha = 0.12f))
    }
}

private data class DeviceInfoContextMenuState(
    val row: DeviceInfoRow,
    val offset: DpOffset,
)

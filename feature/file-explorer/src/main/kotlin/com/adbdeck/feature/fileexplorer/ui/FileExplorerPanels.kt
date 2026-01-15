package com.adbdeck.feature.fileexplorer.ui

import adbdeck.feature.file_explorer.generated.resources.Res
import adbdeck.feature.file_explorer.generated.resources.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.DriveFileMove
import androidx.compose.material.icons.automirrored.outlined.TextSnippet
import androidx.compose.material.icons.outlined.CreateNewFolder
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.FileCopy
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.VerticalAlignTop
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.adbdeck.core.ui.EmptyView
import com.adbdeck.core.ui.LoadingView
import com.adbdeck.core.utils.formatBytes
import com.adbdeck.feature.fileexplorer.ExplorerFileItem
import com.adbdeck.feature.fileexplorer.ExplorerFileType
import com.adbdeck.feature.fileexplorer.ExplorerListState
import com.adbdeck.feature.fileexplorer.ExplorerPanelState
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun ExplorerPanel(
    title: String,
    state: ExplorerPanelState,
    deviceRoots: List<String> = emptyList(),
    isBusy: Boolean,
    onUp: () -> Unit,
    onRefresh: () -> Unit,
    onOpenDirectory: (String) -> Unit,
    onSelect: (String) -> Unit,
    onSelectRoot: ((String) -> Unit)? = null,
    onRequestCreateDirectory: () -> Unit,
    onRequestRename: () -> Unit,
    onRequestDelete: () -> Unit,
    onCopyPath: (String) -> Unit,
    onCopyError: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val selected = selectedItem(state)

    Column(modifier = modifier.fillMaxHeight()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(38.dp)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            ToolbarActionButton(
                label = stringResource(Res.string.file_explorer_action_up),
                enabled = !isBusy,
                onClick = onUp,
                icon = { Icon(Icons.Outlined.VerticalAlignTop, contentDescription = null, modifier = Modifier.size(16.dp)) },
            )
            ToolbarActionButton(
                label = stringResource(Res.string.file_explorer_action_refresh),
                enabled = !isBusy,
                onClick = onRefresh,
                icon = { Icon(Icons.Outlined.Refresh, contentDescription = null, modifier = Modifier.size(16.dp)) },
            )
            ToolbarActionButton(
                label = stringResource(Res.string.file_explorer_action_open),
                enabled = !isBusy && selected?.isDirectory == true,
                onClick = { selected?.takeIf { it.isDirectory }?.let { onOpenDirectory(it.fullPath) } },
                icon = { Icon(Icons.Outlined.FolderOpen, contentDescription = null, modifier = Modifier.size(16.dp)) },
            )
            ToolbarActionButton(
                label = stringResource(Res.string.file_explorer_action_copy_path),
                enabled = selected != null,
                onClick = { selected?.let { onCopyPath(it.fullPath) } },
                icon = { Icon(Icons.Outlined.FileCopy, contentDescription = null, modifier = Modifier.size(16.dp)) },
            )
            ToolbarActionButton(
                label = stringResource(Res.string.file_explorer_action_new_folder),
                enabled = !isBusy,
                onClick = onRequestCreateDirectory,
                icon = { Icon(Icons.Outlined.CreateNewFolder, contentDescription = null, modifier = Modifier.size(16.dp)) },
            )
            ToolbarActionButton(
                label = stringResource(Res.string.file_explorer_action_rename),
                enabled = !isBusy && selected != null,
                onClick = onRequestRename,
                icon = { Icon(Icons.AutoMirrored.Outlined.TextSnippet, contentDescription = null, modifier = Modifier.size(16.dp)) },
            )
            ToolbarActionButton(
                label = stringResource(Res.string.file_explorer_action_delete),
                enabled = !isBusy && selected != null,
                onClick = onRequestDelete,
                icon = { Icon(Icons.Outlined.Delete, contentDescription = null, modifier = Modifier.size(16.dp)) },
                danger = true,
            )
        }

        HorizontalDivider()

        SelectionContainer {
            Text(
                text = state.currentPath,
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
            )
        }

        HorizontalDivider()

        if (onSelectRoot != null && deviceRoots.isNotEmpty()) {
            DeviceRootsSelector(
                roots = deviceRoots,
                currentPath = state.currentPath,
                onSelectRoot = onSelectRoot,
            )
            HorizontalDivider()
        }

        FileListContent(
            state = state,
            onOpenDirectory = onOpenDirectory,
            onSelect = onSelect,
            onCopyError = onCopyError,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
internal fun FileListContent(
    state: ExplorerPanelState,
    onOpenDirectory: (String) -> Unit,
    onSelect: (String) -> Unit,
    onCopyError: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (val listState = state.listState) {
        is ExplorerListState.NoDevice -> EmptyView(
            message = listState.message.ifBlank {
                stringResource(Res.string.file_explorer_no_device_select_active)
            },
            modifier = modifier.fillMaxWidth(),
        )

        is ExplorerListState.Loading -> LoadingView(modifier = modifier.fillMaxWidth())

        is ExplorerListState.Empty -> EmptyView(
            message = stringResource(Res.string.file_explorer_empty_directory),
            modifier = modifier.fillMaxWidth(),
        )

        is ExplorerListState.Error -> FileErrorView(
            message = listState.message,
            onCopy = { onCopyError(listState.message) },
            modifier = modifier.fillMaxWidth(),
        )

        is ExplorerListState.Success -> {
            LazyColumn(modifier = modifier.fillMaxWidth()) {
                item(key = "header") {
                    FileListHeader()
                }
                items(listState.items, key = { it.fullPath }) { item ->
                    val isSelected = item.fullPath == state.selectedPath
                    FileRow(
                        item = item,
                        isSelected = isSelected,
                        onSelect = { onSelect(item.fullPath) },
                        onOpen = { if (item.isDirectory) onOpenDirectory(item.fullPath) },
                    )
                }
            }
        }
    }
}

@Composable
private fun FileErrorView(
    message: String,
    onCopy: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(20.dp),
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            OutlinedButton(onClick = onCopy) {
                Text(stringResource(Res.string.file_explorer_action_copy))
            }
        }
    }
}

@Composable
private fun FileListHeader() {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(Res.string.file_explorer_header_name),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.48f),
        )
        Text(
            text = stringResource(Res.string.file_explorer_header_type),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.16f),
        )
        Text(
            text = stringResource(Res.string.file_explorer_header_size),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.16f),
        )
        Text(
            text = stringResource(Res.string.file_explorer_header_modified),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.20f),
        )
    }
    HorizontalDivider()
}

@Composable
private fun FileRow(
    item: ExplorerFileItem,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onOpen: () -> Unit,
) {
    val bg = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
    } else {
        MaterialTheme.colorScheme.surface
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bg)
            .pointerInput(item.fullPath) {
                detectTapGestures(
                    onTap = { onSelect() },
                    onDoubleTap = { onOpen() },
                )
            }
            .padding(horizontal = 10.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier.weight(0.48f),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = iconForType(item.type),
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = if (item.isDirectory) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = item.name,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Text(
            text = labelForType(item.type),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.16f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        Text(
            text = item.sizeBytes?.formatBytes() ?: "—",
            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(0.16f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        Text(
            text = item.modifiedEpochMillis?.let(::formatEpochMillis) ?: "—",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.20f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun ToolbarActionButton(
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
    danger: Boolean = false,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
        colors = if (danger) {
            ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
        } else {
            ButtonDefaults.outlinedButtonColors()
        },
    ) {
        icon()
        Spacer(Modifier.width(6.dp))
        Text(text = label, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun DeviceRootsSelector(
    roots: List<String>,
    currentPath: String,
    onSelectRoot: (String) -> Unit,
) {
    val expandedState = remember(roots) { mutableStateOf(false) }
    val selectedRoot = currentRootForPath(roots = roots, currentPath = currentPath)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(Res.string.file_explorer_device_root_label),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.width(8.dp))

        Box(modifier = Modifier.weight(1f)) {
            OutlinedButton(
                onClick = { expandedState.value = true },
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
            ) {
                Text(
                    text = selectedRoot ?: stringResource(Res.string.file_explorer_device_root_select),
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
            }

            androidx.compose.material3.DropdownMenu(
                expanded = expandedState.value,
                onDismissRequest = { expandedState.value = false },
            ) {
                roots.forEach { root ->
                    androidx.compose.material3.DropdownMenuItem(
                        text = {
                            Text(
                                text = root,
                                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                            )
                        },
                        onClick = {
                            expandedState.value = false
                            onSelectRoot(root)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun labelForType(type: ExplorerFileType): String = when (type) {
    ExplorerFileType.DIRECTORY -> stringResource(Res.string.file_explorer_type_directory)
    ExplorerFileType.FILE -> stringResource(Res.string.file_explorer_type_file)
    ExplorerFileType.SYMLINK -> stringResource(Res.string.file_explorer_type_link)
    ExplorerFileType.OTHER -> stringResource(Res.string.file_explorer_type_other)
}

private fun iconForType(type: ExplorerFileType) = when (type) {
    ExplorerFileType.DIRECTORY -> Icons.Outlined.Folder
    ExplorerFileType.FILE -> Icons.Outlined.Description
    ExplorerFileType.SYMLINK -> Icons.AutoMirrored.Outlined.DriveFileMove
    ExplorerFileType.OTHER -> Icons.Outlined.Description
}

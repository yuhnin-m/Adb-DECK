package com.adbdeck.feature.fileexplorer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.DriveFileMove
import androidx.compose.material.icons.automirrored.outlined.TextSnippet
import androidx.compose.material.icons.outlined.CreateNewFolder
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.FileCopy
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.outlined.VerticalAlignTop
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.adbdeck.core.ui.AdbBanner
import com.adbdeck.core.ui.AdbBannerDismissStyle
import com.adbdeck.core.ui.AdbBannerType
import com.adbdeck.core.ui.EmptyView
import com.adbdeck.core.ui.LoadingView
import com.adbdeck.core.utils.formatBytes
import com.adbdeck.feature.fileexplorer.CreateDirectoryDialogState
import com.adbdeck.feature.fileexplorer.DeleteDialogState
import com.adbdeck.feature.fileexplorer.ExplorerFileItem
import com.adbdeck.feature.fileexplorer.ExplorerFileType
import com.adbdeck.feature.fileexplorer.ExplorerListState
import com.adbdeck.feature.fileexplorer.ExplorerPanelState
import com.adbdeck.feature.fileexplorer.ExplorerSide
import com.adbdeck.feature.fileexplorer.FileExplorerComponent
import com.adbdeck.feature.fileexplorer.FileExplorerState
import com.adbdeck.feature.fileexplorer.RenameDialogState
import com.adbdeck.feature.fileexplorer.TransferConflictDialogState
import com.adbdeck.feature.fileexplorer.TransferDirection
import com.adbdeck.feature.fileexplorer.TransferState
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Корневой экран двухпанельного File Explorer.
 */
@Composable
fun FileExplorerScreen(component: FileExplorerComponent) {
    val state by component.state.collectAsState()
    val clipboard = LocalClipboardManager.current

    Column(modifier = Modifier.fillMaxSize()) {
        Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
            ExplorerPanel(
                title = "Local File System",
                state = state.localPanel,
                isBusy = state.isActionRunning || state.transferState != null,
                onUp = component::onLocalUp,
                onRefresh = component::onRefreshLocal,
                onOpenDirectory = component::onOpenLocalDirectory,
                onSelect = component::onSelectLocal,
                onRequestCreateDirectory = { component.onRequestCreateDirectory(ExplorerSide.LOCAL) },
                onRequestRename = { component.onRequestRename(ExplorerSide.LOCAL) },
                onRequestDelete = { component.onRequestDelete(ExplorerSide.LOCAL) },
                onCopyPath = { path ->
                    clipboard.setText(AnnotatedString(path))
                    component.onPathCopied(path)
                },
                onCopyError = { error ->
                    clipboard.setText(AnnotatedString(error))
                    component.onPathCopied(error)
                },
                modifier = Modifier.weight(1f),
            )

            VerticalDivider()

            ExplorerPanel(
                title = "Device File System",
                state = state.devicePanel,
                deviceRoots = if (state.activeDeviceId != null) state.deviceRoots else emptyList(),
                isBusy = state.isActionRunning || state.transferState != null,
                onUp = component::onDeviceUp,
                onRefresh = component::onRefreshDevice,
                onOpenDirectory = component::onOpenDeviceDirectory,
                onSelect = component::onSelectDevice,
                onSelectRoot = component::onSelectDeviceRoot,
                onRequestCreateDirectory = { component.onRequestCreateDirectory(ExplorerSide.DEVICE) },
                onRequestRename = { component.onRequestRename(ExplorerSide.DEVICE) },
                onRequestDelete = { component.onRequestDelete(ExplorerSide.DEVICE) },
                onCopyPath = { path ->
                    clipboard.setText(AnnotatedString(path))
                    component.onPathCopied(path)
                },
                onCopyError = { error ->
                    clipboard.setText(AnnotatedString(error))
                    component.onPathCopied(error)
                },
                modifier = Modifier.weight(1f),
            )
        }

        HorizontalDivider()
        TransferActions(
            state = state,
            onPush = component::onPushSelected,
            onPull = component::onPullSelected,
            onCancelTransfer = component::onCancelTransfer,
            modifier = Modifier.fillMaxWidth(),
        )

        state.transferState?.let { transfer ->
            HorizontalDivider()
            TransferStatus(
                transfer = transfer,
                onCancelTransfer = component::onCancelTransfer,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        state.feedback?.let { feedback ->
            HorizontalDivider()
            AdbBanner(
                message = feedback.message,
                type = if (feedback.isError) AdbBannerType.ERROR else AdbBannerType.SUCCESS,
                onDismiss = component::onDismissFeedback,
                dismissStyle = AdbBannerDismissStyle.TEXT,
                dismissText = "OK",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 8.dp),
            )
        }
    }

    state.deleteDialog?.let {
        DeleteDialog(
            state = it,
            isRunning = state.isActionRunning,
            onConfirm = component::onConfirmDelete,
            onCancel = component::onCancelDelete,
        )
    }

    state.createDirectoryDialog?.let {
        CreateDirectoryDialog(
            state = it,
            isRunning = state.isActionRunning,
            onNameChanged = component::onCreateDirectoryNameChanged,
            onConfirm = component::onConfirmCreateDirectory,
            onCancel = component::onCancelCreateDirectory,
        )
    }

    state.renameDialog?.let {
        RenameDialog(
            state = it,
            isRunning = state.isActionRunning,
            onNameChanged = component::onRenameValueChanged,
            onConfirm = component::onConfirmRename,
            onCancel = component::onCancelRename,
        )
    }

    state.transferConflictDialog?.let {
        TransferConflictDialog(
            state = it,
            isRunning = state.isActionRunning,
            onConfirm = component::onConfirmTransferConflict,
            onCancel = component::onCancelTransferConflict,
        )
    }
}

@Composable
private fun ExplorerPanel(
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
                label = "Вверх",
                enabled = !isBusy,
                onClick = onUp,
                icon = { Icon(Icons.Outlined.VerticalAlignTop, contentDescription = null, modifier = Modifier.size(16.dp)) },
            )
            ToolbarActionButton(
                label = "Обновить",
                enabled = !isBusy,
                onClick = onRefresh,
                icon = { Icon(Icons.Outlined.Refresh, contentDescription = null, modifier = Modifier.size(16.dp)) },
            )
            ToolbarActionButton(
                label = "Открыть",
                enabled = !isBusy && selected?.isDirectory == true,
                onClick = { selected?.takeIf { it.isDirectory }?.let { onOpenDirectory(it.fullPath) } },
                icon = { Icon(Icons.Outlined.FolderOpen, contentDescription = null, modifier = Modifier.size(16.dp)) },
            )
            ToolbarActionButton(
                label = "Копировать путь",
                enabled = selected != null,
                onClick = { selected?.let { onCopyPath(it.fullPath) } },
                icon = { Icon(Icons.Outlined.FileCopy, contentDescription = null, modifier = Modifier.size(16.dp)) },
            )
            ToolbarActionButton(
                label = "Новая папка",
                enabled = !isBusy,
                onClick = onRequestCreateDirectory,
                icon = { Icon(Icons.Outlined.CreateNewFolder, contentDescription = null, modifier = Modifier.size(16.dp)) },
            )
            ToolbarActionButton(
                label = "Переименовать",
                enabled = !isBusy && selected != null,
                onClick = onRequestRename,
                icon = { Icon(Icons.AutoMirrored.Outlined.TextSnippet, contentDescription = null, modifier = Modifier.size(16.dp)) },
            )
            ToolbarActionButton(
                label = "Удалить",
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
private fun FileListContent(
    state: ExplorerPanelState,
    onOpenDirectory: (String) -> Unit,
    onSelect: (String) -> Unit,
    onCopyError: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (val listState = state.listState) {
        is ExplorerListState.NoDevice -> EmptyView(
            message = listState.message,
            modifier = modifier.fillMaxWidth(),
        )

        is ExplorerListState.Loading -> LoadingView(modifier = modifier.fillMaxWidth())

        is ExplorerListState.Empty -> EmptyView(
            message = "Папка пуста",
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
                Text("Скопировать")
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
            text = "Имя",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.48f),
        )
        Text(
            text = "Тип",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.16f),
        )
        Text(
            text = "Размер",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.16f),
        )
        Text(
            text = "Изменён",
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
    val expandedState = androidx.compose.runtime.remember(roots) { androidx.compose.runtime.mutableStateOf(false) }
    val selectedRoot = currentRootForPath(roots = roots, currentPath = currentPath)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "Раздел:",
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
                    text = selectedRoot ?: "Выбрать раздел",
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
private fun TransferActions(
    state: FileExplorerState,
    onPush: () -> Unit,
    onPull: () -> Unit,
    onCancelTransfer: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val localSelected = selectedItem(state.localPanel)
    val deviceSelected = selectedItem(state.devicePanel)
    val canPush = localSelected != null && state.activeDeviceId != null && state.transferState == null
    val canPull = deviceSelected != null && state.activeDeviceId != null && state.transferState == null

    Row(
        modifier = modifier.padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = "Transfer",
            style = MaterialTheme.typography.titleSmall,
        )
        Spacer(Modifier.weight(1f))

        Button(
            onClick = onPush,
            enabled = canPush,
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
        ) {
            Icon(Icons.AutoMirrored.Outlined.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text("Push →")
        }

        Button(
            onClick = onPull,
            enabled = canPull,
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
        ) {
            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text("← Pull")
        }

        if (state.transferState != null) {
            OutlinedButton(
                onClick = onCancelTransfer,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error,
                ),
            ) {
                Text("Отменить")
            }
        }
    }
}

@Composable
private fun TransferStatus(
    transfer: TransferState,
    onCancelTransfer: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ElevatedCard(modifier = modifier.padding(10.dp)) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.SwapHoriz, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    text = when (transfer.direction) {
                        TransferDirection.PUSH -> "Push (host → device)"
                        TransferDirection.PULL -> "Pull (device → host)"
                    },
                    style = MaterialTheme.typography.labelLarge,
                )
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onCancelTransfer) { Text("Отменить") }
            }

            Text(
                text = "Источник: ${transfer.sourcePath}",
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "Назначение: ${transfer.targetPath}",
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            if (transfer.progress == null) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            } else {
                LinearProgressIndicator(progress = { transfer.progress }, modifier = Modifier.fillMaxWidth())
            }
            Text(
                text = transfer.status,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun DeleteDialog(
    state: DeleteDialogState,
    isRunning: Boolean,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = { if (!isRunning) onCancel() },
        title = { Text("Удалить элемент?") },
        text = {
            Text("Будет удалён: ${state.item.fullPath}")
        },
        confirmButton = {
            if (isRunning) {
                CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
            } else {
                Button(onClick = onConfirm) { Text("Удалить") }
            }
        },
        dismissButton = {
            if (!isRunning) {
                OutlinedButton(onClick = onCancel) { Text("Отмена") }
            }
        },
    )
}

@Composable
private fun CreateDirectoryDialog(
    state: CreateDirectoryDialogState,
    isRunning: Boolean,
    onNameChanged: (String) -> Unit,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = { if (!isRunning) onCancel() },
        title = { Text("Новая директория") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Путь: ${state.parentPath}", style = MaterialTheme.typography.bodySmall)
                OutlinedTextField(
                    value = state.name,
                    onValueChange = onNameChanged,
                    singleLine = true,
                    enabled = !isRunning,
                    label = { Text("Имя директории") },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None),
                )
            }
        },
        confirmButton = {
            if (isRunning) {
                CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
            } else {
                Button(onClick = onConfirm) { Text("Создать") }
            }
        },
        dismissButton = {
            if (!isRunning) {
                OutlinedButton(onClick = onCancel) { Text("Отмена") }
            }
        },
    )
}

@Composable
private fun RenameDialog(
    state: RenameDialogState,
    isRunning: Boolean,
    onNameChanged: (String) -> Unit,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = { if (!isRunning) onCancel() },
        title = { Text("Переименовать") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Текущий путь: ${state.item.fullPath}", style = MaterialTheme.typography.bodySmall)
                OutlinedTextField(
                    value = state.newName,
                    onValueChange = onNameChanged,
                    singleLine = true,
                    enabled = !isRunning,
                    label = { Text("Новое имя") },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None),
                )
            }
        },
        confirmButton = {
            if (isRunning) {
                CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
            } else {
                Button(onClick = onConfirm) { Text("Сохранить") }
            }
        },
        dismissButton = {
            if (!isRunning) {
                OutlinedButton(onClick = onCancel) { Text("Отмена") }
            }
        },
    )
}

@Composable
private fun TransferConflictDialog(
    state: TransferConflictDialogState,
    isRunning: Boolean,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = { if (!isRunning) onCancel() },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.WarningAmber, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                Spacer(Modifier.width(6.dp))
                Text("Файл уже существует")
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = when (state.direction) {
                        TransferDirection.PUSH -> "На устройстве уже есть элемент с таким именем."
                        TransferDirection.PULL -> "На хосте уже есть элемент с таким именем."
                    }
                )
                Text("Источник: ${state.sourcePath}", style = MaterialTheme.typography.bodySmall)
                Text("Назначение: ${state.targetPath}", style = MaterialTheme.typography.bodySmall)
                Text("Перезаписать существующий элемент?", style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            if (isRunning) {
                CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
            } else {
                Button(onClick = onConfirm) { Text("Перезаписать") }
            }
        },
        dismissButton = {
            if (!isRunning) {
                OutlinedButton(onClick = onCancel) { Text("Отмена") }
            }
        },
    )
}

private fun selectedItem(panel: ExplorerPanelState): ExplorerFileItem? {
    val items = (panel.listState as? ExplorerListState.Success)?.items ?: return null
    return items.firstOrNull { it.fullPath == panel.selectedPath }
}

private fun currentRootForPath(roots: List<String>, currentPath: String): String? {
    val normalizedPath = currentPath.trimEnd('/').ifBlank { "/" }
    return roots
        .map { it.trimEnd('/').ifBlank { "/" } }
        .filter { root -> normalizedPath == root || normalizedPath.startsWith("$root/") }
        .maxByOrNull { it.length }
        ?: roots.firstOrNull()
}

private fun iconForType(type: ExplorerFileType) = when (type) {
    ExplorerFileType.DIRECTORY -> Icons.Outlined.Folder
    ExplorerFileType.FILE -> Icons.Outlined.Description
    ExplorerFileType.SYMLINK -> Icons.AutoMirrored.Outlined.DriveFileMove
    ExplorerFileType.OTHER -> Icons.Outlined.Description
}

private fun labelForType(type: ExplorerFileType): String = when (type) {
    ExplorerFileType.DIRECTORY -> "Dir"
    ExplorerFileType.FILE -> "File"
    ExplorerFileType.SYMLINK -> "Link"
    ExplorerFileType.OTHER -> "Other"
}

private val dateFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

private fun formatEpochMillis(epochMillis: Long): String =
    dateFormatter.format(Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()))

package com.adbdeck.feature.fileexplorer.ui

import adbdeck.feature.file_explorer.generated.resources.Res
import adbdeck.feature.file_explorer.generated.resources.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.adbdeck.core.ui.buttons.AdbButtonSize
import com.adbdeck.core.ui.buttons.AdbButtonType
import com.adbdeck.core.ui.buttons.AdbFilledButton
import com.adbdeck.core.ui.buttons.AdbOutlinedButton
import com.adbdeck.core.ui.textfields.AdbOutlinedTextField
import com.adbdeck.core.ui.textfields.AdbTextFieldSize
import com.adbdeck.core.ui.textfields.AdbTextFieldType
import com.adbdeck.feature.fileexplorer.CreateDirectoryDialogState
import com.adbdeck.feature.fileexplorer.DeleteDialogState
import com.adbdeck.feature.fileexplorer.RenameDialogState
import com.adbdeck.feature.fileexplorer.TransferConflictDialogState
import com.adbdeck.feature.fileexplorer.TransferDirection
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun DeleteDialog(
    state: DeleteDialogState,
    isRunning: Boolean,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = { if (!isRunning) onCancel() },
        title = { Text(stringResource(Res.string.file_explorer_dialog_delete_title)) },
        text = {
            Text(stringResource(Res.string.file_explorer_dialog_delete_message, state.item.fullPath))
        },
        confirmButton = {
            if (isRunning) {
                CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
            } else {
                AdbFilledButton(
                    onClick = onConfirm,
                    text = stringResource(Res.string.file_explorer_action_delete),
                    size = AdbButtonSize.MEDIUM,
                    type = AdbButtonType.DANGER,
                )
            }
        },
        dismissButton = {
            if (!isRunning) {
                AdbOutlinedButton(
                    onClick = onCancel,
                    text = stringResource(Res.string.file_explorer_action_cancel),
                    size = AdbButtonSize.MEDIUM,
                )
            }
        },
    )
}

@Composable
internal fun CreateDirectoryDialog(
    state: CreateDirectoryDialogState,
    isRunning: Boolean,
    onNameChanged: (String) -> Unit,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = { if (!isRunning) onCancel() },
        title = { Text(stringResource(Res.string.file_explorer_dialog_create_dir_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    stringResource(Res.string.file_explorer_dialog_path, state.parentPath),
                    style = MaterialTheme.typography.bodySmall,
                )
                AdbOutlinedTextField(
                    value = state.name,
                    onValueChange = onNameChanged,
                    singleLine = true,
                    enabled = !isRunning,
                    placeholder = stringResource(Res.string.file_explorer_dialog_dir_name),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None),
                    modifier = Modifier.fillMaxWidth(),
                    size = AdbTextFieldSize.MEDIUM,
                    type = AdbTextFieldType.NEUTRAL,
                )
            }
        },
        confirmButton = {
            if (isRunning) {
                CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
            } else {
                AdbFilledButton(
                    onClick = onConfirm,
                    text = stringResource(Res.string.file_explorer_action_create),
                    size = AdbButtonSize.MEDIUM,
                )
            }
        },
        dismissButton = {
            if (!isRunning) {
                AdbOutlinedButton(
                    onClick = onCancel,
                    text = stringResource(Res.string.file_explorer_action_cancel),
                    size = AdbButtonSize.MEDIUM,
                )
            }
        },
    )
}

@Composable
internal fun RenameDialog(
    state: RenameDialogState,
    isRunning: Boolean,
    onNameChanged: (String) -> Unit,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = { if (!isRunning) onCancel() },
        title = { Text(stringResource(Res.string.file_explorer_dialog_rename_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    stringResource(Res.string.file_explorer_dialog_current_path, state.item.fullPath),
                    style = MaterialTheme.typography.bodySmall,
                )
                AdbOutlinedTextField(
                    value = state.newName,
                    onValueChange = onNameChanged,
                    singleLine = true,
                    enabled = !isRunning,
                    placeholder = stringResource(Res.string.file_explorer_dialog_new_name),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None),
                    modifier = Modifier.fillMaxWidth(),
                    size = AdbTextFieldSize.MEDIUM,
                    type = AdbTextFieldType.NEUTRAL,
                )
            }
        },
        confirmButton = {
            if (isRunning) {
                CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
            } else {
                AdbFilledButton(
                    onClick = onConfirm,
                    text = stringResource(Res.string.file_explorer_action_save),
                    size = AdbButtonSize.MEDIUM,
                )
            }
        },
        dismissButton = {
            if (!isRunning) {
                AdbOutlinedButton(
                    onClick = onCancel,
                    text = stringResource(Res.string.file_explorer_action_cancel),
                    size = AdbButtonSize.MEDIUM,
                )
            }
        },
    )
}

@Composable
internal fun TransferConflictDialog(
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
                Spacer(androidx.compose.ui.Modifier.width(6.dp))
                Text(stringResource(Res.string.file_explorer_dialog_conflict_title))
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = when (state.direction) {
                        TransferDirection.PUSH -> stringResource(Res.string.file_explorer_dialog_conflict_push_message)
                        TransferDirection.PULL -> stringResource(Res.string.file_explorer_dialog_conflict_pull_message)
                    }
                )
                Text(
                    stringResource(Res.string.file_explorer_transfer_source, state.sourcePath),
                    style = MaterialTheme.typography.bodySmall,
                )
                Text(
                    stringResource(Res.string.file_explorer_transfer_target, state.targetPath),
                    style = MaterialTheme.typography.bodySmall,
                )
                Text(
                    stringResource(Res.string.file_explorer_dialog_conflict_question),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        },
        confirmButton = {
            if (isRunning) {
                CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
            } else {
                AdbFilledButton(
                    onClick = onConfirm,
                    text = stringResource(Res.string.file_explorer_action_overwrite),
                    size = AdbButtonSize.MEDIUM,
                    type = AdbButtonType.DANGER,
                )
            }
        },
        dismissButton = {
            if (!isRunning) {
                AdbOutlinedButton(
                    onClick = onCancel,
                    text = stringResource(Res.string.file_explorer_action_cancel),
                    size = AdbButtonSize.MEDIUM,
                )
            }
        },
    )
}

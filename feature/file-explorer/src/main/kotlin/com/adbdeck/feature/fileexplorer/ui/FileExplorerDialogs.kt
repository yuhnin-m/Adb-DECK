package com.adbdeck.feature.fileexplorer.ui

import adbdeck.feature.file_explorer.generated.resources.Res
import adbdeck.feature.file_explorer.generated.resources.*
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import com.adbdeck.core.designsystem.AdbTheme
import com.adbdeck.core.designsystem.Dimensions
import com.adbdeck.core.ui.alertdialogs.AdbAlertDialog
import com.adbdeck.core.ui.alertdialogs.AdbAlertDialogAction
import com.adbdeck.core.ui.buttons.AdbButtonType
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
    AdbAlertDialog(
        onDismissRequest = onCancel,
        title = stringResource(Res.string.file_explorer_dialog_delete_title),
        confirmAction = AdbAlertDialogAction(
            text = stringResource(Res.string.file_explorer_action_delete),
            onClick = onConfirm,
            type = AdbButtonType.DANGER,
            loading = isRunning,
        ),
        dismissAction = AdbAlertDialogAction(
            text = stringResource(Res.string.file_explorer_action_cancel),
            onClick = onCancel,
        ),
    ) {
        Text(stringResource(Res.string.file_explorer_dialog_delete_message, state.item.fullPath))
    }
}

@Composable
internal fun CreateDirectoryDialog(
    state: CreateDirectoryDialogState,
    isRunning: Boolean,
    onNameChanged: (String) -> Unit,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
    AdbAlertDialog(
        onDismissRequest = onCancel,
        title = stringResource(Res.string.file_explorer_dialog_create_dir_title),
        confirmAction = AdbAlertDialogAction(
            text = stringResource(Res.string.file_explorer_action_create),
            onClick = onConfirm,
            loading = isRunning,
        ),
        dismissAction = AdbAlertDialogAction(
            text = stringResource(Res.string.file_explorer_action_cancel),
            onClick = onCancel,
        ),
    ) {
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
}

@Composable
internal fun RenameDialog(
    state: RenameDialogState,
    isRunning: Boolean,
    onNameChanged: (String) -> Unit,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
    AdbAlertDialog(
        onDismissRequest = onCancel,
        title = stringResource(Res.string.file_explorer_dialog_rename_title),
        confirmAction = AdbAlertDialogAction(
            text = stringResource(Res.string.file_explorer_action_save),
            onClick = onConfirm,
            loading = isRunning,
        ),
        dismissAction = AdbAlertDialogAction(
            text = stringResource(Res.string.file_explorer_action_cancel),
            onClick = onCancel,
        ),
    ) {
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
}

@Composable
internal fun TransferConflictDialog(
    state: TransferConflictDialogState,
    isRunning: Boolean,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
    AdbAlertDialog(
        onDismissRequest = onCancel,
        title = stringResource(Res.string.file_explorer_dialog_conflict_title),
        titleIcon = Icons.Outlined.WarningAmber,
        titleIconTint = AdbTheme.colorScheme.error,
        confirmAction = AdbAlertDialogAction(
            text = stringResource(Res.string.file_explorer_action_overwrite),
            onClick = onConfirm,
            type = AdbButtonType.DANGER,
            loading = isRunning,
        ),
        dismissAction = AdbAlertDialogAction(
            text = stringResource(Res.string.file_explorer_action_cancel),
            onClick = onCancel,
        ),
        contentSpacing = Dimensions.paddingXSmall,
    ) {
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
}

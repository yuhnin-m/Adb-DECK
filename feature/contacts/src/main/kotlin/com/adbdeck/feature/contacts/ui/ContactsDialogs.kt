package com.adbdeck.feature.contacts.ui

import adbdeck.feature.contacts.generated.resources.Res
import adbdeck.feature.contacts.generated.resources.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.adbdeck.core.adb.api.contacts.Contact
import com.adbdeck.core.ui.alertdialogs.AdbAlertDialog
import com.adbdeck.core.ui.alertdialogs.AdbAlertDialogAction
import com.adbdeck.core.ui.buttons.AdbButtonType
import com.adbdeck.feature.contacts.ContactsComponent
import com.adbdeck.feature.contacts.models.ContactsOperationState
import com.adbdeck.feature.contacts.models.ContactsState
import com.adbdeck.feature.contacts.models.toDialogsState
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun ContactsDialogs(component: ContactsComponent) {
    val flow = remember(component) {
        component.state
            .map(ContactsState::toDialogsState)
            .distinctUntilChanged()
    }
    val initial = remember(component) { component.state.value.toDialogsState() }
    val state by flow.collectAsState(initial = initial)

    state.addForm?.let { form ->
        AddContactDialog(
            form = form,
            availableAccounts = state.availableAccounts,
            component = component,
        )
    }

    state.pendingDeleteContact?.let { contact ->
        ConfirmDeleteDialog(
            contact = contact,
            isActionRunning = state.isActionRunning,
            onConfirm = { component.onConfirmDelete() },
            onDismiss = { component.onCancelDelete() },
        )
    }
}

@Composable
private fun ConfirmDeleteDialog(
    contact: Contact,
    isActionRunning: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AdbAlertDialog(
        onDismissRequest = onDismiss,
        title = stringResource(Res.string.contacts_delete_title),
        hideDismissWhenBusy = false,
        confirmAction = AdbAlertDialogAction(
            text = stringResource(Res.string.contacts_delete_confirm),
            onClick = onConfirm,
            enabled = !isActionRunning,
            loading = isActionRunning,
            type = AdbButtonType.DANGER,
        ),
        dismissAction = AdbAlertDialogAction(
            text = stringResource(Res.string.contacts_delete_cancel),
            onClick = onDismiss,
            enabled = !isActionRunning,
        ),
    ) {
        Text(stringResource(Res.string.contacts_delete_message, contact.displayName))
        if (isActionRunning) {
            Text(
                text = stringResource(Res.string.contacts_delete_in_progress),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
internal fun OperationProgressDialog(
    operation: ContactsOperationState,
    onCancel: () -> Unit,
) {
    AdbAlertDialog(
        onDismissRequest = { },
        title = operation.title,
        confirmAction = AdbAlertDialogAction(
            text = stringResource(Res.string.contacts_operation_cancel),
            onClick = onCancel,
            type = AdbButtonType.DANGER,
        ),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (operation.isIndeterminate) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            } else {
                val current = operation.currentStep ?: 0
                val total = operation.totalSteps ?: 1
                LinearProgressIndicator(
                    progress = { (current.toFloat() / total.coerceAtLeast(1).toFloat()) },
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    text = "$current / $total",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Text(
                text = operation.status,
                style = MaterialTheme.typography.bodyMedium,
            )

            Surface(
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth().height(220.dp),
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    itemsIndexed(operation.logs) { _, line ->
                        Text(
                            text = line,
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }
    }
}

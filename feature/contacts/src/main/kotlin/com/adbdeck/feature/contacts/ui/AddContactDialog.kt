package com.adbdeck.feature.contacts.ui

import adbdeck.feature.contacts.generated.resources.Res
import adbdeck.feature.contacts.generated.resources.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.adbdeck.core.adb.api.contacts.ContactAccount
import com.adbdeck.core.adb.api.contacts.EmailType
import com.adbdeck.core.adb.api.contacts.PhoneType
import com.adbdeck.core.ui.alertdialogs.AdbAlertDialog
import com.adbdeck.core.ui.alertdialogs.AdbAlertDialogAction
import com.adbdeck.core.ui.textfields.AdbDropdownOption
import com.adbdeck.core.ui.textfields.AdbOutlinedDropdownTextField
import com.adbdeck.core.ui.textfields.AdbOutlinedTextField
import com.adbdeck.core.ui.textfields.AdbTextFieldSize
import com.adbdeck.feature.contacts.ContactsComponent
import com.adbdeck.feature.contacts.localizedLabel
import com.adbdeck.feature.contacts.models.AddContactFormState
import com.adbdeck.feature.contacts.models.ContactFormMode
import org.jetbrains.compose.resources.stringResource

/**
 * Диалог создания/редактирования контакта на Android-устройстве.
 *
 * Контакт сохраняется в выбранном аккаунте синхронизации или локально.
 */
@Composable
fun AddContactDialog(
    form: AddContactFormState,
    availableAccounts: List<ContactAccount>,
    component: ContactsComponent,
) {
    val isEditMode = form.mode == ContactFormMode.EDIT
    val dialogTitle = if (isEditMode) {
        stringResource(Res.string.contacts_add_title_edit)
    } else {
        stringResource(Res.string.contacts_add_title_new)
    }
    val confirmButtonText = if (isEditMode) {
        stringResource(Res.string.contacts_add_confirm_save)
    } else {
        stringResource(Res.string.contacts_add_confirm_add)
    }
    val localAccountLabel = stringResource(Res.string.contacts_account_local_label)
    val selectedAccount = availableAccounts.firstOrNull {
        it.accountName == form.accountName && it.accountType == form.accountType
    } ?: ContactAccount(form.accountName, form.accountType)
    val infoText = if (isEditMode) {
        stringResource(
            Res.string.contacts_add_info_edit,
            selectedAccount.localizedLabel(localAccountLabel),
        )
    } else {
        stringResource(
            Res.string.contacts_add_info_create,
            selectedAccount.localizedLabel(localAccountLabel),
        )
    }

    val accountOptions = remember(availableAccounts, selectedAccount, localAccountLabel) {
        val base = if (availableAccounts.isNotEmpty()) {
            availableAccounts
        } else {
            listOf(ContactAccount.local())
        }
        val resolved = if (base.any { it.stableKey == selectedAccount.stableKey }) {
            base
        } else {
            base + selectedAccount
        }
        resolved
            .distinctBy { it.stableKey }
            .map {
                AdbDropdownOption(
                    value = it,
                    label = it.localizedLabel(localAccountLabel),
                )
            }
    }
    val phoneTypeHomeLabel = stringResource(Res.string.contacts_phone_type_home)
    val phoneTypeMobileLabel = stringResource(Res.string.contacts_phone_type_mobile)
    val phoneTypeWorkLabel = stringResource(Res.string.contacts_phone_type_work)
    val phoneTypeOtherLabel = stringResource(Res.string.contacts_phone_type_other)
    val phoneTypeOptions = remember(
        phoneTypeHomeLabel,
        phoneTypeMobileLabel,
        phoneTypeWorkLabel,
        phoneTypeOtherLabel,
    ) {
        PhoneType.entries.map { type ->
            val label = when (type) {
                PhoneType.HOME -> phoneTypeHomeLabel
                PhoneType.MOBILE -> phoneTypeMobileLabel
                PhoneType.WORK -> phoneTypeWorkLabel
                PhoneType.OTHER -> phoneTypeOtherLabel
            }
            AdbDropdownOption(value = type, label = label)
        }
    }
    val emailTypeHomeLabel = stringResource(Res.string.contacts_email_type_home)
    val emailTypeWorkLabel = stringResource(Res.string.contacts_email_type_work)
    val emailTypeOtherLabel = stringResource(Res.string.contacts_email_type_other)
    val emailTypeOptions = remember(
        emailTypeHomeLabel,
        emailTypeWorkLabel,
        emailTypeOtherLabel,
    ) {
        EmailType.entries.map { type ->
            val label = when (type) {
                EmailType.HOME -> emailTypeHomeLabel
                EmailType.WORK -> emailTypeWorkLabel
                EmailType.OTHER -> emailTypeOtherLabel
            }
            AdbDropdownOption(value = type, label = label)
        }
    }

    AdbAlertDialog(
        onDismissRequest = {
            if (!form.isSubmitting) component.onDismissAddForm()
        },
        title = dialogTitle,
        hideDismissWhenBusy = false,
        confirmAction = AdbAlertDialogAction(
            text = confirmButtonText,
            onClick = { component.onSubmitAddForm() },
            enabled = !form.isSubmitting,
            loading = form.isSubmitting,
        ),
        dismissAction = AdbAlertDialogAction(
            text = stringResource(Res.string.contacts_add_cancel),
            onClick = { component.onDismissAddForm() },
            enabled = !form.isSubmitting,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(Res.string.contacts_add_account_label),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            AdbOutlinedDropdownTextField(
                options = accountOptions,
                selectedValue = selectedAccount,
                onValueSelected = { component.onAddFormAccountChanged(it) },
                enabled = !form.isSubmitting,
                size = AdbTextFieldSize.MEDIUM,
                modifier = Modifier.fillMaxWidth(),
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                AdbOutlinedTextField(
                    value = form.firstName,
                    onValueChange = { component.onAddFormFirstNameChanged(it) },
                    placeholder = stringResource(Res.string.contacts_add_placeholder_first_name),
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    enabled = !form.isSubmitting,
                    size = AdbTextFieldSize.MEDIUM,
                )
                AdbOutlinedTextField(
                    value = form.lastName,
                    onValueChange = { component.onAddFormLastNameChanged(it) },
                    placeholder = stringResource(Res.string.contacts_add_placeholder_last_name),
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    enabled = !form.isSubmitting,
                    size = AdbTextFieldSize.MEDIUM,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AdbOutlinedTextField(
                    value = form.phone1,
                    onValueChange = { component.onAddFormPhone1Changed(it) },
                    placeholder = stringResource(Res.string.contacts_add_placeholder_phone_1),
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    enabled = !form.isSubmitting,
                    size = AdbTextFieldSize.MEDIUM,
                )
                PhoneTypeDropdownField(
                    selected = form.phone1Type,
                    options = phoneTypeOptions,
                    enabled = !form.isSubmitting,
                    onSelect = { component.onAddFormPhone1TypeChanged(it) },
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AdbOutlinedTextField(
                    value = form.phone2,
                    onValueChange = { component.onAddFormPhone2Changed(it) },
                    placeholder = stringResource(Res.string.contacts_add_placeholder_phone_2),
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    enabled = !form.isSubmitting,
                    size = AdbTextFieldSize.MEDIUM,
                )
                PhoneTypeDropdownField(
                    selected = form.phone2Type,
                    options = phoneTypeOptions,
                    enabled = !form.isSubmitting,
                    onSelect = { component.onAddFormPhone2TypeChanged(it) },
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AdbOutlinedTextField(
                    value = form.email,
                    onValueChange = { component.onAddFormEmailChanged(it) },
                    placeholder = stringResource(Res.string.contacts_add_placeholder_email),
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    enabled = !form.isSubmitting,
                    size = AdbTextFieldSize.MEDIUM,
                )
                EmailTypeDropdownField(
                    selected = form.emailType,
                    options = emailTypeOptions,
                    enabled = !form.isSubmitting,
                    onSelect = { component.onAddFormEmailTypeChanged(it) },
                )
            }

            AdbOutlinedTextField(
                value = form.organization,
                onValueChange = { component.onAddFormOrganizationChanged(it) },
                placeholder = stringResource(Res.string.contacts_add_placeholder_organization),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = !form.isSubmitting,
                size = AdbTextFieldSize.MEDIUM,
            )

            AdbOutlinedTextField(
                value = form.notes,
                onValueChange = { component.onAddFormNotesChanged(it) },
                placeholder = stringResource(Res.string.contacts_add_placeholder_notes),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(84.dp),
                singleLine = false,
                enabled = !form.isSubmitting,
                size = AdbTextFieldSize.MEDIUM,
            )

            Spacer(Modifier.height(4.dp))
            Row(
                verticalAlignment = Alignment.Top,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(4.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .size(14.dp)
                        .padding(top = 1.dp),
                )
                Text(
                    text = infoText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 6.dp),
                )
            }

            form.error?.let { errorText ->
                Text(
                    text = errorText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
private fun PhoneTypeDropdownField(
    selected: PhoneType,
    options: List<AdbDropdownOption<PhoneType>>,
    enabled: Boolean,
    onSelect: (PhoneType) -> Unit,
) {
    AdbOutlinedDropdownTextField(
        options = options,
        selectedValue = selected,
        onValueSelected = onSelect,
        enabled = enabled,
        size = AdbTextFieldSize.MEDIUM,
        modifier = Modifier.width(140.dp),
    )
}

@Composable
private fun EmailTypeDropdownField(
    selected: EmailType,
    options: List<AdbDropdownOption<EmailType>>,
    enabled: Boolean,
    onSelect: (EmailType) -> Unit,
) {
    AdbOutlinedDropdownTextField(
        options = options,
        selectedValue = selected,
        onValueSelected = onSelect,
        enabled = enabled,
        size = AdbTextFieldSize.MEDIUM,
        modifier = Modifier.width(140.dp),
    )
}

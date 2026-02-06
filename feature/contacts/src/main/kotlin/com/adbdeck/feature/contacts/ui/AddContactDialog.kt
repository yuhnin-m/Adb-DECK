package com.adbdeck.feature.contacts.ui

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
import com.adbdeck.core.adb.api.contacts.EmailType.Companion.label
import com.adbdeck.core.adb.api.contacts.PhoneType
import com.adbdeck.core.adb.api.contacts.PhoneType.Companion.label
import com.adbdeck.core.ui.alertdialogs.AdbAlertDialog
import com.adbdeck.core.ui.alertdialogs.AdbAlertDialogAction
import com.adbdeck.core.ui.textfields.AdbDropdownOption
import com.adbdeck.core.ui.textfields.AdbOutlinedDropdownTextField
import com.adbdeck.core.ui.textfields.AdbOutlinedTextField
import com.adbdeck.core.ui.textfields.AdbTextFieldSize
import com.adbdeck.feature.contacts.ContactsComponent
import com.adbdeck.feature.contacts.models.AddContactFormState
import com.adbdeck.feature.contacts.models.ContactFormMode

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
    val dialogTitle = if (isEditMode) "Редактировать контакт" else "Новый контакт"
    val confirmButtonText = if (isEditMode) "Сохранить" else "Добавить"
    val selectedAccount = availableAccounts.firstOrNull {
        it.accountName == form.accountName && it.accountType == form.accountType
    } ?: ContactAccount(form.accountName, form.accountType)
    val infoText = if (isEditMode) {
        "Контакт будет сохранён в аккаунте: ${selectedAccount.uiLabel()}."
    } else {
        "Контакт будет создан в выбранном аккаунте: ${selectedAccount.uiLabel()}. " +
            "Если устройство отклонит запись, попробуйте выбрать другой аккаунт."
    }

    val accountOptions = remember(availableAccounts, selectedAccount) {
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
            .map { AdbDropdownOption(value = it, label = it.uiLabel()) }
    }
    val phoneTypeOptions = remember {
        PhoneType.entries.map { type ->
            AdbDropdownOption(value = type, label = type.label())
        }
    }
    val emailTypeOptions = remember {
        EmailType.entries.map { type ->
            AdbDropdownOption(value = type, label = type.label())
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
            text = "Отмена",
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
                text = "Аккаунт",
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
                    placeholder = "Имя",
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    enabled = !form.isSubmitting,
                    size = AdbTextFieldSize.MEDIUM,
                )
                AdbOutlinedTextField(
                    value = form.lastName,
                    onValueChange = { component.onAddFormLastNameChanged(it) },
                    placeholder = "Фамилия",
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
                    placeholder = "Телефон 1",
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
                    placeholder = "Телефон 2",
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
                    placeholder = "Email",
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
                placeholder = "Организация",
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = !form.isSubmitting,
                size = AdbTextFieldSize.MEDIUM,
            )

            AdbOutlinedTextField(
                value = form.notes,
                onValueChange = { component.onAddFormNotesChanged(it) },
                placeholder = "Заметки",
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

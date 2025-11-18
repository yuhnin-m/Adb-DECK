package com.adbdeck.feature.contacts.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.adbdeck.core.adb.api.EmailType
import com.adbdeck.core.adb.api.PhoneType
import com.adbdeck.feature.contacts.AddContactFormState
import com.adbdeck.feature.contacts.ContactsComponent

/**
 * Диалог добавления нового контакта на Android-устройство.
 *
 * Форма содержит поля: имя, фамилия, два телефона с типом,
 * email с типом, организация, заметки.
 *
 * Контакт создаётся как локальный (пустые `account_type` / `account_name`),
 * о чём пользователь предупреждается внутри диалога.
 *
 * @param form      Текущее состояние формы.
 * @param component Компонент для делегирования событий.
 */
@Composable
fun AddContactDialog(
    form: AddContactFormState,
    component: ContactsComponent,
) {
    AlertDialog(
        onDismissRequest = { if (!form.isSubmitting) component.onDismissAddForm() },
        title   = { Text("Новый контакт") },
        text    = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // ── Имя / Фамилия ─────────────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedTextField(
                        value           = form.firstName,
                        onValueChange   = { component.onAddFormFirstNameChanged(it) },
                        label           = { Text("Имя") },
                        modifier        = Modifier.weight(1f),
                        singleLine      = true,
                        enabled         = !form.isSubmitting,
                    )
                    OutlinedTextField(
                        value           = form.lastName,
                        onValueChange   = { component.onAddFormLastNameChanged(it) },
                        label           = { Text("Фамилия") },
                        modifier        = Modifier.weight(1f),
                        singleLine      = true,
                        enabled         = !form.isSubmitting,
                    )
                }

                // ── Телефон 1 ─────────────────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedTextField(
                        value         = form.phone1,
                        onValueChange = { component.onAddFormPhone1Changed(it) },
                        label         = { Text("Телефон 1") },
                        modifier      = Modifier.weight(1f),
                        singleLine    = true,
                        enabled       = !form.isSubmitting,
                    )
                    PhoneTypeDropdown(
                        selected  = form.phone1Type,
                        enabled   = !form.isSubmitting,
                        onSelect  = { component.onAddFormPhone1TypeChanged(it) },
                    )
                }

                // ── Телефон 2 ─────────────────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedTextField(
                        value         = form.phone2,
                        onValueChange = { component.onAddFormPhone2Changed(it) },
                        label         = { Text("Телефон 2") },
                        modifier      = Modifier.weight(1f),
                        singleLine    = true,
                        enabled       = !form.isSubmitting,
                    )
                    PhoneTypeDropdown(
                        selected  = form.phone2Type,
                        enabled   = !form.isSubmitting,
                        onSelect  = { component.onAddFormPhone2TypeChanged(it) },
                    )
                }

                // ── Email ─────────────────────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedTextField(
                        value         = form.email,
                        onValueChange = { component.onAddFormEmailChanged(it) },
                        label         = { Text("Email") },
                        modifier      = Modifier.weight(1f),
                        singleLine    = true,
                        enabled       = !form.isSubmitting,
                    )
                    EmailTypeDropdown(
                        selected  = form.emailType,
                        enabled   = !form.isSubmitting,
                        onSelect  = { component.onAddFormEmailTypeChanged(it) },
                    )
                }

                // ── Организация ───────────────────────────────────────────
                OutlinedTextField(
                    value         = form.organization,
                    onValueChange = { component.onAddFormOrganizationChanged(it) },
                    label         = { Text("Организация") },
                    modifier      = Modifier.fillMaxWidth(),
                    singleLine    = true,
                    enabled       = !form.isSubmitting,
                )

                // ── Заметки ───────────────────────────────────────────────
                OutlinedTextField(
                    value         = form.notes,
                    onValueChange = { component.onAddFormNotesChanged(it) },
                    label         = { Text("Заметки") },
                    modifier      = Modifier.fillMaxWidth().height(80.dp),
                    enabled       = !form.isSubmitting,
                    maxLines      = 4,
                )

                // ── Предупреждение об ограничениях ────────────────────────
                Spacer(Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.Top,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp),
                ) {
                    Icon(
                        Icons.Filled.Info,
                        contentDescription = null,
                        tint     = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp).padding(top = 1.dp),
                    )
                    Text(
                        text     = "Контакт будет создан как локальный. На некоторых устройствах, " +
                            "где обязателен облачный аккаунт, операция может завершиться ошибкой.",
                        style    = MaterialTheme.typography.bodySmall,
                        color    = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 6.dp),
                    )
                }

                // ── Сообщение об ошибке ───────────────────────────────────
                if (form.error != null) {
                    Text(
                        text  = form.error,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick  = { component.onSubmitAddForm() },
                enabled  = !form.isSubmitting,
            ) {
                if (form.isSubmitting) {
                    CircularProgressIndicator(
                        modifier    = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color       = MaterialTheme.colorScheme.onPrimary,
                    )
                    Spacer(Modifier.width(8.dp))
                }
                Text("Добавить")
            }
        },
        dismissButton = {
            TextButton(
                onClick  = { component.onDismissAddForm() },
                enabled  = !form.isSubmitting,
            ) {
                Text("Отмена")
            }
        },
    )
}

// ── Выпадающие списки типов ───────────────────────────────────────────────────

/**
 * Выпадающий список для выбора типа телефона.
 */
@Composable
private fun PhoneTypeDropdown(
    selected: PhoneType,
    enabled: Boolean,
    onSelect: (PhoneType) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        OutlinedButton(
            onClick  = { if (enabled) expanded = true },
            enabled  = enabled,
            modifier = Modifier.width(110.dp),
        ) {
            Text(
                text  = selected.label(),
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.weight(1f),
            )
            Icon(Icons.Filled.ArrowDropDown, contentDescription = null, modifier = Modifier.size(16.dp))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            PhoneType.entries.forEach { type ->
                DropdownMenuItem(
                    text    = { Text(type.label()) },
                    onClick = { onSelect(type); expanded = false },
                )
            }
        }
    }
}

/**
 * Выпадающий список для выбора типа email.
 */
@Composable
private fun EmailTypeDropdown(
    selected: EmailType,
    enabled: Boolean,
    onSelect: (EmailType) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        OutlinedButton(
            onClick  = { if (enabled) expanded = true },
            enabled  = enabled,
            modifier = Modifier.width(110.dp),
        ) {
            Text(
                text  = selected.label(),
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.weight(1f),
            )
            Icon(Icons.Filled.ArrowDropDown, contentDescription = null, modifier = Modifier.size(16.dp))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            EmailType.entries.forEach { type ->
                DropdownMenuItem(
                    text    = { Text(type.label()) },
                    onClick = { onSelect(type); expanded = false },
                )
            }
        }
    }
}

// ── Расширения для отображения типов ─────────────────────────────────────────

private fun PhoneType.label(): String = when (this) {
    PhoneType.MOBILE -> "Мобильный"
    PhoneType.HOME   -> "Домашний"
    PhoneType.WORK   -> "Рабочий"
    PhoneType.OTHER  -> "Другой"
}

private fun EmailType.label(): String = when (this) {
    EmailType.HOME  -> "Личный"
    EmailType.WORK  -> "Рабочий"
    EmailType.OTHER -> "Другой"
}

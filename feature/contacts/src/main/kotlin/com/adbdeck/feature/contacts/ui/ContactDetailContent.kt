package com.adbdeck.feature.contacts.ui

import adbdeck.feature.contacts.generated.resources.Res
import adbdeck.feature.contacts.generated.resources.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.adbdeck.core.adb.api.contacts.Contact
import com.adbdeck.core.adb.api.contacts.ContactDetails
import com.adbdeck.core.ui.buttons.AdbButtonSize
import com.adbdeck.core.ui.buttons.AdbButtonType
import com.adbdeck.core.ui.buttons.AdbFilledButton
import com.adbdeck.core.ui.buttons.AdbOutlinedButton
import com.adbdeck.core.ui.filedialogs.HostFileDialogFilter
import com.adbdeck.core.ui.filedialogs.SaveFileDialogConfig
import com.adbdeck.core.ui.filedialogs.SaveFileExtensionPolicy
import com.adbdeck.core.ui.filedialogs.showSaveFileDialog
import com.adbdeck.feature.contacts.ContactsComponent
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun DetailContent(
    details: ContactDetails,
    contact: Contact,
    component: ContactsComponent,
) {
    val localAccountLabel = stringResource(Res.string.contacts_detail_account_local)
    val jsonFileDescription = stringResource(Res.string.contacts_file_desc_json)
    val vcardFileDescription = stringResource(Res.string.contacts_file_desc_vcard)
    val saveDialogTitle = stringResource(Res.string.contacts_file_save_title)
    val fallbackName = stringResource(Res.string.contacts_detail_contact_fallback, contact.id)
    val exportBaseName = contact.displayName.ifBlank { fallbackName }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        val hasNameFields = details.firstName.isNotEmpty() ||
            details.lastName.isNotEmpty() ||
            details.middleName.isNotEmpty()
        if (hasNameFields) {
            DetailSection(title = stringResource(Res.string.contacts_detail_section_name)) {
                if (details.firstName.isNotEmpty()) {
                    DetailRow(stringResource(Res.string.contacts_detail_label_first_name), details.firstName)
                }
                if (details.lastName.isNotEmpty()) {
                    DetailRow(stringResource(Res.string.contacts_detail_label_last_name), details.lastName)
                }
                if (details.middleName.isNotEmpty()) {
                    DetailRow(stringResource(Res.string.contacts_detail_label_middle_name), details.middleName)
                }
            }
        }

        if (details.phones.isNotEmpty()) {
            DetailSection(title = stringResource(Res.string.contacts_detail_section_phones)) {
                for (phone in details.phones) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 2.dp),
                    ) {
                        Icon(
                            Icons.Filled.Phone,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            text = phone.value,
                            style = MaterialTheme.typography.bodyMedium,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier
                                .padding(start = 6.dp)
                                .weight(1f),
                        )
                        PhoneTypeBadge(phone.type)
                    }
                }
            }
        }

        if (details.emails.isNotEmpty()) {
            DetailSection(title = stringResource(Res.string.contacts_detail_section_email)) {
                for (email in details.emails) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 2.dp),
                    ) {
                        Icon(
                            Icons.Filled.Email,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            text = email.value,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier
                                .padding(start = 6.dp)
                                .weight(1f),
                        )
                        EmailTypeBadge(email.type)
                    }
                }
            }
        }

        details.organization?.let { org ->
            if (org.company.isNotEmpty() || org.title.isNotEmpty()) {
                DetailSection(title = stringResource(Res.string.contacts_detail_section_organization)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.Business,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        if (org.company.isNotEmpty()) {
                            Text(
                                text = org.company,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(start = 6.dp),
                            )
                        }
                    }
                    if (org.title.isNotEmpty()) {
                        DetailRow(stringResource(Res.string.contacts_detail_label_position), org.title)
                    }
                }
            }
        }

        if (details.addresses.isNotEmpty()) {
            DetailSection(title = stringResource(Res.string.contacts_detail_section_addresses)) {
                for (addr in details.addresses) {
                    Row(
                        verticalAlignment = Alignment.Top,
                        modifier = Modifier.padding(vertical = 2.dp),
                    ) {
                        Icon(
                            Icons.Filled.Home,
                            contentDescription = null,
                            modifier = Modifier
                                .size(14.dp)
                                .padding(top = 2.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            text = addr.formatted,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(start = 6.dp),
                        )
                    }
                }
            }
        }

        if (details.notes.isNotEmpty()) {
            DetailSection(title = stringResource(Res.string.contacts_detail_section_notes)) {
                Text(
                    text = details.notes,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        if (details.rawContacts.isNotEmpty()) {
            DetailSection(title = stringResource(Res.string.contacts_detail_section_accounts)) {
                for (raw in details.rawContacts) {
                    val accountLabel = when {
                        raw.accountName.isNotEmpty() && raw.accountType.isNotEmpty() ->
                            "${raw.accountName} (${raw.accountType})"
                        raw.accountName.isNotEmpty() -> raw.accountName
                        raw.accountType.isNotEmpty() -> raw.accountType
                        else -> localAccountLabel
                    }
                    Text(
                        text = accountLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        DetailSection(title = stringResource(Res.string.contacts_detail_section_identifiers)) {
            DetailRow(
                stringResource(Res.string.contacts_detail_label_contact_id),
                details.id.toString(),
                monospace = true,
            )
            if (details.rawContacts.isNotEmpty()) {
                DetailRow(
                    label = stringResource(Res.string.contacts_detail_label_raw_ids),
                    value = details.rawContacts.joinToString(", ") { it.rawContactId.toString() },
                    monospace = true,
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        DetailSection(title = stringResource(Res.string.contacts_detail_section_actions)) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                AdbOutlinedButton(
                    onClick = { component.onShowEditForm() },
                    text = stringResource(Res.string.contacts_detail_action_edit),
                    leadingIcon = Icons.Filled.Edit,
                    size = AdbButtonSize.SMALL,
                    fullWidth = true,
                )

                AdbOutlinedButton(
                    onClick = { component.onRefreshDetail() },
                    text = stringResource(Res.string.contacts_detail_action_refresh),
                    leadingIcon = Icons.Filled.Refresh,
                    size = AdbButtonSize.SMALL,
                    fullWidth = true,
                )

                AdbOutlinedButton(
                    onClick = {
                        val path = showContactSaveFileDialog(
                            title = saveDialogTitle,
                            defaultName = "$exportBaseName.json",
                            extension = "json",
                            filterDescription = jsonFileDescription,
                        )
                        if (path != null) {
                            component.onExportContactToJson(contact, path)
                        }
                    },
                    text = stringResource(Res.string.contacts_detail_action_export_json),
                    leadingIcon = Icons.Outlined.FileDownload,
                    size = AdbButtonSize.SMALL,
                    fullWidth = true,
                )

                AdbOutlinedButton(
                    onClick = {
                        val path = showContactSaveFileDialog(
                            title = saveDialogTitle,
                            defaultName = "$exportBaseName.vcf",
                            extension = "vcf",
                            filterDescription = vcardFileDescription,
                        )
                        if (path != null) {
                            component.onExportContactToVcf(contact, path)
                        }
                    },
                    text = stringResource(Res.string.contacts_detail_action_export_vcf),
                    leadingIcon = Icons.Outlined.FileDownload,
                    size = AdbButtonSize.SMALL,
                    fullWidth = true,
                )

                AdbFilledButton(
                    onClick = { component.onRequestDelete(contact) },
                    text = stringResource(Res.string.contacts_detail_action_delete),
                    leadingIcon = Icons.Filled.Delete,
                    type = AdbButtonType.DANGER,
                    size = AdbButtonSize.SMALL,
                    fullWidth = true,
                )
            }
        }

        Spacer(Modifier.height(16.dp))
    }
}

/**
 * Диалог сохранения файла отдельного контакта.
 *
 * Добавляет расширение [extension], если пользователь его не указал.
 */
private fun showContactSaveFileDialog(
    title: String,
    defaultName: String,
    extension: String,
    filterDescription: String,
): String? = showSaveFileDialog(
    SaveFileDialogConfig(
        title = title,
        defaultFileName = defaultName,
        filters = listOf(
            HostFileDialogFilter(
                description = "$filterDescription (*.$extension)",
                extensions = listOf(extension),
            ),
        ),
        isAcceptAllFileFilterUsed = false,
        extensionPolicy = SaveFileExtensionPolicy.AppendIfMissing(defaultExtension = extension),
    ),
)

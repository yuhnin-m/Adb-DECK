package com.adbdeck.feature.contacts.ui

import adbdeck.feature.contacts.generated.resources.Res
import adbdeck.feature.contacts.generated.resources.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Upload
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.adbdeck.core.adb.api.contacts.Contact
import com.adbdeck.core.adb.api.contacts.ContactAccount
import com.adbdeck.core.ui.buttons.AdbButtonSize
import com.adbdeck.core.ui.buttons.AdbOutlinedButton
import com.adbdeck.core.ui.filedialogs.showOpenFileDialog
import com.adbdeck.core.ui.filedialogs.showSaveFileDialog
import com.adbdeck.core.ui.menubuttons.AdbMenuButtonOption
import com.adbdeck.core.ui.menubuttons.AdbOutlinedMenuButton
import com.adbdeck.core.ui.textfields.AdbDropdownOption
import com.adbdeck.core.ui.textfields.AdbOutlinedDropdownTextField
import com.adbdeck.core.ui.textfields.AdbOutlinedTextField
import com.adbdeck.core.ui.textfields.AdbTextFieldSize
import com.adbdeck.feature.contacts.ContactsComponent
import com.adbdeck.feature.contacts.localizedLabel
import com.adbdeck.feature.contacts.models.ContactsListState
import org.jetbrains.compose.resources.stringResource

private enum class ContactsExportAction {
    ALL_JSON,
    ALL_VCF,
    SELECTED_JSON,
    SELECTED_VCF,
}

private enum class ContactsImportAction {
    JSON,
    VCF,
}

@Composable
internal fun ContactsToolbar(
    listState: ContactsListState,
    searchQuery: String,
    accounts: List<ContactAccount>,
    selectedAccount: ContactAccount,
    hasSelectedContact: Boolean,
    selectedContact: Contact?,
    component: ContactsComponent,
) {
    val isLoading = listState is ContactsListState.Loading
    val localAccountLabel = stringResource(Res.string.contacts_account_local_label)
    val exportAllJsonLabel = stringResource(Res.string.contacts_toolbar_export_all_json)
    val exportAllVcfLabel = stringResource(Res.string.contacts_toolbar_export_all_vcf)
    val exportSelectedJsonLabel = stringResource(Res.string.contacts_toolbar_export_selected_json)
    val exportSelectedVcfLabel = stringResource(Res.string.contacts_toolbar_export_selected_vcf)
    val importFromJsonLabel = stringResource(Res.string.contacts_toolbar_import_from_json)
    val importFromVcfLabel = stringResource(Res.string.contacts_toolbar_import_from_vcf)
    val jsonFileDescription = stringResource(Res.string.contacts_file_desc_json)
    val vcardFileDescription = stringResource(Res.string.contacts_file_desc_vcard)
    val pickJsonTitle = stringResource(Res.string.contacts_file_pick_json)
    val pickVcfTitle = stringResource(Res.string.contacts_file_pick_vcf)

    val accountOptions = remember(accounts, selectedAccount, localAccountLabel) {
        val base = if (accounts.isNotEmpty()) {
            accounts
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
            .map { account ->
                AdbDropdownOption(
                    value = account,
                    label = account.localizedLabel(localAccountLabel),
                )
            }
    }
    val exportOptions = remember(
        hasSelectedContact,
        selectedContact,
        exportAllJsonLabel,
        exportAllVcfLabel,
        exportSelectedJsonLabel,
        exportSelectedVcfLabel,
    ) {
        buildList {
            add(
                AdbMenuButtonOption(
                    value = ContactsExportAction.ALL_JSON,
                    label = exportAllJsonLabel,
                ),
            )
            add(
                AdbMenuButtonOption(
                    value = ContactsExportAction.ALL_VCF,
                    label = exportAllVcfLabel,
                ),
            )
            if (hasSelectedContact && selectedContact != null) {
                add(
                    AdbMenuButtonOption(
                        value = ContactsExportAction.SELECTED_JSON,
                        label = exportSelectedJsonLabel,
                    ),
                )
                add(
                    AdbMenuButtonOption(
                        value = ContactsExportAction.SELECTED_VCF,
                        label = exportSelectedVcfLabel,
                    ),
                )
            }
        }
    }
    val importOptions = remember(importFromJsonLabel, importFromVcfLabel) {
        listOf(
            AdbMenuButtonOption(
                value = ContactsImportAction.JSON,
                label = importFromJsonLabel,
            ),
            AdbMenuButtonOption(
                value = ContactsImportAction.VCF,
                label = importFromVcfLabel,
            ),
        )
    }

    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        AdbOutlinedButton(
            onClick = { component.onRefresh() },
            text = stringResource(Res.string.contacts_toolbar_refresh),
            enabled = !isLoading,
            loading = isLoading,
            leadingIcon = Icons.Filled.Refresh,
            size = AdbButtonSize.SMALL,
        )

        AdbOutlinedButton(
            onClick = { component.onShowAddForm() },
            text = stringResource(Res.string.contacts_toolbar_add),
            leadingIcon = Icons.Filled.PersonAdd,
            size = AdbButtonSize.SMALL,
        )

        AdbOutlinedDropdownTextField(
            options = accountOptions,
            selectedValue = selectedAccount,
            onValueSelected = { component.onSelectTargetAccount(it) },
            modifier = Modifier.width(240.dp),
            placeholder = stringResource(Res.string.contacts_toolbar_account_placeholder),
            leadingIcon = Icons.Filled.ManageAccounts,
            size = AdbTextFieldSize.SMALL,
            enabled = !isLoading,
        )

        AdbOutlinedMenuButton(
            text = stringResource(Res.string.contacts_toolbar_export),
            leadingIcon = Icons.Outlined.Download,
            size = AdbButtonSize.SMALL,
            options = exportOptions,
            onOptionSelected = { action ->
                when (action) {
                    ContactsExportAction.ALL_JSON -> {
                        val path = showSaveFileDialog("contacts.json", "json", jsonFileDescription)
                        if (path != null) component.onExportAllToJson(path)
                    }
                    ContactsExportAction.ALL_VCF -> {
                        val path = showSaveFileDialog("contacts.vcf", "vcf", vcardFileDescription)
                        if (path != null) component.onExportAllToVcf(path)
                    }
                    ContactsExportAction.SELECTED_JSON -> {
                        val contact = selectedContact ?: return@AdbOutlinedMenuButton
                        val path = showSaveFileDialog(
                            "${contact.displayName}.json",
                            "json",
                            jsonFileDescription,
                        )
                        if (path != null) component.onExportContactToJson(contact, path)
                    }
                    ContactsExportAction.SELECTED_VCF -> {
                        val contact = selectedContact ?: return@AdbOutlinedMenuButton
                        val path = showSaveFileDialog(
                            "${contact.displayName}.vcf",
                            "vcf",
                            vcardFileDescription,
                        )
                        if (path != null) component.onExportContactToVcf(contact, path)
                    }
                }
            },
        )

        AdbOutlinedMenuButton(
            text = stringResource(Res.string.contacts_toolbar_import),
            leadingIcon = Icons.Outlined.Upload,
            size = AdbButtonSize.SMALL,
            options = importOptions,
            onOptionSelected = { action ->
                when (action) {
                    ContactsImportAction.JSON -> {
                        val path = showOpenFileDialog(pickJsonTitle, "json")
                        if (path != null) component.onImportFromJson(path)
                    }
                    ContactsImportAction.VCF -> {
                        val path = showOpenFileDialog(pickVcfTitle, "vcf")
                        if (path != null) component.onImportFromVcf(path)
                    }
                }
            },
        )

        Spacer(Modifier.weight(1f))

        AdbOutlinedTextField(
            value = searchQuery,
            onValueChange = { component.onSearchChanged(it) },
            placeholder = stringResource(Res.string.contacts_toolbar_search_placeholder),
            modifier = Modifier.width(300.dp),
            singleLine = true,
            size = AdbTextFieldSize.SMALL,
            trailingIcon = if (searchQuery.isNotBlank()) Icons.Outlined.Close else null,
            onTrailingIconClick = if (searchQuery.isNotBlank()) {
                { component.onSearchChanged("") }
            } else {
                null
            },
        )
    }
}

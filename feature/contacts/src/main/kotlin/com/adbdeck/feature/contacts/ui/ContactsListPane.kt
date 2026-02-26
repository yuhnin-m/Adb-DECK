package com.adbdeck.feature.contacts.ui

import adbdeck.feature.contacts.generated.resources.Res
import adbdeck.feature.contacts.generated.resources.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.adbdeck.core.adb.api.contacts.Contact
import com.adbdeck.core.ui.EmptyView
import com.adbdeck.core.ui.ErrorView
import com.adbdeck.core.ui.LoadingView
import com.adbdeck.feature.contacts.models.ContactsListState
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun ContactsListPane(
    listState: ContactsListState,
    filteredContacts: List<Contact>,
    searchQuery: String,
    selectedContactId: Long?,
    onRefresh: () -> Unit,
    onSelectContact: (Contact) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Box(modifier = Modifier.fillMaxSize()) {
            when (listState) {
                is ContactsListState.NoDevice -> EmptyView(
                    message = stringResource(Res.string.contacts_list_no_active_device),
                    modifier = Modifier.fillMaxSize(),
                )
                is ContactsListState.Loading -> LoadingView(modifier = Modifier.fillMaxSize())
                is ContactsListState.Error -> ErrorView(
                    message = listState.message,
                    onRetry = onRefresh,
                    modifier = Modifier.fillMaxSize(),
                )
                is ContactsListState.Success -> {
                    if (filteredContacts.isEmpty()) {
                        EmptyView(
                            message = if (searchQuery.isBlank()) {
                                stringResource(Res.string.contacts_list_empty)
                            } else {
                                stringResource(Res.string.contacts_list_empty_search, searchQuery)
                            },
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else {
                        ContactsList(
                            contacts = filteredContacts,
                            selectedContactId = selectedContactId,
                            onSelectContact = onSelectContact,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ContactsList(
    contacts: List<Contact>,
    selectedContactId: Long?,
    onSelectContact: (Contact) -> Unit,
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(items = contacts, key = { it.id }) { contact ->
            ContactRow(
                contact = contact,
                isSelected = contact.id == selectedContactId,
                onClick = { onSelectContact(contact) },
            )
            HorizontalDivider(modifier = Modifier.padding(start = 56.dp))
        }
    }
}

@Composable
private fun ContactRow(
    contact: Contact,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val bg = if (isSelected) {
        MaterialTheme.colorScheme.secondaryContainer
    } else {
        MaterialTheme.colorScheme.surface
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bg)
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Filled.Person,
            contentDescription = null,
            modifier = Modifier.size(36.dp).padding(4.dp),
            tint = MaterialTheme.colorScheme.primary,
        )

        Column(modifier = Modifier.padding(start = 8.dp).weight(1f)) {
            Text(
                text = contact.displayName.ifEmpty {
                    stringResource(Res.string.contacts_list_name_without_name)
                },
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            if (contact.phones.isNotEmpty()) {
                Text(
                    text = contact.phones.first().value,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (contact.primaryEmail.isNotEmpty()) {
                Text(
                    text = contact.primaryEmail,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        if (contact.accountType.isNotEmpty()) {
            Text(
                text = contact.accountType,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant,
                        MaterialTheme.shapes.small,
                    )
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            )
        }
    }
}

@Composable
internal fun ContactsStatusBar(
    listState: ContactsListState,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        val statusText = when (listState) {
            is ContactsListState.NoDevice -> stringResource(Res.string.contacts_status_no_device)
            is ContactsListState.Loading -> stringResource(Res.string.contacts_status_loading)
            is ContactsListState.Error -> stringResource(Res.string.contacts_status_error)
            is ContactsListState.Success -> stringResource(
                Res.string.contacts_status_count,
                listState.contacts.size,
            )
        }
        Text(
            text = statusText,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

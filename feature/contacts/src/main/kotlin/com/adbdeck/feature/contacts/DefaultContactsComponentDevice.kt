package com.adbdeck.feature.contacts

import adbdeck.feature.contacts.generated.resources.Res
import adbdeck.feature.contacts.generated.resources.*
import com.adbdeck.core.adb.api.contacts.Contact
import com.adbdeck.core.adb.api.contacts.ContactAccount
import com.adbdeck.core.adb.api.device.DeviceState
import com.adbdeck.feature.contacts.models.ContactDetailState
import com.adbdeck.feature.contacts.models.ContactFeedback
import com.adbdeck.feature.contacts.models.ContactsListState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal fun DefaultContactsComponent.observeSelectedDevice() {
    scope.launch {
        deviceManager.selectedDeviceFlow.collect { device ->
            when {
                device == null || device.state != DeviceState.DEVICE -> {
                    val hadRunningOperation = operationJob?.isActive == true
                    loadJob?.cancel()
                    detailJob?.cancel()
                    operationJob?.cancel(
                        CancellationException(
                            contactsString(Res.string.contacts_component_operation_aborted_unavailable),
                        ),
                    )
                    activeDeviceId = null
                    _state.update {
                        it.copy(
                            listState = ContactsListState.NoDevice,
                            filteredContacts = emptyList(),
                            availableAccounts = listOf(ContactAccount.local()),
                            selectedAccount = ContactAccount.local(),
                            selectedContactId = null,
                            detailState = ContactDetailState.Idle,
                            addForm = null,
                            pendingDeleteContact = null,
                            operationState = null,
                        )
                    }
                    if (hadRunningOperation) {
                        showFeedback(
                            ContactFeedback(
                                message = contactsString(
                                    Res.string.contacts_component_operation_aborted_unavailable,
                                ),
                                isError = true,
                            ),
                        )
                    }
                }

                else -> {
                    val isDeviceChanged = activeDeviceId != device.deviceId
                    val needsReload = _state.value.listState !is ContactsListState.Success
                    if (isDeviceChanged) {
                        detailJob?.cancel()
                        if (operationJob?.isActive == true) {
                            operationJob?.cancel(
                                CancellationException(
                                    contactsString(Res.string.contacts_component_device_switched),
                                ),
                            )
                        }
                        _state.update {
                            it.copy(
                                selectedContactId = null,
                                detailState = ContactDetailState.Idle,
                                pendingDeleteContact = null,
                                operationState = null,
                            )
                        }
                    }
                    activeDeviceId = device.deviceId
                    if (isDeviceChanged || needsReload) {
                        loadContacts()
                    }
                }
            }
        }
    }
}

internal fun DefaultContactsComponent.handleRefresh() {
    val device = deviceManager.selectedDeviceFlow.value
    if (device == null || device.state != DeviceState.DEVICE) return
    loadContacts()
}

internal fun DefaultContactsComponent.handleSearchChanged(query: String) {
    _state.update { current ->
        val allContacts = (current.listState as? ContactsListState.Success)?.contacts ?: return@update current
        current.copy(
            searchQuery = query,
            filteredContacts = applySearch(allContacts, query),
        )
    }
}

internal fun DefaultContactsComponent.handleSelectTargetAccount(account: ContactAccount) {
    _state.update { current ->
        val resolved = current.availableAccounts.firstOrNull { it.stableKey == account.stableKey }
            ?: current.selectedAccount
        val updatedForm = current.addForm?.copy(
            accountName = resolved.accountName,
            accountType = resolved.accountType,
        )
        current.copy(
            selectedAccount = resolved,
            addForm = updatedForm,
        )
    }
}

internal fun DefaultContactsComponent.loadContacts() {
    loadJob?.cancel()
    loadJob = scope.launch {
        val device = deviceManager.selectedDeviceFlow.value ?: return@launch
        if (device.state != DeviceState.DEVICE) return@launch

        val requestDeviceId = device.deviceId
        val adbPath = adbPath()

        _state.update { it.copy(listState = ContactsListState.Loading) }

        val contactsResult = contactsClient.getContacts(deviceId = requestDeviceId, adbPath = adbPath)
        if (contactsResult.isFailure) {
            if (!isRequestStillValid(requestDeviceId)) return@launch
            val message = contactsResult.exceptionOrNull()?.message
                ?: contactsString(Res.string.contacts_component_error_unknown)
            _state.update {
                it.copy(
                    listState = ContactsListState.Error(message),
                    filteredContacts = emptyList(),
                    availableAccounts = listOf(ContactAccount.local()),
                    selectedAccount = ContactAccount.local(),
                )
            }
            return@launch
        }

        val contacts = contactsResult.getOrThrow()
        if (!isRequestStillValid(requestDeviceId)) return@launch

        val accountsResult = contactsClient.getAvailableAccounts(
            deviceId = requestDeviceId,
            adbPath = adbPath,
        )
        val accounts = if (accountsResult.isSuccess) {
            accountsResult.getOrThrow().ifEmpty { listOf(ContactAccount.local()) }
        } else {
            if (isRequestStillValid(requestDeviceId)) {
                showFeedback(
                    ContactFeedback(
                        message = contactsString(
                            Res.string.contacts_component_accounts_load_failed,
                            accountsResult.exceptionOrNull()?.message.orEmpty(),
                        ),
                        isError = true,
                    ),
                )
            }
            listOf(ContactAccount.local())
        }

        _state.update { current ->
            val selectedStillExists =
                current.selectedContactId != null &&
                    contacts.any { it.id == current.selectedContactId }
            val selectedAccount = selectAccountForState(
                currentSelected = current.selectedAccount,
                availableAccounts = accounts,
            )
            val updatedAddForm = current.addForm?.let { form ->
                val formAccount = accounts.firstOrNull {
                    it.accountName == form.accountName &&
                        it.accountType == form.accountType
                } ?: selectedAccount
                form.copy(
                    accountName = formAccount.accountName,
                    accountType = formAccount.accountType,
                )
            }
            current.copy(
                listState = ContactsListState.Success(contacts),
                filteredContacts = applySearch(contacts, current.searchQuery),
                availableAccounts = accounts,
                selectedAccount = selectedAccount,
                selectedContactId = if (selectedStillExists) current.selectedContactId else null,
                detailState = if (selectedStillExists) current.detailState else ContactDetailState.Idle,
                addForm = updatedAddForm,
            )
        }
    }
}

internal fun DefaultContactsComponent.handleSelectContact(contact: Contact) {
    if (_state.value.selectedContactId == contact.id &&
        _state.value.detailState is ContactDetailState.Success
    ) {
        return
    }

    detailJob?.cancel()
    _state.update {
        it.copy(
            selectedContactId = contact.id,
            detailState = ContactDetailState.Loading,
        )
    }

    detailJob = scope.launch {
        loadDetails(contact.id)
    }
}

internal fun DefaultContactsComponent.handleCloseDetail() {
    detailJob?.cancel()
    _state.update {
        it.copy(
            selectedContactId = null,
            detailState = ContactDetailState.Idle,
        )
    }
}

internal fun DefaultContactsComponent.handleRefreshDetail() {
    val contactId = _state.value.selectedContactId ?: return
    detailJob?.cancel()
    _state.update { it.copy(detailState = ContactDetailState.Loading) }
    detailJob = scope.launch { loadDetails(contactId) }
}

internal suspend fun DefaultContactsComponent.loadDetails(contactId: Long) {
    val device = deviceManager.selectedDeviceFlow.value ?: return
    if (device.state != DeviceState.DEVICE) return
    val requestDeviceId = device.deviceId
    val adbPath = adbPath()

    val result = contactsClient.getContactDetails(requestDeviceId, contactId, adbPath)
    if (!isRequestStillValid(requestDeviceId)) return
    if (_state.value.selectedContactId != contactId) return

    if (result.isSuccess) {
        _state.update { it.copy(detailState = ContactDetailState.Success(result.getOrThrow())) }
    } else {
        _state.update {
            it.copy(
                detailState = ContactDetailState.Error(
                    result.exceptionOrNull()?.message
                        ?: contactsString(Res.string.contacts_component_error_load_details),
                ),
            )
        }
    }
}

internal fun applySearch(contacts: List<Contact>, query: String): List<Contact> {
    if (query.isBlank()) return contacts
    val q = query.trim()
    return contacts.filter { c ->
        c.displayName.contains(q, ignoreCase = true) ||
            c.phones.any { it.value.contains(q) } ||
            c.primaryEmail.contains(q, ignoreCase = true)
    }
}

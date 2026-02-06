package com.adbdeck.feature.contacts

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
                    operationJob?.cancel(CancellationException("Операция прервана: устройство недоступно."))
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
                                message = "Операция прервана: устройство отключено или недоступно",
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
                                CancellationException("Операция прервана: выбрано другое устройство."),
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

        contactsClient.getContacts(deviceId = requestDeviceId, adbPath = adbPath)
            .onSuccess { contacts ->
                if (!isRequestStillValid(requestDeviceId)) return@onSuccess

                val accounts = contactsClient.getAvailableAccounts(
                    deviceId = requestDeviceId,
                    adbPath = adbPath,
                ).getOrElse { error ->
                    if (isRequestStillValid(requestDeviceId)) {
                        showFeedback(
                            ContactFeedback(
                                message = "Не удалось загрузить аккаунты контактов: ${error.message}",
                                isError = true,
                            ),
                        )
                    }
                    listOf(ContactAccount.local())
                }.ifEmpty { listOf(ContactAccount.local()) }

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
            .onFailure { error ->
                if (!isRequestStillValid(requestDeviceId)) return@onFailure
                _state.update {
                    it.copy(
                        listState = ContactsListState.Error(error.message ?: "Неизвестная ошибка"),
                        filteredContacts = emptyList(),
                        availableAccounts = listOf(ContactAccount.local()),
                        selectedAccount = ContactAccount.local(),
                    )
                }
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

    contactsClient.getContactDetails(requestDeviceId, contactId, adbPath)
        .onSuccess { details ->
            if (!isRequestStillValid(requestDeviceId)) return@onSuccess
            if (_state.value.selectedContactId != contactId) return@onSuccess
            _state.update { it.copy(detailState = ContactDetailState.Success(details)) }
        }
        .onFailure { error ->
            if (!isRequestStillValid(requestDeviceId)) return@onFailure
            if (_state.value.selectedContactId != contactId) return@onFailure
            _state.update {
                it.copy(detailState = ContactDetailState.Error(error.message ?: "Ошибка загрузки"))
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

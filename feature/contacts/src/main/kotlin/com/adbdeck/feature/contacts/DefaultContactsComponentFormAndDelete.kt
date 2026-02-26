package com.adbdeck.feature.contacts

import adbdeck.feature.contacts.generated.resources.Res
import adbdeck.feature.contacts.generated.resources.*
import com.adbdeck.core.adb.api.contacts.Contact
import com.adbdeck.core.adb.api.contacts.ContactAccount
import com.adbdeck.core.adb.api.contacts.EmailType
import com.adbdeck.core.adb.api.contacts.NewContactData
import com.adbdeck.core.adb.api.contacts.PhoneType
import com.adbdeck.feature.contacts.models.AddContactFormState
import com.adbdeck.feature.contacts.models.ContactDetailState
import com.adbdeck.feature.contacts.models.ContactFeedback
import com.adbdeck.feature.contacts.models.ContactFormMode
import com.adbdeck.feature.contacts.models.ContactsListState
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal fun DefaultContactsComponent.handleShowAddForm() {
    val selected = _state.value.selectedAccount
    _state.update {
        it.copy(
            addForm = AddContactFormState(
                accountName = selected.accountName,
                accountType = selected.accountType,
            ),
        )
    }
}

internal fun DefaultContactsComponent.handleShowEditForm() {
    val current = _state.value
    val details = (current.detailState as? ContactDetailState.Success)?.details ?: return
    val rawAccount = details.rawContacts.firstOrNull()?.let {
        ContactAccount(
            accountName = it.accountName,
            accountType = it.accountType,
        )
    }
    val selectedAccount = rawAccount ?: current.selectedAccount
    val resolvedAccount = current.availableAccounts.firstOrNull { it.stableKey == selectedAccount.stableKey }
        ?: selectedAccount
    val phone1 = details.phones.getOrNull(0)
    val phone2 = details.phones.getOrNull(1)
    val email = details.emails.getOrNull(0)
    val originalDisplayName = details.displayName.ifBlank {
        buildDisplayName(details.firstName, details.lastName)
    }

    _state.update {
        it.copy(
            addForm = AddContactFormState(
                mode = ContactFormMode.EDIT,
                editingContactId = details.id,
                originalDisplayName = originalDisplayName,
                accountName = resolvedAccount.accountName,
                accountType = resolvedAccount.accountType,
                firstName = details.firstName,
                lastName = details.lastName,
                phone1 = phone1?.value.orEmpty(),
                phone1Type = phone1?.type ?: PhoneType.MOBILE,
                phone2 = phone2?.value.orEmpty(),
                phone2Type = phone2?.type ?: PhoneType.MOBILE,
                email = email?.value.orEmpty(),
                emailType = email?.type ?: EmailType.HOME,
                organization = details.organization?.company.orEmpty(),
                notes = details.notes,
            ),
        )
    }
}

internal fun DefaultContactsComponent.handleAddFormFirstNameChanged(value: String) {
    _state.update { it.copy(addForm = it.addForm?.copy(firstName = value)) }
}

internal fun DefaultContactsComponent.handleAddFormLastNameChanged(value: String) {
    _state.update { it.copy(addForm = it.addForm?.copy(lastName = value)) }
}

internal fun DefaultContactsComponent.handleAddFormAccountChanged(account: ContactAccount) {
    _state.update {
        it.copy(
            addForm = it.addForm?.copy(
                accountName = account.accountName,
                accountType = account.accountType,
            ),
        )
    }
}

internal fun DefaultContactsComponent.handleAddFormPhone1Changed(value: String) {
    _state.update { it.copy(addForm = it.addForm?.copy(phone1 = value)) }
}

internal fun DefaultContactsComponent.handleAddFormPhone1TypeChanged(type: PhoneType) {
    _state.update { it.copy(addForm = it.addForm?.copy(phone1Type = type)) }
}

internal fun DefaultContactsComponent.handleAddFormPhone2Changed(value: String) {
    _state.update { it.copy(addForm = it.addForm?.copy(phone2 = value)) }
}

internal fun DefaultContactsComponent.handleAddFormPhone2TypeChanged(type: PhoneType) {
    _state.update { it.copy(addForm = it.addForm?.copy(phone2Type = type)) }
}

internal fun DefaultContactsComponent.handleAddFormEmailChanged(value: String) {
    _state.update { it.copy(addForm = it.addForm?.copy(email = value)) }
}

internal fun DefaultContactsComponent.handleAddFormEmailTypeChanged(type: EmailType) {
    _state.update { it.copy(addForm = it.addForm?.copy(emailType = type)) }
}

internal fun DefaultContactsComponent.handleAddFormOrganizationChanged(value: String) {
    _state.update { it.copy(addForm = it.addForm?.copy(organization = value)) }
}

internal fun DefaultContactsComponent.handleAddFormNotesChanged(value: String) {
    _state.update { it.copy(addForm = it.addForm?.copy(notes = value)) }
}

internal fun DefaultContactsComponent.handleSubmitAddForm() {
    val initialForm = _state.value.addForm ?: return
    if (initialForm.isSubmitting) return

    scope.launch {
        val form = _state.value.addForm ?: return@launch
        if (form.isSubmitting) return@launch

        if (form.mode == ContactFormMode.ADD && form.firstName.isBlank() && form.lastName.isBlank()) {
            _state.update {
                it.copy(
                    addForm = form.copy(
                        error = contactsString(Res.string.contacts_component_name_required),
                    ),
                )
            }
            return@launch
        }

        val firstName = form.firstName.trim()
        val lastName = form.lastName.trim()
        val displayName = buildDisplayName(firstName, lastName)
        val normalizedDisplayName = displayName.ifBlank {
            form.originalDisplayName.ifBlank {
                contactsString(Res.string.contacts_component_name_untitled)
            }
        }

        val contact = NewContactData(
            firstName = firstName,
            lastName = lastName,
            displayName = normalizedDisplayName,
            phone1 = form.phone1.trim(),
            phone1Type = form.phone1Type,
            phone2 = form.phone2.trim(),
            phone2Type = form.phone2Type,
            email = form.email.trim(),
            emailType = form.emailType,
            organization = form.organization.trim(),
            notes = form.notes.trim(),
            accountName = form.accountName,
            accountType = form.accountType,
        )

        _state.update { it.copy(addForm = form.copy(isSubmitting = true, error = null)) }

        val (deviceId, adbPath) = requireDeviceAndPath() ?: run {
            _state.update {
                it.copy(
                    addForm = it.addForm?.copy(
                        isSubmitting = false,
                        error = contactsString(Res.string.contacts_component_device_unavailable),
                    ),
                )
            }
            return@launch
        }

        when (form.mode) {
            ContactFormMode.ADD -> {
                val addResult = contactsClient.addContact(deviceId, contact, adbPath)
                if (!isRequestStillValid(deviceId)) {
                    _state.update {
                        it.copy(
                            addForm = it.addForm?.copy(
                                isSubmitting = false,
                                error = contactsString(Res.string.contacts_component_device_disconnected),
                            ),
                        )
                    }
                    return@launch
                }

                if (addResult.isSuccess) {
                    _state.update { it.copy(addForm = null) }
                    showFeedback(
                        ContactFeedback(
                            contactsString(
                                Res.string.contacts_component_feedback_contact_added,
                                normalizedDisplayName,
                            ),
                            isError = false,
                        ),
                    )
                    loadContacts()
                } else {
                    _state.update {
                        it.copy(
                            addForm = it.addForm?.copy(
                                isSubmitting = false,
                                error = addResult.exceptionOrNull()?.message
                                    ?: contactsString(Res.string.contacts_component_form_error_create),
                            ),
                        )
                    }
                }
            }

            ContactFormMode.EDIT -> {
                val editingContactId = form.editingContactId
                if (editingContactId == null) {
                    _state.update {
                        it.copy(
                            addForm = it.addForm?.copy(
                                isSubmitting = false,
                                error = contactsString(Res.string.contacts_component_form_error_edit_id_missing),
                            ),
                        )
                    }
                    return@launch
                }

                val updateResult = contactsClient.updateContact(
                    deviceId = deviceId,
                    contactId = editingContactId,
                    contact = contact,
                    adbPath = adbPath,
                )

                if (!isRequestStillValid(deviceId)) {
                    _state.update {
                        it.copy(
                            addForm = it.addForm?.copy(
                                isSubmitting = false,
                                error = contactsString(Res.string.contacts_component_device_disconnected),
                            ),
                        )
                    }
                    return@launch
                }

                if (updateResult.isSuccess) {
                    _state.update {
                        it.copy(
                            addForm = null,
                            selectedContactId = null,
                            detailState = ContactDetailState.Idle,
                        )
                    }
                    showFeedback(
                        ContactFeedback(
                            contactsString(
                                Res.string.contacts_component_feedback_contact_updated,
                                normalizedDisplayName,
                            ),
                            isError = false,
                        ),
                    )
                    loadContacts()
                } else {
                    _state.update {
                        it.copy(
                            addForm = it.addForm?.copy(
                                isSubmitting = false,
                                error = updateResult.exceptionOrNull()?.message
                                    ?: contactsString(Res.string.contacts_component_form_error_update),
                            ),
                        )
                    }
                }
            }
        }
    }
}

internal fun DefaultContactsComponent.handleDismissAddForm() {
    _state.update { it.copy(addForm = null) }
}

internal fun DefaultContactsComponent.handleRequestDelete(contact: Contact) {
    _state.update { it.copy(pendingDeleteContact = contact) }
}

internal fun DefaultContactsComponent.handleConfirmDelete() {
    val contact = _state.value.pendingDeleteContact ?: return
    _state.update { it.copy(pendingDeleteContact = null) }

    scope.launch {
        _state.update { it.copy(isActionRunning = true) }
        try {
            val (deviceId, adbPath) = requireDeviceAndPath() ?: return@launch

            val deleteResult = contactsClient.deleteContact(deviceId, contact.id, adbPath)
            if (!isRequestStillValid(deviceId)) return@launch

            if (deleteResult.isSuccess) {
                _state.update { current ->
                    val allContacts = (current.listState as? ContactsListState.Success)
                        ?.contacts?.filter { it.id != contact.id }
                        ?: return@update current
                    current.copy(
                        listState = ContactsListState.Success(allContacts),
                        filteredContacts = applySearch(allContacts, current.searchQuery),
                        selectedContactId = if (current.selectedContactId == contact.id) null else current.selectedContactId,
                        detailState = if (current.selectedContactId == contact.id) ContactDetailState.Idle else current.detailState,
                    )
                }
                val displayName = contact.displayName.ifBlank {
                    contactsString(Res.string.contacts_component_name_untitled)
                }
                showFeedback(
                    ContactFeedback(
                        contactsString(
                            Res.string.contacts_component_feedback_contact_deleted,
                            displayName,
                        ),
                        isError = false,
                    ),
                )
            } else {
                showFeedback(
                    ContactFeedback(
                        deleteResult.exceptionOrNull()?.message
                            ?: contactsString(Res.string.contacts_component_form_error_delete),
                        isError = true,
                    ),
                )
            }
        } finally {
            _state.update { it.copy(isActionRunning = false) }
        }
    }
}

internal fun DefaultContactsComponent.handleCancelDelete() {
    _state.update { it.copy(pendingDeleteContact = null) }
}

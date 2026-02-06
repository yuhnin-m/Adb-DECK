package com.adbdeck.feature.contacts.models

import com.adbdeck.core.adb.api.contacts.Contact
import com.adbdeck.core.adb.api.contacts.ContactAccount

internal data class ContactsMainContentState(
    val showSafetyBanner: Boolean,
    val listState: ContactsListState,
    val filteredContacts: List<Contact>,
    val searchQuery: String,
    val availableAccounts: List<ContactAccount>,
    val selectedAccount: ContactAccount,
    val selectedContactId: Long?,
    val detailState: ContactDetailState,
)

internal data class ContactsOverlayState(
    val actionFeedback: ContactFeedback?,
    val operationState: ContactsOperationState?,
)

internal data class ContactsDialogsState(
    val addForm: AddContactFormState?,
    val availableAccounts: List<ContactAccount>,
    val pendingDeleteContact: Contact?,
    val isActionRunning: Boolean,
)

internal fun ContactsState.toMainContentState(): ContactsMainContentState {
    return ContactsMainContentState(
        showSafetyBanner = showSafetyBanner,
        listState = listState,
        filteredContacts = filteredContacts,
        searchQuery = searchQuery,
        availableAccounts = availableAccounts,
        selectedAccount = selectedAccount,
        selectedContactId = selectedContactId,
        detailState = detailState,
    )
}

internal fun ContactsState.toOverlayState(): ContactsOverlayState = ContactsOverlayState(
    actionFeedback = actionFeedback,
    operationState = operationState,
)

internal fun ContactsState.toDialogsState(): ContactsDialogsState = ContactsDialogsState(
    addForm = addForm,
    availableAccounts = availableAccounts,
    pendingDeleteContact = pendingDeleteContact,
    isActionRunning = isActionRunning,
)

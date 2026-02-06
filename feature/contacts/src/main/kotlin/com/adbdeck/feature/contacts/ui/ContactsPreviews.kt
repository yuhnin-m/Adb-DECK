package com.adbdeck.feature.contacts.ui

import org.jetbrains.compose.ui.tooling.preview.Preview
import androidx.compose.runtime.Composable
import com.adbdeck.core.adb.api.contacts.Contact
import com.adbdeck.core.adb.api.contacts.ContactAccount
import com.adbdeck.core.adb.api.contacts.ContactDetails
import com.adbdeck.core.adb.api.contacts.ContactEmail
import com.adbdeck.core.adb.api.contacts.ContactOrganization
import com.adbdeck.core.adb.api.contacts.ContactPhone
import com.adbdeck.core.adb.api.contacts.EmailType
import com.adbdeck.core.adb.api.contacts.PhoneType
import com.adbdeck.core.adb.api.contacts.RawContactInfo
import com.adbdeck.core.designsystem.AdbDeckTheme
import com.adbdeck.feature.contacts.ContactsComponent
import com.adbdeck.feature.contacts.models.AddContactFormState
import com.adbdeck.feature.contacts.models.ContactDetailState
import com.adbdeck.feature.contacts.models.ContactFeedback
import com.adbdeck.feature.contacts.models.ContactsListState
import com.adbdeck.feature.contacts.models.ContactsState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

// ── Тестовые данные ──────────────────────────────────────────────────────────

private val previewContacts = listOf(
    Contact(
        id           = 1L,
        displayName  = "Иван Иванов",
        phones       = listOf(ContactPhone("+79001234567", PhoneType.MOBILE)),
        primaryEmail = "ivan@example.com",
        accountType  = "com.google",
    ),
    Contact(
        id           = 2L,
        displayName  = "Мария Петрова",
        phones       = listOf(ContactPhone("+79007654321", PhoneType.HOME)),
        primaryEmail = "maria@example.com",
    ),
    Contact(
        id           = 3L,
        displayName  = "Алексей Сидоров",
        phones       = listOf(ContactPhone("+79009998877", PhoneType.WORK)),
        primaryEmail = "",
    ),
)

private val previewDetails = ContactDetails(
    id           = 1L,
    displayName  = "Иван Иванов",
    firstName    = "Иван",
    lastName     = "Иванов",
    middleName   = "Петрович",
    phones       = listOf(
        ContactPhone("+79001234567", PhoneType.MOBILE),
        ContactPhone("+74951234567", PhoneType.WORK),
    ),
    emails       = listOf(
        ContactEmail("ivan@example.com", EmailType.HOME),
        ContactEmail("ivan@company.com", EmailType.WORK),
    ),
    organization = ContactOrganization("Рога и Копыта", "Инженер"),
    notes        = "Коллега из проекта ADB Deck",
    rawContacts  = listOf(
        RawContactInfo(rawContactId = 10L, accountName = "ivan@gmail.com", accountType = "com.google"),
    ),
)

// ── Preview-компонент ─────────────────────────────────────────────────────────

/**
 * Заглушка компонента контактов для @Preview-функций.
 */
private class PreviewContactsComponent(
    override val state: StateFlow<ContactsState>,
) : ContactsComponent {
    override fun onRefresh()                             = Unit
    override fun onSearchChanged(query: String)          = Unit
    override fun onSelectTargetAccount(account: ContactAccount) = Unit
    override fun onSelectContact(contact: Contact)       = Unit
    override fun onCloseDetail()                         = Unit
    override fun onRefreshDetail()                       = Unit
    override fun onShowAddForm()                         = Unit
    override fun onShowEditForm()                        = Unit
    override fun onAddFormFirstNameChanged(value: String)  = Unit
    override fun onAddFormLastNameChanged(value: String)   = Unit
    override fun onAddFormAccountChanged(account: ContactAccount) = Unit
    override fun onAddFormPhone1Changed(value: String)     = Unit
    override fun onAddFormPhone1TypeChanged(type: PhoneType) = Unit
    override fun onAddFormPhone2Changed(value: String)     = Unit
    override fun onAddFormPhone2TypeChanged(type: PhoneType) = Unit
    override fun onAddFormEmailChanged(value: String)      = Unit
    override fun onAddFormEmailTypeChanged(type: EmailType)  = Unit
    override fun onAddFormOrganizationChanged(value: String) = Unit
    override fun onAddFormNotesChanged(value: String)      = Unit
    override fun onSubmitAddForm()                         = Unit
    override fun onDismissAddForm()                        = Unit
    override fun onRequestDelete(contact: Contact)         = Unit
    override fun onConfirmDelete()                         = Unit
    override fun onCancelDelete()                          = Unit
    override fun onExportAllToJson(path: String)           = Unit
    override fun onExportAllToVcf(path: String)            = Unit
    override fun onExportContactToJson(contact: Contact, path: String) = Unit
    override fun onExportContactToVcf(contact: Contact, path: String)  = Unit
    override fun onImportFromJson(path: String)            = Unit
    override fun onImportFromVcf(path: String)             = Unit
    override fun onCancelOperation()                       = Unit
    override fun onDismissSafetyBanner()                   = Unit
    override fun onDismissFeedback()                       = Unit
}

// ── @Preview ─────────────────────────────────────────────────────────────────

@Preview
@Composable
fun ContactsScreenPreview_List() {
    AdbDeckTheme {
        ContactsScreen(
            component = PreviewContactsComponent(
                state = MutableStateFlow(
                    ContactsState(
                        listState        = ContactsListState.Success(previewContacts),
                        filteredContacts = previewContacts,
                        searchQuery      = "",
                    ),
                ),
            ),
        )
    }
}

@Preview
@Composable
fun ContactsScreenPreview_WithDetail() {
    AdbDeckTheme {
        ContactsScreen(
            component = PreviewContactsComponent(
                state = MutableStateFlow(
                    ContactsState(
                        listState         = ContactsListState.Success(previewContacts),
                        filteredContacts  = previewContacts,
                        selectedContactId = 1L,
                        detailState       = ContactDetailState.Success(previewDetails),
                    ),
                ),
            ),
        )
    }
}

@Preview
@Composable
fun ContactsScreenPreview_Loading() {
    AdbDeckTheme {
        ContactsScreen(
            component = PreviewContactsComponent(
                state = MutableStateFlow(
                    ContactsState(listState = ContactsListState.Loading),
                ),
            ),
        )
    }
}

@Preview
@Composable
fun ContactsScreenPreview_NoDevice() {
    AdbDeckTheme {
        ContactsScreen(
            component = PreviewContactsComponent(
                state = MutableStateFlow(
                    ContactsState(listState = ContactsListState.NoDevice),
                ),
            ),
        )
    }
}

@Preview
@Composable
fun ContactsScreenPreview_AddDialog() {
    AdbDeckTheme {
        ContactsScreen(
            component = PreviewContactsComponent(
                state = MutableStateFlow(
                    ContactsState(
                        listState        = ContactsListState.Success(previewContacts),
                        filteredContacts = previewContacts,
                        addForm          = AddContactFormState(
                            firstName = "Иван",
                            lastName  = "Иванов",
                            phone1    = "+79001234567",
                        ),
                    ),
                ),
            ),
        )
    }
}

@Preview
@Composable
fun ContactsScreenPreview_FeedbackSuccess() {
    AdbDeckTheme {
        ContactsScreen(
            component = PreviewContactsComponent(
                state = MutableStateFlow(
                    ContactsState(
                        listState        = ContactsListState.Success(previewContacts),
                        filteredContacts = previewContacts,
                        actionFeedback   = ContactFeedback("Контакт добавлен", isError = false),
                    ),
                ),
            ),
        )
    }
}

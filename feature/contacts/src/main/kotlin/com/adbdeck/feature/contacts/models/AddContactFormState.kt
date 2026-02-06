package com.adbdeck.feature.contacts.models

import com.adbdeck.core.adb.api.contacts.EmailType
import com.adbdeck.core.adb.api.contacts.PhoneType

enum class ContactFormMode {
    ADD,
    EDIT,
}

/**
 * Состояние формы создания/редактирования контакта.
 *
 * Хранится в [ContactsState.addForm]; если `null` — диалог закрыт.
 */
data class AddContactFormState(
    val mode: ContactFormMode = ContactFormMode.ADD,
    val editingContactId: Long? = null,
    val originalDisplayName: String = "",
    val accountName: String = "",
    val accountType: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val phone1: String = "",
    val phone1Type: PhoneType = PhoneType.MOBILE,
    val phone2: String = "",
    val phone2Type: PhoneType = PhoneType.MOBILE,
    val email: String = "",
    val emailType: EmailType = EmailType.HOME,
    val organization: String = "",
    val notes: String = "",
    val isSubmitting: Boolean = false,
    val error: String? = null,
)

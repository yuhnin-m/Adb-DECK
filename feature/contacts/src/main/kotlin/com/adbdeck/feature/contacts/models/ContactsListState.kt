package com.adbdeck.feature.contacts.models

import com.adbdeck.core.adb.api.contacts.Contact

/**
 * Состояние списка контактов устройства.
 */
sealed class ContactsListState {

    /** Активное устройство не выбрано. */
    data object NoDevice : ContactsListState()

    /** Идёт загрузка контактов. */
    data object Loading : ContactsListState()

    /** Контакты успешно загружены. */
    data class Success(val contacts: List<Contact>) : ContactsListState()

    /** Ошибка при загрузке. */
    data class Error(val message: String) : ContactsListState()
}

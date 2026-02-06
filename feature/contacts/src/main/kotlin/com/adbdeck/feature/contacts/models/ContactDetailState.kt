package com.adbdeck.feature.contacts.models

import com.adbdeck.core.adb.api.contacts.ContactDetails

/**
 * Состояние панели с детальной информацией о контакте.
 */
sealed class ContactDetailState {

    /** Контакт не выбран. */
    data object Idle : ContactDetailState()

    /** Идёт загрузка деталей. */
    data object Loading : ContactDetailState()

    /** Детали успешно загружены. */
    data class Success(val details: ContactDetails) : ContactDetailState()

    /** Ошибка загрузки деталей. */
    data class Error(val message: String) : ContactDetailState()
}

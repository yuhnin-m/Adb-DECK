package com.adbdeck.feature.contacts

import com.adbdeck.core.adb.api.Contact
import com.adbdeck.core.adb.api.ContactDetails
import com.adbdeck.core.adb.api.EmailType
import com.adbdeck.core.adb.api.PhoneType

// ──────────────────────────────────────────────────────────────────────────────
// Состояние списка контактов
// ──────────────────────────────────────────────────────────────────────────────

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

// ──────────────────────────────────────────────────────────────────────────────
// Состояние детальной информации
// ──────────────────────────────────────────────────────────────────────────────

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

// ──────────────────────────────────────────────────────────────────────────────
// Состояние формы добавления
// ──────────────────────────────────────────────────────────────────────────────

/**
 * Состояние формы добавления нового контакта.
 *
 * Хранится в [ContactsState.addForm]; если `null` — диалог закрыт.
 */
data class AddContactFormState(
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

// ──────────────────────────────────────────────────────────────────────────────
// Feedback-уведомление
// ──────────────────────────────────────────────────────────────────────────────

/**
 * Короткое уведомление об итоге операции (успех / ошибка).
 *
 * Показывается в баре в нижней части экрана и автоматически
 * исчезает через 3 секунды.
 *
 * @param message  Текст уведомления.
 * @param isError  `true` если операция завершилась ошибкой.
 */
data class ContactFeedback(val message: String, val isError: Boolean)

// ──────────────────────────────────────────────────────────────────────────────
// Агрегированное состояние экрана
// ──────────────────────────────────────────────────────────────────────────────

/**
 * Полное состояние экрана "Контакты".
 *
 * @param listState           Состояние списка контактов.
 * @param filteredContacts    Отфильтрованный список (применяется [searchQuery]).
 * @param searchQuery         Текущий поисковый запрос.
 * @param selectedContactId   ID выбранного контакта (или `null`).
 * @param detailState         Состояние детальной панели.
 * @param addForm             Состояние формы добавления (`null` = диалог закрыт).
 * @param pendingDeleteContact Контакт, ожидающий подтверждения удаления (`null` = диалог закрыт).
 * @param actionFeedback      Текущее feedback-уведомление (или `null`).
 * @param isActionRunning     `true`, если выполняется длительная операция (удаление, импорт).
 */
data class ContactsState(
    val listState: ContactsListState = ContactsListState.NoDevice,
    val filteredContacts: List<Contact> = emptyList(),
    val searchQuery: String = "",
    val selectedContactId: Long? = null,
    val detailState: ContactDetailState = ContactDetailState.Idle,
    val addForm: AddContactFormState? = null,
    val pendingDeleteContact: Contact? = null,
    val actionFeedback: ContactFeedback? = null,
    val isActionRunning: Boolean = false,
)

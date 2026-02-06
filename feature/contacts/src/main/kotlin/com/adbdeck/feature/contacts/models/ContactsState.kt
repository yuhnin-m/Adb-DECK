package com.adbdeck.feature.contacts.models

import com.adbdeck.core.adb.api.contacts.Contact
import com.adbdeck.core.adb.api.contacts.ContactAccount

/**
 * Полное состояние экрана "Контакты".
 *
 * @param listState           Состояние списка контактов.
 * @param filteredContacts    Отфильтрованный список (применяется [searchQuery]).
 * @param searchQuery         Текущий поисковый запрос.
 * @param availableAccounts   Доступные аккаунты контактов.
 * @param selectedAccount     Выбранный аккаунт по умолчанию для add/import.
 * @param selectedContactId   ID выбранного контакта (или `null`).
 * @param detailState         Состояние детальной панели.
 * @param addForm             Состояние формы создания/редактирования (`null` = диалог закрыт).
 * @param pendingDeleteContact Контакт, ожидающий подтверждения удаления (`null` = диалог закрыт).
 * @param actionFeedback      Текущее feedback-уведомление (или `null`).
 * @param operationState      Состояние текущей длительной операции импорта/экспорта.
 * @param isActionRunning     `true`, если выполняется короткое действие (например, удаление).
 * @param showSafetyBanner    Показывать предупреждение про ограничения ADB.
 */
data class ContactsState(
    val listState: ContactsListState = ContactsListState.NoDevice,
    val filteredContacts: List<Contact> = emptyList(),
    val searchQuery: String = "",
    val availableAccounts: List<ContactAccount> = listOf(ContactAccount.local()),
    val selectedAccount: ContactAccount = ContactAccount.local(),
    val selectedContactId: Long? = null,
    val detailState: ContactDetailState = ContactDetailState.Idle,
    val addForm: AddContactFormState? = null,
    val pendingDeleteContact: Contact? = null,
    val actionFeedback: ContactFeedback? = null,
    val operationState: ContactsOperationState? = null,
    val isActionRunning: Boolean = false,
    val showSafetyBanner: Boolean = true,
)

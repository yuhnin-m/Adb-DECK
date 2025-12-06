package com.adbdeck.core.adb.api.contacts

/**
 * Краткая информация о контакте для отображения в списке.
 *
 * Загружается за 3 запроса к Contacts Provider (contacts + phones + emails) и хранится в кэше.
 *
 * @param id           ID контакта в таблице `contacts`.
 * @param displayName  Отображаемое имя.
 * @param phones       Список телефонных номеров.
 * @param primaryEmail Первый email-адрес (или "") — для отображения в строке списка.
 * @param accountName  Имя основного аккаунта (из первого raw contact).
 * @param accountType  Тип основного аккаунта.
 */
data class Contact(
    val id: Long,
    val displayName: String,
    val phones: List<ContactPhone> = emptyList(),
    val primaryEmail: String = "",
    val accountName: String = "",
    val accountType: String = "",
)

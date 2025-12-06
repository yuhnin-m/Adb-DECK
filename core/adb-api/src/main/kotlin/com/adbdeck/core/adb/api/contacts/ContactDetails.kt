package com.adbdeck.core.adb.api.contacts

/**
 * Полная информация о контакте для панели деталей.
 *
 * Загружается по требованию (при открытии деталей).
 *
 * @param id           ID контакта.
 * @param displayName  Отображаемое имя (data1 mimetype/name).
 * @param firstName    Имя (data2).
 * @param lastName     Фамилия (data3).
 * @param middleName   Отчество / среднее имя (data4).
 * @param phones       Все телефонные номера.
 * @param emails       Все email-адреса.
 * @param organization Организация и должность.
 * @param addresses    Список адресов.
 * @param notes        Заметка (первая строка, если их несколько).
 * @param rawContacts  Список raw contacts с информацией об аккаунтах.
 */
data class ContactDetails(
    val id: Long,
    val displayName: String,
    val firstName: String = "",
    val lastName: String = "",
    val middleName: String = "",
    val phones: List<ContactPhone> = emptyList(),
    val emails: List<ContactEmail> = emptyList(),
    val organization: ContactOrganization? = null,
    val addresses: List<ContactAddress> = emptyList(),
    val notes: String = "",
    val rawContacts: List<RawContactInfo> = emptyList(),
)

package com.adbdeck.core.adb.api.contacts

/**
 * Данные контакта для импорта (из VCF / JSON) на устройство.
 *
 * @param displayName  Отображаемое имя.
 * @param firstName    Имя.
 * @param lastName     Фамилия.
 * @param phones       Список телефонов.
 * @param emails       Список email.
 * @param organization Название организации.
 * @param notes        Заметка.
 * @param accountName  Имя аккаунта назначения (пусто для локального).
 * @param accountType  Тип аккаунта назначения (пусто для локального).
 */
data class ContactImportData(
    val displayName: String,
    val firstName: String = "",
    val lastName: String = "",
    val phones: List<ContactPhone> = emptyList(),
    val emails: List<ContactEmail> = emptyList(),
    val organization: String = "",
    val notes: String = "",
    val accountName: String = "",
    val accountType: String = "",
)

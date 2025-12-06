package com.adbdeck.core.adb.api.contacts

/**
 * Данные формы для создания нового контакта.
 *
 * Передаётся из UI в [ContactsClient.addContact].
 *
 * @param firstName    Имя.
 * @param lastName     Фамилия.
 * @param displayName  Отображаемое имя (обычно "Имя Фамилия").
 * @param phone1       Первый телефон (пустая строка = не добавлять).
 * @param phone1Type   Тип первого телефона.
 * @param phone2       Второй телефон (пустая строка = не добавлять).
 * @param phone2Type   Тип второго телефона.
 * @param email        Email (пустая строка = не добавлять).
 * @param emailType    Тип email.
 * @param organization Организация (пустая строка = не добавлять).
 * @param notes        Заметка (пустая строка = не добавлять).
 * @param accountName  Имя аккаунта для raw_contact (пусто для локального).
 * @param accountType  Тип аккаунта для raw_contact (пусто для локального).
 */
data class NewContactData(
    val firstName: String,
    val lastName: String,
    val displayName: String,
    val phone1: String = "",
    val phone1Type: PhoneType = PhoneType.MOBILE,
    val phone2: String = "",
    val phone2Type: PhoneType = PhoneType.MOBILE,
    val email: String = "",
    val emailType: EmailType = EmailType.HOME,
    val organization: String = "",
    val notes: String = "",
    val accountName: String = "",
    val accountType: String = "",
)

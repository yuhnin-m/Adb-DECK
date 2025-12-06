package com.adbdeck.core.adb.api.contacts

/**
 * Телефонный номер контакта.
 *
 * @param value  Строка номера (data1 в Contacts Provider).
 * @param type   Тип номера.
 * @param label  Пользовательская метка для типа CUSTOM (data4).
 */
data class ContactPhone(
    val value: String,
    val type: PhoneType = PhoneType.MOBILE,
    val label: String = "",
)

/**
 * Email-адрес контакта.
 *
 * @param value  Email-адрес (data1).
 * @param type   Тип адреса.
 * @param label  Пользовательская метка для типа CUSTOM (data3).
 */
data class ContactEmail(
    val value: String,
    val type: EmailType = EmailType.OTHER,
    val label: String = "",
)

/**
 * Организация и должность контакта.
 *
 * @param company Название компании (data1).
 * @param title   Должность (data4).
 */
data class ContactOrganization(
    val company: String,
    val title: String = "",
)

/**
 * Адрес контакта (форматированная строка).
 *
 * @param formatted Полный адрес как одна строка (data1).
 */
data class ContactAddress(
    val formatted: String,
)

/**
 * Информация о raw contact — одной «записи» внутри агрегированного контакта.
 *
 * Каждый контакт может иметь несколько raw contacts, соответствующих разным аккаунтам синхронизации.
 *
 * @param rawContactId ID raw contact в таблице `raw_contacts`.
 * @param accountName  Имя аккаунта (например, "user@gmail.com") или "" для локального.
 * @param accountType  Тип аккаунта (например, "com.google") или "" для локального.
 */
data class RawContactInfo(
    val rawContactId: Long,
    val accountName: String,
    val accountType: String,
)

/**
 * Аккаунт контактов на устройстве (Google, Exchange, локальный и т.д.).
 *
 * @param accountName Имя аккаунта (например, `user@gmail.com`), пусто для локального.
 * @param accountType Тип аккаунта (например, `com.google`), пусто для локального.
 */
data class ContactAccount(
    val accountName: String,
    val accountType: String,
) {
    /** Признак локального аккаунта (без синхронизации). */
    val isLocal: Boolean get() = accountName.isBlank() && accountType.isBlank()

    /** Стабильный ключ аккаунта для UI-идентификации. */
    val stableKey: String get() = "${accountType}|${accountName}"

    /** Подпись аккаунта для отображения в UI. */
    fun uiLabel(): String = when {
        isLocal -> "Локальный (без аккаунта)"
        accountName.isNotBlank() -> "$accountName ($accountType)"
        else -> accountType
    }

    companion object {
        /** Локальный аккаунт по умолчанию. */
        fun local(): ContactAccount = ContactAccount(accountName = "", accountType = "")
    }
}

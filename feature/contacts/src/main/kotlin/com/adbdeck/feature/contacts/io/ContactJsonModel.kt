package com.adbdeck.feature.contacts.io

import com.adbdeck.core.adb.api.contacts.ContactDetails
import com.adbdeck.core.adb.api.contacts.ContactEmail
import com.adbdeck.core.adb.api.contacts.ContactImportData
import com.adbdeck.core.adb.api.contacts.ContactPhone
import com.adbdeck.core.adb.api.contacts.EmailType
import com.adbdeck.core.adb.api.contacts.PhoneType
import kotlinx.serialization.Serializable

// ──────────────────────────────────────────────────────────────────────────────
// JSON-модели для экспорта/импорта контактов
// ──────────────────────────────────────────────────────────────────────────────

/**
 * Корневой объект JSON-файла контактов.
 *
 * @param version  Версия формата (сейчас — 1).
 * @param contacts Список записей.
 */
@Serializable
data class ContactsJsonFile(
    val version: Int = 1,
    val contacts: List<ContactJsonEntry>,
)

/**
 * Одна запись контакта в JSON.
 *
 * @param displayName  Отображаемое имя.
 * @param firstName    Имя.
 * @param lastName     Фамилия.
 * @param phones       Список телефонных номеров.
 * @param emails       Список email-адресов.
 * @param organization Название организации.
 * @param jobTitle     Должность.
 * @param notes        Заметки.
 * @param accountName  Имя аккаунта синхронизации (из raw_contact).
 * @param accountType  Тип аккаунта (из raw_contact).
 */
@Serializable
data class ContactJsonEntry(
    val displayName: String,
    val firstName: String = "",
    val lastName: String = "",
    val phones: List<PhoneJsonEntry> = emptyList(),
    val emails: List<EmailJsonEntry> = emptyList(),
    val organization: String? = null,
    val jobTitle: String? = null,
    val notes: String? = null,
    val accountName: String? = null,
    val accountType: String? = null,
)

/**
 * Телефонный номер в JSON.
 *
 * @param value Номер телефона.
 * @param type  Тип: "mobile", "home", "work", "other".
 */
@Serializable
data class PhoneJsonEntry(
    val value: String,
    val type: String,
)

/**
 * Email-адрес в JSON.
 *
 * @param value Email-адрес.
 * @param type  Тип: "home", "work", "other".
 */
@Serializable
data class EmailJsonEntry(
    val value: String,
    val type: String,
)

// ──────────────────────────────────────────────────────────────────────────────
// Конвертеры
// ──────────────────────────────────────────────────────────────────────────────

/** Конвертировать [PhoneType] в строку для JSON. */
private fun PhoneType.toJsonString(): String = name.lowercase()

/** Конвертировать строку из JSON в [PhoneType]. */
private fun String.toPhoneType(): PhoneType =
    PhoneType.entries.firstOrNull { it.name.equals(this, ignoreCase = true) } ?: PhoneType.OTHER

/** Конвертировать [EmailType] в строку для JSON. */
private fun EmailType.toJsonString(): String = name.lowercase()

/** Конвертировать строку из JSON в [EmailType]. */
private fun String.toEmailType(): EmailType =
    EmailType.entries.firstOrNull { it.name.equals(this, ignoreCase = true) } ?: EmailType.OTHER

/**
 * Преобразовать [ContactDetails] в [ContactJsonEntry] для сохранения в JSON.
 */
fun ContactDetails.toJsonEntry(): ContactJsonEntry = ContactJsonEntry(
    displayName  = displayName,
    firstName    = firstName,
    lastName     = lastName,
    phones       = phones.map { PhoneJsonEntry(it.value, it.type.toJsonString()) },
    emails       = emails.map { EmailJsonEntry(it.value, it.type.toJsonString()) },
    organization = organization?.company,
    jobTitle     = organization?.title?.takeIf { it.isNotEmpty() },
    notes        = notes.takeIf { it.isNotEmpty() },
    accountName  = rawContacts.firstOrNull()?.accountName?.takeIf { it.isNotEmpty() },
    accountType  = rawContacts.firstOrNull()?.accountType?.takeIf { it.isNotEmpty() },
)

/**
 * Преобразовать [ContactJsonEntry] в [ContactImportData] для импорта на устройство.
 */
fun ContactJsonEntry.toImportData(): ContactImportData = ContactImportData(
    displayName  = displayName,
    firstName    = firstName,
    lastName     = lastName,
    phones       = phones.map { ContactPhone(it.value, it.type.toPhoneType()) },
    emails       = emails.map { ContactEmail(it.value, it.type.toEmailType()) },
    organization = organization.orEmpty(),
    notes        = notes.orEmpty(),
)

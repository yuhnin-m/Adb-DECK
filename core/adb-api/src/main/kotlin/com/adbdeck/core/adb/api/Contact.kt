package com.adbdeck.core.adb.api

/**
 * Тип телефонного номера (соответствует целочисленным константам Android Contacts Provider).
 *
 * @property adbInt Числовой код, хранящийся в столбце `data2` таблицы `data`.
 */
enum class PhoneType(val adbInt: Int) {
    HOME(1),
    MOBILE(2),
    WORK(3),
    OTHER(7);

    companion object {
        /**
         * Преобразует числовой код Android Contacts Provider в [PhoneType].
         * При неизвестном коде возвращает [OTHER].
         */
        fun fromAdbInt(value: Int): PhoneType =
            entries.firstOrNull { it.adbInt == value } ?: OTHER

        /**
         * Преобразует строковое название типа (mobile/home/work/other) в [PhoneType].
         * При неизвестном значении возвращает [OTHER].
         */
        fun fromString(value: String): PhoneType =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: OTHER

        /** Человекочитаемое название для UI. */
        fun PhoneType.label(): String = when (this) {
            HOME   -> "Дом"
            MOBILE -> "Мобильный"
            WORK   -> "Работа"
            OTHER  -> "Другой"
        }
    }
}

/**
 * Тип адреса электронной почты (соответствует константам Android Contacts Provider).
 *
 * @property adbInt Числовой код, хранящийся в столбце `data2` таблицы `data`.
 */
enum class EmailType(val adbInt: Int) {
    HOME(1),
    WORK(2),
    OTHER(3);

    companion object {
        /** Преобразует числовой код в [EmailType]. При неизвестном — [OTHER]. */
        fun fromAdbInt(value: Int): EmailType =
            entries.firstOrNull { it.adbInt == value } ?: OTHER

        /** Преобразует строковое название (home/work/other) в [EmailType]. */
        fun fromString(value: String): EmailType =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: OTHER

        /** Человекочитаемое название для UI. */
        fun EmailType.label(): String = when (this) {
            HOME  -> "Личный"
            WORK  -> "Рабочий"
            OTHER -> "Другой"
        }
    }
}

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

// ── Краткая запись для списка ─────────────────────────────────────────────────

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

// ── Полная запись для деталей ─────────────────────────────────────────────────

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

// ── Данные для создания нового контакта ───────────────────────────────────────

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
)

// ── Данные для импорта ────────────────────────────────────────────────────────

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
 */
data class ContactImportData(
    val displayName: String,
    val firstName: String = "",
    val lastName: String = "",
    val phones: List<ContactPhone> = emptyList(),
    val emails: List<ContactEmail> = emptyList(),
    val organization: String = "",
    val notes: String = "",
)

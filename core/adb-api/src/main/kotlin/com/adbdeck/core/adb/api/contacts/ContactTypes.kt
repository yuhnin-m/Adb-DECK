package com.adbdeck.core.adb.api.contacts

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

package com.adbdeck.feature.logcat

/**
 * Семейство шрифта для отображения строк logcat.
 *
 * Хранится в [com.adbdeck.core.settings.AppSettings] как строка ([name]),
 * чтобы не создавать зависимость core:settings → feature:logcat.
 */
enum class LogcatFontFamily {
    MONOSPACE,
    SANS_SERIF,
    SERIF,
    DEFAULT;

    companion object {
        /**
         * Безопасное восстановление из строки (например, из JSON-настроек).
         * Если строка неизвестна — возвращает [MONOSPACE].
         */
        fun fromString(name: String): LogcatFontFamily =
            entries.firstOrNull { it.name == name } ?: MONOSPACE
    }
}

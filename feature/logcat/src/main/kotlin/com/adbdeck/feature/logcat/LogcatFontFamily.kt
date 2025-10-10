package com.adbdeck.feature.logcat

/**
 * Семейство шрифта для отображения строк logcat.
 *
 * Хранится в [com.adbdeck.core.settings.AppSettings] как строка ([name]),
 * чтобы не создавать зависимость core:settings → feature:logcat.
 *
 * @param displayName Читаемое название для UI.
 */
enum class LogcatFontFamily(val displayName: String) {
    MONOSPACE("Monospace"),
    SANS_SERIF("Sans Serif"),
    SERIF("Serif"),
    DEFAULT("System");

    companion object {
        /**
         * Безопасное восстановление из строки (например, из JSON-настроек).
         * Если строка неизвестна — возвращает [MONOSPACE].
         */
        fun fromString(name: String): LogcatFontFamily =
            entries.firstOrNull { it.name == name } ?: MONOSPACE
    }
}

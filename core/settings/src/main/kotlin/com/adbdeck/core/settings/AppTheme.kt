package com.adbdeck.core.settings

import kotlinx.serialization.Serializable

/**
 * Режим темы приложения.
 *
 * Хранится в настройках и применяется через [AdbDeckTheme].
 */
@Serializable
enum class AppTheme {
    /** Светлая тема. */
    LIGHT,

    /** Тёмная тема. */
    DARK,

    /** Следовать системным настройкам ОС. */
    SYSTEM,
}

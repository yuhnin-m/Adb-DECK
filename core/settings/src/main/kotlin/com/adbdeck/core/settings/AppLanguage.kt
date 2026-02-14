package com.adbdeck.core.settings

import kotlinx.serialization.Serializable

/**
 * Язык интерфейса приложения.
 *
 * Хранится в настройках и применяется на уровне root-compose дерева.
 */
@Serializable
enum class AppLanguage {
    /** Следовать системной локали ОС. */
    SYSTEM,

    /** Английский язык. */
    ENGLISH,

    /** Русский язык. */
    RUSSIAN,
}

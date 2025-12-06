package com.adbdeck.core.adb.api.intents

/**
 * Режим запуска activity через ADB.
 */
enum class LaunchMode {
    /** Запуск через deep link (ACTION_VIEW + URI). */
    DEEP_LINK,

    /** Явный Intent с набором параметров. */
    INTENT,
}

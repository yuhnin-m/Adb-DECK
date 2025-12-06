package com.adbdeck.core.adb.api.intents

/**
 * Параметры явного Intent для `adb shell am start`.
 *
 * @param action      Intent action (опционально).
 * @param dataUri     URI данных (опционально).
 * @param packageName Целевой пакет (опционально).
 * @param component   Целевой компонент `package/.Activity` (опционально).
 * @param categories  Список categories (опционально).
 * @param flags       Флаги в hex (напр. `0x10000000`).
 * @param extras      Список typed-extras.
 */
data class IntentParams(
    val action: String = "",
    val dataUri: String = "",
    val packageName: String = "",
    val component: String = "",
    val categories: List<String> = emptyList(),
    val flags: String = "",
    val extras: List<IntentExtra> = emptyList(),
)

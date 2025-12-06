package com.adbdeck.core.adb.api.intents

/**
 * Один дополнительный параметр (extra) intent'а.
 *
 * @param key   Ключ extra.
 * @param type  Тип extra ([ExtraType]).
 * @param value Значение extra в виде строки.
 */
data class IntentExtra(
    val key: String = "",
    val type: ExtraType = ExtraType.STRING,
    val value: String = "",
)

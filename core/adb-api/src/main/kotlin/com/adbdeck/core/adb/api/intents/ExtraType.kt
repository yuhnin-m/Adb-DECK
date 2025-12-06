package com.adbdeck.core.adb.api.intents

/**
 * Тип typed-extra для `am start`.
 *
 * @param flag  ADB-флаг для передачи значения (напр. `--es`, `--ei`).
 * @param label Отображаемое название типа.
 */
enum class ExtraType(val flag: String, val label: String) {
    STRING("--es", "String"),
    INT("--ei", "Int"),
    LONG("--el", "Long"),
    BOOLEAN("--ez", "Boolean"),
    FLOAT("--ef", "Float"),
}

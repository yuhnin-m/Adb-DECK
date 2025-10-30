package com.adbdeck.core.utils

/**
 * Возвращает строку, усеченную до [maxLength] символов.
 * Если строка длиннее — добавляет суффикс "…".
 *
 * @param maxLength Максимальная длина результата.
 */
fun String.truncate(maxLength: Int): String {
    return if (length <= maxLength) this else take(maxLength - 1) + "…"
}

/**
 * Возвращает строку или null, если она пустая / состоит только из пробелов.
 */
fun String.nullIfBlank(): String? = ifBlank { null }

// ── Форматирование размеров данных ────────────────────────────────────────────

/**
 * Форматирует значение в килобайтах в человекочитаемую строку.
 *
 * Примеры:
 * - `1048576L.formatKb()` → `"1.0 GB"`
 * - `2048L.formatKb()`    → `"2.0 MB"`
 * - `512L.formatKb()`     → `"512 KB"`
 *
 * @receiver Значение в килобайтах.
 */
fun Long.formatKb(): String = when {
    this >= 1_048_576L -> "%.1f GB".format(this / 1_048_576.0)
    this >= 1_024L     -> "%.1f MB".format(this / 1_024.0)
    else               -> "$this KB"
}

/**
 * Форматирует значение в байтах в человекочитаемую строку.
 *
 * @receiver Значение в байтах.
 */
fun Long.formatBytes(): String = when {
    this >= 1_073_741_824L -> "%.1f GB".format(this / 1_073_741_824.0)
    this >= 1_048_576L     -> "%.1f MB".format(this / 1_048_576.0)
    this >= 1_024L         -> "%.1f KB".format(this / 1_024.0)
    else                   -> "$this B"
}

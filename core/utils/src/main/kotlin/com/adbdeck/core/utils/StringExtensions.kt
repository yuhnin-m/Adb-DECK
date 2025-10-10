package com.adbdeck.core.utils

/**
 * Возвращает строку, усечённую до [maxLength] символов.
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

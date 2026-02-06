package com.adbdeck.feature.contacts.models

/**
 * Короткое уведомление об итоге операции (успех / ошибка).
 *
 * Показывается в баре в нижней части экрана и автоматически
 * исчезает через 3 секунды.
 *
 * @param message  Текст уведомления.
 * @param isError  `true` если операция завершилась ошибкой.
 */
data class ContactFeedback(val message: String, val isError: Boolean)

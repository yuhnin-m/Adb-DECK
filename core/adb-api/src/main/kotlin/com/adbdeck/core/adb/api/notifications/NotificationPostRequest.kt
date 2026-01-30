package com.adbdeck.core.adb.api.notifications

/**
 * Тип визуального стиля для `cmd notification post`.
 */
enum class NotificationPostStyle {
    /** Без дополнительного стиля. */
    NONE,

    /** `-S bigtext` */
    BIGTEXT,

    /** `-S bigpicture --picture <iconspec>` */
    BIGPICTURE,

    /** `-S inbox --line <text> ...` */
    INBOX,

    /** `-S messaging --conversation <title> --message <who>:<text> ...` */
    MESSAGING,

    /** `-S media` */
    MEDIA,
}

/**
 * Одно сообщение для `messaging`-стиля.
 */
data class NotificationPostMessage(
    val who: String,
    val text: String,
)

/**
 * Параметры отправки тестового уведомления через `adb shell cmd notification post`.
 *
 * Поля являются best-effort: часть параметров зависит от Android-версии и vendor-реализации.
 *
 * @param tag Тег уведомления (обязателен для команды `post`).
 * @param text Основной текст уведомления (обязателен).
 * @param title Заголовок (`-t`), опционально.
 * @param verbose Включить verbose-режим (`-v`).
 * @param iconSpec Иконка (`-i`) в формате iconspec.
 * @param largeIconSpec Большая иконка (`-I`) в формате iconspec.
 * @param style Стиль уведомления (`-S ...`).
 * @param bigText Текст для `BIGTEXT`.
 * @param pictureSpec Картинка для `BIGPICTURE`.
 * @param inboxLines Линии для `INBOX`.
 * @param messagingConversationTitle Заголовок диалога для `MESSAGING`.
 * @param messagingMessages Сообщения для `MESSAGING`.
 * @param contentIntentSpec Intent spec после `-c` (например, `activity -a ...`).
 */
data class NotificationPostRequest(
    val tag: String,
    val text: String,
    val title: String? = null,
    val verbose: Boolean = false,
    val iconSpec: String? = null,
    val largeIconSpec: String? = null,
    val style: NotificationPostStyle = NotificationPostStyle.NONE,
    val bigText: String? = null,
    val pictureSpec: String? = null,
    val inboxLines: List<String> = emptyList(),
    val messagingConversationTitle: String? = null,
    val messagingMessages: List<NotificationPostMessage> = emptyList(),
    val contentIntentSpec: String? = null,
)

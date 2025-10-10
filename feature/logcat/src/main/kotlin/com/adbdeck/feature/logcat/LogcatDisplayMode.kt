package com.adbdeck.feature.logcat

/**
 * Режим отображения строк logcat.
 *
 * - [COMPACT] — только уровень, тег и сообщение (опционально время).
 *   Больше строк влезает на экран.
 * - [FULL] — все поля: дата, время, PID, TID, уровень, тег, сообщение.
 *   Полная информация для отладки.
 */
enum class LogcatDisplayMode {
    COMPACT,
    FULL,
}

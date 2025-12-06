package com.adbdeck.core.adb.api.logcat

/**
 * Распарсенная строка вывода `adb logcat`.
 *
 * @param id      Уникальный монотонно возрастающий ID — используется как `key` в LazyColumn.
 * @param raw     Оригинальная строка вывода.
 * @param date    Дата: MM-DD.
 * @param time    Время: HH:MM:SS.
 * @param millis  Миллисекунды (3 цифры).
 * @param pid     Process ID.
 * @param tid     Thread ID.
 * @param level   Уровень логирования.
 * @param tag     Тег сообщения.
 * @param message Тело сообщения.
 */
data class LogcatEntry(
    val id: Long,
    val raw: String,
    val date: String = "",
    val time: String = "",
    val millis: String = "",
    val pid: String = "",
    val tid: String = "",
    val level: LogcatLevel = LogcatLevel.UNKNOWN,
    val tag: String = "",
    val message: String = "",
)

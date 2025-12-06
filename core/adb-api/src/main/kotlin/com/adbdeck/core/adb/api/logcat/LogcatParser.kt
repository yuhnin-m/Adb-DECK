package com.adbdeck.core.adb.api.logcat

import java.util.concurrent.atomic.AtomicLong

/**
 * Парсер строк формата `threadtime` (`adb logcat -v threadtime`).
 *
 * Формат: `MM-DD HH:MM:SS.mmm  PID   TID  LEVEL TAG    : Message`
 *
 * Потокобезопасен: использует [AtomicLong] для генерации ID.
 */
object LogcatParser {

    private val idCounter = AtomicLong(0L)

    /**
     * Сопоставляет фиксированный заголовок строки threadtime.
     * Пример: `03-15 14:23:45.123  1234  5678 D `
     */
    private val HEADER_RE = Regex(
        """^(\d{2}-\d{2})\s+(\d{2}:\d{2}:\d{2})\.(\d{3})\s+(\d+)\s+(\d+)\s+([VDIWEFS])\s+"""
    )

    /**
     * Парсит одну строку вывода logcat.
     *
     * Нераспознанные строки (например, начальные баннеры) возвращаются
     * как [LogcatEntry] с только полями [LogcatEntry.raw] и [LogcatEntry.message].
     */
    fun parse(raw: String): LogcatEntry {
        val id = idCounter.incrementAndGet()
        val trimmed = raw.trimEnd()
        val match = HEADER_RE.find(trimmed)

        if (match == null) {
            return LogcatEntry(id = id, raw = raw, message = trimmed)
        }

        val (date, time, millis, pid, tid, levelStr) = match.destructured
        // Все после совпавшего заголовка: "TAG    : Message"
        val rest = trimmed.substring(match.range.last + 1)
        val colonIdx = rest.indexOf(": ")
        val tag = if (colonIdx >= 0) rest.substring(0, colonIdx).trim() else rest.trim()
        val message = if (colonIdx >= 0) rest.substring(colonIdx + 2) else ""

        return LogcatEntry(
            id = id,
            raw = raw,
            date = date,
            time = time,
            millis = millis,
            pid = pid,
            tid = tid,
            level = LogcatLevel.fromChar(levelStr[0]),
            tag = tag,
            message = message,
        )
    }

    /** Сбросить счетчик ID (только для тестов). */
    internal fun resetIdCounter() {
        idCounter.set(0)
    }
}

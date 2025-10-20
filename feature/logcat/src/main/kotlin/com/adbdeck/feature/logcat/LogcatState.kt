package com.adbdeck.feature.logcat

import com.adbdeck.core.adb.api.LogcatEntry
import com.adbdeck.core.adb.api.LogcatLevel

/**
 * Полное состояние экрана Logcat.
 *
 * @param isRunning        Захват logcat активен.
 * @param activeDeviceId   ID устройства, для которого открыт поток.
 * @param error            Сообщение об ошибке или `null`.
 *
 * @param entries          Буфер всех полученных строк (ограничен [maxBufferedLines]).
 * @param filteredEntries  Строки после применения активных фильтров.
 * @param totalLineCount   Общий счетчик принятых строк (включая вытесненные).
 *
 * @param searchQuery      Текстовый поиск в tag + message.
 * @param tagFilter        Фильтр по тегу (case-insensitive substring).
 * @param packageFilter    Фильтр по имени пакета / класса (применяется к tag).
 * @param levelFilter      Минимальный уровень логирования (`null` = все).
 *
 * @param displayMode      Компактный или полный режим отображения.
 * @param showDate         Отображать дату (MM-DD).
 * @param showTime         Отображать время (HH:MM:SS).
 * @param showMillis       Отображать миллисекунды.
 * @param coloredLevels    Цветовая подсветка по уровню.
 * @param autoScroll       Автоскролл вниз при поступлении новых строк.
 * @param maxBufferedLines Лимит буфера (из AppSettings на момент старта).
 */
data class LogcatState(
    val isRunning: Boolean = false,
    val activeDeviceId: String? = null,
    val error: String? = null,

    val entries: List<LogcatEntry> = emptyList(),
    val filteredEntries: List<LogcatEntry> = emptyList(),
    val totalLineCount: Int = 0,

    val searchQuery: String = "",
    val tagFilter: String = "",
    val packageFilter: String = "",
    val levelFilter: LogcatLevel? = null,

    val displayMode: LogcatDisplayMode = LogcatDisplayMode.COMPACT,
    val showDate: Boolean = false,
    val showTime: Boolean = true,
    val showMillis: Boolean = true,
    val coloredLevels: Boolean = true,
    val autoScroll: Boolean = true,
    val maxBufferedLines: Int = 5_000,
)

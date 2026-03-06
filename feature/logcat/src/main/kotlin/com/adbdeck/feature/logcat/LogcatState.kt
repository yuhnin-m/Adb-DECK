package com.adbdeck.feature.logcat

import com.adbdeck.core.adb.api.logcat.LogcatEntry
import com.adbdeck.core.adb.api.logcat.LogcatLevel

/**
 * Полное состояние экрана Logcat.
 *
 * @param isRunning        Захват logcat активен.
 * @param activeDeviceId   ID устройства, для которого открыт поток.
 * @param error            Ошибка экрана или `null`.
 *
 * @param entries          Буфер всех полученных строк (ограничен [maxBufferedLines]).
 * @param filteredEntries  Строки после применения активных фильтров.
 * @param totalLineCount   Общий счетчик принятых строк (включая вытесненные).
 *
 * @param searchQuery      Текстовый поиск в tag + message.
 * @param tagFilter        Фильтр по тегу (case-insensitive substring).
 * @param packageFilter    Фильтр по имени пакета/процесса (через сопоставление PID -> package/process).
 * @param packageSuggestions Подсказки имён пакетов для autocomplete в фильтре.
 * @param isPackageSuggestionsLoading Идёт ли загрузка подсказок пакетов.
 * @param levelFilter      Минимальный уровень логирования (`null` = все).
 *
 * @param displayMode      Компактный или полный режим отображения.
 * @param showDate         Отображать дату (MM-DD).
 * @param showTime         Отображать время (HH:MM:SS).
 * @param showMillis       Отображать миллисекунды.
 * @param coloredLevels    Цветовая подсветка по уровню.
 * @param autoScroll       Автоскролл вниз при поступлении новых строк.
 * @param maxBufferedLines Лимит буфера (из AppSettings на момент старта).
 * @param fontFamily       Шрифтовое семейство для строк лога.
 * @param fontSizeSp       Размер шрифта строк лога в sp (диапазон 8–24).
 */
data class LogcatState(
    val isRunning: Boolean = false,
    val activeDeviceId: String? = null,
    val error: LogcatError? = null,

    val entries: List<LogcatEntry> = emptyList(),
    val filteredEntries: List<LogcatEntry> = emptyList(),
    val totalLineCount: Int = 0,

    val searchQuery: String = "",
    val tagFilter: String = "",
    val packageFilter: String = "",
    val packageSuggestions: List<String> = emptyList(),
    val isPackageSuggestionsLoading: Boolean = false,
    val levelFilter: LogcatLevel? = null,

    val displayMode: LogcatDisplayMode = LogcatDisplayMode.COMPACT,
    val showDate: Boolean = false,
    val showTime: Boolean = true,
    val showMillis: Boolean = true,
    val coloredLevels: Boolean = true,
    val autoScroll: Boolean = true,
    val maxBufferedLines: Int = 5_000,
    val fontFamily: LogcatFontFamily = LogcatFontFamily.MONOSPACE,
    val fontSizeSp: Int = 12,
)

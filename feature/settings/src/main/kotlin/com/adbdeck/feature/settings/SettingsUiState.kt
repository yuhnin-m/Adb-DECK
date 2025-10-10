package com.adbdeck.feature.settings

import com.adbdeck.core.settings.AppTheme

/**
 * Состояние UI экрана настроек.
 *
 * @param adbPath            Текущее значение поля пути к adb.
 * @param adbCheckResult     Текст результата последней проверки adb (пустой — проверки не было).
 * @param isCheckingAdb      Флаг выполняющейся проверки adb.
 * @param isSaved            Флаг успешного сохранения (используется для краткосрочной анимации).
 * @param currentTheme       Текущая выбранная тема.
 *
 * Logcat:
 * @param logcatCompactMode      true = компактный режим отображения строк.
 * @param logcatShowDate         Показывать дату (MM-DD) в строке лога.
 * @param logcatShowTime         Показывать время (HH:MM:SS).
 * @param logcatShowMillis       Показывать миллисекунды.
 * @param logcatColoredLevels    Цветовая подсветка уровней лога.
 * @param logcatMaxBufferedLines Максимум строк в буфере памяти (FIFO).
 * @param logcatAutoScroll       Автоматический скролл вниз при поступлении новых строк.
 */
data class SettingsUiState(
    val adbPath: String = "adb",
    val adbCheckResult: String = "",
    val isCheckingAdb: Boolean = false,
    val isSaved: Boolean = false,
    val currentTheme: AppTheme = AppTheme.SYSTEM,

    // ── Logcat ──────────────────────────────────────────────────
    val logcatCompactMode: Boolean = true,
    val logcatShowDate: Boolean = false,
    val logcatShowTime: Boolean = true,
    val logcatShowMillis: Boolean = true,
    val logcatColoredLevels: Boolean = true,
    val logcatMaxBufferedLines: Int = 5_000,
    val logcatAutoScroll: Boolean = true,
)

package com.adbdeck.core.settings

import kotlinx.serialization.Serializable

/**
 * Модель настроек приложения ADB Deck.
 *
 * Сериализуется в JSON и хранится в файле `~/.adbdeck/settings.json`.
 * Новые поля с дефолтами — обратно совместимы благодаря `ignoreUnknownKeys = true`.
 *
 * @param adbPath                Путь к adb. Пусто → системный PATH.
 * @param bundletoolPath         Путь к bundletool (исполняемый файл или `.jar`).
 * @param theme                  Режим темы приложения.
 * @param language               Язык интерфейса приложения.
 * @param knownEndpoints         Сохраненные TCP/IP endpoint-ы ("host:port").
 *
 * Logcat:
 * @param logcatCompactMode      true = компактный режим, false = полный (все поля).
 * @param logcatShowDate         Показывать дату (MM-DD) в строке лога.
 * @param logcatShowTime         Показывать время (HH:MM:SS).
 * @param logcatShowMillis       Показывать миллисекунды.
 * @param logcatColoredLevels    Цветовая подсветка уровней лога.
 * @param logcatMaxBufferedLines Максимум строк в in-memory буфере (FIFO, удаляются старые).
 * @param logcatAutoScroll       Автоматический скролл вниз при поступлении новых строк.
 * @param logcatFontFamily       Семейство шрифта (имя enum LogcatFontFamily, напр. "MONOSPACE").
 * @param logcatFontSizeSp       Размер шрифта в sp (диапазон 8–24).
 *
 * Screen Tools:
 * @param screenToolsScreenshotOutputDir Папка сохранения screenshot-файлов.
 * @param screenToolsScreenrecordOutputDir Папка сохранения screenrecord-видео.
 */
@Serializable
data class AppSettings(
    val adbPath: String = "adb",
    val bundletoolPath: String = "bundletool",
    val theme: AppTheme = AppTheme.SYSTEM,
    val language: AppLanguage = AppLanguage.SYSTEM,
    val knownEndpoints: List<String> = emptyList(),

    // ── Logcat ──────────────────────────────────────────────────
    val logcatCompactMode: Boolean = true,
    val logcatShowDate: Boolean = false,
    val logcatShowTime: Boolean = true,
    val logcatShowMillis: Boolean = true,
    val logcatColoredLevels: Boolean = true,
    val logcatMaxBufferedLines: Int = 5_000,
    val logcatAutoScroll: Boolean = true,
    val logcatFontFamily: String = "MONOSPACE",
    val logcatFontSizeSp: Int = 12,

    // ── Screen Tools ─────────────────────────────────────────────
    val screenToolsScreenshotOutputDir: String = "",
    val screenToolsScreenrecordOutputDir: String = "",
)

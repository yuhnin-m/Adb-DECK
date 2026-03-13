package com.adbdeck.core.settings

import kotlinx.serialization.Serializable

/**
 * Персистентная запись о ранее подключенном Wi-Fi-устройстве.
 *
 * @param address     Сетевой адрес `host:port`.
 * @param deviceId    Последний известный deviceId.
 * @param displayName Человекочитаемое имя устройства.
 * @param lastSeenAt  Время последнего обнаружения устройства (Unix ms).
 */
@Serializable
data class SavedWifiDeviceSettingsEntry(
    val address: String,
    val deviceId: String = "",
    val displayName: String = "",
    val lastSeenAt: Long = 0L,
)

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
 * @param knownWifiDevices       История ранее подключенных Wi-Fi-устройств.
 *
 * Logcat:
 * @param logcatCompactMode      true = компактный режим, false = полный (все поля).
 * @param logcatShowDate         Показывать дату (MM-DD) в строке лога.
 * @param logcatShowTime         Показывать время (HH:MM:SS).
 * @param logcatShowMillis       Показывать миллисекунды.
 * @param logcatColoredLevels    Цветовая подсветка уровней лога.
 * @param logcatMaxBufferedLines Максимум строк в in-memory буфере (FIFO, удаляются старые).
 * @param logcatAutoScroll       Автоматический скролл вниз при поступлении новых строк.
 * @param logcatSmoothStreamAnimation Плавная по-кадровая публикация новых строк (эффект "живого" потока).
 * @param logcatFontFamily       Семейство шрифта (имя enum LogcatFontFamily, напр. "MONOSPACE").
 * @param logcatFontSizeSp       Размер шрифта в sp (диапазон 8–24).
 *
 * Screen Tools:
 * @param screenToolsScreenshotOutputDir Папка сохранения screenshot-файлов.
 * @param screenToolsScreenrecordOutputDir Папка сохранения screenrecord-видео.
 *
 * Scrcpy:
 * @param scrcpyPath Путь к исполняемому файлу scrcpy. Пусто → системный PATH.
 */
@Serializable
data class AppSettings(
    val adbPath: String = DEFAULT_ADB_EXECUTABLE,
    val bundletoolPath: String = "bundletool",
    val scrcpyPath: String = "scrcpy",
    val theme: AppTheme = AppTheme.SYSTEM,
    val language: AppLanguage = AppLanguage.SYSTEM,
    val knownEndpoints: List<String> = emptyList(),
    val knownWifiDevices: List<SavedWifiDeviceSettingsEntry> = emptyList(),

    // ── Logcat ──────────────────────────────────────────────────
    val logcatCompactMode: Boolean = true,
    val logcatShowDate: Boolean = false,
    val logcatShowTime: Boolean = true,
    val logcatShowMillis: Boolean = true,
    val logcatColoredLevels: Boolean = true,
    val logcatMaxBufferedLines: Int = 5_000,
    val logcatAutoScroll: Boolean = true,
    val logcatSmoothStreamAnimation: Boolean = true,
    val logcatFontFamily: String = "MONOSPACE",
    val logcatFontSizeSp: Int = 12,

    // ── Screen Tools ─────────────────────────────────────────────
    val screenToolsScreenshotOutputDir: String = "",
    val screenToolsScreenrecordOutputDir: String = "",
)

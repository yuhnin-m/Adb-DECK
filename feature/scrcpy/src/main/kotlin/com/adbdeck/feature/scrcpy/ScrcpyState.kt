package com.adbdeck.feature.scrcpy

// ── Состояния процесса scrcpy ─────────────────────────────────────────────────

/**
 * Состояние жизненного цикла внешнего процесса scrcpy.
 */
enum class ScrcpyProcessState {
    /** Процесс не запущен. */
    IDLE,

    /** Процесс запускается. */
    STARTING,

    /** Процесс работает. */
    RUNNING,

    /** Запрошена остановка процесса. */
    STOPPING,

    /** Процесс завершился с ошибкой. */
    ERROR,
}

// ── Параметры конфигурации ────────────────────────────────────────────────────

/**
 * Ограничение максимального разрешения экрана для scrcpy.
 *
 * @param displayValue Строка для отображения в UI. `null` означает "без ограничений".
 * @param pixels Значение в пикселях для флага `--max-size`. `null` — не передаётся.
 */
enum class ScrcpyMaxResolution(val displayValue: String?, val pixels: Int?) {
    NO_LIMIT(null, null),
    P480("480p", 480),
    P720("720p", 720),
    P1080("1080p", 1080),
    P1920("1920p", 1920),
}

/**
 * Частота кадров для зеркалирования.
 *
 * @param value Числовое значение fps.
 */
enum class ScrcpyFps(val value: Int) {
    FPS_15(15),
    FPS_30(30),
    FPS_60(60),
}

/**
 * Видеокодек для кодирования потока.
 *
 * @param cliValue Значение флага `--video-codec`.
 */
enum class ScrcpyVideoCodec(val cliValue: String) {
    H264("h264"),
    H265("h265"),
    AV1("av1"),
}

/**
 * Режим эмуляции устройства ввода (клавиатура / мышь).
 *
 * @param cliValue Значение флагов `--keyboard` / `--mouse`.
 */
enum class ScrcpyInputMode(val cliValue: String) {
    SDK("sdk"),
    UHID("uhid"),
    AOA("aoa"),
}

// ── Конфигурация запуска ──────────────────────────────────────────────────────

/**
 * Полная конфигурация параметров запуска scrcpy.
 *
 * Каждое поле соответствует флагу командной строки scrcpy.
 *
 * @param maxResolution   Максимальное разрешение (флаг `--max-size`).
 * @param fps             Частота кадров (флаг `--max-fps`).
 * @param bitrate         Битрейт видео в Мбит/с (флаг `--video-bit-rate`). Пустая строка — не передаётся.
 * @param allowInput      Разрешить управление устройством (без флага `--no-control`).
 * @param turnScreenOff   Выключить экран устройства (флаг `--turn-screen-off`).
 * @param showTouches     Показывать касания на экране (флаг `--show-touches`).
 * @param stayAwake       Не засыпать пока подключено (флаг `--stay-awake`).
 * @param fullscreen      Запустить в полноэкранном режиме (флаг `--fullscreen`).
 * @param alwaysOnTop     Окно поверх остальных (флаг `--always-on-top`).
 * @param borderless      Окно без рамки (флаг `--window-borderless`).
 * @param windowWidth     Ширина окна в пикселях (флаг `--window-width`). Пустая строка — авто.
 * @param windowHeight    Высота окна в пикселях (флаг `--window-height`). Пустая строка — авто.
 * @param videoCodec      Видеокодек (флаг `--video-codec`).
 * @param keyboardMode    Режим клавиатуры (флаг `--keyboard`).
 * @param mouseMode       Режим мыши (флаг `--mouse`).
 */
data class ScrcpyConfig(
    val maxResolution: ScrcpyMaxResolution = ScrcpyMaxResolution.NO_LIMIT,
    val fps: ScrcpyFps = ScrcpyFps.FPS_60,
    val bitrate: String = "",
    val allowInput: Boolean = true,
    val turnScreenOff: Boolean = false,
    val showTouches: Boolean = false,
    val stayAwake: Boolean = false,
    val fullscreen: Boolean = false,
    val alwaysOnTop: Boolean = false,
    val borderless: Boolean = false,
    val windowWidth: String = "",
    val windowHeight: String = "",
    val videoCodec: ScrcpyVideoCodec = ScrcpyVideoCodec.H264,
    val keyboardMode: ScrcpyInputMode = ScrcpyInputMode.SDK,
    val mouseMode: ScrcpyInputMode = ScrcpyInputMode.SDK,
)

// ── Обратная связь ────────────────────────────────────────────────────────────

/**
 * Краткосрочное сообщение обратной связи.
 *
 * Автоматически скрывается через 3 секунды.
 *
 * @param message Текст для отображения.
 * @param isError `true` — сообщение об ошибке (красный цвет), иначе — успех (зелёный).
 */
data class ScrcpyFeedback(
    val message: String,
    val isError: Boolean = false,
)

// ── Корневое состояние экрана ─────────────────────────────────────────────────

/**
 * Полное состояние экрана scrcpy.
 *
 * @param activeDeviceId  Серийный номер активного устройства или `null`.
 * @param processState    Текущее состояние процесса scrcpy.
 * @param config          Параметры запуска scrcpy.
 * @param scrcpyPath      Путь к исполняемому файлу scrcpy (из настроек).
 * @param isConfigured    `true` если путь к scrcpy не пустой (не гарантирует корректность).
 * @param feedback        Краткосрочное сообщение обратной связи или `null`.
 */
data class ScrcpyState(
    val activeDeviceId: String? = null,
    val processState: ScrcpyProcessState = ScrcpyProcessState.IDLE,
    val config: ScrcpyConfig = ScrcpyConfig(),
    val scrcpyPath: String = "scrcpy",
    val isConfigured: Boolean = true,
    val feedback: ScrcpyFeedback? = null,
)

/** Минимальная допустимая размерность окна scrcpy в пикселях. */
const val SCRCPY_MIN_WINDOW_DIMENSION: Int = 240

/** Максимальная допустимая размерность окна scrcpy в пикселях. */
const val SCRCPY_MAX_WINDOW_DIMENSION: Int = 4320

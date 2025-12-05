package com.adbdeck.core.adb.api.logcat

/**
 * Уровни логирования ADB Logcat.
 *
 * @param code        Однобуквенный код из вывода adb logcat.
 * @param displayName Отображаемое название.
 * @param priority    Числовой приоритет (чем выше — тем важнее).
 *                    Используется для фильтрации по минимальному уровню.
 */
enum class LogcatLevel(val code: Char, val displayName: String, val priority: Int) {
    VERBOSE('V', "Verbose", 2),
    DEBUG('D', "Debug", 3),
    INFO('I', "Info", 4),
    WARNING('W', "Warning", 5),
    ERROR('E', "Error", 6),
    FATAL('F', "Fatal", 7),
    SILENT('S', "Silent", 8),
    UNKNOWN('?', "Unknown", 1);

    companion object {
        /** Возвращает [LogcatLevel] по однобуквенному коду. */
        fun fromChar(c: Char): LogcatLevel =
            entries.firstOrNull { it.code == c } ?: UNKNOWN
    }
}

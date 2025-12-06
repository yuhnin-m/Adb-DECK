package com.adbdeck.core.adb.api.monitoring.process

/**
 * Состояние процесса Android-устройства.
 *
 * Соответствует однобуквенным обозначениям в выводе `ps` и `top`.
 *
 * @property symbol      Символ состояния в выводе ADB.
 * @property displayName Человекочитаемое название.
 */
enum class ProcessState(val symbol: Char, val displayName: String) {
    /** Процесс активно выполняется. */
    RUNNING('R', "Running"),

    /** Прерываемый сон (ожидание ресурса). */
    SLEEPING('S', "Sleeping"),

    /** Неприрываемый сон (I/O ожидание). */
    DISK_SLEEP('D', "Disk Sleep"),

    /** Остановлен сигналом SIGSTOP. */
    STOPPED('T', "Stopped"),

    /** Зомби — завершён, но не собран родителем. */
    ZOMBIE('Z', "Zombie"),

    /** Отслеживается ptrace. */
    TRACING('t', "Tracing"),

    /** Idle (только ядерные потоки, Android 5+). */
    IDLE('I', "Idle"),

    /** Состояние не определено / не удалось распарсить. */
    UNKNOWN('?', "Unknown");

    companion object {
        /** Создаёт [ProcessState] из однобуквенного символа. */
        fun fromSymbol(c: Char): ProcessState =
            entries.find { it.symbol == c } ?: UNKNOWN

        /** Создаёт [ProcessState] из строки (берёт первый символ). */
        fun fromString(s: String): ProcessState =
            fromSymbol(s.trim().firstOrNull() ?: '?')
    }
}

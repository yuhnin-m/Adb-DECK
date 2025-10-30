package com.adbdeck.core.adb.api

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

/**
 * Базовая информация об одном процессе Android-устройства.
 *
 * Получается из вывода `top -b -n 1` или `ps -A -o PID,USER,PPID,NAME,S,RSS,VSZ`.
 *
 * @param pid          Идентификатор процесса.
 * @param ppid         Идентификатор родительского процесса (0 если недоступен).
 * @param user         Пользователь, под которым запущен процесс.
 * @param name         Имя процесса / CMDLINE (из `top`) или NAME (из `ps`).
 * @param packageName  Имя пакета, если процесс является Android-приложением.
 *                     Определяется эвристически по имени (наличие точек, обратный DNS).
 * @param state        Текущее состояние процесса.
 * @param cpuPercent   Использование CPU в % (0–100+, на многоядерных может быть > 100).
 *                     Равно 0 при использовании `ps` без top.
 * @param memPercent   Использование RAM в % (0–100).
 * @param rssKb        Resident Set Size в KB — физическая занятая память.
 * @param vszKb        Virtual Size в KB — виртуальное адресное пространство.
 */
data class ProcessInfo(
    val pid: Int,
    val ppid: Int = 0,
    val user: String,
    val name: String,
    val packageName: String = "",
    val state: ProcessState = ProcessState.UNKNOWN,
    val cpuPercent: Float = 0f,
    val memPercent: Float = 0f,
    val rssKb: Long = 0L,
    val vszKb: Long = 0L,
) {
    /**
     * Отображаемое имя: имя пакета если доступно, иначе имя процесса.
     */
    val displayName: String get() = packageName.ifEmpty { name }

    /**
     * `true` если имя процесса выглядит как Android-пакет (reverse-DNS с точками).
     *
     * Используется для решения: показывать ли кнопку "Force Stop" и "Open in Packages".
     */
    val looksLikePackage: Boolean
        get() = name.contains('.') &&
            !name.startsWith('/') &&
            !name.startsWith('[') &&
            !name.startsWith('-') &&
            name.length > 3
}

/**
 * Снимок состояния всей системы: список процессов + системные метрики.
 *
 * Возвращается из [SystemMonitorClient.getProcessSnapshot] как единый объект,
 * чтобы минимизировать количество ADB-вызовов.
 *
 * @param processes        Список всех процессов устройства.
 * @param systemCpuPercent Суммарное использование CPU (0–100).
 *                         Парсится из заголовка `top` или вычисляется из `/proc/stat`.
 * @param totalRamKb       Общий объём RAM в KB.
 * @param usedRamKb        Используемая RAM в KB.
 * @param freeRamKb        Свободная RAM в KB.
 */
data class ProcessSnapshot(
    val processes: List<ProcessInfo>,
    val systemCpuPercent: Float = 0f,
    val totalRamKb: Long = 0L,
    val usedRamKb: Long = 0L,
    val freeRamKb: Long = 0L,
)

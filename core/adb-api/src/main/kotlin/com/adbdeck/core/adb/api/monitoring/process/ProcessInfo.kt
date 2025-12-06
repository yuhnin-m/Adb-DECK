package com.adbdeck.core.adb.api.monitoring.process

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

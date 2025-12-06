package com.adbdeck.core.adb.api.monitoring.process

/**
 * Расширенная информация о конкретном процессе Android-устройства.
 *
 * Собирается из нескольких ADB-источников:
 * - `/proc/<pid>/status` — имя, состояние, ppid, количество потоков, VM-статистика
 * - `dumpsys meminfo <pid>` — детальная разбивка памяти (PSS, Native/Java Heap)
 * - `/proc/<pid>/cmdline` — полная командная строка запуска процесса
 *
 * Все числовые поля по умолчанию равны 0 — это значит «данные недоступны»,
 * а не нулевое потребление.
 *
 * @param pid               Идентификатор процесса.
 * @param name              Имя процесса (из status или top).
 * @param packageName       Имя пакета Android-приложения (если применимо).
 * @param user              Пользователь UNIX.
 * @param state             Текущее состояние.
 * @param ppid              PID родительского процесса.
 * @param threads           Количество потоков.
 * @param cmdline           Полная командная строка запуска.
 * @param rssKb             Resident Set Size в KB.
 * @param vszKb             Virtual Size в KB.
 * @param pssKb             Proportional Set Size в KB (из `dumpsys meminfo`).
 * @param ussKb             Unique Set Size в KB (приватная память, из `dumpsys meminfo`).
 * @param heapSizeKb        Размер Java/Dalvik Heap в KB.
 * @param heapAllocKb       Выделено в Java/Dalvik Heap в KB.
 * @param heapFreeKb        Свободно в Java/Dalvik Heap в KB.
 * @param nativeHeapSizeKb  Размер Native Heap в KB.
 * @param nativeHeapAllocKb Выделено в Native Heap в KB.
 * @param openFiles         Количество открытых файловых дескрипторов.
 */
data class ProcessDetails(
    val pid: Int,
    val name: String,
    val packageName: String = "",
    val user: String = "",
    val state: ProcessState = ProcessState.UNKNOWN,
    val ppid: Int = 0,
    val threads: Int = 0,
    val cmdline: String = "",
    val rssKb: Long = 0L,
    val vszKb: Long = 0L,
    val pssKb: Long = 0L,
    val ussKb: Long = 0L,
    val heapSizeKb: Long = 0L,
    val heapAllocKb: Long = 0L,
    val heapFreeKb: Long = 0L,
    val nativeHeapSizeKb: Long = 0L,
    val nativeHeapAllocKb: Long = 0L,
    val openFiles: Int = 0,
) {
    /** `true` если доступны данные из `dumpsys meminfo` (PSS > 0). */
    val hasMemInfoData: Boolean get() = pssKb > 0L

    /** `true` если есть данные о Java Heap. */
    val hasHeapData: Boolean get() = heapSizeKb > 0L
}

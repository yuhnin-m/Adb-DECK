package com.adbdeck.core.adb.api.monitoring

import com.adbdeck.core.adb.api.monitoring.process.ProcessDetails
import com.adbdeck.core.adb.api.monitoring.process.ProcessSnapshot
import com.adbdeck.core.adb.api.monitoring.storage.StoragePartition
import com.adbdeck.core.adb.api.packages.PackageClient

/**
 * Контракт клиента для мониторинга состояния Android-устройства.
 *
 * Охватывает два аспекта:
 * - Управление процессами (список с CPU/RAM, детали, kill)
 * - Информация о файловой системе / хранилище
 *
 * Все методы `suspend` и делегируют I/O в [com.adbdeck.core.process.ProcessRunner].
 * Все возвращают [Result]: [Result.failure] содержит понятное сообщение об ошибке.
 *
 * ## ADB-команды, используемые реализацией:
 * - `top -b -n 1` — список процессов с CPU%, заголовок с системными метриками
 * - `ps -A -o PID,USER,PPID,NAME,S,RSS,VSZ` — резервный список без CPU%
 * - `cat /proc/meminfo` — память системы (резервный источник)
 * - `cat /proc/<pid>/status` — детали процесса (ppid, threads, VM)
 * - `dumpsys meminfo <pid>` — детальная разбивка памяти (PSS, heap)
 * - `cat /proc/<pid>/cmdline` — командная строка запуска
 * - `kill -9 <pid>` — принудительное завершение процесса
 * - `df` / `df -k` — информация о файловых системах
 */
interface SystemMonitorClient {

    /**
     * Получить снимок состояния всех процессов и системные метрики CPU/RAM.
     *
     * ## Стратегия получения данных:
     * 1. Пробует `top -b -n 1` — дает CPU% на процесс и системные метрики из заголовка.
     * 2. Если `top` недоступен или вывод не поддается парсингу:
     *    - Процессы: `ps -A -o PID,USER,PPID,NAME,S,RSS,VSZ` (CPU% = 0)
     *    - Метрики: `cat /proc/meminfo`
     *
     * @param deviceId Серийный номер / адрес устройства (для `adb -s`).
     * @param adbPath  Путь к исполняемому файлу `adb` (по умолчанию `"adb"`).
     * @return [ProcessSnapshot] с списком процессов и системными метриками.
     */
    suspend fun getProcessSnapshot(
        deviceId: String,
        adbPath: String = "adb",
    ): Result<ProcessSnapshot>

    /**
     * Получить расширенную информацию о конкретном процессе.
     *
     * Выполняет несколько ADB-команд:
     * - `cat /proc/<pid>/status` — имя, состояние, потоки, VM-статистика
     * - `cat /proc/<pid>/cmdline` — аргументы запуска
     * - `dumpsys meminfo <pid>` — детальная разбивка памяти (может не работать без root)
     *
     * @param deviceId Серийный номер / адрес устройства.
     * @param pid      Идентификатор процесса.
     * @param adbPath  Путь к `adb`.
     */
    suspend fun getProcessDetails(
        deviceId: String,
        pid: Int,
        adbPath: String = "adb",
    ): Result<ProcessDetails>

    /**
     * Принудительно завершить процесс (`kill -9 <pid>`).
     *
     * ## Ограничения:
     * - Обычный ADB-пользователь (`shell`) может убить только процессы своего UID.
     * - Системные процессы и приложения других пользователей требуют root.
     * - Для Android-приложений предпочтительнее `am force-stop <package>` —
     *   используй [PackageClient.forceStop] вместо этого метода.
     *
     * @param deviceId Серийный номер / адрес устройства.
     * @param pid      Идентификатор процесса.
     * @param adbPath  Путь к `adb`.
     */
    suspend fun killProcess(
        deviceId: String,
        pid: Int,
        adbPath: String = "adb",
    ): Result<Unit>

    /**
     * Получить информацию о разделах файловой системы устройства.
     *
     * ## Стратегия:
     * 1. Пробует `df -k` (всегда выводит в KB, надёжнее).
     * 2. Если не поддерживается — пробует `df` с парсингом human-readable формата.
     *
     * Псевдо-файловые системы фильтруются через [StoragePartition.isRelevant].
     *
     * @param deviceId Серийный номер / адрес устройства.
     * @param adbPath  Путь к `adb`.
     * @return Список разделов файловой системы (включая нерелевантные — для полноты картины).
     */
    suspend fun getStorageInfo(
        deviceId: String,
        adbPath: String = "adb",
    ): Result<List<StoragePartition>>

    /**
     * Выполнить произвольную shell-команду на устройстве.
     *
     * Команда запускается через:
     * `adb -s <id> shell sh -c "<shellCommand>"`.
     *
     * В отличие от методов, возвращающих типизированные данные, этот метод
     * не бросает ошибку на `exitCode != 0`, а возвращает [ShellCommandResult].
     * Ошибка возвращается только при проблеме запуска процесса (например, adb недоступен).
     *
     * @param deviceId Серийный номер / адрес устройства.
     * @param shellCommand Команда для выполнения внутри `sh -c`.
     * @param adbPath Путь к `adb`.
     */
    suspend fun runShellCommand(
        deviceId: String,
        shellCommand: String,
        adbPath: String = "adb",
    ): Result<ShellCommandResult>

    /**
     * Получить сырой вывод `dumpsys diskstats`.
     *
     * Нужен для feature-уровня, где требуется tolerant parsing полей
     * (App Size, Downloads Size, Data-Free и т.д.).
     *
     * @param deviceId Серийный номер / адрес устройства.
     * @param adbPath Путь к `adb`.
     */
    suspend fun getDiskstats(
        deviceId: String,
        adbPath: String = "adb",
    ): Result<String>
}

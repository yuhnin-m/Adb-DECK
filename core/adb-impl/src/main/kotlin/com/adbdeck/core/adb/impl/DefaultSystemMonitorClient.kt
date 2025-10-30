package com.adbdeck.core.adb.impl

import com.adbdeck.core.adb.api.ProcessDetails
import com.adbdeck.core.adb.api.ProcessInfo
import com.adbdeck.core.adb.api.ProcessSnapshot
import com.adbdeck.core.adb.api.ProcessState
import com.adbdeck.core.adb.api.StoragePartition
import com.adbdeck.core.adb.api.SystemMonitorClient
import com.adbdeck.core.process.ProcessRunner
import com.adbdeck.core.utils.runCatchingPreserveCancellation

/**
 * Реализация [SystemMonitorClient] поверх системного `adb`.
 *
 * ## Стратегия получения данных о процессах:
 *
 * 1. `top -b -n 1` — единственный вызов, дающий CPU% на процесс И системные метрики.
 *    Поддерживается на Android 6+ (API 23). Парсятся два формата заголовка:
 *    - **Новый**: `Mem: N total, N used, ...` + `N%cpu N%user ... N%idle`
 *    - **Старый**: `User N%, System N%, IOW N%, IRQ N%`
 *    Поддерживаются два формата процессных строк (Android 6+ vs Android 5).
 *
 * 2. Если `top` недоступен или парсинг не дал результатов:
 *    - Процессы: `ps -A -o PID,USER,PPID,NAME,S,RSS,VSZ` (cpu% = 0)
 *    - Метрики: `/proc/meminfo`
 *
 * ## Потокобезопасность:
 * Класс не содержит изменяемого состояния — все методы чисто функциональные.
 * I/O делегируется [ProcessRunner] (Dispatchers.IO).
 *
 * @param processRunner Абстракция запуска внешних процессов.
 */
class DefaultSystemMonitorClient(
    private val processRunner: ProcessRunner,
) : SystemMonitorClient {

    // ── Процессы ────────────────────────────────────────────────────────────────

    override suspend fun getProcessSnapshot(
        deviceId: String,
        adbPath: String,
    ): Result<ProcessSnapshot> = runCatchingPreserveCancellation {
        // Попытка 1: top — дает и процессы, и CPU%
        val topResult = processRunner.run(
            adbPath, "-s", deviceId, "shell", "top", "-b", "-n", "1",
        )
        if (topResult.isSuccess && topResult.stdout.isNotBlank()) {
            val snapshot = parseTopOutput(topResult.stdout)
            if (snapshot != null) return@runCatchingPreserveCancellation snapshot
        }

        // Попытка 2: ps + /proc/meminfo
        val psResult = processRunner.run(
            adbPath, "-s", deviceId, "shell",
            "ps", "-A", "-o", "PID,USER,PPID,NAME,S,RSS,VSZ",
        )
        if (!psResult.isSuccess || psResult.stdout.isBlank()) {
            error("Не удалось получить список процессов: ${psResult.stderr.take(200)}")
        }
        val processes = parsePsOutput(psResult.stdout)

        val memInfo = fetchMemInfo(deviceId, adbPath)
        ProcessSnapshot(
            processes = processes,
            systemCpuPercent = 0f,
            totalRamKb = memInfo.first,
            usedRamKb = memInfo.second,
            freeRamKb = memInfo.third,
        )
    }

    // ── Парсинг top ──────────────────────────────────────────────────────────────

    /**
     * Разбирает вывод `top -b -n 1`.
     *
     * Возвращает null если вывод пустой или формат не распознан.
     * Поддерживает Android 5 (колонки PRpriority, CPU%, #THR, VSS, RSS)
     * и Android 6+ (колонки PR, NI, VIRT, RES, SHR, S, %CPU, %MEM, TIME+, ARGS).
     */
    private fun parseTopOutput(output: String): ProcessSnapshot? {
        val lines = output.lines()
        if (lines.size < 5) return null

        var totalRam = 0L
        var usedRam = 0L
        var freeRam = 0L
        var cpuPercent = 0f

        // ── Парсим заголовок ──────────────────────────────────────────────────

        for (line in lines.take(10)) {
            val trimmed = line.trim()

            // Формат новый: "Mem:   3801600K total,  2951136K used,   850464K free"
            if (trimmed.startsWith("Mem:", ignoreCase = true)) {
                totalRam = extractKb(trimmed, "total") ?: totalRam
                usedRam = extractKb(trimmed, "used") ?: usedRam
                freeRam = extractKb(trimmed, "free") ?: freeRam
                continue
            }

            // Формат новый CPU: "400%cpu   0%user   1%nice  15%sys 384%idle"
            if (trimmed.contains("%cpu", ignoreCase = true) && trimmed.contains("%idle", ignoreCase = true)) {
                val idleMatch = Regex("(\\d+(?:\\.\\d+)?)%idle", RegexOption.IGNORE_CASE).find(trimmed)
                val totalCpu = trimmed.substringBefore("%cpu").trim()
                    .split(Regex("\\s+")).lastOrNull()?.toFloatOrNull() ?: 100f
                val idle = idleMatch?.groupValues?.get(1)?.toFloatOrNull() ?: 0f
                // normalised: idle is relative to totalCpu (e.g. 400%cpu, 384%idle → 96% idle → 4% used)
                cpuPercent = if (totalCpu > 0) ((totalCpu - idle) / totalCpu * 100f).coerceIn(0f, 100f) else 0f
                continue
            }

            // Формат старый CPU: "User 5%, System 9%, IOW 0%, IRQ 0%"
            if (trimmed.startsWith("User", ignoreCase = true) && trimmed.contains("System", ignoreCase = true)) {
                val user = Regex("User\\s+(\\d+)%", RegexOption.IGNORE_CASE).find(trimmed)
                    ?.groupValues?.get(1)?.toFloatOrNull() ?: 0f
                val sys = Regex("System\\s+(\\d+)%", RegexOption.IGNORE_CASE).find(trimmed)
                    ?.groupValues?.get(1)?.toFloatOrNull() ?: 0f
                cpuPercent = (user + sys).coerceIn(0f, 100f)
                continue
            }

            // Старый формат памяти: "User 123 + Nice 0 + Sys 234 + Idle 2048 ... = N"
            if (trimmed.startsWith("User") && trimmed.contains("Idle") && trimmed.contains("=")) {
                // Нет прямых данных об общей RAM — будет получено из /proc/meminfo в другом пути
                continue
            }
        }

        // ── Находим строку-заголовок колонок ──────────────────────────────────

        val headerIndex = lines.indexOfFirst { it.trim().startsWith("PID") }
        if (headerIndex < 0 || headerIndex >= lines.lastIndex) return null

        val headerLine = lines[headerIndex].trim()
        val isNewFormat = headerLine.contains("%CPU", ignoreCase = true) ||
            headerLine.contains("[%CPU]", ignoreCase = true)

        // ── Парсим строки процессов ───────────────────────────────────────────

        val processes = lines.drop(headerIndex + 1)
            .filter { it.trim().isNotEmpty() }
            .mapNotNull { line ->
                if (isNewFormat) parseTopProcessLineNew(line)
                else parseTopProcessLineOld(line)
            }

        if (processes.isEmpty()) return null

        return ProcessSnapshot(
            processes = processes,
            systemCpuPercent = cpuPercent,
            totalRamKb = totalRam,
            usedRamKb = usedRam,
            freeRamKb = if (freeRam > 0) freeRam else (totalRam - usedRam).coerceAtLeast(0),
        )
    }

    /**
     * Парсит строку процесса из `top` Android 6+ формата.
     *
     * Ожидаемые колонки: `PID USER PR NI VIRT RES SHR S[%CPU] %MEM TIME+ ARGS`
     * Количество колонок: ≥ 9 (ARGS может отсутствовать).
     */
    private fun parseTopProcessLineNew(line: String): ProcessInfo? {
        val parts = line.trim().split(Regex("\\s+"))
        if (parts.size < 9) return null

        val pid = parts[0].toIntOrNull() ?: return null
        val user = parts[1]
        // PR, NI — пропускаем (индексы 2,3)
        // VIRT (4), RES (5), SHR (6) — размеры
        val virt = parseMemValue(parts[4])
        val res = parseMemValue(parts[5])
        // S — состояние (индекс 7)
        val state = ProcessState.fromString(parts[7])
        // %CPU (индекс 8), %MEM (индекс 9)
        val cpu = parts.getOrNull(8)?.trimStart('[')?.trimEnd(']')?.toFloatOrNull() ?: 0f
        val mem = parts.getOrNull(9)?.toFloatOrNull() ?: 0f
        // ARGS — последний(е) элемент(ы), пропускаем TIME+
        val name = parts.drop(11).joinToString(" ").trimStart('+').trim()
            .ifEmpty { parts.lastOrNull() ?: "" }

        return buildProcessInfo(pid, user, name, state, cpu, mem, res, virt)
    }

    /**
     * Парсит строку процесса из `top` Android 5 формата.
     *
     * Ожидаемые колонки: `PID PR CPU% S #THR VSS RSS PCY WCHAN PC NAME`
     */
    private fun parseTopProcessLineOld(line: String): ProcessInfo? {
        val parts = line.trim().split(Regex("\\s+"))
        if (parts.size < 9) return null

        val pid = parts[0].toIntOrNull() ?: return null
        val user = "?"
        val cpu = parts.getOrNull(2)?.trimEnd('%')?.toFloatOrNull() ?: 0f
        val state = ProcessState.fromString(parts.getOrNull(3) ?: "?")
        val vss = parseMemValue(parts.getOrNull(5) ?: "0")
        val rss = parseMemValue(parts.getOrNull(6) ?: "0")
        val name = parts.lastOrNull() ?: return null

        return buildProcessInfo(pid, user, name, state, cpu, 0f, rss, vss)
    }

    // ── Парсинг ps ──────────────────────────────────────────────────────────────

    /**
     * Разбирает вывод `ps -A -o PID,USER,PPID,NAME,S,RSS,VSZ`.
     */
    private fun parsePsOutput(output: String): List<ProcessInfo> {
        val lines = output.lines().drop(1) // пропускаем заголовок
        return lines.mapNotNull { line ->
            val parts = line.trim().split(Regex("\\s+"))
            if (parts.size < 6) return@mapNotNull null

            val pid = parts[0].toIntOrNull() ?: return@mapNotNull null
            val user = parts[1]
            val ppid = parts[2].toIntOrNull() ?: 0
            val name = parts[3]
            val state = ProcessState.fromString(parts[4])
            val rss = parts.getOrNull(5)?.toLongOrNull() ?: 0L
            val vsz = parts.getOrNull(6)?.toLongOrNull() ?: 0L

            val pkg = if (name.looksLikePackage) name else ""
            ProcessInfo(
                pid = pid,
                ppid = ppid,
                user = user,
                name = name,
                packageName = pkg,
                state = state,
                rssKb = rss,
                vszKb = vsz,
            )
        }
    }

    // ── Вспомогательные функции ───────────────────────────────────────────────

    /**
     * Строит [ProcessInfo] из распарсенных полей.
     * Определяет packageName эвристически из имени процесса.
     */
    private fun buildProcessInfo(
        pid: Int,
        user: String,
        name: String,
        state: ProcessState,
        cpuPercent: Float,
        memPercent: Float,
        rssKb: Long,
        vszKb: Long,
        ppid: Int = 0,
    ): ProcessInfo {
        val cleanName = name.trim()
        val packageName = if (cleanName.looksLikePackage) cleanName else ""
        return ProcessInfo(
            pid = pid,
            ppid = ppid,
            user = user,
            name = cleanName,
            packageName = packageName,
            state = state,
            cpuPercent = cpuPercent.coerceAtLeast(0f),
            memPercent = memPercent.coerceAtLeast(0f),
            rssKb = rssKb,
            vszKb = vszKb,
        )
    }

    /** Извлекает значение в KB из строки вида "... 3801600K total ..." */
    private fun extractKb(line: String, keyword: String): Long? {
        val pattern = Regex("(\\d+)([KMG]?)\\s*$keyword", RegexOption.IGNORE_CASE)
        val match = pattern.find(line) ?: return null
        val value = match.groupValues[1].toLongOrNull() ?: return null
        return when (match.groupValues[2].uppercase()) {
            "M" -> value * 1024L
            "G" -> value * 1024L * 1024L
            else -> value
        }
    }

    /**
     * Парсит размер памяти из `top` (форматы: "29M", "2.5M", "2048K", "1234567", "1.2G").
     * Возвращает значение в KB.
     */
    private fun parseMemValue(raw: String): Long {
        val trimmed = raw.trim().trimEnd('+')
        val match = Regex("^(\\d+(?:\\.\\d+)?)([KMGTkmgt])?$").find(trimmed) ?: return 0L
        val value = match.groupValues[1].toDoubleOrNull() ?: return 0L
        return when (match.groupValues[2].uppercase()) {
            "G", "T" -> (value * 1024 * 1024).toLong()
            "M"      -> (value * 1024).toLong()
            "K", ""  -> value.toLong()
            else     -> value.toLong()
        }
    }

    /** Эвристика "выглядит как пакет" для String (дублирует ProcessInfo.looksLikePackage). */
    private val String.looksLikePackage: Boolean
        get() = contains('.') && !startsWith('/') && !startsWith('[') && length > 3

    /** Получает данные из /proc/meminfo. Возвращает Triple(total, used, free) в KB. */
    private suspend fun fetchMemInfo(deviceId: String, adbPath: String): Triple<Long, Long, Long> {
        val result = processRunner.run(
            adbPath, "-s", deviceId, "shell", "cat", "/proc/meminfo",
        )
        if (!result.isSuccess || result.stdout.isBlank()) return Triple(0, 0, 0)
        return parseMemInfo(result.stdout)
    }

    private fun parseMemInfo(output: String): Triple<Long, Long, Long> {
        val map = mutableMapOf<String, Long>()
        for (line in output.lines()) {
            val parts = line.split(":")
            if (parts.size >= 2) {
                val key = parts[0].trim()
                val value = parts[1].trim().split(Regex("\\s+")).firstOrNull()?.toLongOrNull() ?: 0L
                map[key] = value
            }
        }
        val total = map["MemTotal"] ?: 0L
        val free = map["MemFree"] ?: 0L
        val available = map["MemAvailable"] ?: free
        val buffers = map["Buffers"] ?: 0L
        val cached = map["Cached"] ?: 0L
        val used = (total - available - buffers - cached).coerceAtLeast(0L)
        return Triple(total, used, free)
    }

    // ── Детали процесса ─────────────────────────────────────────────────────────

    override suspend fun getProcessDetails(
        deviceId: String,
        pid: Int,
        adbPath: String,
    ): Result<ProcessDetails> = runCatchingPreserveCancellation {
        // Читаем /proc/<pid>/status
        val statusResult = processRunner.run(
            adbPath, "-s", deviceId, "shell", "cat", "/proc/$pid/status",
        )

        // Читаем /proc/<pid>/cmdline
        val cmdlineResult = processRunner.run(
            adbPath, "-s", deviceId, "shell", "cat", "/proc/$pid/cmdline",
        )

        // Читаем dumpsys meminfo <pid>
        val memInfoResult = processRunner.run(
            adbPath, "-s", deviceId, "shell", "dumpsys", "meminfo", pid.toString(),
        )

        val status = if (statusResult.isSuccess) parseProcessStatus(statusResult.stdout) else emptyMap()
        val cmdline = cmdlineResult.stdout.replace('\u0000', ' ').trim()
        val memInfo = if (memInfoResult.isSuccess) parseDumpsysMemInfo(memInfoResult.stdout) else emptyMap()

        val name = status["Name"] ?: "pid/$pid"
        val packageFromName = if (name.contains('.') && !name.startsWith('/')) name else ""

        ProcessDetails(
            pid = pid,
            name = name,
            packageName = packageFromName,
            user = status["Uid"]?.split("\t")?.firstOrNull() ?: "",
            state = ProcessState.fromString(status["State"] ?: "?"),
            ppid = status["PPid"]?.toIntOrNull() ?: 0,
            threads = status["Threads"]?.toIntOrNull() ?: 0,
            cmdline = cmdline,
            rssKb = status["VmRSS"]?.toLongOrNull() ?: 0L,
            vszKb = status["VmSize"]?.toLongOrNull() ?: 0L,
            pssKb = memInfo["pss_total"] ?: 0L,
            ussKb = memInfo["private_dirty_total"] ?: 0L,
            heapSizeKb = memInfo["java_heap_size"] ?: 0L,
            heapAllocKb = memInfo["java_heap_alloc"] ?: 0L,
            heapFreeKb = memInfo["java_heap_free"] ?: 0L,
            nativeHeapSizeKb = memInfo["native_heap_size"] ?: 0L,
            nativeHeapAllocKb = memInfo["native_heap_alloc"] ?: 0L,
            openFiles = status["FDSize"]?.toIntOrNull() ?: 0,
        )
    }

    /**
     * Разбирает /proc/<pid>/status в Map<ключ, значение без единиц>.
     *
     * Формат: `Name:\tprocess_name\nState:\tS (sleeping)\nPid:\t1234`
     * Числовые значения типа "2048 kB" → "2048".
     */
    private fun parseProcessStatus(output: String): Map<String, String> {
        val result = mutableMapOf<String, String>()
        for (line in output.lines()) {
            val idx = line.indexOf(':')
            if (idx < 0) continue
            val key = line.substring(0, idx).trim()
            val raw = line.substring(idx + 1).trim()
            // Числа с единицами ("2048 kB") — берём только число
            val value = if (raw.endsWith("kB", ignoreCase = true)) {
                raw.substringBefore(' ').trim()
            } else {
                // State: "S (sleeping)" — берём строку целиком
                raw
            }
            result[key] = value
        }
        return result
    }

    /**
     * Разбирает вывод `dumpsys meminfo <pid>` в Map<ключ, KB>.
     *
     * Ищет строки:
     * - "Native Heap   SIZE   ALLOC   FREE   ..."
     * - "Dalvik Heap   SIZE   ALLOC   FREE   ..."
     * - "TOTAL         PSS    ..."
     */
    private fun parseDumpsysMemInfo(output: String): Map<String, Long> {
        val result = mutableMapOf<String, Long>()
        val lines = output.lines()
        for (line in lines) {
            val trimmed = line.trim()
            when {
                trimmed.startsWith("Native Heap", ignoreCase = true) -> {
                    val nums = trimmed.extractNumbers()
                    if (nums.size >= 3) {
                        result["native_heap_size"] = nums[0]
                        result["native_heap_alloc"] = nums[1]
                    }
                }
                trimmed.startsWith("Dalvik Heap", ignoreCase = true) ||
                    trimmed.startsWith("Java Heap", ignoreCase = true) -> {
                    val nums = trimmed.extractNumbers()
                    if (nums.size >= 3) {
                        result["java_heap_size"] = nums[0]
                        result["java_heap_alloc"] = nums[1]
                        result["java_heap_free"] = nums[2]
                    }
                }
                trimmed.startsWith("TOTAL", ignoreCase = true) -> {
                    val nums = trimmed.extractNumbers()
                    if (nums.isNotEmpty()) {
                        result["pss_total"] = nums[0]
                        if (nums.size >= 2) result["private_dirty_total"] = nums[1]
                    }
                }
            }
        }
        return result
    }

    private fun String.extractNumbers(): List<Long> =
        Regex("\\d+").findAll(this).map { it.value.toLong() }.toList()

    // ── Kill process ──────────────────────────────────────────────────────────

    override suspend fun killProcess(
        deviceId: String,
        pid: Int,
        adbPath: String,
    ): Result<Unit> = runCatchingPreserveCancellation {
        val result = processRunner.run(
            adbPath, "-s", deviceId, "shell", "kill", "-9", pid.toString(),
        )
        // kill -9 на чужой процесс вернёт stderr — не считаем это ошибкой сети,
        // но сообщаем пользователю
        if (!result.isSuccess && result.stderr.isNotBlank()) {
            error("kill -9 $pid: ${result.stderr.trim().take(150)}")
        }
    }

    // ── Хранилище ─────────────────────────────────────────────────────────────

    override suspend fun getStorageInfo(
        deviceId: String,
        adbPath: String,
    ): Result<List<StoragePartition>> = runCatchingPreserveCancellation {
        // Попытка 1: df -k (гарантированный вывод в KB)
        val dfK = processRunner.run(adbPath, "-s", deviceId, "shell", "df", "-k")
        if (dfK.isSuccess && dfK.stdout.isNotBlank()) {
            val parsed = parseDf(dfK.stdout, alreadyKb = true)
            if (parsed.isNotEmpty()) return@runCatchingPreserveCancellation parsed
        }

        // Попытка 2: df без флагов (human-readable)
        val df = processRunner.run(adbPath, "-s", deviceId, "shell", "df")
        if (!df.isSuccess || df.stdout.isBlank()) {
            error("Не удалось получить информацию о хранилище: ${df.stderr.take(200)}")
        }
        parseDf(df.stdout, alreadyKb = false)
    }

    /**
     * Разбирает вывод команды `df` (обе версии: `-k` и human-readable).
     *
     * Поддерживаемые форматы:
     *
     * Linux/Android `-k`:
     * ```
     * Filesystem       1K-blocks   Used Available Use% Mounted on
     * /dev/block/dm-0    4194304 1234567   2959737  29% /
     * ```
     *
     * Android human-readable:
     * ```
     * Filesystem              Size  Used Avail Use% Mounted on
     * /dev/block/bootdevice   2.0G  1.5G  500M  75% /system
     * ```
     *
     * Некоторые Android-версии выводят разбитые строки (filesystem на одной,
     * числа на следующей).
     *
     * @param alreadyKb Если `true`, числа интерпретируются как KB без суффикса.
     */
    private fun parseDf(output: String, alreadyKb: Boolean): List<StoragePartition> {
        val result = mutableListOf<StoragePartition>()
        val lines = output.lines()

        var pendingFilesystem: String? = null

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isBlank() ||
                trimmed.startsWith("Filesystem", ignoreCase = true) ||
                trimmed.startsWith("df:", ignoreCase = true)
            ) continue

            // Android может разбить строку: filesystem на первой, числа на следующей
            val parts = trimmed.split(Regex("\\s+"))

            if (parts.size == 1 && !trimmed.contains('%')) {
                // Только имя файловой системы, числа будут на следующей строке
                pendingFilesystem = trimmed
                continue
            }

            val (filesystem, dataparts) = if (pendingFilesystem != null) {
                Pair(pendingFilesystem!!, parts).also { pendingFilesystem = null }
            } else {
                Pair(parts[0], parts.drop(1))
            }

            if (dataparts.size < 4) continue

            // Найти индекс поля Use% (содержит '%')
            val usePctIdx = dataparts.indexOfFirst { it.endsWith('%') }
            if (usePctIdx < 0) continue

            val mountPoint = dataparts.drop(usePctIdx + 1).joinToString(" ")
            if (mountPoint.isBlank()) continue

            // Числа: total, used, available — перед Use%
            val nums = dataparts.take(usePctIdx)
            if (nums.size < 3) continue

            val total = parseStorageValue(nums[0], alreadyKb)
            val used = parseStorageValue(nums[1], alreadyKb)
            val avail = parseStorageValue(nums[2], alreadyKb)
            val usePct = dataparts[usePctIdx].trimEnd('%').toIntOrNull() ?: 0

            result.add(
                StoragePartition(
                    filesystem = filesystem,
                    totalKb = total,
                    usedKb = used,
                    freeKb = avail,
                    usedPercent = usePct,
                    mountPoint = mountPoint,
                )
            )
        }
        return result
    }

    /**
     * Парсит значение размера из `df` в KB.
     *
     * @param raw        Строка вроде "2.0G", "500M", "1234567", "4194304K".
     * @param alreadyKb  Если `true` — raw уже в KB, суффикс не нужен.
     */
    private fun parseStorageValue(raw: String, alreadyKb: Boolean): Long {
        if (raw == "-" || raw.isBlank()) return 0L
        if (alreadyKb) return raw.toLongOrNull() ?: 0L

        val match = Regex("^(\\d+(?:\\.\\d+)?)([KMGT]?)$", RegexOption.IGNORE_CASE).find(raw.trim())
            ?: return 0L
        val value = match.groupValues[1].toDoubleOrNull() ?: return 0L
        return when (match.groupValues[2].uppercase()) {
            "T" -> (value * 1024 * 1024 * 1024).toLong()
            "G" -> (value * 1024 * 1024).toLong()
            "M" -> (value * 1024).toLong()
            "K", "" -> value.toLong()
            else -> value.toLong()
        }
    }
}

package com.adbdeck.core.adb.impl

import com.adbdeck.core.adb.api.DeviceFileClient
import com.adbdeck.core.adb.api.DeviceFileEntry
import com.adbdeck.core.adb.api.DeviceFileType
import com.adbdeck.core.process.ProcessResult
import com.adbdeck.core.process.ProcessRunner
import com.adbdeck.core.utils.runCatchingPreserveCancellation

/**
 * Реализация [DeviceFileClient] поверх системного `adb`.
 *
 * Поддерживает базовые файловые операции на устройстве:
 * - чтение содержимого директории
 * - проверка существования пути
 * - mkdir / delete / rename
 * - push / pull
 *
 * Для shell-команд используется `sh -c` с передачей путей через positional arguments,
 * чтобы корректно работать с пробелами и спецсимволами в пути.
 */
class SystemDeviceFileClient(
    private val processRunner: ProcessRunner,
) : DeviceFileClient {

    private companion object {
        private const val FIELD_SEPARATOR = '\u001F'
        private const val DIR_NOT_FOUND_PREFIX = "__ERR__NOT_FOUND__:"
        private const val NOT_DIRECTORY_PREFIX = "__ERR__NOT_DIRECTORY__:"
        private const val PERMISSION_DENIED_PREFIX = "__ERR__PERMISSION_DENIED__:"
        private const val DEBUG_ENV = "ADBDECK_DEBUG_ADB_FILES"
        private const val DEBUG_PROP = "adbdeck.debug.adb.files"

        /**
         * Shell-скрипт листинга директории.
         *
         * Формат строки результата:
         * `name<US>type<US>sizeBytes<US>mtimeEpochSeconds`
         * где `<US>` — символ Unit Separator (0x1F).
         */
        private val LIST_DIRECTORY_SCRIPT = """
            dir="${'$'}1"
            probe=${'$'}(ls -ld "${'$'}dir" 2>&1)
            probe_code=${'$'}?
            if [ ${'$'}probe_code -ne 0 ]; then
              case "${'$'}probe" in
                *"No such file"*|*"not found"*)
                  echo "__ERR__NOT_FOUND__:${'$'}dir"
                  exit 3
                  ;;
                *"Permission denied"*)
                  echo "__ERR__PERMISSION_DENIED__:${'$'}dir"
                  exit 5
                  ;;
                *)
                  echo "__ERR__PERMISSION_DENIED__:${'$'}dir"
                  echo "${'$'}probe" 1>&2
                  exit 7
                  ;;
              esac
            fi

            if [ ! -d "${'$'}dir" ]; then
              echo "__ERR__NOT_DIRECTORY__:${'$'}dir"
              exit 4
            fi

            if [ ! -r "${'$'}dir" ] || [ ! -x "${'$'}dir" ]; then
              echo "__ERR__PERMISSION_DENIED__:${'$'}dir"
              exit 5
            fi

            if ! ls -A "${'$'}dir" >/dev/null 2>&1; then
              echo "__ERR__PERMISSION_DENIED__:${'$'}dir"
              exit 6
            fi

            for entry in "${'$'}dir"/* "${'$'}dir"/.[!.]* "${'$'}dir"/..?*; do
              [ -e "${'$'}entry" ] || continue

              name=${'$'}(basename "${'$'}entry")
              if [ -d "${'$'}entry" ]; then
                type="d"
              elif [ -f "${'$'}entry" ]; then
                type="f"
              elif [ -L "${'$'}entry" ]; then
                type="l"
              else
                type="o"
              fi

              size=""
              mtime=""
              stat_pair=""
              if command -v toybox >/dev/null 2>&1; then
                stat_pair=${'$'}(toybox stat -c '%s|%Y' "${'$'}entry" 2>/dev/null || true)
              fi
              if [ -z "${'$'}stat_pair" ]; then
                stat_pair=${'$'}(stat -c '%s|%Y' "${'$'}entry" 2>/dev/null || stat -f '%z|%m' "${'$'}entry" 2>/dev/null || true)
              fi

              if [ -n "${'$'}stat_pair" ]; then
                stat_size=${'$'}{stat_pair%%|*}
                stat_mtime=${'$'}{stat_pair##*|}
                mtime="${'$'}stat_mtime"
                if [ "${'$'}type" = "f" ]; then
                  size="${'$'}stat_size"
                fi
              fi

              printf '%s\037%s\037%s\037%s\n' "${'$'}name" "${'$'}type" "${'$'}size" "${'$'}mtime"
            done
        """.trimIndent()
    }

    /** Включает подробные логи формирования adb shell-команд для диагностики. */
    private val debugEnabled: Boolean =
        java.lang.System.getenv(DEBUG_ENV) == "1" ||
            java.lang.Boolean.getBoolean(DEBUG_PROP)

    override suspend fun listDirectory(
        deviceId: String,
        directoryPath: String,
        adbPath: String,
    ): Result<List<DeviceFileEntry>> = runCatchingPreserveCancellation {
        val result = runAdbShell(deviceId, adbPath, LIST_DIRECTORY_SCRIPT, directoryPath)

        if (!result.isSuccess) {
            error(buildDirectoryErrorMessage(directoryPath, result))
        }

        parseDirectoryListing(directoryPath, result.stdout)
            .sortedWith(compareByDescending<DeviceFileEntry> { it.isDirectory }.thenBy { it.name.lowercase() })
    }

    override suspend fun exists(
        deviceId: String,
        path: String,
        adbPath: String,
    ): Result<Boolean> = runCatchingPreserveCancellation {
        val result = runAdbShell(deviceId, adbPath, "[ -e \"$1\" ]", path)
        result.isSuccess
    }

    override suspend fun createDirectory(
        deviceId: String,
        directoryPath: String,
        adbPath: String,
    ): Result<Unit> = runCatchingPreserveCancellation {
        val result = runAdbShell(deviceId, adbPath, "mkdir -p \"$1\"", directoryPath)
        if (!result.isSuccess) {
            val message = result.stderr.ifBlank { result.stdout }.trim().ifBlank { "Не удалось создать директорию" }
            error(message)
        }
    }

    override suspend fun delete(
        deviceId: String,
        path: String,
        recursive: Boolean,
        adbPath: String,
    ): Result<Unit> = runCatchingPreserveCancellation {
        val script = if (recursive) "rm -rf -- \"$1\"" else "rm -f -- \"$1\""
        val result = runAdbShell(deviceId, adbPath, script, path)
        if (!result.isSuccess) {
            val message = result.stderr.ifBlank { result.stdout }.trim().ifBlank { "Не удалось удалить путь '$path'" }
            error(message)
        }
    }

    override suspend fun rename(
        deviceId: String,
        sourcePath: String,
        targetPath: String,
        adbPath: String,
    ): Result<Unit> = runCatchingPreserveCancellation {
        val result = runAdbShell(deviceId, adbPath, "mv \"$1\" \"$2\"", sourcePath, targetPath)
        if (!result.isSuccess) {
            val message = result.stderr.ifBlank { result.stdout }.trim().ifBlank {
                "Не удалось переименовать '$sourcePath'"
            }
            error(message)
        }
    }

    override suspend fun push(
        deviceId: String,
        localPath: String,
        remotePath: String,
        adbPath: String,
    ): Result<Unit> = runCatchingPreserveCancellation {
        val result = processRunner.run(adbPath, "-s", deviceId, "push", localPath, remotePath)
        if (!result.isSuccess) {
            val message = result.stderr.ifBlank { result.stdout }
                .lineSequence()
                .firstOrNull { it.isNotBlank() }
                ?.trim()
                .orEmpty()
            error("Push не выполнен: ${message.ifBlank { "unknown error" }}")
        }
    }

    override suspend fun pull(
        deviceId: String,
        remotePath: String,
        localPath: String,
        adbPath: String,
    ): Result<Unit> = runCatchingPreserveCancellation {
        val result = processRunner.run(adbPath, "-s", deviceId, "pull", remotePath, localPath)
        if (!result.isSuccess) {
            val message = result.stderr.ifBlank { result.stdout }
                .lineSequence()
                .firstOrNull { it.isNotBlank() }
                ?.trim()
                .orEmpty()
            error("Pull не выполнен: ${message.ifBlank { "unknown error" }}")
        }
    }

    /** Выполняет shell-скрипт на устройстве через `adb shell sh -c <script>`. */
    private suspend fun runAdbShell(
        deviceId: String,
        adbPath: String,
        script: String,
        vararg args: String,
    ): ProcessResult {
        val interpolatedScript = interpolateShellArgs(script, args)
        val quotedScript = shellQuote(interpolatedScript)
        val command = buildList {
            add(adbPath)
            add("-s")
            add(deviceId)
            add("shell")
            add("sh")
            add("-c")
            // Важно: adb shell склеивает аргументы в одну строку для удаленного shell.
            // Если не экранировать целый скрипт, пробелы/переносы разорвут `-c` аргумент.
            add(quotedScript)
        }

        debugLog(
            buildString {
                append("runAdbShell: ")
                append(command.take(6).joinToString(" "))
                append(" <script>")
                appendLine()
                append("script=")
                append(interpolatedScript)
            }
        )

        val result = processRunner.run(command)
        debugLog(
            "runAdbShell result: exitCode=${result.exitCode}, " +
                "stdout=\"${truncateForLog(oneLine(result.stdout))}\", " +
                "stderr=\"${truncateForLog(oneLine(result.stderr))}\""
        )
        return result
    }

    /**
     * Подставляет аргументы в shell-скрипт как безопасно экранированные литералы.
     *
     * Важно: `adb shell sh -c` не всегда корректно прокидывает позиционные аргументы
     * (`$1`, `$2`) на разных версиях Android shell. Поэтому подставляем значения
     * заранее на стороне desktop-приложения.
     */
    private fun interpolateShellArgs(
        script: String,
        args: Array<out String>,
    ): String {
        var result = script
        for (index in args.indices.reversed()) {
            val tokenNumber = index + 1
            val token = "$$tokenNumber"
            val quoted = shellQuote(args[index])

            // Сначала заменяем шаблоны в кавычках: "$1" -> '/path'
            result = result.replace("\"$token\"", quoted)
            result = result.replace("'$token'", quoted)

            // Затем остаточные вхождения: $1 -> '/path'
            val key = Regex("\\$${tokenNumber}(?!\\d)")
            result = result.replace(key, quoted)
        }
        return result
    }

    /** Экранирует строку для безопасной вставки в shell-команду. */
    private fun shellQuote(value: String): String =
        "'" + value.replace("'", "'\"'\"'") + "'"

    /** Печатает debug-лог в stderr, если включен флаг диагностики. */
    private fun debugLog(message: String) {
        if (!debugEnabled) return
        System.err.println("[ADB-FILES] $message")
    }

    /** Уплощает многострочный текст в одну строку для компактных логов. */
    private fun oneLine(value: String): String =
        value.replace('\n', ' ').replace('\r', ' ').trim()

    /** Ограничивает длину лога, чтобы не засорять вывод. */
    private fun truncateForLog(value: String, maxLength: Int = 600): String =
        if (value.length <= maxLength) value else value.take(maxLength) + "…"

    /** Парсит stdout shell-листинга в список [DeviceFileEntry]. */
    private fun parseDirectoryListing(parentPath: String, output: String): List<DeviceFileEntry> {
        if (output.isBlank()) return emptyList()

        return output.lineSequence()
            .mapNotNull { rawLine ->
                val line = rawLine.trimEnd()
                if (line.isBlank() || isDirectoryMarkerLine(line)) return@mapNotNull null

                val parts = line.split(FIELD_SEPARATOR, limit = 4)
                if (parts.size < 2) return@mapNotNull null

                val name = parts[0].trim()
                if (name.isBlank()) return@mapNotNull null

                val type = when (parts[1].trim()) {
                    "d" -> DeviceFileType.DIRECTORY
                    "f" -> DeviceFileType.FILE
                    "l" -> DeviceFileType.SYMLINK
                    else -> DeviceFileType.OTHER
                }

                val size = parts.getOrNull(2)
                    ?.trim()
                    ?.takeIf { it.isNotEmpty() }
                    ?.toLongOrNull()

                val modifiedEpochSeconds = parts.getOrNull(3)
                    ?.trim()
                    ?.takeIf { it.isNotEmpty() }
                    ?.toLongOrNull()

                DeviceFileEntry(
                    name = name,
                    fullPath = resolveChildPath(parentPath, name),
                    type = type,
                    sizeBytes = size,
                    modifiedEpochSeconds = modifiedEpochSeconds,
                )
            }
            .toList()
    }

    /** Формирует детальную ошибку чтения директории с категорией и ответом ADB. */
    private fun buildDirectoryErrorMessage(
        requestedPath: String,
        result: ProcessResult,
    ): String {
        val markerLine = result.stdout
            .lineSequence()
            .map { it.trim() }
            .firstOrNull(::isDirectoryMarkerLine)

        val category = when {
            markerLine?.startsWith(DIR_NOT_FOUND_PREFIX) == true -> {
                val path = markerLine.removePrefix(DIR_NOT_FOUND_PREFIX).ifBlank { requestedPath }
                "путь не найден ($path)"
            }

            markerLine?.startsWith(NOT_DIRECTORY_PREFIX) == true -> {
                val path = markerLine.removePrefix(NOT_DIRECTORY_PREFIX).ifBlank { requestedPath }
                "путь не является директорией ($path)"
            }

            markerLine?.startsWith(PERMISSION_DENIED_PREFIX) == true -> {
                val path = markerLine.removePrefix(PERMISSION_DENIED_PREFIX).ifBlank { requestedPath }
                "нет прав доступа к директории ($path)"
            }

            else -> inferCategoryFromAdbOutput(result)
        }

        val reason = firstMeaningfulErrorLine(result)
            ?.take(220)
            ?: "детали не получены"

        return "Не удалось открыть директорию '$requestedPath'. " +
            "Категория: $category. Причина: $reason. ${buildAdbDiagnostics(result)}"
    }

    /** Пробует вывести категорию ошибки по stderr/stdout, если marker недоступен. */
    private fun inferCategoryFromAdbOutput(result: ProcessResult): String {
        val probe = buildString {
            appendLine(result.stderr)
            appendLine(result.stdout)
        }.lowercase()

        return when {
            "permission denied" in probe -> "нет прав доступа"
            "not a directory" in probe -> "путь не является директорией"
            "no such file" in probe || "cannot access" in probe -> "путь не найден"
            "device offline" in probe || "device not found" in probe || "no devices/emulators found" in probe ->
                "устройство недоступно для ADB"

            else -> "ошибка ADB/shell"
        }
    }

    /** Первая осмысленная строка из stderr/stdout без служебных marker-строк. */
    private fun firstMeaningfulErrorLine(result: ProcessResult): String? {
        val stderrLine = result.stderr
            .lineSequence()
            .firstOrNull { it.isNotBlank() }
            ?.trim()
        if (!stderrLine.isNullOrBlank()) return stderrLine

        return result.stdout
            .lineSequence()
            .map { it.trim() }
            .firstOrNull { it.isNotBlank() && !isDirectoryMarkerLine(it) }
    }

    /** Короткая диагностика ответа ADB для отображения пользователю. */
    private fun buildAdbDiagnostics(result: ProcessResult): String {
        val stderr = result.stderr
            .lineSequence()
            .firstOrNull { it.isNotBlank() }
            ?.trim()
            ?.take(220)

        val stdout = result.stdout
            .lineSequence()
            .map { it.trim() }
            .firstOrNull { it.isNotBlank() && !isDirectoryMarkerLine(it) }
            ?.take(220)

        val details = buildList {
            add("exitCode=${result.exitCode}")
            if (!stderr.isNullOrBlank()) add("stderr=\"$stderr\"")
            if (!stdout.isNullOrBlank()) add("stdout=\"$stdout\"")
        }

        return "ADB: ${details.joinToString(", ")}"
    }

    /** Проверка, является ли строка служебным marker-кодом ошибки директории. */
    private fun isDirectoryMarkerLine(line: String): Boolean =
        line.startsWith(DIR_NOT_FOUND_PREFIX) ||
            line.startsWith(NOT_DIRECTORY_PREFIX) ||
            line.startsWith(PERMISSION_DENIED_PREFIX)

    /** Склеивает путь дочернего элемента без дублирования `/`. */
    private fun resolveChildPath(parent: String, name: String): String {
        val normalizedParent = parent.ifBlank { "/" }.trimEnd('/').ifBlank { "/" }
        return if (normalizedParent == "/") "/$name" else "$normalizedParent/$name"
    }
}

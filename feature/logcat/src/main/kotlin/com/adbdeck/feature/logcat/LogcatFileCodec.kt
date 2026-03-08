package com.adbdeck.feature.logcat

import com.adbdeck.core.adb.api.logcat.LogcatEntry
import com.adbdeck.core.adb.api.logcat.LogcatLevel
import com.adbdeck.core.adb.api.logcat.LogcatParser
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.longOrNull

/**
 * Результат импорта logcat-файла.
 *
 * @param entries               Импортированные строки в формате [LogcatEntry].
 * @param packageIndexByEntryId Индекс `entry.id -> package/process` для package-фильтра в file mode.
 */
internal data class ImportedLogcatFile(
    val entries: List<LogcatEntry>,
    val packageIndexByEntryId: Map<Long, Set<String>>,
)

/**
 * Кодек импорта/экспорта файлов Logcat.
 *
 * Поддерживает:
 * - обычный текстовый формат `threadtime` (строка на запись);
 * - JSON-файл Android Studio (`.logcat`) с массивом `logcatMessages`.
 */
internal object LogcatFileCodec {

    private val json = Json { ignoreUnknownKeys = true }
    private val zoneId = ZoneId.systemDefault()
    private val dateFormatter = DateTimeFormatter.ofPattern("MM-dd")
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")

    /**
     * Импортирует содержимое logcat-файла в универсальный формат UI.
     *
     * Сначала пробует Android Studio JSON, затем fallback на plain-text `threadtime`.
     */
    fun importText(content: String): ImportedLogcatFile {
        val normalized = content.removePrefix("\uFEFF")
        parseAndroidStudioJson(normalized)?.let { return it }
        return parseThreadtimeText(normalized)
    }

    /**
     * Формирует текст для экспорта на диск.
     *
     * Источник — текущий буфер записей из state. Если у строки есть [LogcatEntry.raw],
     * используется он; иначе строится fallback-строка в формате `threadtime`.
     */
    fun exportAsText(entries: List<LogcatEntry>): String {
        if (entries.isEmpty()) return ""
        return entries.joinToString(separator = System.lineSeparator()) { entry ->
            val raw = entry.raw.trimEnd('\r', '\n')
            if (raw.isNotBlank()) {
                raw
            } else {
                formatFallbackEntry(entry)
            }
        }
    }

    private fun parseThreadtimeText(content: String): ImportedLogcatFile {
        val entries = content
            .lineSequence()
            .map { it.trimEnd('\r') }
            .filter { it.isNotBlank() }
            .map(LogcatParser::parse)
            .toList()
        return ImportedLogcatFile(
            entries = entries,
            packageIndexByEntryId = emptyMap(),
        )
    }

    /**
     * Импорт JSON-формата Android Studio:
     * `{ metadata: ..., logcatMessages: [{ header: ..., message: ... }] }`.
     */
    private fun parseAndroidStudioJson(content: String): ImportedLogcatFile? {
        val root = runCatching {
            json.parseToJsonElement(content).jsonObject
        }.getOrNull() ?: return null

        val messages = root["logcatMessages"].asJsonArrayOrNull() ?: return null

        val entries = ArrayList<LogcatEntry>(messages.size)
        val packageIndex = mutableMapOf<Long, Set<String>>()

        messages.forEach { messageElement ->
            val messageObject = messageElement.asJsonObjectOrNull() ?: return@forEach
            val header = messageObject["header"].asJsonObjectOrNull() ?: return@forEach

            val timestamp = header["timestamp"].asJsonObjectOrNull()
            val (date, time, millis) = timestampToThreadtime(timestamp)

            val pid = header["pid"].asJsonPrimitiveContent().orEmpty().ifBlank { "0" }
            val tid = header["tid"].asJsonPrimitiveContent().orEmpty().ifBlank { "0" }
            val level = androidStudioLevelToChar(header["logLevel"].asJsonPrimitiveContent())
            val tag = header["tag"].asJsonPrimitiveContent().orEmpty().ifBlank { "Logcat" }
            val message = sanitizeMessage(messageObject["message"].asJsonPrimitiveContent().orEmpty())

            val rawLine = buildThreadtimeLine(
                date = date,
                time = time,
                millis = millis,
                pid = pid,
                tid = tid,
                level = level,
                tag = tag,
                message = message,
            )
            val entry = LogcatParser.parse(rawLine)
            entries += entry

            val packageCandidates = buildPackageCandidates(
                applicationId = header["applicationId"].asJsonPrimitiveContent(),
                processName = header["processName"].asJsonPrimitiveContent(),
            )
            if (packageCandidates.isNotEmpty()) {
                packageIndex[entry.id] = packageCandidates
            }
        }

        return ImportedLogcatFile(
            entries = entries,
            packageIndexByEntryId = packageIndex,
        )
    }

    private fun formatFallbackEntry(entry: LogcatEntry): String {
        if (entry.date.isBlank() || entry.time.isBlank()) {
            return sanitizeMessage(entry.message.ifBlank { entry.tag })
        }

        val level = if (entry.level == LogcatLevel.UNKNOWN) '?' else entry.level.code
        return buildThreadtimeLine(
            date = entry.date,
            time = entry.time,
            millis = entry.millis.ifBlank { "000" },
            pid = entry.pid.ifBlank { "0" },
            tid = entry.tid.ifBlank { "0" },
            level = level,
            tag = entry.tag.ifBlank { "Logcat" },
            message = sanitizeMessage(entry.message),
        )
    }

    private fun timestampToThreadtime(timestamp: JsonObject?): Triple<String, String, String> {
        val seconds = timestamp?.get("seconds").asLongOrNull()
        val nanos = timestamp?.get("nanos").asLongOrNull() ?: 0L
        if (seconds == null) {
            return Triple("01-01", "00:00:00", "000")
        }

        val instant = runCatching { Instant.ofEpochSecond(seconds, nanos) }.getOrNull()
            ?: return Triple("01-01", "00:00:00", "000")

        val zonedDateTime = instant.atZone(zoneId)
        return Triple(
            zonedDateTime.format(dateFormatter),
            zonedDateTime.format(timeFormatter),
            (zonedDateTime.nano / 1_000_000).toString().padStart(3, '0'),
        )
    }

    private fun buildThreadtimeLine(
        date: String,
        time: String,
        millis: String,
        pid: String,
        tid: String,
        level: Char,
        tag: String,
        message: String,
    ): String = buildString {
        append(date)
        append(' ')
        append(time)
        append('.')
        append(millis.takeLast(3).padStart(3, '0'))
        append(' ')
        append(pid.trim().ifEmpty { "0" }.padStart(5, ' '))
        append(' ')
        append(tid.trim().ifEmpty { "0" }.padStart(5, ' '))
        append(' ')
        append(level)
        append(' ')
        append(tag)
        append(": ")
        append(message)
    }

    private fun buildPackageCandidates(
        applicationId: String?,
        processName: String?,
    ): Set<String> = linkedSetOf<String>().apply {
        addCandidate(applicationId)
        addCandidate(processName)
    }

    private fun MutableSet<String>.addCandidate(value: String?) {
        val normalized = value?.trim().orEmpty()
        if (normalized.isBlank()) return
        add(normalized)
        add(normalized.substringBefore(':'))
    }

    private fun androidStudioLevelToChar(level: String?): Char = when (level?.trim()?.uppercase()) {
        "VERBOSE" -> 'V'
        "DEBUG" -> 'D'
        "INFO" -> 'I'
        "WARN", "WARNING" -> 'W'
        "ERROR" -> 'E'
        "ASSERT", "FATAL" -> 'F'
        "SILENT" -> 'S'
        else -> '?'
    }

    private fun sanitizeMessage(message: String): String = message
        .replace('\r', ' ')
        .replace('\n', ' ')

    private fun JsonElement?.asJsonArrayOrNull(): JsonArray? = this as? JsonArray
    private fun JsonElement?.asJsonObjectOrNull(): JsonObject? = this as? JsonObject
    private fun JsonElement?.asLongOrNull(): Long? = (this as? JsonPrimitive)?.longOrNull
    private fun JsonElement?.asJsonPrimitiveContent(): String? = (this as? JsonPrimitive)?.contentOrNull
}

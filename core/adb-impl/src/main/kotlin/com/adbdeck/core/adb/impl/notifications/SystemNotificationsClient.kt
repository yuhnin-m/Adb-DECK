package com.adbdeck.core.adb.impl.notifications

import com.adbdeck.core.adb.api.notifications.NotificationRecord
import com.adbdeck.core.adb.api.notifications.NotificationPostRequest
import com.adbdeck.core.adb.api.notifications.NotificationPostStyle
import com.adbdeck.core.adb.api.notifications.NotificationsClient
import com.adbdeck.core.process.ProcessRunner
import com.adbdeck.core.utils.runCatchingPreserveCancellation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.collections.iterator

/**
 * Реализация [com.adbdeck.core.adb.api.notifications.NotificationsClient] через `adb shell dumpsys notification`.
 *
 * Парсинг выполняется по принципу best-effort:
 * - Формат вывода `dumpsys notification` варьируется между версиями Android и вендорами.
 * - Если поле не удалось извлечь, используется null или значение по умолчанию.
 * - [parseBlock] возвращает null только если packageName отсутствует в блоке.
 * - Исходный блок дампа всегда сохраняется в [com.adbdeck.core.adb.api.notifications.NotificationRecord.rawBlock].
 *
 * @param processRunner Исполнитель внешних процессов.
 */
class SystemNotificationsClient(
    private val processRunner: ProcessRunner,
) : NotificationsClient {

    override suspend fun getNotifications(
        deviceId: String,
        adbPath: String,
    ): Result<List<NotificationRecord>> = runCatchingPreserveCancellation {
        val noRedactResult = processRunner.run(
            listOf(adbPath, "-s", deviceId, "shell", "dumpsys", "notification", "--noredact")
        )
        val output = when {
            // На новых Android `--noredact` дает не замаскированные title/text/extras.
            noRedactResult.isSuccess && noRedactResult.stdout.isNotBlank() -> noRedactResult.stdout

            else -> {
                val fallbackResult = processRunner.run(
                    listOf(adbPath, "-s", deviceId, "shell", "dumpsys", "notification")
                )
                if (!fallbackResult.isSuccess && fallbackResult.stdout.isBlank()) {
                    val reason = fallbackResult.stderr
                        .ifBlank { noRedactResult.stderr }
                        .ifBlank { "Не удалось выполнить dumpsys notification" }
                        .trim()
                    error("Не удалось получить уведомления: ${reason.take(300)}")
                }
                fallbackResult.stdout
            }
        }

        // Парсинг может быть заметно тяжелым на больших дампах, поэтому уводим в background.
        withContext(Dispatchers.Default) {
            parseNotificationDump(output)
        }
    }

    override suspend fun postNotification(
        deviceId: String,
        request: NotificationPostRequest,
        adbPath: String,
    ): Result<Unit> = runCatchingPreserveCancellation {
        val tag = request.tag.trim()
        val text = request.text.trim()
        require(tag.isNotEmpty()) { "Tag уведомления не должен быть пустым." }
        require(text.isNotEmpty()) { "Текст уведомления не должен быть пустым." }

        val command = mutableListOf(
            adbPath,
            "-s",
            deviceId,
            "shell",
            "cmd",
            "notification",
            "post",
        )

        if (request.verbose) {
            command += "--verbose"
        }

        request.title?.trim()?.takeIf { it.isNotEmpty() }?.let { title ->
            command += "--title"
            command += title
        }

        request.iconSpec?.trim()?.takeIf { it.isNotEmpty() }?.let { icon ->
            command += "--icon"
            command += icon
        }

        request.largeIconSpec?.trim()?.takeIf { it.isNotEmpty() }?.let { icon ->
            command += "--large-icon"
            command += icon
        }

        appendStyleArguments(command = command, request = request)

        request.contentIntentSpec?.trim()?.takeIf { it.isNotEmpty() }?.let { rawIntentSpec ->
            val intentTokens = parseShellTokens(rawIntentSpec)
            require(intentTokens.isNotEmpty()) {
                "Intent spec для --content-intent не должен быть пустым."
            }
            command += "--content-intent"
            command += intentTokens
        }

        command += tag
        command += text

        val result = processRunner.run(command)
        if (!result.isSuccess) {
            val reason = result.stderr
                .ifBlank { result.stdout }
                .ifBlank { "Не удалось выполнить cmd notification post." }
            error("Не удалось отправить уведомление: ${reason.take(400)}")
        }
    }

    // ── Парсинг дампа ────────────────────────────────────────────────────────

    /**
     * Разбить вывод `dumpsys notification` на список записей уведомлений.
     */
    private fun parseNotificationDump(output: String): List<NotificationRecord> =
        splitIntoBlocks(output).mapNotNull { block -> parseBlock(block) }

    /**
     * Разделить весь вывод dumpsys на блоки, каждый из которых соответствует одному уведомлению.
     * Граница блока — строка, содержащая "NotificationRecord(".
     */
    private fun splitIntoBlocks(output: String): List<String> {
        val lines = output.lines()
        val blocks = mutableListOf<String>()
        val current = StringBuilder()

        for (line in lines) {
            if (line.contains("NotificationRecord(") && current.isNotEmpty()) {
                blocks.add(current.toString())
                current.clear()
            }
            if (line.contains("NotificationRecord(") || current.isNotEmpty()) {
                current.appendLine(line)
            }
        }
        if (current.isNotEmpty()) blocks.add(current.toString())

        return blocks
    }

    /**
     * Извлечь [NotificationRecord] из одного блока дампа.
     * Возвращает null, если packageName не удалось определить.
     */
    private fun parseBlock(block: String): NotificationRecord? {
        // packageName обязателен
        val packageName = PKG_RE.find(block)?.groupValues?.get(1) ?: return null

        val notificationId = ID_RE.find(block)?.groupValues?.get(1)?.toIntOrNull() ?: 0
        val rawTag         = TAG_RE.find(block)?.groupValues?.get(1)
        val tag            = if (rawTag == "null" || rawTag.isNullOrBlank()) null else rawTag
        val importance     = IMP_RE.find(block)?.groupValues?.get(1)?.toIntOrNull() ?: 0
        val channelId      = CHANNEL_RE.find(block)?.groupValues?.get(1)
        val postedAt       = WHEN_RE.find(block)?.groupValues?.get(1)?.toLongOrNull()
        val rawGroup       = GROUP_RE.find(block)?.groupValues?.get(1)
        val group          = if (rawGroup == "null" || rawGroup.isNullOrBlank()) null else rawGroup
        val rawSortKey     = SORT_RE.find(block)?.groupValues?.get(1)
        val sortKey        = if (rawSortKey == "null" || rawSortKey.isNullOrBlank()) null else rawSortKey

        // Флаги
        val flagsHex   = FLAGS_RE.find(block)?.groupValues?.get(1)
        val flags      = flagsHex?.toLong(16)?.toInt() ?: 0
        val isOngoing  = (flags and FLAG_ONGOING) != 0
        val isClearable = !isOngoing && (flags and FLAG_NO_CLEAR) == 0

        // Составной ключ
        val rawKey = KEY_RE.find(block)?.groupValues?.get(1)
        val key = if (!rawKey.isNullOrBlank() && rawKey != "null") {
            rawKey
        } else {
            "0|$packageName|$notificationId|$tag|0"
        }

        // Ключ-значение из дампа (линии + extras Bundle).
        val keyValueMap = parseKeyValues(block)
        val textFields = parseTextFields(keyValueMap)
        val title = textFields["title"]
        val text = textFields["text"]
        val subText = textFields["subText"]
        val bigText = textFields["bigText"]
        val summaryText = textFields["summaryText"]
        val category = textFields["category"]

        val actionsCount = parseActionsCount(block)
        val actionTitles = parseActionTitles(block)
        val imageParameters = parseImageParameters(block, keyValueMap)

        return NotificationRecord(
            key = key,
            packageName = packageName,
            notificationId = notificationId,
            tag = tag,
            importance = importance,
            channelId = channelId,
            title = title,
            text = text,
            subText = subText,
            bigText = bigText,
            summaryText = summaryText,
            category = category,
            flags = flags,
            isOngoing = isOngoing,
            isClearable = isClearable,
            postedAt = postedAt,
            group = group,
            sortKey = sortKey,
            actionsCount = actionsCount,
            actionTitles = actionTitles,
            imageParameters = imageParameters,
            rawBlock = block,
        )
    }

    /**
     * Извлечь текстовые поля уведомления (title/text/subText/bigText/...).
     */
    private fun parseTextFields(keyValueMap: Map<String, String>): Map<String, String> {
        val result = LinkedHashMap<String, String>()
        for ((rawField, canonicalField) in TEXT_FIELD_ALIASES) {
            val value = keyValueMap[rawField] ?: continue
            if (!result.containsKey(canonicalField)) {
                result[canonicalField] = value
            }
        }
        return result
    }

    /**
     * Извлечь key-value параметры из строк блока и `extras = Bundle[{...}]`.
     */
    private fun parseKeyValues(block: String): Map<String, String> {
        val result = LinkedHashMap<String, String>()

        for (line in block.lines()) {
            val trimmed = line.trim()
            val match = KEY_VALUE_LINE_RE.matchEntire(trimmed) ?: continue
            val key = match.groupValues[1]
            val rawValue = match.groupValues[2]
            val cleaned = cleanFieldValue(rawValue) ?: continue
            if (!result.containsKey(key)) {
                result[key] = cleaned
            }
        }

        parseBundleExtras(block, result)
        return result
    }

    /**
     * Парсить extras из Bundle-строки вида:
     *   extras = Bundle[{android.title=My Title, android.text=My Text, ...}]
     *
     * Встречается в Android 10+ когда extras умещаются в одну строку.
     * Заполняет [result], не перезаписывая уже найденные ключи.
     */
    private fun parseBundleExtras(block: String, result: MutableMap<String, String>) {
        val bundleMatch = BUNDLE_EXTRAS_RE.find(block) ?: return
        val bundleContent = bundleMatch.groupValues[1]
        // Разобрать "key=value" пары внутри Bundle[{...}]
        // Значения могут содержать запятые внутри (нестрого, парсим жадно до следующего ", key=")
        val pairs = BUNDLE_PAIR_RE.findAll(bundleContent)
        for (pair in pairs) {
            val key   = pair.groupValues[1]
            val value = pair.groupValues[2].trimEnd(',', ' ')
            val cleaned = cleanFieldValue(value)
            if (cleaned != null && !result.containsKey(key)) {
                result[key] = cleaned
            }
        }
    }

    /**
     * Извлекает количество действий в уведомлении (`actions=2` и похожие варианты).
     */
    private fun parseActionsCount(block: String): Int? =
        ACTIONS_COUNT_RE.find(block)?.groupValues?.get(1)?.toIntOrNull()

    /**
     * Извлекает подписи action-кнопок из блока уведомления (best-effort).
     */
    private fun parseActionTitles(block: String): List<String> {
        val titles = LinkedHashSet<String>()

        for (line in block.lines()) {
            val trimmed = line.trim()
            if (trimmed.isEmpty()) continue

            val indexedQuoted = ACTION_INDEX_QUOTED_RE.find(trimmed)?.groupValues?.get(1)
            if (!indexedQuoted.isNullOrBlank()) {
                val normalized = normalizeInlineValue(indexedQuoted)
                if (normalized != null) titles += normalized
                continue
            }

            val explicitActionTitle = ACTION_TITLE_DIRECT_RE.find(trimmed)?.groupValues?.get(1)
            if (!explicitActionTitle.isNullOrBlank()) {
                val normalized = normalizeInlineValue(explicitActionTitle)
                if (normalized != null) titles += normalized
                continue
            }

            if (!ACTION_LINE_HINT_RE.containsMatchIn(trimmed)) continue

            val titleFromActionLine = ACTION_TITLE_IN_LINE_RE.find(trimmed)?.groupValues?.get(1)
            if (!titleFromActionLine.isNullOrBlank()) {
                val normalized = normalizeInlineValue(titleFromActionLine)
                if (normalized != null) titles += normalized
            }
        }

        return titles.toList()
    }

    /**
     * Извлекает параметры визуализации уведомления: иконки/картинки/template/contentView.
     */
    private fun parseImageParameters(
        block: String,
        keyValueMap: Map<String, String>,
    ): Map<String, String> {
        val result = LinkedHashMap<String, String>()

        // 1) Значения из key-value строк и extras Bundle.
        for ((key, value) in keyValueMap) {
            if (isImageParameterKey(key)) {
                result[key] = value
            }
        }

        // 2) In-line параметры внутри строк Notification(... key=value, ...).
        for (match in INLINE_KEY_VALUE_RE.findAll(block)) {
            val key = match.groupValues[1]
            if (!isImageParameterKey(key) || result.containsKey(key)) continue

            val rawValue = match.groupValues[2]
            val normalized = normalizeInlineValue(rawValue) ?: continue
            result[key] = normalized
        }

        return result
    }

    /**
     * Проверяет, относится ли ключ к визуальным параметрам уведомления.
     */
    private fun isImageParameterKey(key: String): Boolean {
        val lowered = key.lowercase()
        return IMAGE_KEYWORDS.any { lowered.contains(it) }
    }

    /**
     * Очистить "сырое" значение поля:
     * - Вернуть null, если это аннотация типа без значения ("String [length=21]")
     * - Обрезать тип-префикс, если значение вида "String [length=21]: actual value"
     * - Вернуть null для "null" и пустых строк
     */
    private fun cleanFieldValue(raw: String): String? {
        if (raw.isBlank() || raw == "null") return null
        // Полная аннотация типа без значения: "String [length=21]", "CharSequence [length=5]"
        if (TYPE_ANNOTATION_RE.matches(raw)) return null
        // Аннотация с реальным значением: "String [length=21]: My Title"
        val prefixed = TYPE_PREFIX_RE.find(raw)
        if (prefixed != null) {
            val actual = prefixed.groupValues[1].trim()
            return if (actual.isBlank() || actual == "null") null else actual
        }
        return raw.trim()
    }

    /**
     * Нормализует inline-значения из одной строки (обрезка кавычек/разделителей).
     */
    private fun normalizeInlineValue(raw: String): String? {
        var value = raw.trim()
        value = value.trimEnd(',', ';')
        if (value.endsWith(")") && value.count { it == '(' } < value.count { it == ')' }) {
            value = value.dropLast(1).trimEnd()
        }
        if (value.length >= 2 && value.first() == '"' && value.last() == '"') {
            value = value.substring(1, value.length - 1).trim()
        }
        return cleanFieldValue(value)
    }

    /**
     * Добавляет флаги стиля (`-S ...`) для `cmd notification post`.
     */
    private fun appendStyleArguments(
        command: MutableList<String>,
        request: NotificationPostRequest,
    ) {
        when (request.style) {
            NotificationPostStyle.NONE -> Unit

            NotificationPostStyle.BIGTEXT -> {
                command += "--style"
                command += "bigtext"
                request.bigText?.trim()?.takeIf { it.isNotEmpty() }?.let { bigText ->
                    command += bigText
                }
            }

            NotificationPostStyle.BIGPICTURE -> {
                val pictureSpec = request.pictureSpec
                    ?.trim()
                    ?.takeIf { it.isNotEmpty() }
                    ?: error("Для стиля BIGPICTURE требуется pictureSpec.")

                command += "--style"
                command += "bigpicture"
                command += "--picture"
                command += pictureSpec
            }

            NotificationPostStyle.INBOX -> {
                val inboxLines = request.inboxLines
                    .asSequence()
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                    .toList()
                require(inboxLines.isNotEmpty()) {
                    "Для стиля INBOX требуется минимум одна строка."
                }

                command += "--style"
                command += "inbox"
                inboxLines.forEach { line ->
                    command += "--line"
                    command += line
                }
            }

            NotificationPostStyle.MESSAGING -> {
                val messages = request.messagingMessages
                    .asSequence()
                    .map { message ->
                        NotificationMessageToken(
                            who = message.who.trim().ifBlank { "User" },
                            text = message.text.trim(),
                        )
                    }
                    .filter { it.text.isNotEmpty() }
                    .toList()
                require(messages.isNotEmpty()) {
                    "Для стиля MESSAGING требуется минимум одно сообщение."
                }

                command += "--style"
                command += "messaging"

                request.messagingConversationTitle
                    ?.trim()
                    ?.takeIf { it.isNotEmpty() }
                    ?.let { conversation ->
                        command += "--conversation"
                        command += conversation
                    }

                messages.forEach { message ->
                    command += "--message"
                    command += "${message.who}:${message.text}"
                }
            }

            NotificationPostStyle.MEDIA -> {
                command += "--style"
                command += "media"
            }
        }
    }

    /**
     * Простой shell-like токенизатор для полей ввода вида `activity -a ...`.
     *
     * Поддерживает:
     * - последовательности без пробелов;
     * - двойные и одинарные кавычки как контейнер токена.
     */
    private fun parseShellTokens(raw: String): List<String> =
        SHELL_TOKEN_RE.findAll(raw)
            .map { match ->
                var token = match.value.trim()
                if (token.length >= 2) {
                    val isDoubleQuoted = token.first() == '"' && token.last() == '"'
                    val isSingleQuoted = token.first() == '\'' && token.last() == '\''
                    if (isDoubleQuoted || isSingleQuoted) {
                        token = token.substring(1, token.length - 1)
                    }
                }
                token
            }
            .filter { it.isNotEmpty() }
            .toList()

    // ── Константы ────────────────────────────────────────────────────────────

    private companion object {
        private data class NotificationMessageToken(
            val who: String,
            val text: String,
        )

        // Разбор аргументов в стиле shell: bare tokens + "quoted" + 'quoted'.
        val SHELL_TOKEN_RE = Regex("""[^\s"']+|"([^"\\]|\\.)*"|'([^'\\]|\\.)*'""")

        // Regex-паттерны для первой строки блока и дополнительных полей
        val PKG_RE     = Regex("""pkg=(\S+)""")
        val ID_RE      = Regex("""\bid=(-?\d+)""")
        val TAG_RE     = Regex("""tag=(\S+)""")
        val KEY_RE     = Regex("""key=([^\s,]+)""")
        val IMP_RE     = Regex("""importance=(\d+)""")
        val FLAGS_RE   = Regex("""flags=0x([0-9a-fA-F]+)""")
        val CHANNEL_RE = Regex("""mId='([^']+)'""")
        val WHEN_RE    = Regex("""when=(\d+)""")
        val GROUP_RE   = Regex("""group=(\S+)""")
        val SORT_RE    = Regex("""sortKey=(\S+)""")
        val ACTIONS_COUNT_RE = Regex("""\bactions=(\d+)""")

        // key=value / key: value для строк вида "android.title=..." или "title: ...".
        val KEY_VALUE_LINE_RE = Regex("""^([A-Za-z0-9_.]+)\s*(?:=|:)\s*(.+)$""")

        // In-line key=value параметры внутри Notification(...) строки.
        val INLINE_KEY_VALUE_RE = Regex("""\b([A-Za-z0-9_.]+)=([^,\n)]+)""")

        // Экстракция action title из разных форматов dumpsys.
        val ACTION_INDEX_QUOTED_RE = Regex("""^\[\d+]\s+"([^"]+)"$""")
        val ACTION_TITLE_DIRECT_RE = Regex("""^(?:android\.)?actionTitle\s*(?:=|:)\s*(.+)$""", RegexOption.IGNORE_CASE)
        val ACTION_LINE_HINT_RE = Regex("""(?i)\b(action\[\d+]|notification\.action|action\(|actions?:)\b""")
        val ACTION_TITLE_IN_LINE_RE = Regex("""(?i)\btitle(?:=|:)\s*([^,]+)""")

        // Ключи визуальных параметров уведомления.
        val IMAGE_KEYWORDS = listOf(
            "icon",
            "image",
            "picture",
            "template",
            "contentview",
            "remoteview",
            "headsup",
            "ticker",
        )

        /**
         * Таблица алиасов текстовых полей:
         *   rawField (как в дампе)  →  canonicalField (в NotificationRecord)
         *
         * Порядок важен: приоритетные варианты идут первыми.
         * Используется LinkedHashMap, чтобы порядок итерации был предсказуем.
         */
        val TEXT_FIELD_ALIASES: Map<String, String> = linkedMapOf(
            // Заголовок (title) — самые частые варианты сначала
            "contentTitle"        to "title",
            "android.title"       to "title",
            "title"               to "title",

            // Основной текст
            "contentText"         to "text",
            "android.text"        to "text",
            "text"                to "text",

            // Подтекст
            "subText"             to "subText",
            "android.subText"     to "subText",

            // Big-текст (развёрнутое уведомление)
            "bigContentTitle"     to "bigText",
            "android.bigText"     to "bigText",
            "bigText"             to "bigText",

            // Summary
            "summaryText"         to "summaryText",
            "android.summaryText" to "summaryText",

            // Категория
            "category"            to "category",
        )

        /**
         * Аннотация типа без значения: "String [length=21]" или "CharSequence [length=5]".
         * Появляется в Android 10+ вместо реального значения поля.
         */
        val TYPE_ANNOTATION_RE = Regex("""^[\w.]+\s*\[length=\d+\]$""")

        /**
         * Аннотация типа с реальным значением: "String [length=21]: My Title".
         * Группа 1 — фактическое значение после двоеточия.
         */
        val TYPE_PREFIX_RE = Regex("""^[\w.]+\s*\[length=\d+\]\s*:\s*(.+)$""")

        /**
         * Однострочный Bundle: extras = Bundle[{key=value, key2=value2, ...}]
         * Группа 1 — содержимое внутри Bundle[{...}].
         */
        val BUNDLE_EXTRAS_RE = Regex("""extras\s*=\s*Bundle\[\{([^}]+)}]""")

        /**
         * Пара key=value внутри Bundle: android.title=My Title
         * Группа 1 — ключ, группа 2 — значение (до следующей пары или конца).
         */
        val BUNDLE_PAIR_RE = Regex("""([\w.]+)=([^,}]+(?:\([^)]*\)[^,}]*)*)""")

        // Битовые флаги Android NotificationRecord
        const val FLAG_ONGOING  = 0x00000002
        const val FLAG_NO_CLEAR = 0x00000020
    }
}

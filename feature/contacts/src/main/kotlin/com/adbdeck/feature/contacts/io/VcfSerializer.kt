package com.adbdeck.feature.contacts.io

import com.adbdeck.core.adb.api.ContactDetails
import com.adbdeck.core.adb.api.EmailType
import com.adbdeck.core.adb.api.PhoneType

/**
 * Генератор vCard 3.0 из [ContactDetails].
 *
 * Поддерживаемые поля:
 * - FN (displayName)
 * - N  (lastName;firstName;middleName;;)
 * - TEL с TYPE (CELL, HOME, WORK, OTHER)
 * - EMAIL с TYPE (HOME, WORK, PREF)
 * - ORG
 * - TITLE
 * - NOTE
 *
 * Спецсимволы: `\n`, `\r\n` в NOTE заменяются на vCard-экранирование `\\n`.
 * Запятые и точки с запятой в ORG/NOTE экранируются `\,` / `\;`.
 */
object VcfSerializer {

    /**
     * Сериализовать один контакт в строку vCard 3.0.
     *
     * @param details Полная информация о контакте.
     * @return Строка в формате vCard 3.0.
     */
    fun serialize(details: ContactDetails): String = buildString {
        appendLine("BEGIN:VCARD")
        appendLine("VERSION:3.0")

        // FN — отображаемое имя
        appendLine("FN:${escapeValue(details.displayName)}")

        // N — структурированное имя: Фамилия;Имя;Отчество;;
        val n = "${escapeValue(details.lastName)};${escapeValue(details.firstName)}" +
                ";${escapeValue(details.middleName)};;"
        appendLine("N:$n")

        // TEL
        for (phone in details.phones) {
            val typeStr = phone.type.toVcfType()
            appendLine("TEL;TYPE=$typeStr:${phone.value}")
        }

        // EMAIL
        for (email in details.emails) {
            val typeStr = email.type.toVcfType()
            appendLine("EMAIL;TYPE=$typeStr:${email.value}")
        }

        // ORG и TITLE
        details.organization?.let { org ->
            if (org.company.isNotEmpty()) {
                appendLine("ORG:${escapeValue(org.company)}")
            }
            if (org.title.isNotEmpty()) {
                appendLine("TITLE:${escapeValue(org.title)}")
            }
        }

        // NOTE
        if (details.notes.isNotEmpty()) {
            appendLine("NOTE:${escapeNote(details.notes)}")
        }

        append("END:VCARD")
    }

    /**
     * Сериализовать список контактов — каждый как отдельный блок vCard 3.0.
     *
     * @param list Список полных деталей контактов.
     * @return Строка с несколькими vCard-блоками, разделёнными пустой строкой.
     */
    fun serializeAll(list: List<ContactDetails>): String =
        list.joinToString(separator = "\n") { serialize(it) }

    // ── Вспомогательные методы ───────────────────────────────────────────────

    /** Экранировать запятую и точку с запятой в значении vCard. */
    private fun escapeValue(s: String): String =
        s.replace("\\", "\\\\")
            .replace(",", "\\,")
            .replace(";", "\\;")

    /** Экранировать переносы строк в NOTE. */
    private fun escapeNote(s: String): String =
        escapeValue(s)
            .replace("\r\n", "\\n")
            .replace("\n", "\\n")
            .replace("\r", "\\n")

    /** Тип телефона для vCard. */
    private fun PhoneType.toVcfType(): String = when (this) {
        PhoneType.MOBILE -> "CELL"
        PhoneType.HOME   -> "HOME"
        PhoneType.WORK   -> "WORK"
        PhoneType.OTHER  -> "OTHER"
    }

    /** Тип email для vCard. */
    private fun EmailType.toVcfType(): String = when (this) {
        EmailType.HOME  -> "HOME"
        EmailType.WORK  -> "WORK"
        EmailType.OTHER -> "PREF"
    }
}

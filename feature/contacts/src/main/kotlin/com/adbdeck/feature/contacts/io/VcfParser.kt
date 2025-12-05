package com.adbdeck.feature.contacts.io

import com.adbdeck.core.adb.api.contacts.ContactEmail
import com.adbdeck.core.adb.api.contacts.ContactImportData
import com.adbdeck.core.adb.api.contacts.ContactPhone
import com.adbdeck.core.adb.api.contacts.EmailType
import com.adbdeck.core.adb.api.contacts.PhoneType

/**
 * Best-effort парсер файлов vCard 3.0 и 4.0.
 *
 * Поддерживает:
 * - FN, N
 * - TEL с TYPE (CELL, MOBILE, HOME, WORK → [PhoneType])
 * - EMAIL с TYPE (HOME, WORK → [EmailType])
 * - ORG, TITLE
 * - NOTE
 * - Склеивание «сложенных» (folded) строк (CRLF/LF + пробел/таб)
 * - ENCODING=QUOTED-PRINTABLE (базовое декодирование)
 * - Несколько vCard в одном файле
 *
 * Неизвестные поля молча пропускаются.
 */
object VcfParser {

    /**
     * Разобрать текст VCF-файла в список [ContactImportData].
     *
     * @param vcfText Полный текст VCF-файла.
     * @return Список распознанных контактов.
     */
    fun parse(vcfText: String): List<ContactImportData> {
        val result = mutableListOf<ContactImportData>()
        val lines = unfoldLines(vcfText)

        var inVcard = false
        var fn = ""
        var firstName = ""
        var lastName = ""
        var middleName = ""
        val phones = mutableListOf<ContactPhone>()
        val emails = mutableListOf<ContactEmail>()
        var organization = ""
        var title = ""
        var note = ""

        for (rawLine in lines) {
            val line = rawLine.trimEnd()
            if (line.isBlank()) continue

            val upper = line.uppercase()

            when {
                upper == "BEGIN:VCARD" -> {
                    inVcard = true
                    fn = ""; firstName = ""; lastName = ""; middleName = ""
                    phones.clear(); emails.clear()
                    organization = ""; title = ""; note = ""
                }

                upper == "END:VCARD" -> {
                    if (inVcard) {
                        val displayName = fn.ifEmpty {
                            buildDisplayName(firstName, lastName)
                        }
                        if (displayName.isNotEmpty() || phones.isNotEmpty() || emails.isNotEmpty()) {
                            result += ContactImportData(
                                displayName  = displayName,
                                firstName    = firstName,
                                lastName     = lastName,
                                phones       = phones.toList(),
                                emails       = emails.toList(),
                                organization = organization,
                                notes        = note,
                            )
                        }
                    }
                    inVcard = false
                }

                !inVcard -> continue

                else -> {
                    // Разделяем property group/name и значение
                    val colonIdx = line.indexOf(':')
                    if (colonIdx < 0) continue

                    val propPart = line.substring(0, colonIdx)
                    val valuePart = decodeValue(line.substring(colonIdx + 1), propPart)

                    // Нормализация: убираем group и параметры для сравнения
                    val propName = propPart
                        .substringAfterLast('.')  // убрать group (e.g. "item1.TEL")
                        .substringBefore(';')     // убрать параметры
                        .uppercase()

                    // Параметры (например TYPE=CELL,HOME)
                    val params = propPart.substringAfter(';', "").uppercase()

                    when (propName) {
                        "FN" -> fn = unescapeValue(valuePart)

                        "N" -> {
                            // N:Фамилия;Имя;Отчество;Префикс;Суффикс
                            val parts = valuePart.split(";")
                            lastName   = unescapeValue(parts.getOrElse(0) { "" })
                            firstName  = unescapeValue(parts.getOrElse(1) { "" })
                            middleName = unescapeValue(parts.getOrElse(2) { "" })
                        }

                        "TEL" -> {
                            val number = valuePart.trim()
                            if (number.isNotEmpty()) {
                                phones += ContactPhone(number, parsePhoneType(params))
                            }
                        }

                        "EMAIL" -> {
                            val addr = valuePart.trim()
                            if (addr.isNotEmpty()) {
                                emails += ContactEmail(addr, parseEmailType(params))
                            }
                        }

                        "ORG" -> {
                            // ORG:Компания;Отдел
                            organization = unescapeValue(valuePart.substringBefore(';'))
                        }

                        "TITLE" -> title = unescapeValue(valuePart)

                        "NOTE" -> note = unescapeNote(valuePart)
                    }
                }
            }
        }

        return result
    }

    // ── Вспомогательные методы ───────────────────────────────────────────────

    /**
     * «Развернуть» сложенные строки vCard:
     * строка, начинающаяся с пробела или таба, является продолжением предыдущей.
     */
    private fun unfoldLines(text: String): List<String> {
        val normalized = text.replace("\r\n", "\n").replace("\r", "\n")
        val result = mutableListOf<String>()
        val current = StringBuilder()

        for (line in normalized.split('\n')) {
            if ((line.startsWith(' ') || line.startsWith('\t')) && result.isNotEmpty()) {
                // Продолжение предыдущей строки
                current.append(line.substring(1))
            } else {
                if (current.isNotEmpty()) {
                    result += current.toString()
                }
                current.clear()
                current.append(line)
            }
        }
        if (current.isNotEmpty()) result += current.toString()

        return result
    }

    /**
     * Декодировать значение с учётом ENCODING=QUOTED-PRINTABLE.
     */
    private fun decodeValue(value: String, propPart: String): String {
        return if (propPart.uppercase().contains("ENCODING=QUOTED-PRINTABLE")) {
            decodeQuotedPrintable(value)
        } else {
            value
        }
    }

    /**
     * Базовое декодирование Quoted-Printable.
     */
    private fun decodeQuotedPrintable(input: String): String {
        val sb = StringBuilder()
        var i = 0
        val bytes = mutableListOf<Byte>()

        fun flushBytes() {
            if (bytes.isNotEmpty()) {
                sb.append(String(bytes.toByteArray(), Charsets.UTF_8))
                bytes.clear()
            }
        }

        while (i < input.length) {
            when {
                input[i] == '=' && i + 2 < input.length -> {
                    val hex = input.substring(i + 1, i + 3)
                    val byte = hex.toIntOrNull(16)
                    if (byte != null) {
                        bytes += byte.toByte()
                        i += 3
                    } else {
                        flushBytes()
                        sb.append(input[i])
                        i++
                    }
                }
                else -> {
                    flushBytes()
                    sb.append(input[i])
                    i++
                }
            }
        }
        flushBytes()
        return sb.toString()
    }

    /** Снять экранирование vCard-значения. */
    private fun unescapeValue(s: String): String =
        s.replace("\\,", ",")
            .replace("\\;", ";")
            .replace("\\\\", "\\")
            .replace("\\n", "\n")
            .replace("\\N", "\n")

    /** Снять экранирование NOTE (плюс восстановить переносы строк). */
    private fun unescapeNote(s: String): String =
        unescapeValue(s)

    /** Сформировать отображаемое имя из имени/фамилии. */
    private fun buildDisplayName(firstName: String, lastName: String): String =
        listOf(firstName, lastName).filter { it.isNotEmpty() }.joinToString(" ")

    /** Определить [PhoneType] из строки параметров (например "TYPE=CELL,HOME"). */
    private fun parsePhoneType(params: String): PhoneType {
        val typeValues = params
            .split(';')
            .filter { it.startsWith("TYPE=") }
            .flatMap { it.removePrefix("TYPE=").split(',') }
            .map { it.trim() }

        return when {
            typeValues.any { it == "CELL" || it == "MOBILE" } -> PhoneType.MOBILE
            typeValues.any { it == "HOME" }                    -> PhoneType.HOME
            typeValues.any { it == "WORK" }                    -> PhoneType.WORK
            else                                               -> PhoneType.OTHER
        }
    }

    /** Определить [EmailType] из строки параметров. */
    private fun parseEmailType(params: String): EmailType {
        val typeValues = params
            .split(';')
            .filter { it.startsWith("TYPE=") }
            .flatMap { it.removePrefix("TYPE=").split(',') }
            .map { it.trim() }

        return when {
            typeValues.any { it == "WORK" } -> EmailType.WORK
            typeValues.any { it == "HOME" } -> EmailType.HOME
            else                            -> EmailType.OTHER
        }
    }
}

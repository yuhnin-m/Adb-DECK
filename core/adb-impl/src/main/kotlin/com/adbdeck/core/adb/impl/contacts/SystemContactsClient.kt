package com.adbdeck.core.adb.impl.contacts

import com.adbdeck.core.adb.api.contacts.Contact
import com.adbdeck.core.adb.api.contacts.ContactAccount
import com.adbdeck.core.adb.api.contacts.ContactAddress
import com.adbdeck.core.adb.api.contacts.ContactDetails
import com.adbdeck.core.adb.api.contacts.ContactEmail
import com.adbdeck.core.adb.api.contacts.ContactImportData
import com.adbdeck.core.adb.api.contacts.ContactOrganization
import com.adbdeck.core.adb.api.contacts.ContactPhone
import com.adbdeck.core.adb.api.contacts.ContactsClient
import com.adbdeck.core.adb.api.contacts.EmailType
import com.adbdeck.core.adb.api.contacts.ImportResult
import com.adbdeck.core.adb.api.contacts.NewContactData
import com.adbdeck.core.adb.api.contacts.PhoneType
import com.adbdeck.core.adb.api.contacts.RawContactInfo
import com.adbdeck.core.process.ProcessResult
import com.adbdeck.core.process.ProcessRunner
import com.adbdeck.core.utils.runCatchingPreserveCancellation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.collections.plusAssign

/**
 * Реализация [com.adbdeck.core.adb.api.contacts.ContactsClient] через `adb shell content query/insert/delete`.
 *
 * ## Модель данных Android Contacts Provider
 *
 * Contacts Provider хранит контакты в трёх связанных таблицах:
 * - `contacts` — агрегированный контакт (один на человека), имеет `_id` и `display_name`
 * - `raw_contacts` — «сырые» записи по одному аккаунту, имеют `contact_id` и `account_*`
 * - `data` — отдельные строки данных с `mimetype` (телефоны, email, имена и т.д.)
 *
 * ## Формат вывода `adb shell content query`
 *
 * ```
 * Row: 0 _id=1, display_name=John Doe, has_phone_number=1
 * Row: 1 _id=2, display_name=Jane Smith, has_phone_number=0
 * ```
 *
 * Парсинг осуществляется с помощью регулярных выражений по известным именам столбцов.
 *
 * ## Создание контактов
 *
 * Контакты создаются как локальные (пустые `account_type` и `account_name`).
 * На некоторых устройствах с настроенным облачным аккаунтом создание локального контакта
 * может быть заблокировано — в таком случае возвращается [Result.failure].
 *
 * @param processRunner Исполнитель внешних процессов.
 */
class SystemContactsClient(
    private val processRunner: ProcessRunner,
) : ContactsClient {

    // ── Регулярные выражения ───────────────────────────────────────────────────

    /** Захватывает содержимое строки вывода content query: «Row: N content». */
    private val ROW_REGEX = Regex("""Row:\s*\d+\s+(.+)""")

    /**
     * Захватывает пары key=value в строке Row.
     * Lookahead гарантирует, что разбиение происходит только перед «, key=», а не перед «,» в значениях.
     */
    private val KV_REGEX = Regex("""([a-zA-Z_]\w*)=(.*?)(?=,\s[a-zA-Z_]\w*=|${'$'})""")

    /** Извлекает числовой ID из URI result: «result: uri=.../raw_contacts/123». */
    private val INSERT_ID_REGEX = Regex("""/(\d+)\s*${'$'}""")

    // ── Публичный API ─────────────────────────────────────────────────────────

    override suspend fun getAvailableAccounts(
        deviceId: String,
        adbPath: String,
    ): Result<List<ContactAccount>> = runCatchingPreserveCancellation {
        withContext(Dispatchers.IO) {
            val rows = queryRows(
                deviceId = deviceId,
                adbPath = adbPath,
                uri = "content://com.android.contacts/raw_contacts",
                projection = "account_name:account_type",
            )

            val accounts = rows
                .map { row ->
                    ContactAccount(
                        accountName = row["account_name"].orEmpty(),
                        accountType = row["account_type"].orEmpty(),
                    )
                }
                .filter { account ->
                    account.isLocal || (account.accountName.isNotBlank() && account.accountType.isNotBlank())
                }
                .distinctBy { it.stableKey }
                .sortedWith(compareBy<ContactAccount> { it.isLocal }.thenBy { it.uiLabel().lowercase() })
                .toMutableList()

            if (accounts.none { it.isLocal }) {
                accounts.add(0, ContactAccount.Companion.local())
            }
            accounts
        }
    }

    override suspend fun getContacts(
        deviceId: String,
        adbPath: String,
    ): Result<List<Contact>> = runCatchingPreserveCancellation {
        withContext(Dispatchers.IO) {
            // 1. Список агрегированных контактов
            val contactRows = queryRows(
                deviceId, adbPath,
                uri = "content://com.android.contacts/contacts",
                projection = "_id:display_name:has_phone_number",
            )

            // 2. Все телефоны → map[contactId → List<ContactPhone>]
            val phoneRows = queryRows(
                deviceId, adbPath,
                uri = "content://com.android.contacts/data/phones",
                projection = "contact_id:data1:data2:data3",
            )
            val phonesMap = buildPhonesMap(phoneRows)

            // 3. Все email → map[contactId → primaryEmail]
            val emailRows = queryRows(
                deviceId, adbPath,
                uri = "content://com.android.contacts/data/emails",
                projection = "contact_id:data1:data2",
            )
            val emailsMap = buildEmailsMap(emailRows)

            // 4. Raw contacts для account info → map[contactId → (accountName, accountType)]
            val rawRows = queryRows(
                deviceId, adbPath,
                uri = "content://com.android.contacts/raw_contacts",
                projection = "_id:contact_id:account_name:account_type",
            )
            val accountMap = buildAccountMap(rawRows)

            // 5. Объединить
            contactRows.mapNotNull { row ->
                val id = row["_id"]?.toLongOrNull() ?: return@mapNotNull null
                val displayName = row["display_name"].orEmpty()
                val (accountName, accountType) = accountMap[id] ?: ("" to "")
                Contact(
                    id = id,
                    displayName = displayName,
                    phones = phonesMap[id] ?: emptyList(),
                    primaryEmail = emailsMap[id] ?: "",
                    accountName = accountName,
                    accountType = accountType,
                )
            }.sortedBy { it.displayName.lowercase() }
        }
    }

    override suspend fun getContactDetails(
        deviceId: String,
        contactId: Long,
        adbPath: String,
    ): Result<ContactDetails> = runCatchingPreserveCancellation {
        withContext(Dispatchers.IO) {
            // Все data-строки для конкретного контакта
            val dataRows = queryRows(
                deviceId, adbPath,
                uri = "content://com.android.contacts/data",
                whereClause = "contact_id=$contactId",
                projection = "_id:raw_contact_id:mimetype:data1:data2:data3:data4:data5:data6",
            )

            // Raw contacts (для информации об аккаунтах)
            val rawRows = queryRows(
                deviceId, adbPath,
                uri = "content://com.android.contacts/raw_contacts",
                whereClause = "contact_id=$contactId",
                projection = "_id:account_name:account_type",
            )

            // Разбор data-строк по mimetype
            var displayName = ""
            var firstName = ""
            var lastName = ""
            var middleName = ""
            val phones = mutableListOf<ContactPhone>()
            val emails = mutableListOf<ContactEmail>()
            val addresses = mutableListOf<ContactAddress>()
            var organization: ContactOrganization? = null
            val notes = StringBuilder()

            for (row in dataRows) {
                val mimetype = row["mimetype"] ?: continue
                val d1 = row["data1"].orEmpty()
                val d2 = row["data2"].orEmpty()
                val d3 = row["data3"].orEmpty()
                val d4 = row["data4"].orEmpty()

                when (mimetype) {
                    "vnd.android.cursor.item/name" -> {
                        displayName = d1
                        firstName = d2
                        lastName = d3
                        middleName = d4
                    }

                    "vnd.android.cursor.item/phone_v2" -> {
                        if (d1.isNotBlank()) {
                            phones += ContactPhone(
                                value = d1,
                                type = PhoneType.Companion.fromAdbInt(d2.toIntOrNull() ?: 0),
                                label = row["data4"].orEmpty(),
                            )
                        }
                    }

                    "vnd.android.cursor.item/email_v2" -> {
                        if (d1.isNotBlank()) {
                            emails += ContactEmail(
                                value = d1,
                                type = EmailType.Companion.fromAdbInt(d2.toIntOrNull() ?: 0),
                                label = d3,
                            )
                        }
                    }

                    "vnd.android.cursor.item/organization" -> {
                        if (d1.isNotBlank() || d4.isNotBlank()) {
                            organization = ContactOrganization(company = d1, title = d4)
                        }
                    }

                    "vnd.android.cursor.item/postal-address_v2" -> {
                        if (d1.isNotBlank()) {
                            addresses += ContactAddress(formatted = d1)
                        }
                    }

                    "vnd.android.cursor.item/note" -> {
                        if (d1.isNotBlank()) {
                            if (notes.isNotEmpty()) notes.append('\n')
                            notes.append(d1)
                        }
                    }
                }
            }

            // Если displayName не нашли из data/name — попробуем получить из contacts
            if (displayName.isBlank()) {
                val contactRows = queryRows(
                    deviceId, adbPath,
                    uri = "content://com.android.contacts/contacts",
                    whereClause = "_id=$contactId",
                    projection = "_id:display_name",
                )
                displayName = contactRows.firstOrNull()?.get("display_name").orEmpty()
            }

            val rawContacts = rawRows.mapNotNull { row ->
                val rawId = row["_id"]?.toLongOrNull() ?: return@mapNotNull null
                RawContactInfo(
                    rawContactId = rawId,
                    accountName = row["account_name"].orEmpty(),
                    accountType = row["account_type"].orEmpty(),
                )
            }

            ContactDetails(
                id = contactId,
                displayName = displayName,
                firstName = firstName,
                lastName = lastName,
                middleName = middleName,
                phones = phones,
                emails = emails,
                organization = organization,
                addresses = addresses,
                notes = notes.toString(),
                rawContacts = rawContacts,
            )
        }
    }

    override suspend fun addContact(
        deviceId: String,
        contact: NewContactData,
        adbPath: String,
    ): Result<Unit> = runCatchingPreserveCancellation {
        withContext(Dispatchers.IO) {
            // Шаг 1: создать raw_contact в выбранном аккаунте (или локально, если поля пустые)
            val rawResult = processRunner.run(
                adbPath, "-s", deviceId,
                "shell", "content", "insert",
                "--uri", "content://com.android.contacts/raw_contacts",
                "--bind", "account_type:s:${contact.accountType}",
                "--bind", "account_name:s:${contact.accountName}",
            )

            // Извлекаем rawContactId из вывода «result: uri=.../raw_contacts/123»
            val rawContactId = INSERT_ID_REGEX
                .find(rawResult.stdout.trim())
                ?.groupValues
                ?.get(1)
                ?.toLongOrNull()
                ?: error(
                    "Не удалось создать контакт: устройство отклонило вставку raw_contact. " +
                            "Возможно, устройство запрещает создание локальных контактов — " +
                            "добавьте контакт вручную на устройстве. Вывод: ${rawResult.stdout.take(300)}"
                )

            // Шаг 2: вставить строку с именем
            insertDataRow(
                deviceId, adbPath, rawContactId,
                mimetype = "vnd.android.cursor.item/name",
                data1 = contact.displayName,
                data2 = contact.firstName,
                data3 = contact.lastName,
            )

            // Шаг 3: вставить телефоны
            if (contact.phone1.isNotBlank()) {
                insertDataRow(
                    deviceId, adbPath, rawContactId,
                    mimetype = "vnd.android.cursor.item/phone_v2",
                    data1 = contact.phone1,
                    data2 = contact.phone1Type.adbInt.toString(),
                )
            }
            if (contact.phone2.isNotBlank()) {
                insertDataRow(
                    deviceId, adbPath, rawContactId,
                    mimetype = "vnd.android.cursor.item/phone_v2",
                    data1 = contact.phone2,
                    data2 = contact.phone2Type.adbInt.toString(),
                )
            }

            // Шаг 4: вставить email
            if (contact.email.isNotBlank()) {
                insertDataRow(
                    deviceId, adbPath, rawContactId,
                    mimetype = "vnd.android.cursor.item/email_v2",
                    data1 = contact.email,
                    data2 = contact.emailType.adbInt.toString(),
                )
            }

            // Шаг 5: вставить организацию
            if (contact.organization.isNotBlank()) {
                insertDataRow(
                    deviceId, adbPath, rawContactId,
                    mimetype = "vnd.android.cursor.item/organization",
                    data1 = contact.organization,
                )
            }

            // Шаг 6: вставить заметку
            if (contact.notes.isNotBlank()) {
                insertDataRow(
                    deviceId, adbPath, rawContactId,
                    mimetype = "vnd.android.cursor.item/note",
                    data1 = contact.notes,
                )
            }
        }
    }

    override suspend fun deleteContact(
        deviceId: String,
        contactId: Long,
        adbPath: String,
    ): Result<Unit> = runCatchingPreserveCancellation {
        withContext(Dispatchers.IO) {
            val result = processRunner.run(
                adbPath, "-s", deviceId,
                "shell", "content", "delete",
                "--uri", "content://com.android.contacts/contacts/$contactId",
            )
            // content delete не всегда возвращает ошибку в stderr при неудаче
            if (result.exitCode != 0 && result.stderr.isNotBlank()) {
                error("Ошибка удаления контакта $contactId: ${result.stderr.take(200)}")
            }
        }
    }

    override suspend fun importContacts(
        deviceId: String,
        contacts: List<ContactImportData>,
        adbPath: String,
    ): Result<ImportResult> = runCatchingPreserveCancellation {
        var successCount = 0
        var failedCount = 0
        val errors = mutableListOf<String>()

        for (contact in contacts) {
            val newData = NewContactData(
                firstName = contact.firstName,
                lastName = contact.lastName,
                displayName = contact.displayName.ifBlank {
                    listOf(contact.firstName, contact.lastName).filter { it.isNotBlank() }.joinToString(" ")
                },
                phone1 = contact.phones.getOrNull(0)?.value ?: "",
                phone1Type = contact.phones.getOrNull(0)?.type ?: PhoneType.MOBILE,
                phone2 = contact.phones.getOrNull(1)?.value ?: "",
                phone2Type = contact.phones.getOrNull(1)?.type ?: PhoneType.MOBILE,
                email = contact.emails.getOrNull(0)?.value ?: "",
                emailType = contact.emails.getOrNull(0)?.type ?: EmailType.HOME,
                organization = contact.organization,
                notes = contact.notes,
                accountName = contact.accountName,
                accountType = contact.accountType,
            )

            addContact(deviceId, newData, adbPath)
                .onSuccess { successCount++ }
                .onFailure { e ->
                    failedCount++
                    errors += "${contact.displayName}: ${e.message?.take(150) ?: "unknown error"}"
                }
        }

        ImportResult(
            successCount = successCount,
            failedCount = failedCount,
            errors = errors,
        )
    }

    // ── Приватные вспомогательные функции ─────────────────────────────────────

    /**
     * Выполняет запрос к Contacts Provider и возвращает список строк как Map[key → value].
     *
     * @param deviceId  ID устройства.
     * @param adbPath   Путь к adb.
     * @param uri       URI таблицы Contacts Provider.
     * @param projection Список столбцов через двоеточие.
     * @param whereClause Условие WHERE (опционально).
     */
    private suspend fun queryRows(
        deviceId: String,
        adbPath: String,
        uri: String,
        projection: String,
        whereClause: String = "",
    ): List<Map<String, String>> {
        val baseArgs = buildList {
            add(adbPath); add("-s"); add(deviceId)
            add("shell"); add("content"); add("query")
            add("--uri"); add(uri)
            add("--projection"); add(projection)
        }
        val result = runQueryWithWhereFallback(
            baseArgs = baseArgs,
            whereClause = whereClause,
        )
        val stdout = result.stdout.trim()
        val stderr = result.stderr.trim()

        if (!result.isSuccess) {
            val details = stderr.ifBlank { stdout }.ifBlank { "неизвестная ошибка" }
            error("Ошибка чтения контактов через content query ($uri): ${details.take(400)}")
        }

        val hasProviderError =
            stdout.contains("Error while accessing provider", ignoreCase = true) ||
                stdout.contains("[ERROR]", ignoreCase = true)
        if (hasProviderError) {
            val reason = stdout.lineSequence().firstOrNull { it.isNotBlank() }?.take(400).orEmpty()
            error("Ошибка чтения контактов через content query ($uri): $reason")
        }

        if (stdout.isBlank() || stdout == "No result found.") {
            return emptyList()
        }

        return stdout.lines().mapNotNull { line ->
            ROW_REGEX.matchEntire(line.trim())?.groupValues?.get(1)?.let { rowContent ->
                KV_REGEX.findAll(rowContent).associate { match ->
                    match.groupValues[1] to normalizeContentValue(match.groupValues[2].trim())
                }
            }
        }
    }

    /**
     * Выполняет `content query` с условием WHERE.
     *
     * На разных Android-сборках используется разный флаг (`--where` или `--selection`),
     * поэтому при ошибке неизвестного флага выполняется автоматический fallback.
     */
    private suspend fun runQueryWithWhereFallback(
        baseArgs: List<String>,
        whereClause: String,
    ) = if (whereClause.isBlank()) {
        processRunner.run(*baseArgs.toTypedArray())
    } else {
        val whereArgs = (baseArgs + listOf("--where", whereClause)).toTypedArray()
        val whereResult = processRunner.run(*whereArgs)
        if (whereResult.isSuccess || !isWhereFlagUnsupported(whereResult)) {
            whereResult
        } else {
            val selectionArgs = (baseArgs + listOf("--selection", whereClause)).toTypedArray()
            processRunner.run(*selectionArgs)
        }
    }

    /**
     * Проверяет, что команда не распознана именно из-за флага `--where`.
     */
    private fun isWhereFlagUnsupported(result: ProcessResult): Boolean {
        val message = buildString {
            append(result.stdout)
            append('\n')
            append(result.stderr)
        }
        val lower = message.lowercase()
        return lower.contains("unknown option") && lower.contains("--where") ||
            lower.contains("unsupported argument") && lower.contains("--where") ||
            lower.contains("argument expected after --where")
    }

    /**
     * `content query` возвращает `NULL` как строку; для UI это шум, конвертируем в пустое значение.
     */
    private fun normalizeContentValue(raw: String): String =
        if (raw == "NULL") "" else raw

    /**
     * Вставляет data-строку в таблицу `data` Contacts Provider.
     *
     * @param deviceId     ID устройства.
     * @param adbPath      Путь к adb.
     * @param rawContactId ID raw contact (FK).
     * @param mimetype     MIME-тип строки.
     * @param data1…data6 Поля данных (пустые строки → не добавляются в команду).
     */
    private suspend fun insertDataRow(
        deviceId: String,
        adbPath: String,
        rawContactId: Long,
        mimetype: String,
        data1: String = "",
        data2: String = "",
        data3: String = "",
        data4: String = "",
        data5: String = "",
        data6: String = "",
    ) {
        val args = buildList {
            add(adbPath); add("-s"); add(deviceId)
            add("shell"); add("content"); add("insert")
            add("--uri"); add("content://com.android.contacts/data")
            add("--bind"); add("raw_contact_id:i:$rawContactId")
            add("--bind"); add("mimetype:s:$mimetype")

            // data2 у phone/email — числовой тип
            val isIntData2 = mimetype.contains("phone") || mimetype.contains("email")

            if (data1.isNotBlank()) { add("--bind"); add("data1:s:$data1") }
            if (data2.isNotBlank()) {
                val bindType = if (isIntData2) "i" else "s"
                add("--bind"); add("data2:$bindType:$data2")
            }
            if (data3.isNotBlank()) { add("--bind"); add("data3:s:$data3") }
            if (data4.isNotBlank()) { add("--bind"); add("data4:s:$data4") }
            if (data5.isNotBlank()) { add("--bind"); add("data5:s:$data5") }
            if (data6.isNotBlank()) { add("--bind"); add("data6:s:$data6") }
        }

        processRunner.run(*args.toTypedArray())
        // Не бросаем ошибку при неудаче data-строки — raw_contact уже создан,
        // частичная вставка лучше полного отказа.
    }

    // ── Вспомогательные построители карт ─────────────────────────────────────

    /**
     * Строит карту [contactId → List<ContactPhone>] из строк таблицы `data`.
     */
    private fun buildPhonesMap(
        rows: List<Map<String, String>>,
    ): Map<Long, List<ContactPhone>> {
        val result = mutableMapOf<Long, MutableList<ContactPhone>>()
        for (row in rows) {
            val contactId = row["contact_id"]?.toLongOrNull() ?: continue
            val number    = row["data1"].orEmpty()
            if (number.isBlank()) continue
            val typeInt = row["data2"]?.toIntOrNull() ?: 0
            result.getOrPut(contactId) { mutableListOf() }.add(
                ContactPhone(
                value = number,
                type = PhoneType.Companion.fromAdbInt(typeInt),
                ),
            )
        }
        return result
    }

    /**
     * Строит карту [contactId → primaryEmail] из строк таблицы `data`.
     * Берёт только первый email для каждого контакта.
     */
    private fun buildEmailsMap(rows: List<Map<String, String>>): Map<Long, String> {
        val result = mutableMapOf<Long, String>()
        for (row in rows) {
            val contactId = row["contact_id"]?.toLongOrNull() ?: continue
            val email     = row["data1"].orEmpty()
            if (email.isBlank()) continue
            if (!result.containsKey(contactId)) {
                result[contactId] = email
            }
        }
        return result
    }

    /**
     * Строит карту [contactId → Pair(accountName, accountType)] из строк таблицы `raw_contacts`.
     * Берёт первый raw_contact для каждого contact_id.
     */
    private fun buildAccountMap(rows: List<Map<String, String>>): Map<Long, Pair<String, String>> {
        val result = mutableMapOf<Long, Pair<String, String>>()
        for (row in rows) {
            val contactId   = row["contact_id"]?.toLongOrNull() ?: continue
            if (result.containsKey(contactId)) continue
            result[contactId] = (row["account_name"].orEmpty() to row["account_type"].orEmpty())
        }
        return result
    }
}

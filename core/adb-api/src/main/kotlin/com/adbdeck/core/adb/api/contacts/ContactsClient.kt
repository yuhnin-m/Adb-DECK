package com.adbdeck.core.adb.api.contacts

/**
 * Результат операции импорта контактов на устройство.
 *
 * @param successCount Количество успешно импортированных контактов.
 * @param failedCount  Количество контактов с ошибками.
 * @param errors       Описания ошибок для конкретных контактов.
 */
data class ImportResult(
    val successCount: Int,
    val failedCount: Int,
    val errors: List<String>,
)

/**
 * Клиент для работы с контактами Android-устройства через Contacts Provider.
 *
 * ## Источник данных
 *
 * Использует трёхтабличную модель Contacts Provider:
 * - `contacts` — агрегированные контакты (один на человека)
 * - `raw_contacts` — «сырые» записи, по одной на аккаунт синхронизации
 * - `data` — отдельные строки данных с mimetype (телефоны, email, имена и т.д.)
 *
 * Доступ осуществляется через `adb shell content query/insert/delete`.
 *
 * ## Создание контактов
 *
 * Контакты создаются в выбранном аккаунте (`account_type` / `account_name`),
 * либо локально, если оба поля пустые.
 * На устройствах, где локальные контакты запрещены, создание без аккаунта
 * может завершиться ошибкой — в таком случае [addContact] вернёт [Result.failure].
 *
 * ## Потокобезопасность
 *
 * Все методы являются `suspend`-функциями и предназначены для вызова
 * из корутины Dispatchers.IO. Имплементации должны использовать `withContext(Dispatchers.IO)`.
 */
interface ContactsClient {

    /**
     * Получить список доступных аккаунтов контактов на устройстве.
     *
     * Используется UI для выбора целевого аккаунта при создании и импорте.
     * В список обычно включается локальный аккаунт (без синхронизации), если он доступен.
     *
     * @param deviceId ID устройства.
     * @param adbPath  Путь к adb.
     * @return Список [ContactAccount] или ошибку.
     */
    suspend fun getAvailableAccounts(
        deviceId: String,
        adbPath: String = "adb",
    ): Result<List<ContactAccount>>

    /**
     * Получить список всех контактов устройства (краткая информация).
     *
     * Выполняет три запроса к Contacts Provider:
     * 1. Список контактов (id + displayName)
     * 2. Все телефонные номера (для обогащения списка)
     * 3. Все email-адреса (для отображения первого email в строке)
     *
     * @param deviceId ID устройства (serial или IP:port).
     * @param adbPath  Путь к исполняемому файлу adb.
     * @return Список [Contact] или ошибку.
     */
    suspend fun getContacts(
        deviceId: String,
        adbPath: String = "adb",
    ): Result<List<Contact>>

    /**
     * Получить полную информацию о конкретном контакте.
     *
     * Запрашивает все data-строки по `contact_id`, а также raw contacts.
     * Разбивает строки по mimetype на телефоны, email, имя, организацию и т.д.
     *
     * @param deviceId  ID устройства.
     * @param contactId ID контакта в таблице `contacts`.
     * @param adbPath   Путь к adb.
     * @return [ContactDetails] или ошибку.
     */
    suspend fun getContactDetails(
        deviceId: String,
        contactId: Long,
        adbPath: String = "adb",
    ): Result<ContactDetails>

    /**
     * Добавить новый контакт на устройство.
     *
     * Создаёт raw_contact с `account_type` и `account_name` из [NewContactData],
     * затем вставляет data-строки (имя, телефоны, email и т.д.).
     *
     * Если устройство запрещает выбранный тип аккаунта, возвращает [Result.failure]
     * с текстом ошибки от Contacts Provider.
     *
     * @param deviceId ID устройства.
     * @param contact  Данные нового контакта.
     * @param adbPath  Путь к adb.
     * @return [Result.success] при успехе, [Result.failure] — при ошибке.
     */
    suspend fun addContact(
        deviceId: String,
        contact: NewContactData,
        adbPath: String = "adb",
    ): Result<Unit>

    /**
     * Удалить контакт по ID.
     *
     * Выполняет `adb shell content delete --uri content://com.android.contacts/contacts/<id>`.
     * Удаление агрегированного контакта автоматически удаляет все связанные raw_contacts и data.
     *
     * @param deviceId  ID устройства.
     * @param contactId ID контакта.
     * @param adbPath   Путь к adb.
     * @return [Result.success] при успехе, [Result.failure] при ошибке.
     */
    suspend fun deleteContact(
        deviceId: String,
        contactId: Long,
        adbPath: String = "adb",
    ): Result<Unit>

    /**
     * Импортировать список контактов на устройство.
     *
     * Вызывает [addContact] для каждого элемента из [contacts]. Для каждого контакта
     * может быть указан собственный аккаунт через [ContactImportData.accountName] /
     * [ContactImportData.accountType]. При ошибке на одном элементе импорт остальных
     * продолжается. Возвращает итоговую статистику в [ImportResult].
     *
     * @param deviceId ID устройства.
     * @param contacts Список контактов для импорта.
     * @param adbPath  Путь к adb.
     * @return [ImportResult] с количеством успехов и ошибок.
     */
    suspend fun importContacts(
        deviceId: String,
        contacts: List<ContactImportData>,
        adbPath: String = "adb",
    ): Result<ImportResult>
}

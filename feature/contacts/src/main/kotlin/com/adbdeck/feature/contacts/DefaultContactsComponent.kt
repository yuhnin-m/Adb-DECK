package com.adbdeck.feature.contacts

import com.adbdeck.core.adb.api.contacts.Contact
import com.adbdeck.core.adb.api.contacts.ContactAccount
import com.adbdeck.core.adb.api.contacts.ContactDetails
import com.adbdeck.core.adb.api.contacts.ContactImportData
import com.adbdeck.core.adb.api.contacts.ContactsClient
import com.adbdeck.core.adb.api.device.DeviceManager
import com.adbdeck.core.adb.api.device.DeviceState
import com.adbdeck.core.adb.api.contacts.EmailType
import com.adbdeck.core.adb.api.contacts.NewContactData
import com.adbdeck.core.adb.api.contacts.PhoneType
import com.adbdeck.core.settings.SettingsRepository
import com.adbdeck.feature.contacts.io.ContactIoService
import com.adbdeck.feature.contacts.io.ContactsJsonFile
import com.adbdeck.feature.contacts.io.VcfParser
import com.adbdeck.feature.contacts.io.VcfSerializer
import com.adbdeck.feature.contacts.io.toImportData
import com.adbdeck.feature.contacts.io.toJsonEntry
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.lifecycle.coroutines.coroutineScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * Реализация [ContactsComponent].
 *
 * Особенности:
 * - синхронизируется с текущим active device из [DeviceManager];
 * - поддерживает выбор аккаунта для создания/импорта контактов;
 * - для длительных операций (импорт/экспорт) показывает модальный прогресс с логом;
 * - при дисконнекте или переключении устройства корректно прерывает операции.
 */
class DefaultContactsComponent(
    componentContext: ComponentContext,
    private val deviceManager: DeviceManager,
    private val contactsClient: ContactsClient,
    private val settingsRepository: SettingsRepository,
) : ContactsComponent, ComponentContext by componentContext {

    private val scope = coroutineScope()

    private val _state = MutableStateFlow(ContactsState())
    override val state: StateFlow<ContactsState> = _state.asStateFlow()

    /** JSON с prettyPrint для удобочитаемого экспорта. */
    private val json = Json {
        prettyPrint = true
        encodeDefaults = true
    }

    // ── Jobs ──────────────────────────────────────────────────────────────────

    /** Job загрузки списка контактов. */
    private var loadJob: Job? = null

    /** Job загрузки деталей контакта. */
    private var detailJob: Job? = null

    /** Job длительной операции (импорт/экспорт). */
    private var operationJob: Job? = null

    /** Job автоочистки feedback-уведомления. */
    private var feedbackJob: Job? = null

    /** DeviceId, с которым синхронизировано текущее состояние. */
    private var activeDeviceId: String? = null

    // ── Инициализация ─────────────────────────────────────────────────────────

    init {
        scope.launch {
            deviceManager.selectedDeviceFlow.collect { device ->
                when {
                    device == null || device.state != DeviceState.DEVICE -> {
                        val hadRunningOperation = operationJob?.isActive == true
                        loadJob?.cancel()
                        detailJob?.cancel()
                        operationJob?.cancel(CancellationException("Операция прервана: устройство недоступно."))
                        activeDeviceId = null
                        _state.update {
                            it.copy(
                                listState = ContactsListState.NoDevice,
                                filteredContacts = emptyList(),
                                availableAccounts = listOf(ContactAccount.local()),
                                selectedAccount = ContactAccount.local(),
                                selectedContactId = null,
                                detailState = ContactDetailState.Idle,
                                addForm = null,
                                pendingDeleteContact = null,
                                operationState = null,
                            )
                        }
                        if (hadRunningOperation) {
                            showFeedback(
                                ContactFeedback(
                                    message = "Операция прервана: устройство отключено или недоступно",
                                    isError = true,
                                ),
                            )
                        }
                    }

                    else -> {
                        val isDeviceChanged = activeDeviceId != device.deviceId
                        val needsReload = _state.value.listState !is ContactsListState.Success
                        if (isDeviceChanged) {
                            detailJob?.cancel()
                            if (operationJob?.isActive == true) {
                                operationJob?.cancel(
                                    CancellationException("Операция прервана: выбрано другое устройство."),
                                )
                            }
                            _state.update {
                                it.copy(
                                    selectedContactId = null,
                                    detailState = ContactDetailState.Idle,
                                    pendingDeleteContact = null,
                                    operationState = null,
                                )
                            }
                        }
                        activeDeviceId = device.deviceId
                        if (isDeviceChanged || needsReload) {
                            loadContacts()
                        }
                    }
                }
            }
        }
    }

    // ── Список ────────────────────────────────────────────────────────────────

    override fun onRefresh() {
        val device = deviceManager.selectedDeviceFlow.value
        if (device == null || device.state != DeviceState.DEVICE) return
        loadContacts()
    }

    override fun onSearchChanged(query: String) {
        _state.update { current ->
            val allContacts = (current.listState as? ContactsListState.Success)?.contacts ?: return
            current.copy(
                searchQuery = query,
                filteredContacts = applySearch(allContacts, query),
            )
        }
    }

    override fun onSelectTargetAccount(account: ContactAccount) {
        _state.update { current ->
            val resolved = current.availableAccounts.firstOrNull { it.stableKey == account.stableKey }
                ?: current.selectedAccount
            val updatedForm = current.addForm?.copy(
                accountName = resolved.accountName,
                accountType = resolved.accountType,
            )
            current.copy(
                selectedAccount = resolved,
                addForm = updatedForm,
            )
        }
    }

    private fun loadContacts() {
        loadJob?.cancel()
        loadJob = scope.launch {
            val device = deviceManager.selectedDeviceFlow.value ?: return@launch
            if (device.state != DeviceState.DEVICE) return@launch

            val requestDeviceId = device.deviceId
            val adbPath = adbPath()

            _state.update { it.copy(listState = ContactsListState.Loading) }

            contactsClient.getContacts(deviceId = requestDeviceId, adbPath = adbPath)
                .onSuccess { contacts ->
                    if (!isRequestStillValid(requestDeviceId)) return@onSuccess

                    val accounts = contactsClient.getAvailableAccounts(
                        deviceId = requestDeviceId,
                        adbPath = adbPath,
                    ).getOrElse { error ->
                        if (isRequestStillValid(requestDeviceId)) {
                            showFeedback(
                                ContactFeedback(
                                    message = "Не удалось загрузить аккаунты контактов: ${error.message}",
                                    isError = true,
                                ),
                            )
                        }
                        listOf(ContactAccount.local())
                    }.ifEmpty { listOf(ContactAccount.local()) }

                    _state.update { current ->
                        val selectedStillExists =
                            current.selectedContactId != null &&
                                contacts.any { it.id == current.selectedContactId }
                        val selectedAccount = selectAccountForState(
                            currentSelected = current.selectedAccount,
                            availableAccounts = accounts,
                        )
                        val updatedAddForm = current.addForm?.let { form ->
                            val formAccount = accounts.firstOrNull {
                                it.accountName == form.accountName &&
                                    it.accountType == form.accountType
                            } ?: selectedAccount
                            form.copy(
                                accountName = formAccount.accountName,
                                accountType = formAccount.accountType,
                            )
                        }
                        current.copy(
                            listState = ContactsListState.Success(contacts),
                            filteredContacts = applySearch(contacts, current.searchQuery),
                            availableAccounts = accounts,
                            selectedAccount = selectedAccount,
                            selectedContactId = if (selectedStillExists) current.selectedContactId else null,
                            detailState = if (selectedStillExists) current.detailState else ContactDetailState.Idle,
                            addForm = updatedAddForm,
                        )
                    }
                }
                .onFailure { error ->
                    if (!isRequestStillValid(requestDeviceId)) return@onFailure
                    _state.update {
                        it.copy(
                            listState = ContactsListState.Error(error.message ?: "Неизвестная ошибка"),
                            filteredContacts = emptyList(),
                            availableAccounts = listOf(ContactAccount.local()),
                            selectedAccount = ContactAccount.local(),
                        )
                    }
                }
        }
    }

    private fun applySearch(contacts: List<Contact>, query: String): List<Contact> {
        if (query.isBlank()) return contacts
        val q = query.trim()
        return contacts.filter { c ->
            c.displayName.contains(q, ignoreCase = true) ||
                c.phones.any { it.value.contains(q) } ||
                c.primaryEmail.contains(q, ignoreCase = true)
        }
    }

    // ── Детали ────────────────────────────────────────────────────────────────

    override fun onSelectContact(contact: Contact) {
        if (_state.value.selectedContactId == contact.id &&
            _state.value.detailState is ContactDetailState.Success
        ) {
            return // Тот же контакт — не перезагружать
        }

        detailJob?.cancel()
        _state.update {
            it.copy(
                selectedContactId = contact.id,
                detailState = ContactDetailState.Loading,
            )
        }

        detailJob = scope.launch {
            loadDetails(contact.id)
        }
    }

    override fun onCloseDetail() {
        detailJob?.cancel()
        _state.update {
            it.copy(
                selectedContactId = null,
                detailState = ContactDetailState.Idle,
            )
        }
    }

    override fun onRefreshDetail() {
        val contactId = _state.value.selectedContactId ?: return
        detailJob?.cancel()
        _state.update { it.copy(detailState = ContactDetailState.Loading) }
        detailJob = scope.launch { loadDetails(contactId) }
    }

    private suspend fun loadDetails(contactId: Long) {
        val device = deviceManager.selectedDeviceFlow.value ?: return
        if (device.state != DeviceState.DEVICE) return
        val requestDeviceId = device.deviceId
        val adbPath = adbPath()

        contactsClient.getContactDetails(requestDeviceId, contactId, adbPath)
            .onSuccess { details ->
                if (!isRequestStillValid(requestDeviceId)) return@onSuccess
                if (_state.value.selectedContactId != contactId) return@onSuccess
                _state.update { it.copy(detailState = ContactDetailState.Success(details)) }
            }
            .onFailure { error ->
                if (!isRequestStillValid(requestDeviceId)) return@onFailure
                if (_state.value.selectedContactId != contactId) return@onFailure
                _state.update {
                    it.copy(detailState = ContactDetailState.Error(error.message ?: "Ошибка загрузки"))
                }
            }
    }

    // ── Форма добавления ──────────────────────────────────────────────────────

    override fun onShowAddForm() {
        val selected = _state.value.selectedAccount
        _state.update {
            it.copy(
                addForm = AddContactFormState(
                    accountName = selected.accountName,
                    accountType = selected.accountType,
                ),
            )
        }
    }

    override fun onAddFormFirstNameChanged(value: String) {
        _state.update { it.copy(addForm = it.addForm?.copy(firstName = value)) }
    }

    override fun onAddFormLastNameChanged(value: String) {
        _state.update { it.copy(addForm = it.addForm?.copy(lastName = value)) }
    }

    override fun onAddFormAccountChanged(account: ContactAccount) {
        _state.update {
            it.copy(
                addForm = it.addForm?.copy(
                    accountName = account.accountName,
                    accountType = account.accountType,
                ),
            )
        }
    }

    override fun onAddFormPhone1Changed(value: String) {
        _state.update { it.copy(addForm = it.addForm?.copy(phone1 = value)) }
    }

    override fun onAddFormPhone1TypeChanged(type: PhoneType) {
        _state.update { it.copy(addForm = it.addForm?.copy(phone1Type = type)) }
    }

    override fun onAddFormPhone2Changed(value: String) {
        _state.update { it.copy(addForm = it.addForm?.copy(phone2 = value)) }
    }

    override fun onAddFormPhone2TypeChanged(type: PhoneType) {
        _state.update { it.copy(addForm = it.addForm?.copy(phone2Type = type)) }
    }

    override fun onAddFormEmailChanged(value: String) {
        _state.update { it.copy(addForm = it.addForm?.copy(email = value)) }
    }

    override fun onAddFormEmailTypeChanged(type: EmailType) {
        _state.update { it.copy(addForm = it.addForm?.copy(emailType = type)) }
    }

    override fun onAddFormOrganizationChanged(value: String) {
        _state.update { it.copy(addForm = it.addForm?.copy(organization = value)) }
    }

    override fun onAddFormNotesChanged(value: String) {
        _state.update { it.copy(addForm = it.addForm?.copy(notes = value)) }
    }

    override fun onSubmitAddForm() {
        val form = _state.value.addForm ?: return
        if (form.isSubmitting) return

        if (form.firstName.isBlank() && form.lastName.isBlank()) {
            _state.update {
                it.copy(addForm = form.copy(error = "Введите имя или фамилию"))
            }
            return
        }

        val displayName = buildDisplayName(form.firstName.trim(), form.lastName.trim())
        val contact = NewContactData(
            firstName = form.firstName.trim(),
            lastName = form.lastName.trim(),
            displayName = displayName,
            phone1 = form.phone1.trim(),
            phone1Type = form.phone1Type,
            phone2 = form.phone2.trim(),
            phone2Type = form.phone2Type,
            email = form.email.trim(),
            emailType = form.emailType,
            organization = form.organization.trim(),
            notes = form.notes.trim(),
            accountName = form.accountName,
            accountType = form.accountType,
        )

        _state.update { it.copy(addForm = form.copy(isSubmitting = true, error = null)) }

        scope.launch {
            val (deviceId, adbPath) = requireDeviceAndPath() ?: run {
                _state.update {
                    it.copy(addForm = it.addForm?.copy(isSubmitting = false, error = "Устройство недоступно"))
                }
                return@launch
            }

            contactsClient.addContact(deviceId, contact, adbPath)
                .onSuccess {
                    if (!isRequestStillValid(deviceId)) {
                        _state.update {
                            it.copy(addForm = it.addForm?.copy(isSubmitting = false, error = "Устройство отключено"))
                        }
                        return@onSuccess
                    }
                    _state.update { it.copy(addForm = null) }
                    showFeedback(ContactFeedback("Контакт «$displayName» добавлен", isError = false))
                    loadContacts()
                }
                .onFailure { error ->
                    if (!isRequestStillValid(deviceId)) {
                        _state.update {
                            it.copy(addForm = it.addForm?.copy(isSubmitting = false, error = "Устройство отключено"))
                        }
                        return@onFailure
                    }
                    _state.update {
                        it.copy(
                            addForm = it.addForm?.copy(
                                isSubmitting = false,
                                error = error.message ?: "Ошибка создания контакта",
                            ),
                        )
                    }
                }
        }
    }

    override fun onDismissAddForm() {
        _state.update { it.copy(addForm = null) }
    }

    // ── Удаление ──────────────────────────────────────────────────────────────

    override fun onRequestDelete(contact: Contact) {
        _state.update { it.copy(pendingDeleteContact = contact) }
    }

    override fun onConfirmDelete() {
        val contact = _state.value.pendingDeleteContact ?: return
        _state.update { it.copy(pendingDeleteContact = null) }

        scope.launch {
            _state.update { it.copy(isActionRunning = true) }
            try {
                val (deviceId, adbPath) = requireDeviceAndPath() ?: return@launch

                contactsClient.deleteContact(deviceId, contact.id, adbPath)
                    .onSuccess {
                        if (!isRequestStillValid(deviceId)) return@onSuccess
                        _state.update { current ->
                            val allContacts = (current.listState as? ContactsListState.Success)
                                ?.contacts?.filter { it.id != contact.id }
                                ?: return@update current
                            current.copy(
                                listState = ContactsListState.Success(allContacts),
                                filteredContacts = applySearch(allContacts, current.searchQuery),
                                selectedContactId = if (current.selectedContactId == contact.id) null else current.selectedContactId,
                                detailState = if (current.selectedContactId == contact.id) ContactDetailState.Idle else current.detailState,
                            )
                        }
                        showFeedback(ContactFeedback("Контакт «${contact.displayName}» удалён", isError = false))
                    }
                    .onFailure { error ->
                        if (!isRequestStillValid(deviceId)) return@onFailure
                        showFeedback(ContactFeedback(error.message ?: "Ошибка удаления", isError = true))
                    }
            } finally {
                _state.update { it.copy(isActionRunning = false) }
            }
        }
    }

    override fun onCancelDelete() {
        _state.update { it.copy(pendingDeleteContact = null) }
    }

    // ── Экспорт ───────────────────────────────────────────────────────────────

    override fun onExportAllToJson(path: String) {
        launchLongOperation(title = "Экспорт контактов в JSON") { deviceId, adbPath ->
            val contacts = (state.value.listState as? ContactsListState.Success)?.contacts.orEmpty()
            if (contacts.isEmpty()) {
                error("Список контактов пуст. Нечего экспортировать.")
            }

            appendOperationLog("Найдено контактов: ${contacts.size}")
            val details = mutableListOf<ContactDetails>()
            contacts.forEachIndexed { index, contact ->
                ensureDeviceStillConnected(deviceId)
                updateOperationProgress(
                    status = "Чтение контактов ${index + 1}/${contacts.size}",
                    currentStep = index + 1,
                    totalSteps = contacts.size,
                )
                appendOperationLog("Чтение: ${contact.displayName.ifBlank { "#${contact.id}" }}")
                val detailsResult = contactsClient.getContactDetails(deviceId, contact.id, adbPath)
                    .getOrElse { throwable ->
                        error("Не удалось прочитать контакт «${contact.displayName}»: ${throwable.message}")
                    }
                details += detailsResult
            }

            updateOperationStatus("Запись файла JSON...", isIndeterminate = true)
            appendOperationLog("Сохранение файла: $path")
            val file = ContactsJsonFile(contacts = details.map { it.toJsonEntry() })
            val jsonText = json.encodeToString(ContactsJsonFile.serializer(), file)
            ContactIoService.writeText(path, jsonText)

            appendOperationLog("Экспорт завершён: ${details.size} контактов")
            showFeedback(ContactFeedback("Экспортировано ${details.size} контактов в JSON", isError = false))
        }
    }

    override fun onExportAllToVcf(path: String) {
        launchLongOperation(title = "Экспорт контактов в VCF") { deviceId, adbPath ->
            val contacts = (state.value.listState as? ContactsListState.Success)?.contacts.orEmpty()
            if (contacts.isEmpty()) {
                error("Список контактов пуст. Нечего экспортировать.")
            }

            appendOperationLog("Найдено контактов: ${contacts.size}")
            val details = mutableListOf<ContactDetails>()
            contacts.forEachIndexed { index, contact ->
                ensureDeviceStillConnected(deviceId)
                updateOperationProgress(
                    status = "Чтение контактов ${index + 1}/${contacts.size}",
                    currentStep = index + 1,
                    totalSteps = contacts.size,
                )
                appendOperationLog("Чтение: ${contact.displayName.ifBlank { "#${contact.id}" }}")
                val detailsResult = contactsClient.getContactDetails(deviceId, contact.id, adbPath)
                    .getOrElse { throwable ->
                        error("Не удалось прочитать контакт «${contact.displayName}»: ${throwable.message}")
                    }
                details += detailsResult
            }

            updateOperationStatus("Запись файла VCF...", isIndeterminate = true)
            appendOperationLog("Сохранение файла: $path")
            val vcfText = VcfSerializer.serializeAll(details)
            ContactIoService.writeText(path, vcfText)

            appendOperationLog("Экспорт завершён: ${details.size} контактов")
            showFeedback(ContactFeedback("Экспортировано ${details.size} контактов в VCF", isError = false))
        }
    }

    override fun onExportContactToJson(contact: Contact, path: String) {
        launchLongOperation(title = "Экспорт контакта в JSON") { deviceId, adbPath ->
            ensureDeviceStillConnected(deviceId)
            updateOperationProgress(
                status = "Чтение контакта",
                currentStep = 1,
                totalSteps = 1,
            )
            appendOperationLog("Чтение: ${contact.displayName.ifBlank { "#${contact.id}" }}")
            val details = contactsClient.getContactDetails(deviceId, contact.id, adbPath)
                .getOrElse { throwable ->
                    error("Не удалось прочитать контакт: ${throwable.message}")
                }

            updateOperationStatus("Запись файла JSON...", isIndeterminate = true)
            appendOperationLog("Сохранение файла: $path")
            val file = ContactsJsonFile(contacts = listOf(details.toJsonEntry()))
            val jsonText = json.encodeToString(ContactsJsonFile.serializer(), file)
            ContactIoService.writeText(path, jsonText)

            appendOperationLog("Экспорт завершён")
            showFeedback(ContactFeedback("Контакт экспортирован в JSON", isError = false))
        }
    }

    override fun onExportContactToVcf(contact: Contact, path: String) {
        launchLongOperation(title = "Экспорт контакта в VCF") { deviceId, adbPath ->
            ensureDeviceStillConnected(deviceId)
            updateOperationProgress(
                status = "Чтение контакта",
                currentStep = 1,
                totalSteps = 1,
            )
            appendOperationLog("Чтение: ${contact.displayName.ifBlank { "#${contact.id}" }}")
            val details = contactsClient.getContactDetails(deviceId, contact.id, adbPath)
                .getOrElse { throwable ->
                    error("Не удалось прочитать контакт: ${throwable.message}")
                }

            updateOperationStatus("Запись файла VCF...", isIndeterminate = true)
            appendOperationLog("Сохранение файла: $path")
            val vcfText = VcfSerializer.serialize(details)
            ContactIoService.writeText(path, vcfText)

            appendOperationLog("Экспорт завершён")
            showFeedback(ContactFeedback("Контакт экспортирован в VCF", isError = false))
        }
    }

    // ── Импорт ────────────────────────────────────────────────────────────────

    override fun onImportFromJson(path: String) {
        launchLongOperation(title = "Импорт контактов из JSON") { deviceId, adbPath ->
            updateOperationStatus("Чтение JSON-файла...", isIndeterminate = true)
            appendOperationLog("Чтение файла: $path")
            val text = ContactIoService.readText(path)
            val file = json.decodeFromString(ContactsJsonFile.serializer(), text)
            val importData = file.contacts.map { it.toImportData() }
            importContactsWithProgress(
                deviceId = deviceId,
                adbPath = adbPath,
                importData = importData,
            )
        }
    }

    override fun onImportFromVcf(path: String) {
        launchLongOperation(title = "Импорт контактов из VCF") { deviceId, adbPath ->
            updateOperationStatus("Чтение VCF-файла...", isIndeterminate = true)
            appendOperationLog("Чтение файла: $path")
            val text = ContactIoService.readText(path)
            val importData = VcfParser.parse(text)
            importContactsWithProgress(
                deviceId = deviceId,
                adbPath = adbPath,
                importData = importData,
            )
        }
    }

    override fun onCancelOperation() {
        operationJob?.cancel(CancellationException("Операция отменена пользователем."))
    }

    private suspend fun importContactsWithProgress(
        deviceId: String,
        adbPath: String,
        importData: List<ContactImportData>,
    ) {
        if (importData.isEmpty()) {
            error("Файл не содержит распознанных контактов.")
        }

        val account = _state.value.selectedAccount
        appendOperationLog("Контактов к импорту: ${importData.size}")
        appendOperationLog("Аккаунт назначения: ${account.uiLabel()}")

        var successCount = 0
        var failedCount = 0
        importData.forEachIndexed { index, source ->
            ensureDeviceStillConnected(deviceId)
            updateOperationProgress(
                status = "Импорт контактов ${index + 1}/${importData.size}",
                currentStep = index + 1,
                totalSteps = importData.size,
            )
            val titledName = source.displayName.ifBlank {
                buildDisplayName(source.firstName, source.lastName).ifBlank { "Без имени" }
            }
            appendOperationLog("Импорт: $titledName")

            val prepared = source.copy(
                accountName = account.accountName,
                accountType = account.accountType,
            )
            contactsClient.addContact(deviceId, prepared.toNewContactData(), adbPath)
                .onSuccess {
                    successCount++
                    appendOperationLog("Успех: $titledName")
                }
                .onFailure { error ->
                    failedCount++
                    appendOperationLog("Ошибка: $titledName — ${error.message ?: "неизвестная ошибка"}")
                }
        }

        if (successCount == 0 && failedCount > 0) {
            error("Не удалось импортировать ни одного контакта. Проверьте аккаунт и права Contacts Provider.")
        }

        appendOperationLog("Импорт завершён: успехов=$successCount, ошибок=$failedCount")
        showFeedback(
            ContactFeedback(
                message = "Импортировано: $successCount, ошибок: $failedCount",
                isError = failedCount > 0,
            ),
        )
        loadContacts()
    }

    // ── Feedback ──────────────────────────────────────────────────────────────

    override fun onDismissFeedback() {
        feedbackJob?.cancel()
        _state.update { it.copy(actionFeedback = null) }
    }

    // ── Вспомогательные функции ───────────────────────────────────────────────

    private fun adbPath(): String =
        settingsRepository.getSettings().adbPath.ifBlank { "adb" }

    private fun requireDeviceAndPath(showFeedbackOnError: Boolean = true): Pair<String, String>? {
        val device = deviceManager.selectedDeviceFlow.value
        if (device == null || device.state != DeviceState.DEVICE) {
            if (showFeedbackOnError) {
                showFeedback(ContactFeedback("Устройство не выбрано или недоступно", isError = true))
            }
            return null
        }
        return device.deviceId to adbPath()
    }

    private fun showFeedback(feedback: ContactFeedback) {
        feedbackJob?.cancel()
        _state.update { it.copy(actionFeedback = feedback) }
        feedbackJob = scope.launch {
            delay(3_000)
            _state.update { it.copy(actionFeedback = null) }
        }
    }

    private fun isRequestStillValid(deviceId: String): Boolean {
        val selected = deviceManager.selectedDeviceFlow.value
        return selected != null &&
            selected.state == DeviceState.DEVICE &&
            selected.deviceId == deviceId &&
            activeDeviceId == deviceId
    }

    private fun ensureDeviceStillConnected(deviceId: String) {
        if (!isRequestStillValid(deviceId)) {
            throw IllegalStateException("Устройство отключено или переключено во время операции.")
        }
    }

    private fun selectAccountForState(
        currentSelected: ContactAccount,
        availableAccounts: List<ContactAccount>,
    ): ContactAccount {
        return availableAccounts.firstOrNull { it.stableKey == currentSelected.stableKey }
            ?: availableAccounts.firstOrNull { !it.isLocal }
            ?: availableAccounts.firstOrNull()
            ?: ContactAccount.local()
    }

    private fun launchLongOperation(
        title: String,
        block: suspend (deviceId: String, adbPath: String) -> Unit,
    ) {
        if (operationJob?.isActive == true) {
            showFeedback(ContactFeedback("Уже выполняется другая операция", isError = true))
            return
        }

        operationJob = scope.launch {
            startOperation(title = title, status = "Подготовка...")
            appendOperationLog("Операция запущена")
            try {
                val (deviceId, adbPath) = requireDeviceAndPath(showFeedbackOnError = false)
                    ?: error("Устройство не выбрано или недоступно.")
                block(deviceId, adbPath)
            } catch (cancelled: CancellationException) {
                val message = cancelled.message ?: "Операция отменена"
                appendOperationLog(message)
                showFeedback(ContactFeedback(message, isError = true))
            } catch (error: Throwable) {
                val message = error.message ?: "Неизвестная ошибка"
                appendOperationLog("Ошибка: $message")
                showFeedback(ContactFeedback(message, isError = true))
            } finally {
                _state.update { it.copy(operationState = null) }
                operationJob = null
            }
        }
    }

    private fun startOperation(title: String, status: String) {
        _state.update {
            it.copy(
                operationState = ContactsOperationState(
                    title = title,
                    status = status,
                    isIndeterminate = true,
                ),
            )
        }
    }

    private fun updateOperationStatus(
        status: String,
        isIndeterminate: Boolean,
    ) {
        _state.update { current ->
            val operation = current.operationState ?: return@update current
            current.copy(
                operationState = operation.copy(
                    status = status,
                    isIndeterminate = isIndeterminate,
                    currentStep = if (isIndeterminate) null else operation.currentStep,
                    totalSteps = if (isIndeterminate) null else operation.totalSteps,
                ),
            )
        }
    }

    private fun updateOperationProgress(
        status: String,
        currentStep: Int,
        totalSteps: Int,
    ) {
        _state.update { current ->
            val operation = current.operationState ?: return@update current
            current.copy(
                operationState = operation.copy(
                    status = status,
                    currentStep = currentStep,
                    totalSteps = totalSteps,
                    isIndeterminate = false,
                ),
            )
        }
    }

    private fun appendOperationLog(message: String) {
        val timestamp = LocalTime.now().format(OPERATION_TIME_FORMAT)
        _state.update { current ->
            val operation = current.operationState ?: return@update current
            val newLogs = (operation.logs + "[$timestamp] $message").takeLast(300)
            current.copy(operationState = operation.copy(logs = newLogs))
        }
    }

    private fun buildDisplayName(firstName: String, lastName: String): String =
        listOf(firstName, lastName).map { it.trim() }.filter { it.isNotEmpty() }.joinToString(" ")

    private fun ContactImportData.toNewContactData(): NewContactData {
        val normalizedDisplayName = displayName.ifBlank {
            buildDisplayName(firstName, lastName)
        }
        return NewContactData(
            firstName = firstName,
            lastName = lastName,
            displayName = normalizedDisplayName.ifBlank { "Без имени" },
            phone1 = phones.getOrNull(0)?.value.orEmpty(),
            phone1Type = phones.getOrNull(0)?.type ?: PhoneType.MOBILE,
            phone2 = phones.getOrNull(1)?.value.orEmpty(),
            phone2Type = phones.getOrNull(1)?.type ?: PhoneType.MOBILE,
            email = emails.getOrNull(0)?.value.orEmpty(),
            emailType = emails.getOrNull(0)?.type ?: EmailType.HOME,
            organization = organization,
            notes = notes,
            accountName = accountName,
            accountType = accountType,
        )
    }

    private companion object {
        val OPERATION_TIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")
    }
}

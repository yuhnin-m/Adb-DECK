package com.adbdeck.feature.contacts

import com.adbdeck.core.adb.api.Contact
import com.adbdeck.core.adb.api.ContactDetails
import com.adbdeck.core.adb.api.ContactImportData
import com.adbdeck.core.adb.api.ContactsClient
import com.adbdeck.core.adb.api.DeviceManager
import com.adbdeck.core.adb.api.DeviceState
import com.adbdeck.core.adb.api.EmailType
import com.adbdeck.core.adb.api.NewContactData
import com.adbdeck.core.adb.api.PhoneType
import com.adbdeck.core.settings.SettingsRepository
import com.adbdeck.feature.contacts.io.ContactIoService
import com.adbdeck.feature.contacts.io.ContactsJsonFile
import com.adbdeck.feature.contacts.io.VcfParser
import com.adbdeck.feature.contacts.io.VcfSerializer
import com.adbdeck.feature.contacts.io.toImportData
import com.adbdeck.feature.contacts.io.toJsonEntry
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.lifecycle.coroutines.coroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

/**
 * Реализация [ContactsComponent].
 *
 * Архитектура:
 * 1. `init` — подписывается на [DeviceManager.selectedDeviceFlow].
 * 2. При изменении устройства перезапускает загрузку списка контактов.
 * 3. Все ADB-операции выполняются в [scope] с проверкой [isRequestStillValid].
 * 4. Экспорт/импорт: файловые пути передаются из UI-слоя (JFileChooser).
 * 5. Feedback-уведомления автоматически скрываются через 3 секунды.
 *
 * @param componentContext   Контекст Decompose (lifecycle, coroutineScope).
 * @param deviceManager      Менеджер устройств (источник активного устройства).
 * @param contactsClient     ADB-клиент для операций с контактами.
 * @param settingsRepository Репозиторий настроек (путь к adb).
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
                        loadJob?.cancel()
                        detailJob?.cancel()
                        activeDeviceId = null
                        _state.update {
                            it.copy(
                                listState          = ContactsListState.NoDevice,
                                filteredContacts   = emptyList(),
                                selectedContactId  = null,
                                detailState        = ContactDetailState.Idle,
                                pendingDeleteContact = null,
                            )
                        }
                    }

                    else -> {
                        val isDeviceChanged = activeDeviceId != device.deviceId
                        val needsReload = _state.value.listState !is ContactsListState.Success
                        if (isDeviceChanged) {
                            detailJob?.cancel()
                            _state.update {
                                it.copy(
                                    selectedContactId    = null,
                                    detailState          = ContactDetailState.Idle,
                                    pendingDeleteContact = null,
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
                    _state.update { current ->
                        val selectedStillExists =
                            current.selectedContactId != null &&
                                contacts.any { it.id == current.selectedContactId }
                        current.copy(
                            listState         = ContactsListState.Success(contacts),
                            filteredContacts  = applySearch(contacts, current.searchQuery),
                            selectedContactId = if (selectedStillExists) current.selectedContactId else null,
                            detailState       = if (selectedStillExists) current.detailState else ContactDetailState.Idle,
                        )
                    }
                }
                .onFailure { error ->
                    if (!isRequestStillValid(requestDeviceId)) return@onFailure
                    _state.update {
                        it.copy(
                            listState        = ContactsListState.Error(error.message ?: "Неизвестная ошибка"),
                            filteredContacts = emptyList(),
                        )
                    }
                }
        }
    }

    override fun onSearchChanged(query: String) {
        _state.update { current ->
            val allContacts = (current.listState as? ContactsListState.Success)?.contacts ?: return
            current.copy(
                searchQuery      = query,
                filteredContacts = applySearch(allContacts, query),
            )
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
                detailState       = ContactDetailState.Loading,
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
                detailState       = ContactDetailState.Idle,
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
        _state.update { it.copy(addForm = AddContactFormState()) }
    }

    override fun onAddFormFirstNameChanged(value: String) {
        _state.update { it.copy(addForm = it.addForm?.copy(firstName = value)) }
    }

    override fun onAddFormLastNameChanged(value: String) {
        _state.update { it.copy(addForm = it.addForm?.copy(lastName = value)) }
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

        // Валидация: хотя бы одно из полей имени обязательно
        if (form.firstName.isBlank() && form.lastName.isBlank()) {
            _state.update {
                it.copy(addForm = form.copy(error = "Введите имя или фамилию"))
            }
            return
        }

        val displayName = buildDisplayName(form.firstName.trim(), form.lastName.trim())

        val contact = NewContactData(
            firstName    = form.firstName.trim(),
            lastName     = form.lastName.trim(),
            displayName  = displayName,
            phone1       = form.phone1.trim(),
            phone1Type   = form.phone1Type,
            phone2       = form.phone2.trim(),
            phone2Type   = form.phone2Type,
            email        = form.email.trim(),
            emailType    = form.emailType,
            organization = form.organization.trim(),
            notes        = form.notes.trim(),
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
                    if (!isRequestStillValid(deviceId)) return@onSuccess
                    _state.update { it.copy(addForm = null) }
                    showFeedback(ContactFeedback("Контакт «$displayName» добавлен", isError = false))
                    loadContacts() // Перезагрузить список
                }
                .onFailure { error ->
                    if (!isRequestStillValid(deviceId)) return@onFailure
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
                        // Убрать контакт из списка без полной перезагрузки
                        _state.update { current ->
                            val allContacts = (current.listState as? ContactsListState.Success)
                                ?.contacts?.filter { it.id != contact.id }
                                ?: return@update current
                            current.copy(
                                listState         = ContactsListState.Success(allContacts),
                                filteredContacts  = applySearch(allContacts, current.searchQuery),
                                selectedContactId = if (current.selectedContactId == contact.id) null else current.selectedContactId,
                                detailState       = if (current.selectedContactId == contact.id) ContactDetailState.Idle else current.detailState,
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
        scope.launch {
            _state.update { it.copy(isActionRunning = true) }
            try {
                val (deviceId, adbPath) = requireDeviceAndPath() ?: return@launch
                val contacts = (state.value.listState as? ContactsListState.Success)
                    ?.contacts ?: return@launch

                // Загружаем полные детали для всех контактов
                val details = mutableListOf<ContactDetails>()
                for (c in contacts) {
                    contactsClient.getContactDetails(deviceId, c.id, adbPath)
                        .onSuccess { details += it }
                }

                if (!isRequestStillValid(deviceId)) return@launch

                val file = ContactsJsonFile(contacts = details.map { it.toJsonEntry() })
                val jsonText = json.encodeToString(ContactsJsonFile.serializer(), file)
                ContactIoService.writeText(path, jsonText)
                showFeedback(ContactFeedback("Экспортировано ${details.size} контактов в JSON", isError = false))
            } catch (e: Exception) {
                showFeedback(ContactFeedback("Ошибка экспорта: ${e.message}", isError = true))
            } finally {
                _state.update { it.copy(isActionRunning = false) }
            }
        }
    }

    override fun onExportAllToVcf(path: String) {
        scope.launch {
            _state.update { it.copy(isActionRunning = true) }
            try {
                val (deviceId, adbPath) = requireDeviceAndPath() ?: return@launch
                val contacts = (state.value.listState as? ContactsListState.Success)
                    ?.contacts ?: return@launch

                val details = mutableListOf<ContactDetails>()
                for (c in contacts) {
                    contactsClient.getContactDetails(deviceId, c.id, adbPath)
                        .onSuccess { details += it }
                }

                if (!isRequestStillValid(deviceId)) return@launch

                val vcfText = VcfSerializer.serializeAll(details)
                ContactIoService.writeText(path, vcfText)
                showFeedback(ContactFeedback("Экспортировано ${details.size} контактов в VCF", isError = false))
            } catch (e: Exception) {
                showFeedback(ContactFeedback("Ошибка экспорта: ${e.message}", isError = true))
            } finally {
                _state.update { it.copy(isActionRunning = false) }
            }
        }
    }

    override fun onExportContactToJson(contact: Contact, path: String) {
        scope.launch {
            _state.update { it.copy(isActionRunning = true) }
            try {
                val (deviceId, adbPath) = requireDeviceAndPath() ?: return@launch

                contactsClient.getContactDetails(deviceId, contact.id, adbPath)
                    .onSuccess { details ->
                        if (!isRequestStillValid(deviceId)) return@onSuccess
                        val file = ContactsJsonFile(contacts = listOf(details.toJsonEntry()))
                        val jsonText = json.encodeToString(ContactsJsonFile.serializer(), file)
                        ContactIoService.writeText(path, jsonText)
                        showFeedback(ContactFeedback("Контакт экспортирован в JSON", isError = false))
                    }
                    .onFailure { e ->
                        showFeedback(ContactFeedback("Ошибка экспорта: ${e.message}", isError = true))
                    }
            } catch (e: Exception) {
                showFeedback(ContactFeedback("Ошибка экспорта: ${e.message}", isError = true))
            } finally {
                _state.update { it.copy(isActionRunning = false) }
            }
        }
    }

    override fun onExportContactToVcf(contact: Contact, path: String) {
        scope.launch {
            _state.update { it.copy(isActionRunning = true) }
            try {
                val (deviceId, adbPath) = requireDeviceAndPath() ?: return@launch

                contactsClient.getContactDetails(deviceId, contact.id, adbPath)
                    .onSuccess { details ->
                        if (!isRequestStillValid(deviceId)) return@onSuccess
                        val vcfText = VcfSerializer.serialize(details)
                        ContactIoService.writeText(path, vcfText)
                        showFeedback(ContactFeedback("Контакт экспортирован в VCF", isError = false))
                    }
                    .onFailure { e ->
                        showFeedback(ContactFeedback("Ошибка экспорта: ${e.message}", isError = true))
                    }
            } catch (e: Exception) {
                showFeedback(ContactFeedback("Ошибка экспорта: ${e.message}", isError = true))
            } finally {
                _state.update { it.copy(isActionRunning = false) }
            }
        }
    }

    // ── Импорт ────────────────────────────────────────────────────────────────

    override fun onImportFromJson(path: String) {
        scope.launch {
            _state.update { it.copy(isActionRunning = true) }
            try {
                val (deviceId, adbPath) = requireDeviceAndPath() ?: return@launch
                val text = ContactIoService.readText(path)
                val file = json.decodeFromString(ContactsJsonFile.serializer(), text)
                val importData: List<ContactImportData> = file.contacts.map { it.toImportData() }

                contactsClient.importContacts(deviceId, importData, adbPath)
                    .onSuccess { result ->
                        if (!isRequestStillValid(deviceId)) return@onSuccess
                        val msg = "Импортировано: ${result.successCount}, ошибок: ${result.failedCount}"
                        showFeedback(ContactFeedback(msg, isError = result.failedCount > 0))
                        loadContacts()
                    }
                    .onFailure { e ->
                        showFeedback(ContactFeedback("Ошибка импорта: ${e.message}", isError = true))
                    }
            } catch (e: Exception) {
                showFeedback(ContactFeedback("Ошибка чтения файла: ${e.message}", isError = true))
            } finally {
                _state.update { it.copy(isActionRunning = false) }
            }
        }
    }

    override fun onImportFromVcf(path: String) {
        scope.launch {
            _state.update { it.copy(isActionRunning = true) }
            try {
                val (deviceId, adbPath) = requireDeviceAndPath() ?: return@launch
                val text = ContactIoService.readText(path)
                val importData = VcfParser.parse(text)

                if (importData.isEmpty()) {
                    showFeedback(ContactFeedback("VCF-файл не содержит распознанных контактов", isError = true))
                    return@launch
                }

                contactsClient.importContacts(deviceId, importData, adbPath)
                    .onSuccess { result ->
                        if (!isRequestStillValid(deviceId)) return@onSuccess
                        val msg = "Импортировано: ${result.successCount}, ошибок: ${result.failedCount}"
                        showFeedback(ContactFeedback(msg, isError = result.failedCount > 0))
                        loadContacts()
                    }
                    .onFailure { e ->
                        showFeedback(ContactFeedback("Ошибка импорта: ${e.message}", isError = true))
                    }
            } catch (e: Exception) {
                showFeedback(ContactFeedback("Ошибка чтения файла: ${e.message}", isError = true))
            } finally {
                _state.update { it.copy(isActionRunning = false) }
            }
        }
    }

    // ── Feedback ──────────────────────────────────────────────────────────────

    override fun onDismissFeedback() {
        feedbackJob?.cancel()
        _state.update { it.copy(actionFeedback = null) }
    }

    // ── Вспомогательные функции ───────────────────────────────────────────────

    private fun adbPath(): String =
        settingsRepository.getSettings().adbPath.ifBlank { "adb" }

    private fun requireDeviceAndPath(): Pair<String, String>? {
        val device = deviceManager.selectedDeviceFlow.value
        if (device == null || device.state != DeviceState.DEVICE) {
            showFeedback(ContactFeedback("Устройство не выбрано или недоступно", isError = true))
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

    private fun buildDisplayName(firstName: String, lastName: String): String =
        listOf(firstName, lastName).filter { it.isNotEmpty() }.joinToString(" ")
}

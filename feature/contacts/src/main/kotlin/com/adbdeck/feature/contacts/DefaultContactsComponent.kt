package com.adbdeck.feature.contacts

import com.adbdeck.core.adb.api.contacts.Contact
import com.adbdeck.core.adb.api.contacts.ContactAccount
import com.adbdeck.core.adb.api.contacts.ContactsClient
import com.adbdeck.core.adb.api.contacts.EmailType
import com.adbdeck.core.adb.api.contacts.PhoneType
import com.adbdeck.core.adb.api.device.DeviceManager
import com.adbdeck.core.settings.SettingsRepository
import com.adbdeck.feature.contacts.models.ContactsState
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.lifecycle.coroutines.coroutineScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json

/**
 * Реализация [ContactsComponent].
 *
 * Оркестрирует подфичи:
 * - device/list/details
 * - add/delete
 * - import/export операции
 */
class DefaultContactsComponent(
    componentContext: ComponentContext,
    internal val deviceManager: DeviceManager,
    internal val contactsClient: ContactsClient,
    internal val settingsRepository: SettingsRepository,
    private val externalScope: CoroutineScope? = null,
) : ContactsComponent, ComponentContext by componentContext {

    internal val scope: CoroutineScope = externalScope ?: coroutineScope()

    internal val _state = MutableStateFlow(ContactsState())
    override val state: StateFlow<ContactsState> = _state.asStateFlow()

    /** JSON с prettyPrint для удобочитаемого экспорта. */
    internal val json = Json {
        prettyPrint = true
        encodeDefaults = true
    }

    // ── Jobs ──────────────────────────────────────────────────────────────────

    /** Job загрузки списка контактов. */
    internal var loadJob: Job? = null

    /** Job загрузки деталей контакта. */
    internal var detailJob: Job? = null

    /** Job длительной операции (импорт/экспорт). */
    internal var operationJob: Job? = null

    /** Job автоочистки feedback-уведомления. */
    internal var feedbackJob: Job? = null

    /** DeviceId, с которым синхронизировано текущее состояние. */
    internal var activeDeviceId: String? = null

    init {
        observeSelectedDevice()
    }

    override fun onRefresh() {
        handleRefresh()
    }

    override fun onSearchChanged(query: String) {
        handleSearchChanged(query)
    }

    override fun onSelectTargetAccount(account: ContactAccount) {
        handleSelectTargetAccount(account)
    }

    override fun onSelectContact(contact: Contact) {
        handleSelectContact(contact)
    }

    override fun onCloseDetail() {
        handleCloseDetail()
    }

    override fun onRefreshDetail() {
        handleRefreshDetail()
    }

    override fun onShowAddForm() {
        handleShowAddForm()
    }

    override fun onShowEditForm() {
        handleShowEditForm()
    }

    override fun onAddFormFirstNameChanged(value: String) {
        handleAddFormFirstNameChanged(value)
    }

    override fun onAddFormLastNameChanged(value: String) {
        handleAddFormLastNameChanged(value)
    }

    override fun onAddFormAccountChanged(account: ContactAccount) {
        handleAddFormAccountChanged(account)
    }

    override fun onAddFormPhone1Changed(value: String) {
        handleAddFormPhone1Changed(value)
    }

    override fun onAddFormPhone1TypeChanged(type: PhoneType) {
        handleAddFormPhone1TypeChanged(type)
    }

    override fun onAddFormPhone2Changed(value: String) {
        handleAddFormPhone2Changed(value)
    }

    override fun onAddFormPhone2TypeChanged(type: PhoneType) {
        handleAddFormPhone2TypeChanged(type)
    }

    override fun onAddFormEmailChanged(value: String) {
        handleAddFormEmailChanged(value)
    }

    override fun onAddFormEmailTypeChanged(type: EmailType) {
        handleAddFormEmailTypeChanged(type)
    }

    override fun onAddFormOrganizationChanged(value: String) {
        handleAddFormOrganizationChanged(value)
    }

    override fun onAddFormNotesChanged(value: String) {
        handleAddFormNotesChanged(value)
    }

    override fun onSubmitAddForm() {
        handleSubmitAddForm()
    }

    override fun onDismissAddForm() {
        handleDismissAddForm()
    }

    override fun onRequestDelete(contact: Contact) {
        handleRequestDelete(contact)
    }

    override fun onConfirmDelete() {
        handleConfirmDelete()
    }

    override fun onCancelDelete() {
        handleCancelDelete()
    }

    override fun onExportAllToJson(path: String) {
        handleExportAllToJson(path)
    }

    override fun onExportAllToVcf(path: String) {
        handleExportAllToVcf(path)
    }

    override fun onExportContactToJson(contact: Contact, path: String) {
        handleExportContactToJson(contact, path)
    }

    override fun onExportContactToVcf(contact: Contact, path: String) {
        handleExportContactToVcf(contact, path)
    }

    override fun onImportFromJson(path: String) {
        handleImportFromJson(path)
    }

    override fun onImportFromVcf(path: String) {
        handleImportFromVcf(path)
    }

    override fun onCancelOperation() {
        handleCancelOperation()
    }

    override fun onDismissSafetyBanner() {
        handleDismissSafetyBanner()
    }

    override fun onDismissFeedback() {
        handleDismissFeedback()
    }
}

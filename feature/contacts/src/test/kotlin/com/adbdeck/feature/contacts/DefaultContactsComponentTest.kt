package com.adbdeck.feature.contacts

import com.adbdeck.core.adb.api.contacts.Contact
import com.adbdeck.core.adb.api.contacts.ContactAccount
import com.adbdeck.core.adb.api.contacts.ContactDetails
import com.adbdeck.core.adb.api.contacts.ContactEmail
import com.adbdeck.core.adb.api.contacts.ContactImportData
import com.adbdeck.core.adb.api.contacts.ContactPhone
import com.adbdeck.core.adb.api.contacts.ContactsClient
import com.adbdeck.core.adb.api.contacts.EmailType
import com.adbdeck.core.adb.api.contacts.ImportResult
import com.adbdeck.core.adb.api.contacts.NewContactData
import com.adbdeck.core.adb.api.contacts.PhoneType
import com.adbdeck.core.adb.api.contacts.RawContactInfo
import com.adbdeck.core.adb.api.device.AdbDevice
import com.adbdeck.core.adb.api.device.DeviceEndpoint
import com.adbdeck.core.adb.api.device.DeviceManager
import com.adbdeck.core.adb.api.device.DeviceState
import com.adbdeck.core.settings.AppSettings
import com.adbdeck.core.settings.SettingsRepository
import com.adbdeck.feature.contacts.models.ContactDetailState
import com.adbdeck.feature.contacts.models.ContactsListState
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.test.resetMain
import kotlin.io.path.createTempFile
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.fail

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultContactsComponentTest {

    private val testDispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `export all contacts to json writes file with contact data`() = runTest(context = testDispatcher) {
        val contact = Contact(
            id = 1L,
            displayName = "Иван Иванов",
            phones = listOf(ContactPhone("+79001234567", PhoneType.MOBILE)),
            primaryEmail = "ivan@example.com",
            accountName = "ivan@gmail.com",
            accountType = "com.google",
        )
        val details = ContactDetails(
            id = 1L,
            displayName = "Иван Иванов",
            firstName = "Иван",
            lastName = "Иванов",
            phones = listOf(ContactPhone("+79001234567", PhoneType.MOBILE)),
            emails = listOf(ContactEmail("ivan@example.com", EmailType.HOME)),
            rawContacts = listOf(RawContactInfo(10L, "ivan@gmail.com", "com.google")),
        )

        val fixture = createFixture(
            contacts = listOf(contact),
            detailsById = mapOf(contact.id to details),
        )
        try {
            awaitCondition("Ожидался загруженный список контактов") {
                fixture.component.state.value.listState is ContactsListState.Success
            }

            val targetFile = createTempFile(prefix = "contacts-export", suffix = ".json")
            fixture.component.onExportAllToJson(targetFile.toString())

            awaitOperationCompleted(fixture.component)

            val exportedText = targetFile.readText()
            assertContains(exportedText, "Иван Иванов")
            assertContains(exportedText, "ivan@example.com")
        } finally {
            fixture.close()
        }
    }

    @Test
    fun `import from json adds contacts through client`() = runTest(context = testDispatcher) {
        val fixture = createFixture()
        try {

            val sourceFile = createTempFile(prefix = "contacts-import", suffix = ".json")
            sourceFile.writeText(
                """
                {
                  "version": 1,
                  "contacts": [
                    {
                      "displayName": "John Doe",
                      "firstName": "John",
                      "lastName": "Doe",
                      "phones": [{ "value": "+12345", "type": "mobile" }],
                      "emails": [{ "value": "john@example.com", "type": "home" }]
                    },
                    {
                      "displayName": "Jane Roe",
                      "firstName": "Jane",
                      "lastName": "Roe",
                      "phones": [],
                      "emails": []
                    }
                  ]
                }
                """.trimIndent(),
            )

            fixture.component.onImportFromJson(sourceFile.toString())

            awaitOperationCompleted(fixture.component)

            assertEquals(2, fixture.contactsClient.addedContacts.size)
            assertEquals("John Doe", fixture.contactsClient.addedContacts[0].displayName)
            assertEquals("Jane Roe", fixture.contactsClient.addedContacts[1].displayName)
        } finally {
            fixture.close()
        }
    }

    @Test
    fun `import from invalid json reports parse error and keeps ui alive`() = runTest(context = testDispatcher) {
        val fixture = createFixture()
        try {

            val sourceFile = createTempFile(prefix = "contacts-invalid", suffix = ".json")
            sourceFile.writeText("{ invalid json")

            fixture.component.onImportFromJson(sourceFile.toString())

            awaitOperationCompleted(fixture.component)

            val feedback = fixture.component.state.value.actionFeedback
            assertNotNull(feedback)
            assertTrue(feedback.isError)
            assertTrue(feedback.message.isNotBlank())
            assertTrue(fixture.contactsClient.addedContacts.isEmpty())
        } finally {
            fixture.close()
        }
    }

    @Test
    fun `import from invalid vcf reports recognized parse error`() = runTest(context = testDispatcher) {
        val fixture = createFixture()
        try {

            val sourceFile = createTempFile(prefix = "contacts-invalid", suffix = ".vcf")
            sourceFile.writeText("THIS IS NOT A VCARD")

            fixture.component.onImportFromVcf(sourceFile.toString())

            awaitOperationCompleted(fixture.component)

            val feedback = fixture.component.state.value.actionFeedback
            assertNotNull(feedback)
            assertTrue(feedback.isError)
            assertTrue(
                feedback.message.contains("распознан", ignoreCase = true) ||
                    feedback.message.contains("recognized", ignoreCase = true),
            )
        } finally {
            fixture.close()
        }
    }

    @Test
    fun `running import is cancelled when active device changes`() = runTest(context = testDispatcher) {
        val fixture = createFixture(addContactDelayMs = 60_000)
        try {
            val sourceFile = createTempFile(prefix = "contacts-import", suffix = ".json")
            sourceFile.writeText(
                """
                {
                  "version": 1,
                  "contacts": [
                    {
                      "displayName": "Delayed Contact",
                      "firstName": "Delayed",
                      "lastName": "Contact"
                    }
                  ]
                }
                """.trimIndent(),
            )

            fixture.component.onImportFromJson(sourceFile.toString())

            awaitCondition(
                errorMessage = "Ожидался запуск длительной операции",
                snapshot = {
                    "operationJob=${fixture.component.operationJob}, " +
                        "operationState=${fixture.component.state.value.operationState}, " +
                        "feedback=${fixture.component.state.value.actionFeedback}"
                },
            ) {
                fixture.component.operationJob != null
            }

            fixture.deviceManager.selectDevice(
                AdbDevice(
                    deviceId = "emulator-5556",
                    state = DeviceState.DEVICE,
                ),
            )

            awaitCondition("Ожидалось завершение операции после смены устройства") {
                fixture.component.state.value.operationState == null
            }

            assertTrue(fixture.contactsClient.addedContacts.isEmpty())
        } finally {
            fixture.close()
        }
    }

    @Test
    fun `edit form updates selected contact through client`() = runTest(context = testDispatcher) {
        val contact = Contact(
            id = 42L,
            displayName = "Иван Иванов",
            phones = listOf(ContactPhone("+79001234567", PhoneType.MOBILE)),
            primaryEmail = "ivan@example.com",
            accountName = "",
            accountType = "",
        )
        val details = ContactDetails(
            id = 42L,
            displayName = "Иван Иванов",
            firstName = "Иван",
            lastName = "Иванов",
            phones = listOf(ContactPhone("+79001234567", PhoneType.MOBILE)),
            emails = listOf(ContactEmail("ivan@example.com", EmailType.HOME)),
            rawContacts = listOf(RawContactInfo(100L, "", "")),
        )
        val fixture = createFixture(
            contacts = listOf(contact),
            detailsById = mapOf(contact.id to details),
        )
        try {
            awaitCondition("Ожидался загруженный список контактов") {
                fixture.component.state.value.listState is ContactsListState.Success
            }

            fixture.component.onSelectContact(contact)

            awaitCondition("Ожидалась загрузка деталей контакта") {
                fixture.component.state.value.detailState is ContactDetailState.Success
            }

            fixture.component.onShowEditForm()

            awaitCondition("Ожидалось открытие формы редактирования") {
                fixture.component.state.value.addForm != null
            }

            fixture.component.onAddFormFirstNameChanged("Пётр")
            fixture.component.onAddFormLastNameChanged("Петров")
            fixture.component.onSubmitAddForm()

            awaitCondition("Ожидалось закрытие формы после сохранения") {
                fixture.component.state.value.addForm == null
            }

            assertEquals(1, fixture.contactsClient.updatedContacts.size)
            val (updatedId, updatedContact) = fixture.contactsClient.updatedContacts.first()
            assertEquals(42L, updatedId)
            assertEquals("Пётр", updatedContact.firstName)
            assertEquals("Петров", updatedContact.lastName)
            assertEquals("Пётр Петров", updatedContact.displayName)
        } finally {
            fixture.close()
        }
    }

    private suspend fun awaitOperationCompleted(component: DefaultContactsComponent) {
        awaitCondition(
            errorMessage = "Ожидалось завершение длительной операции",
            snapshot = {
                "operationJob=${component.operationJob}, " +
                    "operationState=${component.state.value.operationState}, " +
                    "feedback=${component.state.value.actionFeedback}"
            },
        ) {
            component.operationJob == null && component.state.value.operationState == null
        }
    }

    private suspend fun awaitCondition(
        errorMessage: String,
        timeoutMs: Long = 5_000,
        snapshot: () -> String = { "" },
        condition: () -> Boolean,
    ) {
        val scheduler = testDispatcher.scheduler
        val deadline = scheduler.currentTime + timeoutMs
        while (!condition()) {
            scheduler.advanceTimeBy(50)
            scheduler.runCurrent()
            Thread.sleep(5)
            if (scheduler.currentTime >= deadline) {
                val suffix = snapshot().takeIf { it.isNotBlank() }?.let { " [$it]" }.orEmpty()
                fail(errorMessage + suffix)
            }
        }
    }

    private fun createFixture(
        contacts: List<Contact> = emptyList(),
        detailsById: Map<Long, ContactDetails> = emptyMap(),
        addContactDelayMs: Long = 0L,
    ): ContactsFixture {
        val device = AdbDevice(deviceId = "emulator-5554", state = DeviceState.DEVICE)
        val deviceManager = FakeDeviceManager(device)
        val contactsClient = FakeContactsClient(
            contacts = contacts,
            detailsById = detailsById,
            addContactDelayMs = addContactDelayMs,
        )
        val settingsRepository = FakeSettingsRepository()
        val componentScope = CoroutineScope(SupervisorJob() + testDispatcher)

        val lifecycle = LifecycleRegistry()
        val component = DefaultContactsComponent(
            componentContext = DefaultComponentContext(lifecycle),
            deviceManager = deviceManager,
            contactsClient = contactsClient,
            settingsRepository = settingsRepository,
            externalScope = componentScope,
        )
        testDispatcher.scheduler.runCurrent()

        return ContactsFixture(
            component = component,
            deviceManager = deviceManager,
            contactsClient = contactsClient,
            scope = componentScope,
        )
    }

    private data class ContactsFixture(
        val component: DefaultContactsComponent,
        val deviceManager: FakeDeviceManager,
        val contactsClient: FakeContactsClient,
        val scope: CoroutineScope,
    ) {
        fun close() {
            scope.cancel()
        }
    }

    private class FakeContactsClient(
        private val contacts: List<Contact>,
        private val detailsById: Map<Long, ContactDetails>,
        private val addContactDelayMs: Long,
    ) : ContactsClient {

        val addedContacts = mutableListOf<NewContactData>()
        val updatedContacts = mutableListOf<Pair<Long, NewContactData>>()

        override suspend fun getAvailableAccounts(
            deviceId: String,
            adbPath: String,
        ): Result<List<ContactAccount>> = Result.success(listOf(ContactAccount.local()))

        override suspend fun getContacts(
            deviceId: String,
            adbPath: String,
        ): Result<List<Contact>> = Result.success(contacts)

        override suspend fun getContactDetails(
            deviceId: String,
            contactId: Long,
            adbPath: String,
        ): Result<ContactDetails> =
            detailsById[contactId]?.let { Result.success(it) }
                ?: Result.failure(IllegalArgumentException("Contact not found: $contactId"))

        override suspend fun addContact(
            deviceId: String,
            contact: NewContactData,
            adbPath: String,
        ): Result<Unit> {
            if (addContactDelayMs > 0) {
                delay(addContactDelayMs)
            }
            addedContacts += contact
            return Result.success(Unit)
        }

        override suspend fun updateContact(
            deviceId: String,
            contactId: Long,
            contact: NewContactData,
            adbPath: String,
        ): Result<Unit> {
            updatedContacts += contactId to contact
            return Result.success(Unit)
        }

        override suspend fun deleteContact(
            deviceId: String,
            contactId: Long,
            adbPath: String,
        ): Result<Unit> = Result.success(Unit)

        override suspend fun importContacts(
            deviceId: String,
            contacts: List<ContactImportData>,
            adbPath: String,
        ): Result<ImportResult> = Result.success(
            ImportResult(
                successCount = contacts.size,
                failedCount = 0,
                errors = emptyList(),
            ),
        )
    }

    private class FakeDeviceManager(initialDevice: AdbDevice?) : DeviceManager {

        override val devicesFlow: MutableStateFlow<List<AdbDevice>> =
            MutableStateFlow(initialDevice?.let(::listOf).orEmpty())

        override val selectedDeviceFlow: MutableStateFlow<AdbDevice?> = MutableStateFlow(initialDevice)

        override val isConnecting: MutableStateFlow<Boolean> = MutableStateFlow(false)

        override val errorFlow: MutableStateFlow<String?> = MutableStateFlow(null)

        override val savedEndpointsFlow: MutableStateFlow<List<DeviceEndpoint>> = MutableStateFlow(emptyList())

        override suspend fun refresh() = Unit

        override suspend fun connect(host: String, port: Int): Result<String> = Result.success("connected")

        override suspend fun disconnect(deviceId: String): Result<Unit> = Result.success(Unit)

        override suspend fun switchToTcpIp(serialId: String, port: Int): Result<Unit> = Result.success(Unit)

        override fun selectDevice(device: AdbDevice) {
            selectedDeviceFlow.value = device
            devicesFlow.value = listOf(device)
        }

        override suspend fun saveEndpoint(endpoint: DeviceEndpoint) = Unit

        override suspend fun removeEndpoint(endpoint: DeviceEndpoint) = Unit

        override fun clearError() {
            errorFlow.value = null
        }
    }

    private class FakeSettingsRepository : SettingsRepository {

        override val settingsFlow: StateFlow<AppSettings> = MutableStateFlow(AppSettings(adbPath = "adb"))

        override fun getSettings(): AppSettings = settingsFlow.value

        override suspend fun saveSettings(settings: AppSettings) = Unit
    }
}

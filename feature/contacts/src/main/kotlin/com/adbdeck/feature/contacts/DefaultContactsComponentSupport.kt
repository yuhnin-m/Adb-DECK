package com.adbdeck.feature.contacts

import com.adbdeck.core.adb.api.contacts.ContactAccount
import com.adbdeck.core.adb.api.contacts.ContactDetails
import com.adbdeck.core.adb.api.contacts.ContactImportData
import com.adbdeck.core.adb.api.contacts.EmailType
import com.adbdeck.core.adb.api.contacts.NewContactData
import com.adbdeck.core.adb.api.contacts.PhoneType
import com.adbdeck.core.adb.api.device.DeviceState
import com.adbdeck.feature.contacts.io.ContactIoService
import com.adbdeck.feature.contacts.io.ContactsJsonFile
import com.adbdeck.feature.contacts.io.VcfParser
import com.adbdeck.feature.contacts.io.VcfSerializer
import com.adbdeck.feature.contacts.models.ContactFeedback
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.format.DateTimeFormatter

internal val OPERATION_TIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")

internal fun DefaultContactsComponent.handleDismissSafetyBanner() {
    _state.update { it.copy(showSafetyBanner = false) }
}

internal fun DefaultContactsComponent.handleDismissFeedback() {
    feedbackJob?.cancel()
    _state.update { it.copy(actionFeedback = null) }
}

internal fun DefaultContactsComponent.adbPath(): String =
    settingsRepository.getSettings().adbPath.ifBlank { "adb" }

internal fun DefaultContactsComponent.requireDeviceAndPath(showFeedbackOnError: Boolean = true): Pair<String, String>? {
    val device = deviceManager.selectedDeviceFlow.value
    if (device == null || device.state != DeviceState.DEVICE) {
        if (showFeedbackOnError) {
            showFeedback(ContactFeedback("Устройство не выбрано или недоступно", isError = true))
        }
        return null
    }
    return device.deviceId to adbPath()
}

internal fun DefaultContactsComponent.showFeedback(feedback: ContactFeedback) {
    feedbackJob?.cancel()
    _state.update { it.copy(actionFeedback = feedback) }
    feedbackJob = scope.launch {
        delay(3_000)
        _state.update { it.copy(actionFeedback = null) }
    }
}

internal fun DefaultContactsComponent.isRequestStillValid(deviceId: String): Boolean {
    val selected = deviceManager.selectedDeviceFlow.value
    return selected != null &&
        selected.state == DeviceState.DEVICE &&
        selected.deviceId == deviceId &&
        activeDeviceId == deviceId
}

internal fun DefaultContactsComponent.ensureDeviceStillConnected(deviceId: String) {
    if (!isRequestStillValid(deviceId)) {
        throw IllegalStateException("Устройство отключено или переключено во время операции.")
    }
}

internal fun selectAccountForState(
    currentSelected: ContactAccount,
    availableAccounts: List<ContactAccount>,
): ContactAccount {
    return availableAccounts.firstOrNull { it.stableKey == currentSelected.stableKey }
        ?: availableAccounts.firstOrNull { !it.isLocal }
        ?: availableAccounts.firstOrNull()
        ?: ContactAccount.local()
}

internal fun buildDisplayName(firstName: String, lastName: String): String =
    listOf(firstName, lastName).map { it.trim() }.filter { it.isNotEmpty() }.joinToString(" ")

internal fun ContactImportData.toNewContactData(): NewContactData {
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

internal suspend fun readFile(path: String): String = withContext(Dispatchers.IO) {
    ContactIoService.readText(path)
}

internal suspend fun writeFile(path: String, content: String) = withContext(Dispatchers.IO) {
    ContactIoService.writeText(path, content)
}

internal suspend fun parseVcf(text: String): List<ContactImportData> = withContext(Dispatchers.Default) {
    VcfParser.parse(text)
}

internal suspend fun serializeVcf(details: ContactDetails): String = withContext(Dispatchers.Default) {
    VcfSerializer.serialize(details)
}

internal suspend fun serializeVcf(details: List<ContactDetails>): String = withContext(Dispatchers.Default) {
    VcfSerializer.serializeAll(details)
}

internal suspend fun DefaultContactsComponent.encodeJson(file: ContactsJsonFile): String = withContext(Dispatchers.Default) {
    json.encodeToString(ContactsJsonFile.serializer(), file)
}

internal suspend fun DefaultContactsComponent.decodeJson(text: String): ContactsJsonFile = withContext(Dispatchers.Default) {
    json.decodeFromString(ContactsJsonFile.serializer(), text)
}

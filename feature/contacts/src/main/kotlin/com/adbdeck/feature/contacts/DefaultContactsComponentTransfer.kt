package com.adbdeck.feature.contacts

import com.adbdeck.core.adb.api.contacts.Contact
import com.adbdeck.core.adb.api.contacts.ContactDetails
import com.adbdeck.core.adb.api.contacts.ContactImportData
import com.adbdeck.feature.contacts.io.ContactsJsonFile
import com.adbdeck.feature.contacts.io.toImportData
import com.adbdeck.feature.contacts.io.toJsonEntry
import com.adbdeck.feature.contacts.models.ContactFeedback
import com.adbdeck.feature.contacts.models.ContactsListState
import com.adbdeck.feature.contacts.models.ContactsOperationState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalTime

internal fun DefaultContactsComponent.handleExportAllToJson(path: String) {
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
        val jsonText = encodeJson(file)
        writeFile(path, jsonText)

        appendOperationLog("Экспорт завершён: ${details.size} контактов")
        showFeedback(ContactFeedback("Экспортировано ${details.size} контактов в JSON", isError = false))
    }
}

internal fun DefaultContactsComponent.handleExportAllToVcf(path: String) {
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
        val vcfText = serializeVcf(details)
        writeFile(path, vcfText)

        appendOperationLog("Экспорт завершён: ${details.size} контактов")
        showFeedback(ContactFeedback("Экспортировано ${details.size} контактов в VCF", isError = false))
    }
}

internal fun DefaultContactsComponent.handleExportContactToJson(contact: Contact, path: String) {
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
        val jsonText = encodeJson(file)
        writeFile(path, jsonText)

        appendOperationLog("Экспорт завершён")
        showFeedback(ContactFeedback("Контакт экспортирован в JSON", isError = false))
    }
}

internal fun DefaultContactsComponent.handleExportContactToVcf(contact: Contact, path: String) {
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
        val vcfText = serializeVcf(details)
        writeFile(path, vcfText)

        appendOperationLog("Экспорт завершён")
        showFeedback(ContactFeedback("Контакт экспортирован в VCF", isError = false))
    }
}

internal fun DefaultContactsComponent.handleImportFromJson(path: String) {
    launchLongOperation(title = "Импорт контактов из JSON") { deviceId, adbPath ->
        updateOperationStatus("Чтение JSON-файла...", isIndeterminate = true)
        appendOperationLog("Чтение файла: $path")
        val text = readFile(path)
        val file = decodeJson(text)
        val importData = file.contacts.map { it.toImportData() }
        importContactsWithProgress(
            deviceId = deviceId,
            adbPath = adbPath,
            importData = importData,
        )
    }
}

internal fun DefaultContactsComponent.handleImportFromVcf(path: String) {
    launchLongOperation(title = "Импорт контактов из VCF") { deviceId, adbPath ->
        updateOperationStatus("Чтение VCF-файла...", isIndeterminate = true)
        appendOperationLog("Чтение файла: $path")
        val text = readFile(path)
        val importData = parseVcf(text)
        importContactsWithProgress(
            deviceId = deviceId,
            adbPath = adbPath,
            importData = importData,
        )
    }
}

internal fun DefaultContactsComponent.handleCancelOperation() {
    operationJob?.cancel(CancellationException("Операция отменена пользователем."))
}

internal suspend fun DefaultContactsComponent.importContactsWithProgress(
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

internal fun DefaultContactsComponent.launchLongOperation(
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

internal fun DefaultContactsComponent.startOperation(title: String, status: String) {
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

internal fun DefaultContactsComponent.updateOperationStatus(
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

internal fun DefaultContactsComponent.updateOperationProgress(
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

internal fun DefaultContactsComponent.appendOperationLog(message: String) {
    val timestamp = LocalTime.now().format(OPERATION_TIME_FORMAT)
    _state.update { current ->
        val operation = current.operationState ?: return@update current
        val newLogs = (operation.logs + "[$timestamp] $message").takeLast(300)
        current.copy(operationState = operation.copy(logs = newLogs))
    }
}

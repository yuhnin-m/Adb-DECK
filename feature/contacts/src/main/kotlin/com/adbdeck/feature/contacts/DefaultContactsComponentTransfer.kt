package com.adbdeck.feature.contacts

import adbdeck.feature.contacts.generated.resources.Res
import adbdeck.feature.contacts.generated.resources.*
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
import org.jetbrains.compose.resources.StringResource
import java.time.LocalTime

internal fun DefaultContactsComponent.handleExportAllToJson(path: String) {
    launchLongOperation(titleRes = Res.string.contacts_component_operation_title_export_all_json) { deviceId, adbPath ->
        val contacts = (state.value.listState as? ContactsListState.Success)?.contacts.orEmpty()
        if (contacts.isEmpty()) {
            error(contactsString(Res.string.contacts_component_operation_error_empty_contacts))
        }

        appendOperationLog(contactsString(Res.string.contacts_component_operation_log_found_contacts, contacts.size))
        val details = mutableListOf<ContactDetails>()
        contacts.forEachIndexed { index, contact ->
            ensureDeviceStillConnected(deviceId)
            updateOperationProgress(
                status = contactsString(
                    Res.string.contacts_component_operation_status_reading_contacts,
                    index + 1,
                    contacts.size,
                ),
                currentStep = index + 1,
                totalSteps = contacts.size,
            )
            appendOperationLog(
                contactsString(
                    Res.string.contacts_component_operation_log_reading_contact,
                    contact.displayName.ifBlank { "#${contact.id}" },
                ),
            )

            val detailsResult = contactsClient.getContactDetails(deviceId, contact.id, adbPath)
            if (detailsResult.isFailure) {
                error(
                    contactsString(
                        Res.string.contacts_component_operation_error_read_contact_named,
                        contact.displayName,
                        detailsResult.exceptionOrNull()?.message.orEmpty(),
                    ),
                )
            }
            details += detailsResult.getOrThrow()
        }

        updateOperationStatus(
            contactsString(Res.string.contacts_component_operation_status_writing_json),
            isIndeterminate = true,
        )
        appendOperationLog(
            contactsString(Res.string.contacts_component_operation_log_saving_file, path),
        )
        val file = ContactsJsonFile(contacts = details.map { it.toJsonEntry() })
        val jsonText = encodeJson(file)
        writeFile(path, jsonText)

        appendOperationLog(
            contactsString(
                Res.string.contacts_component_operation_log_export_completed_count,
                details.size,
            ),
        )
        showFeedback(
            ContactFeedback(
                contactsString(Res.string.contacts_component_feedback_exported_count_json, details.size),
                isError = false,
            ),
        )
    }
}

internal fun DefaultContactsComponent.handleExportAllToVcf(path: String) {
    launchLongOperation(titleRes = Res.string.contacts_component_operation_title_export_all_vcf) { deviceId, adbPath ->
        val contacts = (state.value.listState as? ContactsListState.Success)?.contacts.orEmpty()
        if (contacts.isEmpty()) {
            error(contactsString(Res.string.contacts_component_operation_error_empty_contacts))
        }

        appendOperationLog(contactsString(Res.string.contacts_component_operation_log_found_contacts, contacts.size))
        val details = mutableListOf<ContactDetails>()
        contacts.forEachIndexed { index, contact ->
            ensureDeviceStillConnected(deviceId)
            updateOperationProgress(
                status = contactsString(
                    Res.string.contacts_component_operation_status_reading_contacts,
                    index + 1,
                    contacts.size,
                ),
                currentStep = index + 1,
                totalSteps = contacts.size,
            )
            appendOperationLog(
                contactsString(
                    Res.string.contacts_component_operation_log_reading_contact,
                    contact.displayName.ifBlank { "#${contact.id}" },
                ),
            )

            val detailsResult = contactsClient.getContactDetails(deviceId, contact.id, adbPath)
            if (detailsResult.isFailure) {
                error(
                    contactsString(
                        Res.string.contacts_component_operation_error_read_contact_named,
                        contact.displayName,
                        detailsResult.exceptionOrNull()?.message.orEmpty(),
                    ),
                )
            }
            details += detailsResult.getOrThrow()
        }

        updateOperationStatus(
            contactsString(Res.string.contacts_component_operation_status_writing_vcf),
            isIndeterminate = true,
        )
        appendOperationLog(
            contactsString(Res.string.contacts_component_operation_log_saving_file, path),
        )
        val vcfText = serializeVcf(details)
        writeFile(path, vcfText)

        appendOperationLog(
            contactsString(
                Res.string.contacts_component_operation_log_export_completed_count,
                details.size,
            ),
        )
        showFeedback(
            ContactFeedback(
                contactsString(Res.string.contacts_component_feedback_exported_count_vcf, details.size),
                isError = false,
            ),
        )
    }
}

internal fun DefaultContactsComponent.handleExportContactToJson(contact: Contact, path: String) {
    launchLongOperation(titleRes = Res.string.contacts_component_operation_title_export_contact_json) { deviceId, adbPath ->
        ensureDeviceStillConnected(deviceId)
        updateOperationProgress(
            status = contactsString(Res.string.contacts_component_operation_status_reading_contact),
            currentStep = 1,
            totalSteps = 1,
        )
        appendOperationLog(
            contactsString(
                Res.string.contacts_component_operation_log_reading_contact,
                contact.displayName.ifBlank { "#${contact.id}" },
            ),
        )
        val detailsResult = contactsClient.getContactDetails(deviceId, contact.id, adbPath)
        if (detailsResult.isFailure) {
            error(
                contactsString(
                    Res.string.contacts_component_operation_error_read_contact,
                    detailsResult.exceptionOrNull()?.message.orEmpty(),
                ),
            )
        }
        val details = detailsResult.getOrThrow()

        updateOperationStatus(
            contactsString(Res.string.contacts_component_operation_status_writing_json),
            isIndeterminate = true,
        )
        appendOperationLog(
            contactsString(Res.string.contacts_component_operation_log_saving_file, path),
        )
        val file = ContactsJsonFile(contacts = listOf(details.toJsonEntry()))
        val jsonText = encodeJson(file)
        writeFile(path, jsonText)

        appendOperationLog(contactsString(Res.string.contacts_component_operation_log_export_completed))
        showFeedback(
            ContactFeedback(
                contactsString(Res.string.contacts_component_feedback_exported_contact_json),
                isError = false,
            ),
        )
    }
}

internal fun DefaultContactsComponent.handleExportContactToVcf(contact: Contact, path: String) {
    launchLongOperation(titleRes = Res.string.contacts_component_operation_title_export_contact_vcf) { deviceId, adbPath ->
        ensureDeviceStillConnected(deviceId)
        updateOperationProgress(
            status = contactsString(Res.string.contacts_component_operation_status_reading_contact),
            currentStep = 1,
            totalSteps = 1,
        )
        appendOperationLog(
            contactsString(
                Res.string.contacts_component_operation_log_reading_contact,
                contact.displayName.ifBlank { "#${contact.id}" },
            ),
        )
        val detailsResult = contactsClient.getContactDetails(deviceId, contact.id, adbPath)
        if (detailsResult.isFailure) {
            error(
                contactsString(
                    Res.string.contacts_component_operation_error_read_contact,
                    detailsResult.exceptionOrNull()?.message.orEmpty(),
                ),
            )
        }
        val details = detailsResult.getOrThrow()

        updateOperationStatus(
            contactsString(Res.string.contacts_component_operation_status_writing_vcf),
            isIndeterminate = true,
        )
        appendOperationLog(
            contactsString(Res.string.contacts_component_operation_log_saving_file, path),
        )
        val vcfText = serializeVcf(details)
        writeFile(path, vcfText)

        appendOperationLog(contactsString(Res.string.contacts_component_operation_log_export_completed))
        showFeedback(
            ContactFeedback(
                contactsString(Res.string.contacts_component_feedback_exported_contact_vcf),
                isError = false,
            ),
        )
    }
}

internal fun DefaultContactsComponent.handleImportFromJson(path: String) {
    launchLongOperation(titleRes = Res.string.contacts_component_operation_title_import_json) { deviceId, adbPath ->
        updateOperationStatus(
            contactsString(Res.string.contacts_component_operation_status_reading_json_file),
            isIndeterminate = true,
        )
        appendOperationLog(
            contactsString(Res.string.contacts_component_operation_log_reading_file, path),
        )
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
    launchLongOperation(titleRes = Res.string.contacts_component_operation_title_import_vcf) { deviceId, adbPath ->
        updateOperationStatus(
            contactsString(Res.string.contacts_component_operation_status_reading_vcf_file),
            isIndeterminate = true,
        )
        appendOperationLog(
            contactsString(Res.string.contacts_component_operation_log_reading_file, path),
        )
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
    scope.launch {
        operationJob?.cancel(
            CancellationException(
                contactsString(Res.string.contacts_component_operation_cancelled_by_user),
            ),
        )
    }
}

internal suspend fun DefaultContactsComponent.importContactsWithProgress(
    deviceId: String,
    adbPath: String,
    importData: List<ContactImportData>,
) {
    if (importData.isEmpty()) {
        error(contactsString(Res.string.contacts_component_operation_error_unrecognized_contacts))
    }

    val account = _state.value.selectedAccount
    appendOperationLog(contactsString(Res.string.contacts_component_operation_log_import_total, importData.size))
    appendOperationLog(
        contactsString(
            Res.string.contacts_component_operation_log_target_account,
            localizedAccountLabel(account),
        ),
    )

    var successCount = 0
    var failedCount = 0
    importData.forEachIndexed { index, source ->
        ensureDeviceStillConnected(deviceId)
        updateOperationProgress(
            status = contactsString(
                Res.string.contacts_component_operation_status_importing_contacts,
                index + 1,
                importData.size,
            ),
            currentStep = index + 1,
            totalSteps = importData.size,
        )
        val titledName = source.displayName.ifBlank {
            buildDisplayName(source.firstName, source.lastName).ifBlank {
                contactsString(Res.string.contacts_component_name_untitled)
            }
        }
        appendOperationLog(
            contactsString(Res.string.contacts_component_operation_log_import_item, titledName),
        )

        val prepared = source.copy(
            accountName = account.accountName,
            accountType = account.accountType,
        )
        val result = contactsClient.addContact(deviceId, prepared.toNewContactData(), adbPath)
        if (result.isSuccess) {
            successCount++
            appendOperationLog(
                contactsString(Res.string.contacts_component_operation_log_success_item, titledName),
            )
        } else {
            failedCount++
            appendOperationLog(
                contactsString(
                    Res.string.contacts_component_operation_log_error_item,
                    titledName,
                    result.exceptionOrNull()?.message
                        ?: contactsString(Res.string.contacts_component_operation_error_unknown_lower),
                ),
            )
        }
    }

    if (successCount == 0 && failedCount > 0) {
        error(contactsString(Res.string.contacts_component_operation_error_import_none))
    }

    appendOperationLog(
        contactsString(
            Res.string.contacts_component_operation_log_import_completed,
            successCount,
            failedCount,
        ),
    )
    showFeedback(
        ContactFeedback(
            message = contactsString(
                Res.string.contacts_component_feedback_import_completed,
                successCount,
                failedCount,
            ),
            isError = failedCount > 0,
        ),
    )
    loadContacts()
}

internal fun DefaultContactsComponent.launchLongOperation(
    titleRes: StringResource,
    vararg titleArgs: Any,
    block: suspend (deviceId: String, adbPath: String) -> Unit,
) {
    if (operationJob?.isActive == true) {
        scope.launch {
            showFeedback(
                ContactFeedback(
                    contactsString(Res.string.contacts_component_feedback_operation_in_progress),
                    isError = true,
                ),
            )
        }
        return
    }

    operationJob = scope.launch {
        val title = contactsString(titleRes, *titleArgs)
        startOperation(
            title = title,
            status = contactsString(Res.string.contacts_component_operation_status_preparing),
        )
        appendOperationLog(contactsString(Res.string.contacts_component_operation_log_started))
        try {
            val (deviceId, adbPath) = requireDeviceAndPath(showFeedbackOnError = false)
                ?: error(contactsString(Res.string.contacts_component_operation_error_device_required))
            block(deviceId, adbPath)
        } catch (cancelled: CancellationException) {
            val message = cancelled.message ?: contactsString(Res.string.contacts_component_operation_cancelled)
            appendOperationLog(message)
            showFeedback(ContactFeedback(message, isError = true))
        } catch (error: Throwable) {
            val message = error.message ?: contactsString(Res.string.contacts_component_error_unknown)
            appendOperationLog(
                contactsString(Res.string.contacts_component_operation_log_error, message),
            )
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

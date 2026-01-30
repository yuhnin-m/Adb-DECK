package com.adbdeck.feature.notifications

import adbdeck.feature.notifications.generated.resources.Res
import adbdeck.feature.notifications.generated.resources.*
import com.adbdeck.core.adb.api.device.DeviceManager
import com.adbdeck.core.adb.api.device.DeviceState
import com.adbdeck.core.adb.api.notifications.NotificationPostRequest
import com.adbdeck.core.adb.api.notifications.NotificationRecord
import com.adbdeck.core.adb.api.notifications.NotificationsClient
import com.adbdeck.core.settings.SettingsRepository
import com.adbdeck.core.utils.runCatchingPreserveCancellation
import com.adbdeck.feature.notifications.storage.NotificationsStorage
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.lifecycle.coroutines.coroutineScope
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.io.File
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString

/**
 * Реализация [NotificationsComponent].
 */
class DefaultNotificationsComponent(
    componentContext: ComponentContext,
    private val deviceManager: DeviceManager,
    private val notificationsClient: NotificationsClient,
    private val settingsRepository: SettingsRepository,
    private val onOpenInPackages: (String) -> Unit,
    private val onOpenInDeepLinks: (String) -> Unit,
) : NotificationsComponent, ComponentContext by componentContext {

    private val scope = coroutineScope()
    private val storage = NotificationsStorage()

    private val _state = MutableStateFlow(NotificationsState())
    override val state: StateFlow<NotificationsState> = _state.asStateFlow()

    private var refreshJob: Job? = null
    private var postNotificationJob: Job? = null
    private var feedbackJob: Job? = null
    private val feedbackMutex = Mutex()

    init {
        scope.launch {
            storage.load()
                .onSuccess { saved ->
                    _state.update { current ->
                        current.copy(savedNotifications = saved).withDisplayed()
                    }
                }
                .onFailure { error ->
                    val details = error.readableDetailsOrNull()
                        ?: getString(Res.string.notifications_error_unknown)
                    showFeedbackMessageResource(
                        messageRes = Res.string.notifications_feedback_storage_load_failed,
                        isError = true,
                        details,
                    )
                }
        }

        scope.launch {
            deviceManager.selectedDeviceFlow.collect { selectedDevice ->
                val nextDeviceId = selectedDevice
                    ?.takeIf { it.state == DeviceState.DEVICE }
                    ?.deviceId
                val previousDeviceId = _state.value.activeDeviceId

                if (nextDeviceId == null) {
                    _state.update { current ->
                        current.copy(
                            activeDeviceId = null,
                            listState = NotificationsListState.NoDevice,
                            currentNotifications = emptyList(),
                            snapshotHistory = emptyList(),
                            selectedKey = null,
                            selectedRecord = null,
                            isRefreshing = false,
                            isPostingNotification = false,
                        ).withDisplayed()
                    }
                    refreshJob?.cancel()
                    postNotificationJob?.cancel()
                    return@collect
                }

                _state.update { current ->
                    current.copy(
                        activeDeviceId = nextDeviceId,
                        listState = if (current.listState is NotificationsListState.NoDevice) {
                            NotificationsListState.Loading
                        } else {
                            current.listState
                        },
                    )
                }

                if (previousDeviceId != nextDeviceId) {
                    onRefresh()
                }
            }
        }
    }

    override fun onRefresh() {
        val selectedDevice = deviceManager.selectedDeviceFlow.value
            ?.takeIf { it.state == DeviceState.DEVICE }
            ?: run {
                _state.update { current ->
                    current.copy(
                        activeDeviceId = null,
                        listState = NotificationsListState.NoDevice,
                        isRefreshing = false,
                    )
                }
                return
            }

        val deviceId = selectedDevice.deviceId
        val adbPath = settingsRepository.getSettings().adbPath.ifBlank { ADB_EXECUTABLE_DEFAULT }

        refreshJob?.cancel()
        _state.update {
            it.copy(
                activeDeviceId = deviceId,
                isRefreshing = true,
                listState = NotificationsListState.Loading,
            )
        }

        refreshJob = scope.launch {
            val unknownErrorMessage = getString(Res.string.notifications_error_unknown)
            val result = notificationsClient.getNotifications(
                deviceId = deviceId,
                adbPath = adbPath,
            )

            if (!isDeviceRequestStillActual(deviceId = deviceId)) return@launch

            result.fold(
                onSuccess = { notifications ->
                    _state.update { current ->
                        val newKeys = notifications.asSequence().map { it.key }.toHashSet()
                        val oldHistoryWithoutDuplicates = current.snapshotHistory
                            .filter { historyItem -> historyItem.key !in newKeys }
                        val mergedHistory = (notifications + oldHistoryWithoutDuplicates).take(MAX_HISTORY)

                        current.copy(
                            listState = NotificationsListState.Success(notifications),
                            currentNotifications = notifications,
                            snapshotHistory = mergedHistory,
                            isRefreshing = false,
                        ).withDisplayed()
                    }
                },
                onFailure = { error ->
                    val message = error.readableDetailsOrNull() ?: unknownErrorMessage
                    _state.update { current ->
                        current.copy(
                            listState = NotificationsListState.Error(message),
                            isRefreshing = false,
                        ).withDisplayed()
                    }
                },
            )
        }
    }

    override fun onSearchChanged(query: String) {
        _state.update { current -> current.copy(searchQuery = query).withDisplayed() }
    }

    override fun onPackageFilterChanged(pkg: String) {
        _state.update { current -> current.copy(packageFilter = pkg).withDisplayed() }
    }

    override fun onFilterChanged(filter: NotificationsFilter) {
        _state.update { current -> current.copy(filter = filter).withDisplayed() }
    }

    override fun onSortOrderChanged(order: NotificationsSortOrder) {
        _state.update { current -> current.copy(sortOrder = order).withDisplayed() }
    }

    override fun onSelectNotification(record: NotificationRecord) {
        _state.update {
            it.copy(
                selectedKey = record.key,
                selectedRecord = record,
                selectedTab = NotificationsTab.DETAILS,
            )
        }
    }

    override fun onCloseDetail() {
        _state.update { it.copy(selectedKey = null, selectedRecord = null) }
    }

    override fun onSelectTab(tab: NotificationsTab) {
        _state.update { it.copy(selectedTab = tab) }
    }

    override fun onCopyPackageName(record: NotificationRecord) {
        copyToClipboardWithFeedback(
            text = record.packageName,
            successRes = Res.string.notifications_feedback_copied_package,
            successArgs = arrayOf(record.packageName),
        )
    }

    override fun onCopyTitle(record: NotificationRecord) {
        val content = record.title ?: record.text ?: record.packageName
        copyToClipboardWithFeedback(
            text = content,
            successRes = Res.string.notifications_feedback_copied_title,
        )
    }

    override fun onCopyText(record: NotificationRecord) {
        val content = record.text ?: record.title ?: record.packageName
        copyToClipboardWithFeedback(
            text = content,
            successRes = Res.string.notifications_feedback_copied_text,
        )
    }

    override fun onCopyRawDump(record: NotificationRecord) {
        copyToClipboardWithFeedback(
            text = record.rawBlock,
            successRes = Res.string.notifications_feedback_copied_raw,
        )
    }

    override fun onSaveNotification(record: NotificationRecord) {
        val newSavedNotification = SavedNotification(
            id = UUID.randomUUID().toString(),
            savedAt = System.currentTimeMillis(),
            record = record,
        )

        _state.update { current ->
            if (current.savedNotifications.any { saved -> saved.record.key == record.key }) {
                return@update current
            }
            val newSavedList = (listOf(newSavedNotification) + current.savedNotifications)
                .take(MAX_SAVED)
            current.copy(savedNotifications = newSavedList).withDisplayed()
        }

        val isSavedNow = _state.value.savedNotifications.any { saved ->
            saved.id == newSavedNotification.id
        }

        if (!isSavedNow) {
            showFeedbackResource(
                messageRes = Res.string.notifications_feedback_already_saved,
                isError = false,
            )
            return
        }

        persistSavedNotificationsAsync()
        showFeedbackResource(
            messageRes = Res.string.notifications_feedback_saved,
            isError = false,
        )
    }

    override fun onDeleteSaved(id: String) {
        _state.update { current ->
            current.copy(
                savedNotifications = current.savedNotifications.filterNot { saved -> saved.id == id },
            ).withDisplayed()
        }
        persistSavedNotificationsAsync()
    }

    override fun onExportToJson(record: NotificationRecord, path: String) {
        scope.launch {
            val unknownErrorMessage = getString(Res.string.notifications_error_unknown)
            val operationResult = runCatchingPreserveCancellation {
                val payload = buildJsonObject {
                    put("key", record.key)
                    put("packageName", record.packageName)
                    put("notificationId", record.notificationId)
                    record.tag?.let { put("tag", it) }
                    put("importance", record.importance)
                    record.channelId?.let { put("channelId", it) }
                    record.title?.let { put("title", it) }
                    record.text?.let { put("text", it) }
                    record.subText?.let { put("subText", it) }
                    record.bigText?.let { put("bigText", it) }
                    record.summaryText?.let { put("summaryText", it) }
                    record.category?.let { put("category", it) }
                    put("flags", record.flags)
                    put("isOngoing", record.isOngoing)
                    put("isClearable", record.isClearable)
                    record.postedAt?.let { put("postedAt", it) }
                    record.group?.let { put("group", it) }
                    record.sortKey?.let { put("sortKey", it) }
                    record.actionsCount?.let { put("actionsCount", it) }
                    if (record.actionTitles.isNotEmpty()) {
                        putJsonArray("actionTitles") {
                            record.actionTitles.forEach { title -> add(JsonPrimitive(title)) }
                        }
                    }
                    if (record.imageParameters.isNotEmpty()) {
                        putJsonObject("imageParameters") {
                            record.imageParameters.forEach { (key, value) ->
                                put(key, JsonPrimitive(value))
                            }
                        }
                    }
                }

                val content = EXPORT_JSON.encodeToString(payload)
                withContext(Dispatchers.IO) {
                    File(path).writeText(content)
                }
            }

            operationResult.onSuccess {
                showFeedbackResource(
                    messageRes = Res.string.notifications_feedback_exported,
                    isError = false,
                    path,
                )
            }.onFailure { error ->
                val details = error.readableDetailsOrNull() ?: unknownErrorMessage
                showFeedbackMessageResource(
                    messageRes = Res.string.notifications_error_export_with_details,
                    isError = true,
                    details,
                )
            }
        }
    }

    override fun onPostNotification(request: NotificationPostRequest) {
        val activeDeviceId = _state.value.activeDeviceId
        if (activeDeviceId.isNullOrBlank()) {
            showFeedbackResource(
                messageRes = Res.string.notifications_error_post_no_device,
                isError = true,
            )
            return
        }

        val normalizedTag = request.tag.trim()
        val normalizedText = request.text.trim()
        if (normalizedTag.isEmpty() || normalizedText.isEmpty()) {
            showFeedbackResource(
                messageRes = Res.string.notifications_error_post_invalid_input,
                isError = true,
            )
            return
        }

        val adbPath = settingsRepository.getSettings().adbPath.ifBlank { ADB_EXECUTABLE_DEFAULT }
        val normalizedRequest = request.copy(
            tag = normalizedTag,
            text = normalizedText,
        )

        postNotificationJob?.cancel()
        _state.update { current -> current.copy(isPostingNotification = true) }

        postNotificationJob = scope.launch {
            val unknownErrorMessage = getString(Res.string.notifications_error_unknown)
            val result = notificationsClient.postNotification(
                deviceId = activeDeviceId,
                request = normalizedRequest,
                adbPath = adbPath,
            )

            if (!isDeviceRequestStillActual(deviceId = activeDeviceId)) {
                _state.update { current -> current.copy(isPostingNotification = false) }
                return@launch
            }

            result.onSuccess {
                _state.update { current -> current.copy(isPostingNotification = false) }
                showFeedbackResource(
                    messageRes = Res.string.notifications_feedback_posted,
                    isError = false,
                    normalizedRequest.tag,
                )
                onRefresh()
            }.onFailure { error ->
                val details = error.readableDetailsOrNull() ?: unknownErrorMessage
                _state.update { current -> current.copy(isPostingNotification = false) }
                showFeedbackMessageResource(
                    messageRes = Res.string.notifications_error_post_with_details,
                    isError = true,
                    details,
                )
            }
        }
    }

    override fun onOpenInPackages(packageName: String) {
        onOpenInPackages.invoke(packageName)
    }

    override fun onOpenInDeepLinks(uri: String) {
        onOpenInDeepLinks.invoke(uri)
    }

    override fun onDismissFeedback() {
        feedbackJob?.cancel()
        _state.update { it.copy(feedback = null) }
    }

    private fun NotificationsState.withDisplayed(): NotificationsState =
        copy(displayedNotifications = calculateDisplayedNotifications(this))

    /**
     * Убедиться, что refresh-ответ все еще относится к актуальному устройству.
     */
    private fun isDeviceRequestStillActual(deviceId: String): Boolean {
        val selectedDevice = deviceManager.selectedDeviceFlow.value
        return selectedDevice != null &&
            selectedDevice.state == DeviceState.DEVICE &&
            selectedDevice.deviceId == deviceId &&
            _state.value.activeDeviceId == deviceId
    }

    private fun copyToClipboardWithFeedback(
        text: String,
        successRes: StringResource,
        successArgs: Array<Any> = emptyArray(),
    ) {
        scope.launch {
            val unknownErrorMessage = getString(Res.string.notifications_error_unknown)
            val copyResult = copyToClipboard(text)

            copyResult.onSuccess {
                showFeedbackMessageResource(
                    messageRes = successRes,
                    isError = false,
                    *successArgs,
                )
            }.onFailure { error ->
                val details = error.readableDetailsOrNull() ?: unknownErrorMessage
                showFeedbackMessageResource(
                    messageRes = Res.string.notifications_error_clipboard_with_details,
                    isError = true,
                    details,
                )
            }
        }
    }

    private fun copyToClipboard(text: String): Result<Unit> = runCatching {
        val selection = StringSelection(text)
        Toolkit.getDefaultToolkit().systemClipboard.setContents(selection, null)
    }

    /**
     * Показывает временный feedback-баннер на 3 секунды.
     */
    private fun showFeedback(message: String, isError: Boolean) {
        feedbackJob?.cancel()
        _state.update {
            it.copy(feedback = NotificationFeedback(message = message, isError = isError))
        }

        feedbackJob = scope.launch {
            delay(FEEDBACK_HIDE_DELAY_MS)
            _state.update { current ->
                if (current.feedback?.message == message) current.copy(feedback = null) else current
            }
        }
    }

    private fun Throwable.readableDetailsOrNull(): String? = message?.takeIf { it.isNotBlank() }

    private fun showFeedbackResource(
        messageRes: StringResource,
        isError: Boolean,
        vararg args: Any,
    ) {
        scope.launch {
            showFeedbackMessageResource(messageRes = messageRes, isError = isError, *args)
        }
    }

    private suspend fun showFeedbackMessageResource(
        messageRes: StringResource,
        isError: Boolean,
        vararg args: Any,
    ) {
        feedbackMutex.withLock {
            val message = getString(messageRes, *args)
            showFeedback(message = message, isError = isError)
        }
    }

    private fun persistSavedNotificationsAsync() {
        val snapshot = _state.value.savedNotifications
        scope.launch {
            val unknownErrorMessage = getString(Res.string.notifications_error_unknown)
            storage.save(snapshot).onFailure { error ->
                val details = error.readableDetailsOrNull() ?: unknownErrorMessage
                showFeedbackMessageResource(
                    messageRes = Res.string.notifications_feedback_storage_save_failed,
                    isError = true,
                    details,
                )
            }
        }
    }

    private companion object {
        val EXPORT_JSON: Json = Json { prettyPrint = true }
        const val MAX_HISTORY = 500
        const val MAX_SAVED = 200
        const val FEEDBACK_HIDE_DELAY_MS = 3_000L
        const val ADB_EXECUTABLE_DEFAULT = "adb"
    }
}

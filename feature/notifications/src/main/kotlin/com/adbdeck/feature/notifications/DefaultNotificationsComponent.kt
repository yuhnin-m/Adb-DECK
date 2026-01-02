package com.adbdeck.feature.notifications

import com.adbdeck.core.adb.api.notifications.NotificationRecord
import com.adbdeck.core.adb.api.notifications.NotificationsClient
import com.adbdeck.core.adb.api.device.DeviceManager
import com.adbdeck.core.settings.SettingsRepository
import com.adbdeck.feature.notifications.storage.NotificationsStorage
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.lifecycle.coroutines.coroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.io.File
import java.util.UUID

/**
 * Реализация [NotificationsComponent].
 *
 * Архитектурные паттерны:
 * - Подписка на [DeviceManager.selectedDeviceFlow] для получения активного устройства.
 * - [refreshJob] — отменяет предыдущий запрос при новом вызове [onRefresh].
 * - `snapshotHistory` — runtime-история до [MAX_HISTORY] записей, не персистируется.
 * - `savedNotifications` — персистентная коллекция через [NotificationsStorage], до [MAX_SAVED] записей.
 * - [applyFiltersAndSort] пересчитывает [NotificationsState.displayedNotifications] при каждом изменении.
 * - Feedback-баннер автоматически скрывается через 3 секунды.
 *
 * @param componentContext      Контекст Decompose-компонента.
 * @param deviceManager         Менеджер ADB-устройств.
 * @param notificationsClient   Клиент для получения уведомлений через ADB.
 * @param settingsRepository    Репозиторий настроек (adbPath).
 * @param onOpenInPackages      Коллбек навигации в экран Packages.
 * @param onOpenInDeepLinks     Коллбек навигации в экран Deep Links.
 */
class DefaultNotificationsComponent(
    componentContext: ComponentContext,
    private val deviceManager: DeviceManager,
    private val notificationsClient: NotificationsClient,
    private val settingsRepository: SettingsRepository,
    private val onOpenInPackages: (String) -> Unit,
    private val onOpenInDeepLinks: (String) -> Unit,
) : NotificationsComponent, ComponentContext by componentContext {

    private val scope   = coroutineScope()
    private val storage = NotificationsStorage()
    private val json    = Json { prettyPrint = true }

    private val _state = MutableStateFlow(NotificationsState())
    override val state: StateFlow<NotificationsState> = _state.asStateFlow()

    private var refreshJob: Job? = null
    private var feedbackJob: Job? = null

    init {
        // Загрузить сохранённые уведомления из файла
        scope.launch {
            val saved = storage.load()
            _state.update { it.copy(savedNotifications = saved).withDisplayed() }
        }

        // Подписка на изменение активного устройства
        scope.launch {
            deviceManager.selectedDeviceFlow.collect { device ->
                val msg = if (device != null) "Устройство: ${device.deviceId}" else "Активное устройство не выбрано"
                // withDisplayed() не нужен — данные ещё не загружены,
                // списки пусты или неизменны, пересчёт бессмысленен
                _state.update { st ->
                    st.copy(
                        activeDeviceId = device?.deviceId,
                        deviceMessage  = msg,
                        listState      = if (device == null) NotificationsListState.NoDevice else st.listState,
                    )
                }
                if (device != null) onRefresh()
            }
        }
    }

    // ── Загрузка ──────────────────────────────────────────────────────────────

    override fun onRefresh() {
        val deviceId = _state.value.activeDeviceId ?: return

        refreshJob?.cancel()
        // withDisplayed() не нужен — пока listState = Loading список всё равно не отображается
        _state.update { it.copy(isRefreshing = true, listState = NotificationsListState.Loading) }

        refreshJob = scope.launch {
            val adbPath = runCatching {
                settingsRepository.settingsFlow.first().adbPath.ifBlank { "adb" }
            }.getOrDefault("adb")

            // Проверяем, что устройство не изменилось пока ждали
            if (_state.value.activeDeviceId != deviceId) return@launch

            val result = notificationsClient.getNotifications(deviceId = deviceId, adbPath = adbPath)

            if (_state.value.activeDeviceId != deviceId) return@launch

            result.fold(
                onSuccess = { notifications ->
                    _state.update { st ->
                        // Добавить в snapshot новые ключи
                        val newKeys      = notifications.map { it.key }.toSet()
                        val historyOld   = st.snapshotHistory.filter { it.key !in newKeys }
                        val mergedHistory = (notifications + historyOld).take(MAX_HISTORY)

                        st.copy(
                            listState            = NotificationsListState.Success(notifications),
                            currentNotifications  = notifications,
                            snapshotHistory      = mergedHistory,
                            isRefreshing         = false,
                        ).withDisplayed()
                    }
                },
                onFailure = { error ->
                    _state.update { st ->
                        st.copy(
                            listState    = NotificationsListState.Error(error.message ?: "Неизвестная ошибка"),
                            isRefreshing = false,
                        ).withDisplayed()
                    }
                },
            )
        }
    }

    // ── Фильтрация и поиск ───────────────────────────────────────────────────

    override fun onSearchChanged(query: String) {
        _state.update { it.copy(searchQuery = query).withDisplayed() }
    }

    override fun onPackageFilterChanged(pkg: String) {
        _state.update { it.copy(packageFilter = pkg).withDisplayed() }
    }

    override fun onFilterChanged(filter: NotificationsFilter) {
        _state.update { it.copy(filter = filter).withDisplayed() }
    }

    override fun onSortOrderChanged(order: NotificationsSortOrder) {
        _state.update { it.copy(sortOrder = order).withDisplayed() }
    }

    // ── Выбор и детали ───────────────────────────────────────────────────────

    override fun onSelectNotification(record: NotificationRecord) {
        _state.update { it.copy(selectedKey = record.key, selectedRecord = record, selectedTab = NotificationsTab.DETAILS) }
    }

    override fun onCloseDetail() {
        _state.update { it.copy(selectedKey = null, selectedRecord = null) }
    }

    override fun onSelectTab(tab: NotificationsTab) {
        _state.update { it.copy(selectedTab = tab) }
    }

    // ── Действия ─────────────────────────────────────────────────────────────

    override fun onCopyPackageName(record: NotificationRecord) {
        copyToClipboard(record.packageName)
        showFeedback("Скопировано: ${record.packageName}")
    }

    override fun onCopyTitle(record: NotificationRecord) {
        val text = record.title ?: record.text ?: record.packageName
        copyToClipboard(text)
        showFeedback("Заголовок скопирован")
    }

    override fun onCopyText(record: NotificationRecord) {
        val text = record.text ?: record.title ?: record.packageName
        copyToClipboard(text)
        showFeedback("Текст скопирован")
    }

    override fun onCopyRawDump(record: NotificationRecord) {
        copyToClipboard(record.rawBlock)
        showFeedback("Исходный блок скопирован")
    }

    override fun onSaveNotification(record: NotificationRecord) {
        val st = _state.value
        // Не сохранять дубли по key
        if (st.savedNotifications.any { it.record.key == record.key }) {
            showFeedback("Уведомление уже сохранено")
            return
        }
        val saved = SavedNotification(
            id      = UUID.randomUUID().toString(),
            savedAt = System.currentTimeMillis(),
            record  = record,
        )
        val newList = (listOf(saved) + st.savedNotifications).take(MAX_SAVED)
        _state.update { it.copy(savedNotifications = newList).withDisplayed() }
        persistAsync()
        showFeedback("Уведомление сохранено в коллекцию")
    }

    override fun onDeleteSaved(id: String) {
        _state.update { st ->
            st.copy(savedNotifications = st.savedNotifications.filter { it.id != id }).withDisplayed()
        }
        persistAsync()
    }

    override fun onExportToJson(record: NotificationRecord, path: String) {
        scope.launch {
            runCatching {
                val obj = buildJsonObject {
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
                val content = json.encodeToString(obj)
                withContext(Dispatchers.IO) { File(path).writeText(content) }
                showFeedback("Экспортировано: $path")
            }.onFailure { e ->
                showFeedback("Ошибка экспорта: ${e.message}", isError = true)
            }
        }
    }

    // ── Интеграция ───────────────────────────────────────────────────────────

    override fun onOpenInPackages(packageName: String) {
        onOpenInPackages.invoke(packageName)
    }

    override fun onOpenInDeepLinks(uri: String) {
        onOpenInDeepLinks.invoke(uri)
    }

    // ── Обратная связь ───────────────────────────────────────────────────────

    override fun onDismissFeedback() {
        feedbackJob?.cancel()
        _state.update { it.copy(feedback = null) }
    }

    // ── Вспомогательные функции ──────────────────────────────────────────────

    /**
     * Пересчитать [NotificationsState.displayedNotifications] на основе текущих фильтров.
     */
    private fun NotificationsState.withDisplayed(): NotificationsState =
        copy(displayedNotifications = applyFiltersAndSort(this))

    /**
     * Применить фильтры и сортировку к источнику записей.
     */
    private fun applyFiltersAndSort(st: NotificationsState): List<NotificationRecord> {
        val source: List<NotificationRecord> = when (st.filter) {
            NotificationsFilter.ALL -> {
                val currentKeys = st.currentNotifications.map { it.key }.toSet()
                val historicalOnly = st.snapshotHistory.filter { it.key !in currentKeys }
                (st.currentNotifications + historicalOnly).distinctBy { it.key }
            }
            NotificationsFilter.CURRENT -> st.currentNotifications
            NotificationsFilter.HISTORICAL -> {
                val currentKeys = st.currentNotifications.map { it.key }.toSet()
                st.snapshotHistory.filter { it.key !in currentKeys }
            }
            NotificationsFilter.SAVED -> st.savedNotifications.map { it.record }
        }

        val filtered = source
            .filter { st.searchQuery.isBlank() || it.matchesQuery(st.searchQuery) }
            .filter { st.packageFilter.isBlank() || it.packageName.contains(st.packageFilter, ignoreCase = true) }

        return when (st.sortOrder) {
            NotificationsSortOrder.NEWEST_FIRST -> filtered.sortedByDescending { it.postedAt ?: 0L }
            NotificationsSortOrder.OLDEST_FIRST -> filtered.sortedBy { it.postedAt ?: Long.MAX_VALUE }
            NotificationsSortOrder.BY_PACKAGE   -> filtered.sortedBy { it.packageName }
        }
    }

    /**
     * Проверить, совпадает ли запись с поисковой строкой.
     * Ищет в packageName, title и text.
     */
    private fun NotificationRecord.matchesQuery(query: String): Boolean {
        val q = query.trim()
        if (q.isBlank()) return true
        return packageName.contains(q, ignoreCase = true) ||
            title?.contains(q, ignoreCase = true) == true ||
            text?.contains(q, ignoreCase = true) == true
    }

    /**
     * Скопировать текст в системный буфер обмена.
     */
    private fun copyToClipboard(text: String) {
        runCatching {
            val selection = StringSelection(text)
            Toolkit.getDefaultToolkit().systemClipboard.setContents(selection, null)
        }
    }

    /**
     * Показать временный баннер обратной связи.
     * Автоматически скрывается через 3 секунды.
     */
    private fun showFeedback(message: String, isError: Boolean = false) {
        feedbackJob?.cancel()
        _state.update { it.copy(feedback = NotificationFeedback(message = message, isError = isError)) }
        feedbackJob = scope.launch {
            delay(3000)
            _state.update { it.copy(feedback = null) }
        }
    }

    /**
     * Асинхронно сохранить текущий список saved в хранилище.
     */
    private fun persistAsync() {
        scope.launch {
            storage.save(_state.value.savedNotifications)
        }
    }

    private companion object {
        /** Максимальное количество записей в runtime-истории. */
        const val MAX_HISTORY = 500

        /** Максимальное количество сохранённых пользователем уведомлений. */
        const val MAX_SAVED = 200
    }
}

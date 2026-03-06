package com.adbdeck.feature.logcat

import com.adbdeck.core.adb.api.device.DeviceManager
import com.adbdeck.core.adb.api.device.DeviceState
import com.adbdeck.core.adb.api.logcat.LogcatEntry
import com.adbdeck.core.adb.api.logcat.LogcatLevel
import com.adbdeck.core.adb.api.logcat.LogcatParser
import com.adbdeck.core.adb.api.logcat.LogcatStreamer
import com.adbdeck.core.adb.api.monitoring.SystemMonitorClient
import com.adbdeck.core.adb.api.monitoring.process.ProcessInfo
import com.adbdeck.core.adb.api.packages.PackageClient
import com.adbdeck.core.settings.SettingsRepository
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.lifecycle.coroutines.coroutineScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

private const val PACKAGE_PID_REFRESH_INTERVAL_MS = 3_000L

/**
 * Реализация [LogcatComponent].
 *
 * ## Архитектура потока данных
 * ```
 * adb logcat (OS process)
 *   → logcatStreamer.stream() : Flow<String>   [callbackFlow, IO]
 *   → LogcatParser.parse()
 *   → pendingChannel : Channel<LogcatEntry>    [capacity = 4096]
 *   → Batcher (каждые 200 мс, Main dispatcher)
 *   → _state.update()
 *   → UI
 * ```
 *
 * ## Безопасность ресурсов
 * - OS-процесс уничтожается при отмене [streamJob] через [awaitClose] в [SystemLogcatStreamer].
 * - [coroutineScope] Essenty отменяется при разрушении Decompose-компонента.
 * - [pendingChannel] дренируется перед [onClear], чтобы батчер не вернул старые данные.
 *
 * ## Производительность
 * - UI обновляется ≤ 5 раз/сек (батч 200 мс) вместо раза на каждую строку.
 * - Буфер ограничен [maxBufferedLines] — FIFO, старые строки удаляются.
 * - [applyFilters] O(n) только на изменение данных или фильтра.
 * - LazyColumn использует `key { entry.id }` — нет лишних recomposition.
 */
class DefaultLogcatComponent(
    componentContext: ComponentContext,
    private val deviceManager: DeviceManager,
    private val logcatStreamer: LogcatStreamer,
    private val systemMonitorClient: SystemMonitorClient,
    private val packageClient: PackageClient,
    private val settingsRepository: SettingsRepository,
) : LogcatComponent, ComponentContext by componentContext {

    private val scope = coroutineScope()

    private val _state = MutableStateFlow(
        settingsRepository.getSettings().let { s ->
            LogcatState(
                displayMode = if (s.logcatCompactMode) LogcatDisplayMode.COMPACT else LogcatDisplayMode.FULL,
                showDate = s.logcatShowDate,
                showTime = s.logcatShowTime,
                showMillis = s.logcatShowMillis,
                coloredLevels = s.logcatColoredLevels,
                autoScroll = s.logcatAutoScroll,
                maxBufferedLines = s.logcatMaxBufferedLines.coerceAtLeast(100),
                activeDeviceId = deviceManager.selectedDeviceFlow.value?.deviceId,
                fontFamily = LogcatFontFamily.fromString(s.logcatFontFamily),
                fontSizeSp = s.logcatFontSizeSp.coerceIn(8, 24),
            )
        }
    )
    override val state: StateFlow<LogcatState> = _state.asStateFlow()

    /**
     * Channel между IO-читателем и UI-батчером.
     * При переполнении `trySend` отбрасывает строки (acceptable — лучше drop, чем OOM).
     */
    private val pendingChannel = Channel<LogcatEntry>(capacity = 4096)
    private val entriesBuffer = ArrayDeque<LogcatEntry>()

    private var streamJob: Job? = null
    private var packageSuggestionsJob: Job? = null
    private var packagePidRefreshJob: Job? = null
    private var packagePidRefreshDeviceId: String? = null
    private var packagePidSnapshotJob: Job? = null
    private var packageSuggestionsDeviceId: String? = null
    private var pidPackageIndex: Map<String, Set<String>> = emptyMap()

    init {
        // Батчер: каждые 200 мс сливает канал в единый state.update
        scope.launch {
            while (isActive) {
                delay(200)
                drainAndUpdateState()
            }
        }

        // Остановка потока при смене активного устройства
        scope.launch {
            deviceManager.selectedDeviceFlow.collect { device ->
                val streamingFor = _state.value.activeDeviceId
                if (_state.value.isRunning && device?.deviceId != streamingFor) {
                    stopStream(reason = null)
                }

                val nextDeviceId = when {
                    device == null -> null
                    device.state != DeviceState.DEVICE -> null
                    else -> device.deviceId
                }

                if (packageSuggestionsDeviceId != nextDeviceId) {
                    packageSuggestionsDeviceId = nextDeviceId
                    loadPackageSuggestions(deviceId = nextDeviceId)
                    pidPackageIndex = emptyMap()
                    packagePidSnapshotJob?.cancel()
                    updatePackagePidRefreshJob()
                    refreshPackagePidIndexOnce()
                }
            }
        }
    }

    // ── Stream control ─────────────────────────────────────────────────────────

    override fun onStart() {
        val device = deviceManager.selectedDeviceFlow.value
        when {
            device == null -> {
                _state.update {
                    it.copy(error = LogcatError.NoActiveDevice)
                }
                return
            }
            device.state != DeviceState.DEVICE -> {
                _state.update {
                    it.copy(
                        error = LogcatError.DeviceUnavailable(
                            deviceId = device.deviceId,
                            deviceStateRaw = device.state.rawValue,
                        )
                    )
                }
                return
            }
            _state.value.isRunning -> return
        }

        val adbPath = settingsRepository.resolvedAdbPath()
        val maxLines = settingsRepository.getSettings().logcatMaxBufferedLines.coerceAtLeast(100)
        val current = _state.value

        trimBufferTo(maxLines)
        val trimmedEntries = entriesBuffer.toList()
        val trimmedFiltered = applyFilters(trimmedEntries, buildFilter(current))

        _state.update {
            it.copy(
                isRunning = true,
                activeDeviceId = device.deviceId,
                error = null,
                maxBufferedLines = maxLines,
                entries = trimmedEntries,
                filteredEntries = trimmedFiltered,
            )
        }
        refreshPackagePidIndexOnce()
        updatePackagePidRefreshJob()

        streamJob = scope.launch {
            try {
                logcatStreamer.stream(device.deviceId, adbPath).collect { line ->
                    pendingChannel.trySend(LogcatParser.parse(line))
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(error = LogcatError.StreamFailure(details = e.message)) }
            } finally {
                _state.update { it.copy(isRunning = false) }
                updatePackagePidRefreshJob()
            }
        }
    }

    override fun onStop() = stopStream(reason = null)

    override fun onClear() {
        // Вычистим канал иначе сборщик добавит старые данные после очистки
        while (pendingChannel.tryReceive().isSuccess) { /* drain */ }
        entriesBuffer.clear()
        _state.update {
            it.copy(
                entries = emptyList(),
                filteredEntries = emptyList(),
                totalLineCount = 0,
                error = null,
            )
        }
    }

    private fun stopStream(reason: LogcatError?) {
        streamJob?.cancel()
        streamJob = null
        _state.update { it.copy(isRunning = false, error = reason) }
        updatePackagePidRefreshJob()
    }

    /**
     * Загружает список пакетов активного устройства для autocomplete-фильтра.
     */
    private fun loadPackageSuggestions(deviceId: String?) {
        packageSuggestionsJob?.cancel()

        if (deviceId == null) {
            _state.update {
                it.copy(
                    packageSuggestions = emptyList(),
                    isPackageSuggestionsLoading = false,
                )
            }
            return
        }

        _state.update {
            it.copy(
                packageSuggestions = emptyList(),
                isPackageSuggestionsLoading = true,
            )
        }

        packageSuggestionsJob = scope.launch {
            val adbPath = settingsRepository.resolvedAdbPath()

            packageClient.getPackages(deviceId = deviceId, adbPath = adbPath)
                .onSuccess { packages ->
                    val packageNames = packages.asSequence()
                        .map { it.packageName }
                        .filter { it.isNotBlank() }
                        .distinct()
                        .toList()

                    _state.update { current ->
                        val selectedDeviceId = deviceManager.selectedDeviceFlow.value
                            ?.takeIf { it.state == DeviceState.DEVICE }
                            ?.deviceId
                        if (selectedDeviceId != deviceId) {
                            current
                        } else {
                            current.copy(
                                packageSuggestions = packageNames,
                                isPackageSuggestionsLoading = false,
                            )
                        }
                    }
                }
                .onFailure {
                    _state.update { current ->
                        val selectedDeviceId = deviceManager.selectedDeviceFlow.value
                            ?.takeIf { it.state == DeviceState.DEVICE }
                            ?.deviceId
                        if (selectedDeviceId != deviceId) {
                            current
                        } else {
                            current.copy(
                                packageSuggestions = emptyList(),
                                isPackageSuggestionsLoading = false,
                            )
                        }
                    }
                }
        }
    }

    private fun trimBufferTo(maxLines: Int) {
        val overflow = (entriesBuffer.size - maxLines).coerceAtLeast(0)
        repeat(overflow) { entriesBuffer.removeFirst() }
    }

    // Сборщик

    /**
     *  собирает их пачкой и обновляет state редко, но крупно
     *  adb logcat может выдавать тысячи строк в секунду
     */
    private fun drainAndUpdateState() {
        val batch = mutableListOf<LogcatEntry>()
        while (true) {
            val next = pendingChannel.tryReceive().getOrNull() ?: break
            batch.add(next)
        }
        if (batch.isEmpty()) return

        _state.update { current ->
            batch.forEach(entriesBuffer::addLast)
            val previousSize = entriesBuffer.size - batch.size
            trimBufferTo(current.maxBufferedLines)
            val overflow = (previousSize + batch.size - current.maxBufferedLines).coerceAtLeast(0)

            val newEntries = entriesBuffer.toList()
            val filter = buildFilter(current)
            val hasActiveFilter = hasActiveFilter(filter)

            val newFiltered = when {
                !hasActiveFilter -> newEntries
                overflow == 0 -> {
                    val matchedBatch = batch.filter { matchesFilter(it, filter) }
                    if (matchedBatch.isEmpty()) current.filteredEntries else current.filteredEntries + matchedBatch
                }
                else -> applyFilters(newEntries, filter)
            }

            current.copy(
                entries = newEntries,
                filteredEntries = newFiltered,
                totalLineCount = current.totalLineCount + batch.size,
            )
        }
    }

    // Фильры

    override fun onSearchChanged(query: String) = updateFilter { copy(searchQuery = query) }
    override fun onTagFilterChanged(tag: String) = updateFilter { copy(tagFilter = tag) }

    override fun onPackageFilterChanged(pkg: String) {
        updateFilter { copy(packageFilter = pkg) }
        refreshPackagePidIndexOnce()
        updatePackagePidRefreshJob()
    }

    override fun onLevelFilterChanged(level: LogcatLevel?) = updateFilter { copy(levelFilter = level) }

    private inline fun updateFilter(transform: LogcatState.() -> LogcatState) {
        _state.update { current ->
            val updated = current.transform()
            val filter = buildFilter(updated)
            updated.copy(filteredEntries = applyFilters(current.entries, filter))
        }
    }

    private data class LogcatFilter(
        val search: String,
        val tag: String,
        val pkg: String,
        val level: LogcatLevel?,
    )

    private fun buildFilter(state: LogcatState): LogcatFilter = LogcatFilter(
        search = state.searchQuery.trim(),
        tag = state.tagFilter.trim(),
        pkg = state.packageFilter.trim(),
        level = state.levelFilter,
    )

    private fun hasActiveFilter(filter: LogcatFilter): Boolean =
        filter.search.isNotEmpty() || filter.tag.isNotEmpty() || filter.pkg.isNotEmpty() || filter.level != null

    private fun applyFilters(entries: List<LogcatEntry>, filter: LogcatFilter): List<LogcatEntry> {
        if (!hasActiveFilter(filter)) return entries
        return entries.filter { matchesFilter(it, filter) }
    }

    private fun matchesFilter(entry: LogcatEntry, filter: LogcatFilter): Boolean {
        if (filter.search.isNotEmpty()) {
            val matchesSearch = entry.tag.contains(filter.search, ignoreCase = true) ||
                entry.message.contains(filter.search, ignoreCase = true)
            if (!matchesSearch) return false
        }
        if (filter.tag.isNotEmpty() && !entry.tag.contains(filter.tag, ignoreCase = true)) return false
        if (filter.pkg.isNotEmpty() && !matchesPackageFilter(entry, filter.pkg)) return false
        if (filter.level != null && entry.level.priority < filter.level.priority) return false
        return true
    }

    /**
     * Проверяет соответствие package-фильтру по PID строки лога.
     *
     * Индекс PID пополняется снапшотами процессов через [SystemMonitorClient].
     */
    private fun matchesPackageFilter(entry: LogcatEntry, packageFilter: String): Boolean {
        if (entry.pid.isBlank()) return false
        val candidates = pidPackageIndex[entry.pid] ?: return false
        return candidates.any { candidate ->
            candidate.contains(packageFilter, ignoreCase = true)
        }
    }

    /**
     * Обновляет периодический refresh `PID -> package/process` только когда это нужно.
     *
     * Требуется, чтобы package filter оставался корректным при рестартах процессов.
     */
    private fun updatePackagePidRefreshJob() {
        val state = _state.value
        val deviceId = selectedOnlineDeviceId()
        val shouldRun = state.isRunning && state.packageFilter.isNotBlank() && deviceId != null

        if (!shouldRun) {
            packagePidRefreshJob?.cancel()
            packagePidRefreshJob = null
            packagePidRefreshDeviceId = null
            return
        }

        if (packagePidRefreshJob?.isActive == true && packagePidRefreshDeviceId == deviceId) return

        packagePidRefreshJob?.cancel()
        packagePidRefreshDeviceId = deviceId
        packagePidRefreshJob = scope.launch {
            while (isActive) {
                val currentDeviceId = selectedOnlineDeviceId()
                val currentState = _state.value
                if (currentDeviceId != deviceId || !currentState.isRunning || currentState.packageFilter.isBlank()) {
                    break
                }

                refreshPackagePidIndex(deviceId = deviceId)
                delay(PACKAGE_PID_REFRESH_INTERVAL_MS)
            }
        }
    }

    /**
     * Однократный refresh индекса PID.
     *
     * Запускается при изменении package filter и при старте стрима для быстрого отклика.
     */
    private fun refreshPackagePidIndexOnce() {
        val packageFilter = _state.value.packageFilter
        val deviceId = selectedOnlineDeviceId()
        if (packageFilter.isBlank() || deviceId == null) return

        packagePidSnapshotJob?.cancel()
        packagePidSnapshotJob = scope.launch {
            refreshPackagePidIndex(deviceId = deviceId)
        }
    }

    /**
     * Забирает список процессов устройства и пополняет индекс `PID -> package/process`.
     *
     * Старые PID не удаляются, чтобы фильтрация сохранялась для уже полученных строк;
     * актуальные PID переопределяются свежими значениями.
     */
    private suspend fun refreshPackagePidIndex(deviceId: String) {
        val adbPath = settingsRepository.resolvedAdbPath()
        systemMonitorClient.getProcessSnapshot(deviceId = deviceId, adbPath = adbPath)
            .onSuccess { snapshot ->
                val fresh = mutableMapOf<String, Set<String>>()
                snapshot.processes.forEach { process ->
                    val pid = process.pid.toString()
                    val candidates = buildPackageCandidates(process)
                    if (candidates.isNotEmpty()) {
                        fresh[pid] = candidates
                    }
                }

                if (fresh.isEmpty()) return@onSuccess

                val merged = pidPackageIndex.toMutableMap()
                fresh.forEach { (pid, candidates) ->
                    merged[pid] = candidates
                }
                pidPackageIndex = merged

                if (_state.value.packageFilter.isNotBlank()) {
                    reapplyAllFilters()
                }
            }
    }

    /**
     * Кандидаты для package match: packageName и processName (+ базовое имя до `:`).
     */
    private fun buildPackageCandidates(process: ProcessInfo): Set<String> {
        val out = linkedSetOf<String>()
        fun add(value: String) {
            val normalized = value.trim()
            if (normalized.isNotEmpty()) {
                out.add(normalized)
                out.add(normalized.substringBefore(':'))
            }
        }
        add(process.packageName)
        add(process.name)
        return out
    }

    private fun selectedOnlineDeviceId(): String? = deviceManager.selectedDeviceFlow.value
        ?.takeIf { it.state == DeviceState.DEVICE }
        ?.deviceId

    private fun reapplyAllFilters() {
        _state.update { current ->
            val filter = buildFilter(current)
            current.copy(filteredEntries = applyFilters(current.entries, filter))
        }
    }

    // Отображение вывода

    /**
     * Компактный или полный режим отображения
     */
    override fun onDisplayModeChanged(mode: LogcatDisplayMode) = _state.update { it.copy(displayMode = mode) }

    /**
     * отбражать дату или нет
     */
    override fun onToggleShowDate() = _state.update { it.copy(showDate = !it.showDate) }

    override fun onToggleShowTime() = _state.update { it.copy(showTime = !it.showTime) }
    override fun onToggleShowMillis() = _state.update { it.copy(showMillis = !it.showMillis) }
    override fun onToggleColoredLevels() = _state.update { it.copy(coloredLevels = !it.coloredLevels) }
    override fun onAutoScrollChanged(value: Boolean) = _state.update { it.copy(autoScroll = value) }

    // ── Font ───────────────────────────────────────────────────────────────────

    override fun onFontFamilyChanged(family: LogcatFontFamily) {
        _state.update { it.copy(fontFamily = family) }
        saveFontSettings()
    }

    override fun onFontSizeChanged(size: Int) {
        val coerced = size.coerceIn(8, 24)
        _state.update { it.copy(fontSizeSp = coerced) }
        saveFontSettings()
    }

    /** Сохраняет только шрифтовые настройки асинхронно. */
    private fun saveFontSettings() {
        scope.launch {
            val current = settingsRepository.getSettings()
            settingsRepository.saveSettings(
                current.copy(
                    logcatFontFamily = _state.value.fontFamily.name,
                    logcatFontSizeSp = _state.value.fontSizeSp,
                )
            )
        }
    }
}

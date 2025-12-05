package com.adbdeck.feature.logcat

import com.adbdeck.core.adb.api.device.DeviceManager
import com.adbdeck.core.adb.api.device.DeviceState
import com.adbdeck.core.adb.api.logcat.LogcatEntry
import com.adbdeck.core.adb.api.logcat.LogcatLevel
import com.adbdeck.core.adb.api.logcat.LogcatParser
import com.adbdeck.core.adb.api.logcat.LogcatStreamer
import com.adbdeck.core.settings.SettingsRepository
import com.adbdeck.feature.logcat.LogcatFontFamily
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
            }
        }
    }

    // ── Stream control ─────────────────────────────────────────────────────────

    override fun onStart() {
        val device = deviceManager.selectedDeviceFlow.value
        when {
            device == null -> {
                _state.update {
                    it.copy(error = "Нет активного устройства. Выберите устройство в верхней панели.")
                }
                return
            }
            device.state != DeviceState.DEVICE -> {
                _state.update {
                    it.copy(error = "Устройство ${device.deviceId} недоступно (${device.state.rawValue}).")
                }
                return
            }
            _state.value.isRunning -> return
        }

        val adbPath = settingsRepository.getSettings().adbPath.ifBlank { "adb" }
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

        streamJob = scope.launch {
            try {
                logcatStreamer.stream(device.deviceId, adbPath).collect { line ->
                    pendingChannel.trySend(LogcatParser.parse(line))
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(error = "Ошибка потока logcat: ${e.message}") }
            } finally {
                _state.update { it.copy(isRunning = false) }
            }
        }
    }

    override fun onStop() = stopStream(reason = null)

    override fun onClear() {
        // Дренируем канал — иначе батчер добавит старые данные после очистки
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

    private fun stopStream(reason: String?) {
        streamJob?.cancel()
        streamJob = null
        _state.update { it.copy(isRunning = false, error = reason) }
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
    override fun onPackageFilterChanged(pkg: String) = updateFilter { copy(packageFilter = pkg) }
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
        if (filter.pkg.isNotEmpty() && !entry.tag.contains(filter.pkg, ignoreCase = true)) return false
        if (filter.level != null && entry.level.priority < filter.level.priority) return false
        return true
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

package com.adbdeck.feature.logcat

import com.adbdeck.core.adb.api.DeviceManager
import com.adbdeck.core.adb.api.DeviceState
import com.adbdeck.core.adb.api.LogcatEntry
import com.adbdeck.core.adb.api.LogcatLevel
import com.adbdeck.core.adb.api.LogcatParser
import com.adbdeck.core.adb.api.LogcatStreamer
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
            )
        }
    )
    override val state: StateFlow<LogcatState> = _state.asStateFlow()

    /**
     * Channel между IO-читателем и UI-батчером.
     * При переполнении `trySend` отбрасывает строки (acceptable — лучше drop, чем OOM).
     */
    private val pendingChannel = Channel<LogcatEntry>(capacity = 4096)

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

        _state.update {
            it.copy(
                isRunning = true,
                activeDeviceId = device!!.deviceId,
                error = null,
                maxBufferedLines = maxLines,
            )
        }

        streamJob = scope.launch {
            try {
                logcatStreamer.stream(device!!.deviceId, adbPath).collect { line ->
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
        @Suppress("ControlFlowWithEmptyBody")
        while (pendingChannel.tryReceive().isSuccess) { /* drain */ }
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

    // ── Batcher ────────────────────────────────────────────────────────────────

    private fun drainAndUpdateState() {
        val batch = buildList<LogcatEntry> {
            while (true) add(pendingChannel.tryReceive().getOrNull() ?: break)
        }
        if (batch.isEmpty()) return

        _state.update { current ->
            val combined = current.entries + batch
            val newEntries = if (combined.size > current.maxBufferedLines) {
                combined.subList(combined.size - current.maxBufferedLines, combined.size)
            } else combined

            current.copy(
                entries = newEntries,
                filteredEntries = applyFilters(newEntries, current),
                totalLineCount = current.totalLineCount + batch.size,
            )
        }
    }

    // ── Filters ────────────────────────────────────────────────────────────────

    override fun onSearchChanged(query: String) = updateFilter { copy(searchQuery = query) }
    override fun onTagFilterChanged(tag: String) = updateFilter { copy(tagFilter = tag) }
    override fun onPackageFilterChanged(pkg: String) = updateFilter { copy(packageFilter = pkg) }
    override fun onLevelFilterChanged(level: LogcatLevel?) = updateFilter { copy(levelFilter = level) }

    private inline fun updateFilter(transform: LogcatState.() -> LogcatState) {
        _state.update { current ->
            val updated = current.transform()
            updated.copy(filteredEntries = applyFilters(current.entries, updated))
        }
    }

    private fun applyFilters(entries: List<LogcatEntry>, state: LogcatState): List<LogcatEntry> {
        val search = state.searchQuery.trim()
        val tagF = state.tagFilter.trim()
        val pkgF = state.packageFilter.trim()
        val levelF = state.levelFilter
        if (search.isEmpty() && tagF.isEmpty() && pkgF.isEmpty() && levelF == null) return entries
        return entries.filter { e ->
            (search.isEmpty() || e.tag.contains(search, ignoreCase = true) || e.message.contains(search, ignoreCase = true)) &&
                    (tagF.isEmpty() || e.tag.contains(tagF, ignoreCase = true)) &&
                    (pkgF.isEmpty() || e.tag.contains(pkgF, ignoreCase = true)) &&
                    (levelF == null || e.level.priority >= levelF.priority)
        }
    }

    // ── Display ────────────────────────────────────────────────────────────────

    override fun onDisplayModeChanged(mode: LogcatDisplayMode) = _state.update { it.copy(displayMode = mode) }
    override fun onToggleShowDate() = _state.update { it.copy(showDate = !it.showDate) }
    override fun onToggleShowTime() = _state.update { it.copy(showTime = !it.showTime) }
    override fun onToggleShowMillis() = _state.update { it.copy(showMillis = !it.showMillis) }
    override fun onToggleColoredLevels() = _state.update { it.copy(coloredLevels = !it.coloredLevels) }
    override fun onAutoScrollChanged(value: Boolean) = _state.update { it.copy(autoScroll = value) }
}

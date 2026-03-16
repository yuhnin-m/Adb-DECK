package com.adbdeck.feature.filesystem

import adbdeck.feature.file_system.generated.resources.Res
import adbdeck.feature.file_system.generated.resources.*
import com.adbdeck.core.adb.api.device.AdbDevice
import com.adbdeck.core.adb.api.device.DeviceManager
import com.adbdeck.core.adb.api.device.DeviceState
import com.adbdeck.core.adb.api.files.DeviceFileClient
import com.adbdeck.core.adb.api.monitoring.SystemMonitorClient
import com.adbdeck.core.adb.api.monitoring.storage.StoragePartition
import com.adbdeck.core.adb.api.monitoring.storage.StorageSummary
import com.adbdeck.core.settings.SettingsRepository
import com.adbdeck.core.utils.runCatchingPreserveCancellation
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.lifecycle.coroutines.coroutineScope
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import kotlin.math.roundToLong
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString

/**
 * Реализация [FileSystemComponent] с защитой от stale-ответов при смене устройства.
 */
class DefaultFileSystemComponent(
    componentContext: ComponentContext,
    private val deviceManager: DeviceManager,
    private val systemMonitorClient: SystemMonitorClient,
    private val deviceFileClient: DeviceFileClient,
    private val settingsRepository: SettingsRepository,
    private val openInFileExplorer: (String) -> Unit = {},
) : FileSystemComponent, ComponentContext by componentContext {

    private val scope = coroutineScope()

    private val _state = MutableStateFlow(FileSystemState())
    override val state: StateFlow<FileSystemState> = _state.asStateFlow()

    private var fetchJob: Job? = null
    private var cleanupJob: Job? = null
    private var activeDeviceId: String? = null

    init {
        scope.launch {
            deviceManager.selectedDeviceFlow.collect { device ->
                handleDeviceChange(device)
            }
        }
    }

    override fun onRefresh() {
        val device = deviceManager.selectedDeviceFlow.value
        if (device == null) {
            resetNoDevice()
            return
        }
        if (device.state != DeviceState.DEVICE) {
            resetDeviceError(device)
            return
        }

        activeDeviceId = device.deviceId
        _state.update {
            it.copy(
                listState = FileSystemListState.Loading,
                contentAnalysis = ContentAnalysisState.Loading,
            )
        }
        launchFetch(device.deviceId)
    }

    override fun onOpenCleanup() {
        _state.update { current ->
            current.copy(cleanup = current.cleanup.copy(isDialogOpen = true))
        }
    }

    override fun onDismissCleanup() {
        if (_state.value.cleanup.running) return
        _state.update { current ->
            current.copy(
                cleanup = current.cleanup.copy(
                    isDialogOpen = false,
                    isConfirmDialogOpen = false,
                )
            )
        }
    }

    override fun onToggleCleanupOption(option: CleanupOption) {
        if (_state.value.cleanup.running) return
        _state.update { current ->
            val selected = current.cleanup.selectedOptions.toMutableSet()
            if (!selected.add(option)) {
                selected.remove(option)
            }
            current.copy(
                cleanup = current.cleanup.copy(selectedOptions = selected),
            )
        }
    }

    override fun onStartCleanup() {
        val cleanup = _state.value.cleanup
        if (cleanup.running || cleanup.selectedOptions.isEmpty()) return

        val device = deviceManager.selectedDeviceFlow.value
        if (device == null || device.state != DeviceState.DEVICE) return

        _state.update { current ->
            current.copy(cleanup = current.cleanup.copy(isConfirmDialogOpen = true))
        }
    }

    override fun onConfirmCleanup() {
        if (_state.value.cleanup.running) return

        val device = deviceManager.selectedDeviceFlow.value
        if (device == null || device.state != DeviceState.DEVICE) {
            resetNoDevice()
            return
        }

        val selectedOptions = _state.value.cleanup.selectedOptions
        if (selectedOptions.isEmpty()) return

        _state.update { current ->
            current.copy(cleanup = current.cleanup.copy(isConfirmDialogOpen = false))
        }

        cleanupJob?.cancel()
        cleanupJob = scope.launch {
            runCleanup(deviceId = device.deviceId, selectedOptions = selectedOptions)
        }
    }

    override fun onDismissCleanupConfirm() {
        if (_state.value.cleanup.running) return
        _state.update { current ->
            current.copy(cleanup = current.cleanup.copy(isConfirmDialogOpen = false))
        }
    }

    override fun onCancelCleanup() {
        cleanupJob?.cancel()
    }

    override fun onCopyCleanupLog() {
        val text = _state.value.cleanup.log.trim()
        if (text.isBlank()) return

        scope.launch {
            val result = runCatchingPreserveCancellation {
                val selection = StringSelection(text)
                Toolkit.getDefaultToolkit().systemClipboard.setContents(selection, null)
            }

            result.onSuccess {
                val copied = getString(Res.string.file_system_cleanup_log_copied)
                appendCleanupLog(copied)
            }

            result.onFailure { error ->
                val unknownError = getString(Res.string.file_system_cleanup_log_unknown_error)
                val details = error.message ?: unknownError
                val failed = getString(Res.string.file_system_cleanup_log_copy_failed, details)
                appendCleanupLog(failed)
            }
        }
    }

    override fun onOpenPartition(path: String) {
        val normalized = normalizePath(path)
        if (normalized.isBlank()) return
        openInFileExplorer(normalized)
    }

    private fun handleDeviceChange(device: AdbDevice?) {
        when {
            device == null -> {
                fetchJob?.cancel()
                fetchJob = null
                cleanupJob?.cancel()
                cleanupJob = null
                resetNoDevice()
            }

            device.state != DeviceState.DEVICE -> {
                fetchJob?.cancel()
                fetchJob = null
                cleanupJob?.cancel()
                cleanupJob = null
                resetDeviceError(device)
            }

            else -> {
                val isChanged = activeDeviceId != device.deviceId
                val needsReload = _state.value.listState !is FileSystemListState.Success
                activeDeviceId = device.deviceId

                if (isChanged || needsReload) {
                    _state.update {
                        it.copy(
                            listState = FileSystemListState.Loading,
                            contentAnalysis = ContentAnalysisState.Loading,
                        )
                    }
                    launchFetch(device.deviceId)
                }
            }
        }
    }

    private fun launchFetch(deviceId: String) {
        fetchJob?.cancel()
        fetchJob = scope.launch {
            fetchStorageAndAnalysis(deviceId)
        }
    }

    private suspend fun fetchStorageAndAnalysis(deviceId: String) {
        if (!isRequestStillValid(deviceId)) return

        val adbPath = settingsRepository.resolvedAdbPath()
        val storageResult = runCatchingPreserveCancellation {
            systemMonitorClient.getStorageInfo(deviceId, adbPath)
        }.getOrElse { error -> Result.failure(error) }

        if (!isRequestStillValid(deviceId)) return

        storageResult.fold(
            onSuccess = { partitions ->
                if (partitions.isEmpty()) {
                    val message = getString(Res.string.file_system_error_data_empty)
                    _state.update {
                        it.copy(
                            listState = FileSystemListState.Error(message),
                            contentAnalysis = ContentAnalysisState.Idle,
                        )
                    }
                    return@fold
                }

                val relevant = partitions.filter { it.isRelevant }
                if (relevant.isEmpty()) {
                    val message = getString(Res.string.file_system_error_no_relevant_partitions)
                    _state.update {
                        it.copy(
                            listState = FileSystemListState.Error(message),
                            contentAnalysis = ContentAnalysisState.Idle,
                        )
                    }
                    return@fold
                }

                val partitionItems = enrichPartitionsWithOpenPath(
                    deviceId = deviceId,
                    adbPath = adbPath,
                    partitions = partitions,
                )

                if (!isRequestStillValid(deviceId)) return@fold

                val summary = StorageSummary(
                    totalKb = relevant.sumOf { it.totalKb },
                    usedKb = relevant.sumOf { it.usedKb },
                    freeKb = relevant.sumOf { it.freeKb },
                )

                val contentState = loadContentAnalysis(
                    deviceId = deviceId,
                    adbPath = adbPath,
                )

                if (!isRequestStillValid(deviceId)) return@fold

                _state.update {
                    it.copy(
                        listState = FileSystemListState.Success(
                            partitions = partitionItems,
                            summary = summary,
                        ),
                        contentAnalysis = contentState,
                    )
                }
            },
            onFailure = { error ->
                val fallback = getString(Res.string.file_system_error_fetch_failed)
                _state.update {
                    it.copy(
                        listState = FileSystemListState.Error(error.message ?: fallback),
                        contentAnalysis = ContentAnalysisState.Idle,
                    )
                }
            },
        )
    }

    private suspend fun enrichPartitionsWithOpenPath(
        deviceId: String,
        adbPath: String,
        partitions: List<StoragePartition>,
    ): List<FileSystemPartitionItem> {
        return partitions.map { partition ->
            if (!partition.isRelevant) {
                return@map FileSystemPartitionItem(partition = partition, openPath = null)
            }

            val openPath = resolveOpenPath(
                deviceId = deviceId,
                adbPath = adbPath,
                mountPoint = partition.mountPoint,
            )

            FileSystemPartitionItem(
                partition = partition,
                openPath = openPath,
            )
        }
    }

    private suspend fun resolveOpenPath(
        deviceId: String,
        adbPath: String,
        mountPoint: String,
    ): String? {
        val candidates = toBrowsablePaths(mountPoint)
        if (candidates.isEmpty()) return null

        for (candidate in candidates) {
            val canOpen = runCatchingPreserveCancellation {
                deviceFileClient.canAccessDirectory(
                    deviceId = deviceId,
                    directoryPath = candidate,
                    adbPath = adbPath,
                )
                    .getOrDefault(false)
            }.getOrDefault(false)

            if (canOpen) return candidate
        }

        return null
    }

    private suspend fun loadContentAnalysis(
        deviceId: String,
        adbPath: String,
    ): ContentAnalysisState {
        val diskstatsResult = runCatchingPreserveCancellation {
            systemMonitorClient.getDiskstats(deviceId = deviceId, adbPath = adbPath)
        }.getOrElse { error -> Result.failure(error) }

        return diskstatsResult.fold(
            onSuccess = { raw ->
                val analysis = parseDiskstats(raw)
                if (analysis.hasValues()) {
                    ContentAnalysisState.Success(analysis)
                } else {
                    val message = getString(Res.string.file_system_error_diskstats_empty)
                    ContentAnalysisState.Error(message)
                }
            },
            onFailure = { error ->
                val fallback = getString(Res.string.file_system_error_diskstats_failed)
                ContentAnalysisState.Error(error.message ?: fallback)
            },
        )
    }

    private suspend fun runCleanup(
        deviceId: String,
        selectedOptions: Set<CleanupOption>,
    ) {
        val cleanupCommands = CleanupCommands.commandsFor(selectedOptions)

        if (cleanupCommands.isEmpty()) return

        val adbPath = settingsRepository.resolvedAdbPath()
        val commandFailedFallback = getString(Res.string.file_system_cleanup_log_command_failed)
        val startLog = getString(
            Res.string.file_system_cleanup_log_started,
            cleanupCommands.size,
        )
        val doneWithErrorsLog = getString(Res.string.file_system_cleanup_log_done_with_errors)
        val doneSuccessLog = getString(Res.string.file_system_cleanup_log_done_success)
        val cancelledLog = getString(Res.string.file_system_cleanup_log_cancelled)
        val unexpectedErrorFallback = getString(Res.string.file_system_cleanup_log_unexpected_error)

        _state.update { current ->
            current.copy(
                cleanup = current.cleanup.copy(
                    running = true,
                    status = CleanupStatus.RUNNING,
                    progress = 0f,
                    isConfirmDialogOpen = false,
                )
            )
        }
        appendCleanupLog(startLog)

        try {
            var hasErrors = false

            cleanupCommands.forEachIndexed { index, command ->
                val step = index + 1
                val total = cleanupCommands.size
                val progress = (step - 1).toFloat() / total.toFloat()

                _state.update { current ->
                    current.copy(cleanup = current.cleanup.copy(progress = progress))
                }

                appendCleanupLog("[$step/$total] $command")

                val commandResult = runCatchingPreserveCancellation {
                    systemMonitorClient.runShellCommand(
                        deviceId = deviceId,
                        shellCommand = command,
                        adbPath = adbPath,
                    )
                }.getOrElse { error -> Result.failure(error) }

                commandResult.fold(
                    onSuccess = { shellResult ->
                        if (shellResult.stdout.isNotBlank()) {
                            appendCleanupLog("[stdout] ${shellResult.stdout.trim()}")
                        }
                        if (shellResult.stderr.isNotBlank()) {
                            appendCleanupLog("[stderr] ${shellResult.stderr.trim()}")
                        }
                        if (shellResult.exitCode != 0) {
                            hasErrors = true
                            appendCleanupLog("[error] exit code ${shellResult.exitCode}")
                        }
                    },
                    onFailure = { error ->
                        hasErrors = true
                        appendCleanupLog("[error] ${error.message ?: commandFailedFallback}")
                    },
                )

                _state.update { current ->
                    current.copy(
                        cleanup = current.cleanup.copy(
                            progress = step.toFloat() / total.toFloat(),
                        )
                    )
                }
            }

            _state.update { current ->
                current.copy(
                    cleanup = current.cleanup.copy(
                        running = false,
                        progress = 1f,
                        status = if (hasErrors) CleanupStatus.ERROR else CleanupStatus.SUCCESS,
                    )
                )
            }

            appendCleanupLog(
                if (hasErrors) {
                    doneWithErrorsLog
                } else {
                    doneSuccessLog
                }
            )
        } catch (_: CancellationException) {
            _state.update { current ->
                current.copy(
                    cleanup = current.cleanup.copy(
                        running = false,
                        progress = 0f,
                        status = CleanupStatus.IDLE,
                    )
                )
            }
            appendCleanupLog(cancelledLog)
        } catch (error: Throwable) {
            _state.update { current ->
                current.copy(
                    cleanup = current.cleanup.copy(
                        running = false,
                        status = CleanupStatus.ERROR,
                    )
                )
            }
            appendCleanupLog("[error] ${error.message ?: unexpectedErrorFallback}")
        }
    }

    private fun appendCleanupLog(line: String) {
        _state.update { current ->
            val log = appendLog(
                current = current.cleanup.log,
                line = line,
            )
            current.copy(cleanup = current.cleanup.copy(log = log))
        }
    }

    private fun appendLog(
        current: String,
        line: String,
        maxLines: Int = 500,
    ): String {
        val normalized = line.trim()
        if (normalized.isBlank()) return current

        val lines = buildList {
            if (current.isNotBlank()) {
                addAll(current.lines().filter { it.isNotBlank() })
            }
            add(normalized)
        }

        return lines.takeLast(maxLines).joinToString(separator = "\n")
    }

    private fun parseDiskstats(output: String): ContentAnalysis {
        val dataLine = parseDataFreeLine(output)

        return ContentAnalysis(
            appSizeKb = parseLabeledSizeKb(output, "App Size"),
            appDataSizeKb = parseLabeledSizeKb(output, "App Data Size"),
            appCacheSizeKb = parseLabeledSizeKb(output, "App Cache Size"),
            photosSizeKb = parseLabeledSizeKb(output, "Photos Size"),
            videosSizeKb = parseLabeledSizeKb(output, "Videos Size"),
            audioSizeKb = parseLabeledSizeKb(output, "Audio Size"),
            downloadsSizeKb = parseLabeledSizeKb(output, "Downloads Size"),
            otherSizeKb = parseLabeledSizeKb(output, "Other Size"),
            systemSizeKb = parseLabeledSizeKb(output, "System Size"),
            dataFreeKb = dataLine.first,
            dataTotalKb = dataLine.second,
        )
    }

    private fun parseDataFreeLine(output: String): Pair<Long?, Long?> {
        val regex = Regex(
            pattern = """(?im)^\s*Data-Free:\s*([^\r\n/]+)\s*/\s*([^\r\n]+?)(?:\s+total\b|\s*=|$)""",
        )
        val match = regex.find(output) ?: return null to null

        val freeKb = parseSizeToKb(match.groupValues[1])
        val totalKb = parseSizeToKb(match.groupValues[2])
        return freeKb to totalKb
    }

    private fun parseLabeledSizeKb(output: String, label: String): Long? {
        val regex = Regex(
            pattern = """(?im)^\s*${Regex.escape(label)}\s*:\s*([^\r\n]+)$""",
        )
        val match = regex.find(output) ?: return null
        return parseSizeToKb(match.groupValues[1])
    }

    /**
     * Парсит размер в KB.
     *
     * Поддерживает форматы: `12345`, `41194052K`, `116G`, `2.5M`, `1GiB`.
     * Для значений без юнита предполагаются bytes.
     */
    private fun parseSizeToKb(raw: String): Long? {
        val cleaned = raw.trim().replace(",", "")
        if (cleaned.isBlank()) return null

        val match = SIZE_REGEX.find(cleaned) ?: return null
        val value = match.groupValues[1].toDoubleOrNull() ?: return null
        if (value < 0.0) return null

        val unit = match.groupValues[2].uppercase()
        val kb = when (unit) {
            "P" -> value * 1024.0 * 1024.0 * 1024.0 * 1024.0
            "T" -> value * 1024.0 * 1024.0 * 1024.0
            "G" -> value * 1024.0 * 1024.0
            "M" -> value * 1024.0
            "K" -> value
            "" -> value / 1024.0
            else -> return null
        }

        return kb.roundToLong().coerceAtLeast(0L)
    }

    private fun ContentAnalysis.hasValues(): Boolean {
        return appSizeKb != null ||
            appDataSizeKb != null ||
            appCacheSizeKb != null ||
            photosSizeKb != null ||
            videosSizeKb != null ||
            audioSizeKb != null ||
            downloadsSizeKb != null ||
            otherSizeKb != null ||
            systemSizeKb != null ||
            dataFreeKb != null ||
            dataTotalKb != null
    }

    private fun toBrowsablePaths(rawPath: String): List<String> {
        val path = normalizePath(rawPath)
        val candidates = when (path) {
            "/" -> emptyList()
            "/storage/emulated" -> listOf("/storage/emulated/0", "/sdcard")
            "/storage/self" -> listOf("/storage/self/primary", "/sdcard")
            "/mnt/shell/emulated" -> listOf("/storage/emulated/0", "/sdcard")
            else -> listOf(path)
        }

        return candidates
            .map(::normalizePath)
            .filter { it.isNotBlank() && it != "/" }
            .distinct()
    }

    private fun normalizePath(path: String): String {
        val trimmed = path.trim().ifBlank { "/" }
            .let { value -> if (value.startsWith('/')) value else "/$value" }
            .replace(Regex("/{2,}"), "/")
            .trimEnd('/')

        return trimmed.ifBlank { "/" }
    }

    private fun resetNoDevice() {
        activeDeviceId = null
        _state.update {
            FileSystemState(
                listState = FileSystemListState.NoDevice,
                contentAnalysis = ContentAnalysisState.Idle,
                cleanup = CleanupState(),
            )
        }
    }

    private fun resetDeviceError(device: AdbDevice) {
        scope.launch {
            val message = getString(
                Res.string.file_system_error_device_unavailable,
                device.state.rawValue,
            )
            val selected = deviceManager.selectedDeviceFlow.value
            if (selected == null || selected.deviceId != device.deviceId || selected.state == DeviceState.DEVICE) {
                return@launch
            }
            activeDeviceId = device.deviceId
            _state.update {
                FileSystemState(
                    listState = FileSystemListState.Error(message),
                    contentAnalysis = ContentAnalysisState.Idle,
                    cleanup = CleanupState(),
                )
            }
        }
    }

    private fun isRequestStillValid(deviceId: String): Boolean {
        val selected = deviceManager.selectedDeviceFlow.value
        return selected != null &&
            selected.deviceId == deviceId &&
            selected.state == DeviceState.DEVICE &&
            activeDeviceId == deviceId
    }

    private companion object {
        private val SIZE_REGEX = Regex(
            pattern = """(-?\d+(?:\.\d+)?)\s*([KMGTPE]?)(?:i?B)?""",
            option = RegexOption.IGNORE_CASE,
        )
    }
}

package com.adbdeck.feature.fileexplorer

import adbdeck.feature.file_explorer.generated.resources.Res
import adbdeck.feature.file_explorer.generated.resources.*
import com.adbdeck.core.adb.api.device.AdbDevice
import com.adbdeck.core.adb.api.device.DeviceManager
import com.adbdeck.core.adb.api.device.DeviceState
import com.adbdeck.core.adb.api.monitoring.SystemMonitorClient
import com.adbdeck.core.settings.SettingsRepository
import com.adbdeck.feature.fileexplorer.service.DeviceFileService
import com.adbdeck.feature.fileexplorer.service.FileTransferService
import com.adbdeck.feature.fileexplorer.service.LocalFileService
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.lifecycle.coroutines.coroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString

/**
 * Реализация [FileExplorerComponent].
 *
 * Ключевые принципы:
 * - активное устройство берётся только из [DeviceManager.selectedDeviceFlow]
 * - все длительные операции выполняются в корутинах без блокировки UI
 * - stale-результаты загрузки списков отбрасываются через request-id
 * - transfer-операции поддерживают conflict-dialog и отмену
 */
class DefaultFileExplorerComponent(
    componentContext: ComponentContext,
    private val deviceManager: DeviceManager,
    private val settingsRepository: SettingsRepository,
    private val systemMonitorClient: SystemMonitorClient,
    private val localFileService: LocalFileService,
    private val deviceFileService: DeviceFileService,
    private val fileTransferService: FileTransferService,
) : FileExplorerComponent, ComponentContext by componentContext {

    private val scope = coroutineScope()

    private val _state = MutableStateFlow(
        FileExplorerState(
            localPanel = ExplorerPanelState(
                currentPath = localFileService.defaultPath(),
                listState = ExplorerListState.Loading,
            ),
            devicePanel = ExplorerPanelState(
                currentPath = deviceFileService.defaultPath(),
                listState = ExplorerListState.NoDevice(""),
            ),
            deviceRoots = defaultDeviceRoots(),
        )
    )
    override val state: StateFlow<FileExplorerState> = _state.asStateFlow()

    private var localLoadJob: Job? = null
    private var deviceLoadJob: Job? = null
    private var rootsLoadJob: Job? = null
    private var actionJob: Job? = null
    private var transferJob: Job? = null
    private var feedbackJob: Job? = null
    private var actionBoundDeviceId: String? = null

    private var localLoadRequestId: Long = 0L
    private var deviceLoadRequestId: Long = 0L

    init {
        // Локальная панель загружается сразу.
        loadLocalDirectory(path = _state.value.localPanel.currentPath, selectedPathToRestore = null)

        // Device-панель синхронизируется с активным устройством приложения.
        scope.launch {
            deviceManager.selectedDeviceFlow.collect { device ->
                handleDeviceChange(device)
            }
        }
    }

    // ── Локальная панель ─────────────────────────────────────────────────────

    override fun onRefreshLocal() {
        loadLocalDirectory(
            path = _state.value.localPanel.currentPath,
            selectedPathToRestore = _state.value.localPanel.selectedPath,
        )
    }

    override fun onLocalUp() {
        val current = _state.value.localPanel.currentPath
        val parent = localFileService.parentPath(current)
        if (parent != null) {
            loadLocalDirectory(path = parent, selectedPathToRestore = null)
        }
    }

    override fun onOpenLocalDirectory(path: String) {
        loadLocalDirectory(path = path, selectedPathToRestore = null)
    }

    override fun onSelectLocal(path: String) {
        _state.update {
            it.copy(localPanel = it.localPanel.copy(selectedPath = path))
        }
    }

    // ── Device-панель ────────────────────────────────────────────────────────

    override fun onRefreshDevice() {
        val deviceId = requireActiveDeviceIdForUi() ?: return
        loadDeviceDirectory(
            deviceId = deviceId,
            path = _state.value.devicePanel.currentPath,
            selectedPathToRestore = _state.value.devicePanel.selectedPath,
            fallbackToRoot = false,
        )
    }

    override fun onDeviceUp() {
        val deviceId = requireActiveDeviceIdForUi() ?: return
        val current = _state.value.devicePanel.currentPath
        val parent = deviceFileService.parentPath(current)
        if (parent != null) {
            loadDeviceDirectory(deviceId = deviceId, path = parent, selectedPathToRestore = null, fallbackToRoot = false)
        }
    }

    override fun onOpenDeviceDirectory(path: String) {
        val deviceId = requireActiveDeviceIdForUi() ?: return
        loadDeviceDirectory(deviceId = deviceId, path = path, selectedPathToRestore = null, fallbackToRoot = false)
    }

    override fun onSelectDevice(path: String) {
        _state.update {
            it.copy(devicePanel = it.devicePanel.copy(selectedPath = path))
        }
    }

    override fun onSelectDeviceRoot(path: String) {
        val deviceId = requireActiveDeviceIdForUi() ?: return
        loadDeviceDirectory(
            deviceId = deviceId,
            path = normalizeDevicePath(path),
            selectedPathToRestore = null,
            fallbackToRoot = false,
        )
    }

    // ── Действия над файлами ─────────────────────────────────────────────────

    override fun onRequestDelete(side: ExplorerSide) {
        val selected = selectedItem(side)
        if (selected == null) {
            showFeedbackResource(
                messageRes = Res.string.file_explorer_error_select_item_for_delete,
                isError = true,
            )
            return
        }

        val deviceId = if (side == ExplorerSide.DEVICE) requireActiveDeviceIdForUi() else null
        if (side == ExplorerSide.DEVICE && deviceId == null) return

        _state.update {
            it.copy(
                deleteDialog = DeleteDialogState(
                    side = side,
                    item = selected,
                    deviceId = deviceId,
                )
            )
        }
    }

    override fun onConfirmDelete() {
        val dialog = _state.value.deleteDialog ?: return
        _state.update { it.copy(deleteDialog = null) }

        val deviceId = when (dialog.side) {
            ExplorerSide.LOCAL -> null
            ExplorerSide.DEVICE -> dialog.deviceId ?: run {
                showFeedbackResource(
                    messageRes = Res.string.file_explorer_error_device_context_lost_retry,
                    isError = true,
                )
                return
            }
        }
        if (deviceId != null && !ensureActiveDeviceMatches(deviceId)) return

        runAction(
            deviceBoundId = deviceId,
            block = {
                when (dialog.side) {
                    ExplorerSide.LOCAL -> localFileService.delete(dialog.item.fullPath)
                    ExplorerSide.DEVICE -> {
                        if (deviceId == null) {
                            return@runAction Result.failure(
                                IllegalStateException(
                                    getString(Res.string.file_explorer_error_device_context_unavailable)
                                )
                            )
                        }
                        deviceFileService.delete(deviceId, dialog.item.fullPath, adbPath())
                    }
                }
            },
            onSuccess = {
                showFeedbackResource(
                    messageRes = Res.string.file_explorer_feedback_deleted,
                    isError = false,
                    dialog.item.name,
                )
                refreshSide(dialog.side)
            },
        )
    }

    override fun onCancelDelete() {
        _state.update { it.copy(deleteDialog = null) }
    }

    override fun onRequestCreateDirectory(side: ExplorerSide) {
        val deviceId = if (side == ExplorerSide.DEVICE) requireActiveDeviceIdForUi() else null
        if (side == ExplorerSide.DEVICE && deviceId == null) return

        val parentPath = when (side) {
            ExplorerSide.LOCAL -> _state.value.localPanel.currentPath
            ExplorerSide.DEVICE -> _state.value.devicePanel.currentPath
        }

        _state.update {
            it.copy(
                createDirectoryDialog = CreateDirectoryDialogState(
                    side = side,
                    parentPath = parentPath,
                    name = "",
                    deviceId = deviceId,
                )
            )
        }
    }

    override fun onCreateDirectoryNameChanged(value: String) {
        _state.update { current ->
            val dialog = current.createDirectoryDialog ?: return@update current
            current.copy(createDirectoryDialog = dialog.copy(name = value))
        }
    }

    override fun onConfirmCreateDirectory() {
        val dialog = _state.value.createDirectoryDialog ?: return
        val normalizedName = dialog.name.trim()
        if (normalizedName.isEmpty()) {
            showFeedbackResource(
                messageRes = Res.string.file_explorer_error_enter_directory_name,
                isError = true,
            )
            return
        }

        _state.update { it.copy(createDirectoryDialog = null) }

        val deviceId = when (dialog.side) {
            ExplorerSide.LOCAL -> null
            ExplorerSide.DEVICE -> dialog.deviceId ?: run {
                showFeedbackResource(
                    messageRes = Res.string.file_explorer_error_device_context_lost_retry,
                    isError = true,
                )
                return
            }
        }
        if (deviceId != null && !ensureActiveDeviceMatches(deviceId)) return

        runAction(
            deviceBoundId = deviceId,
            block = {
                when (dialog.side) {
                    ExplorerSide.LOCAL -> {
                        val targetPath = localFileService.resolveChildPath(dialog.parentPath, normalizedName)
                        localFileService.createDirectory(targetPath)
                    }

                    ExplorerSide.DEVICE -> {
                        if (deviceId == null) {
                            return@runAction Result.failure(
                                IllegalStateException(
                                    getString(Res.string.file_explorer_error_device_context_unavailable)
                                )
                            )
                        }
                        val targetPath = deviceFileService.resolveChildPath(dialog.parentPath, normalizedName)
                        deviceFileService.createDirectory(deviceId, targetPath, adbPath())
                    }
                }
            },
            onSuccess = {
                showFeedbackResource(
                    messageRes = Res.string.file_explorer_feedback_directory_created,
                    isError = false,
                    normalizedName,
                )
                refreshSide(dialog.side)
            },
        )
    }

    override fun onCancelCreateDirectory() {
        _state.update { it.copy(createDirectoryDialog = null) }
    }

    override fun onRequestRename(side: ExplorerSide) {
        val selected = selectedItem(side)
        if (selected == null) {
            showFeedbackResource(
                messageRes = Res.string.file_explorer_error_select_item_for_rename,
                isError = true,
            )
            return
        }

        val deviceId = if (side == ExplorerSide.DEVICE) requireActiveDeviceIdForUi() else null
        if (side == ExplorerSide.DEVICE && deviceId == null) return

        _state.update {
            it.copy(
                renameDialog = RenameDialogState(
                    side = side,
                    item = selected,
                    newName = selected.name,
                    deviceId = deviceId,
                )
            )
        }
    }

    override fun onRenameValueChanged(value: String) {
        _state.update { current ->
            val dialog = current.renameDialog ?: return@update current
            current.copy(renameDialog = dialog.copy(newName = value))
        }
    }

    override fun onConfirmRename() {
        val dialog = _state.value.renameDialog ?: return
        val newName = dialog.newName.trim()

        if (newName.isEmpty()) {
            showFeedbackResource(
                messageRes = Res.string.file_explorer_error_enter_new_name,
                isError = true,
            )
            return
        }

        if (newName == dialog.item.name) {
            _state.update { it.copy(renameDialog = null) }
            return
        }

        _state.update { it.copy(renameDialog = null) }

        val deviceId = when (dialog.side) {
            ExplorerSide.LOCAL -> null
            ExplorerSide.DEVICE -> dialog.deviceId ?: run {
                showFeedbackResource(
                    messageRes = Res.string.file_explorer_error_device_context_lost_retry,
                    isError = true,
                )
                return
            }
        }
        if (deviceId != null && !ensureActiveDeviceMatches(deviceId)) return

        runAction(
            deviceBoundId = deviceId,
            block = {
                when (dialog.side) {
                    ExplorerSide.LOCAL -> localFileService.rename(dialog.item.fullPath, newName).map { Unit }

                    ExplorerSide.DEVICE -> {
                        if (deviceId == null) {
                            return@runAction Result.failure(
                                IllegalStateException(
                                    getString(Res.string.file_explorer_error_device_context_unavailable)
                                )
                            )
                        }
                        deviceFileService.rename(deviceId, dialog.item.fullPath, newName, adbPath()).map { Unit }
                    }
                }
            },
            onSuccess = {
                showFeedbackResource(
                    messageRes = Res.string.file_explorer_feedback_renamed,
                    isError = false,
                    dialog.item.name,
                    newName,
                )
                refreshSide(dialog.side)
            },
        )
    }

    override fun onCancelRename() {
        _state.update { it.copy(renameDialog = null) }
    }

    // ── Transfer: Push / Pull ────────────────────────────────────────────────

    override fun onPushSelected() {
        if (_state.value.transferState != null) {
            showFeedbackResource(
                messageRes = Res.string.file_explorer_error_wait_transfer_completion,
                isError = true,
            )
            return
        }

        val source = selectedItem(ExplorerSide.LOCAL)
        if (source == null) {
            showFeedbackResource(
                messageRes = Res.string.file_explorer_error_select_local_for_push,
                isError = true,
            )
            return
        }

        val deviceId = requireActiveDeviceIdForUi() ?: return
        val target = deviceFileService.resolveChildPath(_state.value.devicePanel.currentPath, source.name)

        checkTransferConflictAndStart(
            direction = TransferDirection.PUSH,
            deviceId = deviceId,
            sourcePath = source.fullPath,
            targetPath = target,
        )
    }

    override fun onPullSelected() {
        if (_state.value.transferState != null) {
            showFeedbackResource(
                messageRes = Res.string.file_explorer_error_wait_transfer_completion,
                isError = true,
            )
            return
        }

        val source = selectedItem(ExplorerSide.DEVICE)
        if (source == null) {
            showFeedbackResource(
                messageRes = Res.string.file_explorer_error_select_device_for_pull,
                isError = true,
            )
            return
        }

        val deviceId = requireActiveDeviceIdForUi() ?: return
        val target = localFileService.resolveChildPath(_state.value.localPanel.currentPath, source.name)

        checkTransferConflictAndStart(
            direction = TransferDirection.PULL,
            deviceId = deviceId,
            sourcePath = source.fullPath,
            targetPath = target,
        )
    }

    override fun onConfirmTransferConflict() {
        val dialog = _state.value.transferConflictDialog ?: return
        _state.update { it.copy(transferConflictDialog = null) }

        if (!ensureActiveDeviceMatches(dialog.deviceId)) return
        startTransfer(
            direction = dialog.direction,
            deviceId = dialog.deviceId,
            sourcePath = dialog.sourcePath,
            targetPath = dialog.targetPath,
            overwrite = true,
        )
    }

    override fun onCancelTransferConflict() {
        _state.update { it.copy(transferConflictDialog = null) }
    }

    override fun onCancelTransfer() {
        transferJob?.cancel()
        transferJob = null
        _state.update { it.copy(transferState = null) }
        showFeedbackResource(
            messageRes = Res.string.file_explorer_feedback_transfer_cancelled,
            isError = false,
        )
    }

    // ── UI feedback ───────────────────────────────────────────────────────────

    override fun onPathCopied(path: String) {
        showFeedbackResource(
            messageRes = Res.string.file_explorer_feedback_copied,
            isError = false,
            path,
        )
    }

    override fun onDismissFeedback() {
        feedbackJob?.cancel()
        feedbackJob = null
        _state.update { it.copy(feedback = null) }
    }

    // ── Внутренняя логика загрузки панелей ───────────────────────────────────

    private suspend fun handleDeviceChange(device: AdbDevice?) {
        when {
            device == null -> {
                cancelDeviceOperations()
                val defaultRoots = defaultDeviceRoots()
                val noDeviceMessage = getString(Res.string.file_explorer_no_device_not_selected)
                _state.update { current ->
                    clearDeviceScopedTransientState(
                        current.copy(
                            activeDeviceId = null,
                            deviceRoots = defaultRoots,
                            devicePanel = current.devicePanel.copy(
                                currentPath = preferredDeviceStartPath(defaultRoots),
                                listState = ExplorerListState.NoDevice(noDeviceMessage),
                                selectedPath = null,
                            ),
                            transferState = null,
                        ),
                    )
                }
            }

            device.state != DeviceState.DEVICE -> {
                cancelDeviceOperations()
                val defaultRoots = defaultDeviceRoots()
                val unavailableMessage = getString(
                    Res.string.file_explorer_no_device_unavailable,
                    device.state.rawValue,
                )
                _state.update { current ->
                    clearDeviceScopedTransientState(
                        current.copy(
                            activeDeviceId = null,
                            deviceRoots = defaultRoots,
                            devicePanel = current.devicePanel.copy(
                                currentPath = preferredDeviceStartPath(defaultRoots),
                                listState = ExplorerListState.NoDevice(unavailableMessage),
                                selectedPath = null,
                            ),
                            transferState = null,
                        ),
                    )
                }
            }

            else -> {
                val isChanged = _state.value.activeDeviceId != device.deviceId
                if (isChanged) {
                    cancelDeviceOperations()
                }
                val roots = if (isChanged) defaultDeviceRoots() else _state.value.deviceRoots.ifEmpty { defaultDeviceRoots() }
                val path = if (isChanged) preferredDeviceStartPath(roots) else _state.value.devicePanel.currentPath
                _state.update { current ->
                    val updated = current.copy(
                        activeDeviceId = device.deviceId,
                        deviceRoots = roots,
                        devicePanel = current.devicePanel.copy(currentPath = path, selectedPath = null),
                    )
                    if (isChanged) clearDeviceScopedTransientState(updated) else updated
                }

                loadDeviceRoots(device.deviceId)
                loadDeviceDirectory(
                    deviceId = device.deviceId,
                    path = path,
                    selectedPathToRestore = null,
                    fallbackToRoot = isChanged,
                )
            }
        }
    }

    private fun loadLocalDirectory(
        path: String,
        selectedPathToRestore: String?,
    ) {
        localLoadJob?.cancel()
        val requestId = ++localLoadRequestId

        _state.update {
            it.copy(
                localPanel = it.localPanel.copy(
                    currentPath = path,
                    listState = ExplorerListState.Loading,
                    selectedPath = selectedPathToRestore,
                )
            )
        }

        localLoadJob = scope.launch {
            val result = localFileService.listDirectory(path)
            if (requestId != localLoadRequestId) return@launch

            if (result.isSuccess) {
                val items = result.getOrDefault(emptyList())
                val listState = if (items.isEmpty()) ExplorerListState.Empty else ExplorerListState.Success(items)
                val selected = items.firstOrNull { it.fullPath == selectedPathToRestore }?.fullPath
                _state.update { current ->
                    current.copy(
                        localPanel = current.localPanel.copy(
                            currentPath = path,
                            listState = listState,
                            selectedPath = selected,
                        )
                    )
                }
                return@launch
            }

            val errorMessage = result.exceptionOrNull()?.message
                ?: getString(Res.string.file_explorer_error_read_local_directory)
            _state.update { current ->
                current.copy(
                    localPanel = current.localPanel.copy(
                        currentPath = path,
                        listState = ExplorerListState.Error(errorMessage),
                        selectedPath = null,
                    )
                )
            }
        }
    }

    private fun loadDeviceDirectory(
        deviceId: String,
        path: String,
        selectedPathToRestore: String?,
        fallbackToRoot: Boolean,
    ) {
        deviceLoadJob?.cancel()
        val requestId = ++deviceLoadRequestId

        _state.update {
            it.copy(
                devicePanel = it.devicePanel.copy(
                    currentPath = path,
                    listState = ExplorerListState.Loading,
                    selectedPath = selectedPathToRestore,
                )
            )
        }

        deviceLoadJob = scope.launch {
            if (!isDeviceRequestValid(deviceId, requestId)) return@launch

            val firstResult = deviceFileService.listDirectory(deviceId, path, adbPath())
            if (!isDeviceRequestValid(deviceId, requestId)) return@launch

            val fallbackPath = preferredDeviceStartPath(_state.value.deviceRoots)
            if (firstResult.isFailure && fallbackToRoot && path != fallbackPath) {
                _state.update {
                    it.copy(
                        devicePanel = it.devicePanel.copy(
                            currentPath = fallbackPath,
                            listState = ExplorerListState.Loading,
                            selectedPath = null,
                        )
                    )
                }

                val secondResult = deviceFileService.listDirectory(deviceId, fallbackPath, adbPath())
                if (!isDeviceRequestValid(deviceId, requestId)) return@launch

                applyDeviceDirectoryResult(
                    path = fallbackPath,
                    selectedPathToRestore = null,
                    result = secondResult,
                )
                return@launch
            }

            applyDeviceDirectoryResult(
                path = path,
                selectedPathToRestore = selectedPathToRestore,
                result = firstResult,
            )
        }
    }

    private suspend fun applyDeviceDirectoryResult(
        path: String,
        selectedPathToRestore: String?,
        result: Result<List<ExplorerFileItem>>,
    ) {
        if (result.isSuccess) {
            val items = result.getOrDefault(emptyList())
            val listState = if (items.isEmpty()) ExplorerListState.Empty else ExplorerListState.Success(items)
            val selected = items.firstOrNull { it.fullPath == selectedPathToRestore }?.fullPath
            _state.update { current ->
                current.copy(
                    devicePanel = current.devicePanel.copy(
                        currentPath = path,
                        listState = listState,
                        selectedPath = selected,
                    )
                )
            }
            return
        }

        val errorMessage = result.exceptionOrNull()?.message
            ?: getString(Res.string.file_explorer_error_read_device_directory)
        _state.update { current ->
            current.copy(
                devicePanel = current.devicePanel.copy(
                    currentPath = path,
                    listState = ExplorerListState.Error(errorMessage),
                    selectedPath = null,
                )
            )
        }
    }

    /** Загружает список корневых разделов устройства из System Monitor (df). */
    private fun loadDeviceRoots(deviceId: String) {
        rootsLoadJob?.cancel()
        rootsLoadJob = scope.launch {
            val result = systemMonitorClient.getStorageInfo(deviceId = deviceId, adbPath = adbPath())
            if (!isActiveDevice(deviceId)) return@launch

            val rootsFromStorage = result
                .getOrNull()
                .orEmpty()
                .asSequence()
                .filter { it.isRelevant }
                .flatMap { partition ->
                    toBrowsablePaths(partition.mountPoint).asSequence()
                }
                .distinct()
                .toList()

            val mergedRoots = (rootsFromStorage + defaultDeviceRoots())
                .map(::normalizeDevicePath)
                .distinct()

            val accessibleRoots = resolveAccessibleRoots(deviceId, mergedRoots)
            if (!isActiveDevice(deviceId)) return@launch

            val fallbackAccessibleRoots = if (accessibleRoots.isEmpty()) {
                resolveAccessibleRoots(
                    deviceId = deviceId,
                    roots = listOf("/sdcard", "/storage/self/primary", "/storage/emulated/0", "/data/local/tmp", "/"),
                )
            } else {
                emptyList()
            }
            if (!isActiveDevice(deviceId)) return@launch

            val finalRoots = (accessibleRoots + fallbackAccessibleRoots)
                .map(::normalizeDevicePath)
                .distinct()

            _state.update { it.copy(deviceRoots = finalRoots) }

            // Если есть валидные корни и текущий путь нерабочий/неподходящий — переходим на лучший.
            val shouldSwitchPath =
                finalRoots.isNotEmpty() && (
                    _state.value.devicePanel.currentPath == "/" ||
                        _state.value.devicePanel.listState is ExplorerListState.Error ||
                        finalRoots.none { root ->
                            val current = normalizeDevicePath(_state.value.devicePanel.currentPath)
                            current == root || current.startsWith("$root/")
                        }
                    )

            if (shouldSwitchPath) {
                val preferred = preferredDeviceStartPath(finalRoots)
                if (preferred != "/") {
                    loadDeviceDirectory(
                        deviceId = deviceId,
                        path = preferred,
                        selectedPathToRestore = null,
                        fallbackToRoot = false,
                    )
                }
            }

            if (finalRoots.isEmpty()) {
                showFeedbackResource(
                    messageRes = Res.string.file_explorer_error_no_accessible_directories,
                    isError = true,
                )
            }
        }
    }

    /**
     * Проверяет какие корни реально доступны shell-пользователю и оставляет только их.
     *
     * Использует лёгкий probe вместо полного listDirectory, чтобы не делать дорогой
     * обход содержимого для каждой категории.
     */
    private suspend fun resolveAccessibleRoots(
        deviceId: String,
        roots: List<String>,
    ): List<String> {
        val adbPath = adbPath()
        val accessible = mutableListOf<String>()

        for (root in roots) {
            if (!isActiveDevice(deviceId)) return emptyList()

            val canOpen = deviceFileService.canAccessDirectory(
                deviceId = deviceId,
                path = root,
                adbPath = adbPath,
            ).getOrDefault(false)

            if (canOpen) {
                accessible += root
            }
        }

        return accessible
    }

    // ── Внутренняя логика transfer ────────────────────────────────────────────

    private fun checkTransferConflictAndStart(
        direction: TransferDirection,
        deviceId: String,
        sourcePath: String,
        targetPath: String,
    ) {
        if (_state.value.isActionRunning) return

        runAction(
            deviceBoundId = deviceId,
            block = {
                when (direction) {
                    TransferDirection.PUSH -> deviceFileService.exists(deviceId, targetPath, adbPath())
                    TransferDirection.PULL -> localFileService.exists(targetPath)
                }.map { targetExists ->
                    if (!isActiveDevice(deviceId)) return@map

                    if (targetExists) {
                        _state.update {
                            it.copy(
                                transferConflictDialog = TransferConflictDialogState(
                                    direction = direction,
                                    deviceId = deviceId,
                                    sourcePath = sourcePath,
                                    targetPath = targetPath,
                                )
                            )
                        }
                    } else {
                        startTransfer(
                            direction = direction,
                            deviceId = deviceId,
                            sourcePath = sourcePath,
                            targetPath = targetPath,
                            overwrite = false,
                        )
                    }
                }
            },
        )
    }

    private fun startTransfer(
        direction: TransferDirection,
        deviceId: String,
        sourcePath: String,
        targetPath: String,
        overwrite: Boolean,
    ) {
        if (!ensureActiveDeviceMatches(deviceId)) return

        transferJob?.cancel()
        transferJob = scope.launch {
            try {
                val runningStatus = when (direction) {
                    TransferDirection.PUSH -> getString(Res.string.file_explorer_transfer_status_push_running)
                    TransferDirection.PULL -> getString(Res.string.file_explorer_transfer_status_pull_running)
                }
                _state.update {
                    it.copy(
                        transferState = TransferState(
                            direction = direction,
                            sourcePath = sourcePath,
                            targetPath = targetPath,
                            status = runningStatus,
                            progress = null,
                        )
                    )
                }

                val result = when (direction) {
                    TransferDirection.PUSH -> fileTransferService.push(
                        deviceId = deviceId,
                        localSourcePath = sourcePath,
                        remoteTargetPath = targetPath,
                        adbPath = adbPath(),
                        overwrite = overwrite,
                    )

                    TransferDirection.PULL -> fileTransferService.pull(
                        deviceId = deviceId,
                        remoteSourcePath = sourcePath,
                        localTargetPath = targetPath,
                        adbPath = adbPath(),
                        overwrite = overwrite,
                    )
                }

                if (!isActiveDevice(deviceId)) return@launch

                result
                    .onSuccess {
                        val refreshStatus = getString(Res.string.file_explorer_transfer_status_refreshing_lists)
                        _state.update { current ->
                            current.copy(
                                transferState = current.transferState?.copy(
                                    status = refreshStatus,
                                )
                            )
                        }

                        onRefreshLocal()
                        onRefreshDevice()

                        showFeedbackResource(
                            messageRes = when (direction) {
                                TransferDirection.PUSH -> Res.string.file_explorer_feedback_push_completed
                                TransferDirection.PULL -> Res.string.file_explorer_feedback_pull_completed
                            },
                            isError = false,
                        )
                    }
                    .onFailure { e ->
                        showFeedback(
                            message = e.message ?: getString(Res.string.file_explorer_error_transfer_generic),
                            isError = true,
                        )
                    }
            } finally {
                _state.update { current ->
                    val transfer = current.transferState
                    if (
                        transfer != null &&
                        transfer.direction == direction &&
                        transfer.sourcePath == sourcePath &&
                        transfer.targetPath == targetPath
                    ) {
                        current.copy(transferState = null)
                    } else {
                        current
                    }
                }
                transferJob = null
            }
        }
    }

    // ── Вспомогательные функции ───────────────────────────────────────────────

    /** Выполняет короткую action-операцию с флагом [FileExplorerState.isActionRunning]. */
    private fun runAction(
        deviceBoundId: String? = null,
        block: suspend () -> Result<Unit>,
        onSuccess: () -> Unit = {},
    ) {
        if (_state.value.isActionRunning) return

        actionJob?.cancel()
        actionBoundDeviceId = deviceBoundId
        _state.update { it.copy(isActionRunning = true) }

        actionJob = scope.launch {
            try {
                val result = block()
                if (result.isSuccess) {
                    onSuccess()
                } else {
                    val message = result.exceptionOrNull()?.message
                        ?: getString(Res.string.file_explorer_error_operation_failed)
                    showFeedback(message, isError = true)
                }
            } finally {
                _state.update { it.copy(isActionRunning = false) }
                actionJob = null
                actionBoundDeviceId = null
            }
        }
    }

    /** Обновить содержимое панели [side] с сохранением текущего выделения. */
    private fun refreshSide(side: ExplorerSide) {
        when (side) {
            ExplorerSide.LOCAL -> onRefreshLocal()
            ExplorerSide.DEVICE -> onRefreshDevice()
        }
    }

    /** Получить выбранный элемент на стороне [side]. */
    private fun selectedItem(side: ExplorerSide): ExplorerFileItem? {
        val panel = when (side) {
            ExplorerSide.LOCAL -> _state.value.localPanel
            ExplorerSide.DEVICE -> _state.value.devicePanel
        }

        val items = (panel.listState as? ExplorerListState.Success)?.items ?: return null
        return items.firstOrNull { it.fullPath == panel.selectedPath }
    }

    /** Проверка актуальности device-запроса для защиты от stale-ответов. */
    private fun isDeviceRequestValid(deviceId: String, requestId: Long): Boolean {
        val selected = deviceManager.selectedDeviceFlow.value
        return selected != null &&
            selected.state == DeviceState.DEVICE &&
            selected.deviceId == deviceId &&
            _state.value.activeDeviceId == deviceId &&
            requestId == deviceLoadRequestId
    }

    /** Вернуть deviceId активного устройства или показать feedback-ошибку. */
    private fun requireActiveDeviceIdForUi(): String? {
        val device = deviceManager.selectedDeviceFlow.value
        if (device == null || device.state != DeviceState.DEVICE) {
            showFeedbackResource(
                messageRes = Res.string.file_explorer_error_active_device_unavailable,
                isError = true,
            )
            return null
        }
        return device.deviceId
    }

    /**
     * Проверяет, что активным остаётся именно [expectedDeviceId].
     *
     * Нужна для защиты от выполнения подтверждённого действия на другом устройстве
     * после переключения в верхнем DeviceBar.
     */
    private fun ensureActiveDeviceMatches(expectedDeviceId: String): Boolean {
        if (isActiveDevice(expectedDeviceId)) return true
        showFeedbackResource(
            messageRes = Res.string.file_explorer_error_active_device_changed,
            isError = true,
        )
        return false
    }

    /** Путь к adb из настроек приложения. */
    private fun adbPath(): String = settingsRepository.getSettings().adbPath.ifBlank { "adb" }

    /** Остановить все фоновые операции, связанные с device-панелью. */
    private fun cancelDeviceOperations() {
        deviceLoadJob?.cancel()
        deviceLoadJob = null
        rootsLoadJob?.cancel()
        rootsLoadJob = null
        transferJob?.cancel()
        transferJob = null

        // Device-scoped action (delete/create/rename/exists-check) нужно отменять при смене девайса.
        if (actionBoundDeviceId != null) {
            actionJob?.cancel()
            actionJob = null
            actionBoundDeviceId = null
            _state.update { it.copy(isActionRunning = false) }
        }
    }

    /** Сбрасывает все transient-состояния, которые завязаны на конкретное устройство. */
    private fun clearDeviceScopedTransientState(state: FileExplorerState): FileExplorerState =
        state.copy(
            deleteDialog = state.deleteDialog?.takeUnless { it.side == ExplorerSide.DEVICE },
            createDirectoryDialog = state.createDirectoryDialog?.takeUnless { it.side == ExplorerSide.DEVICE },
            renameDialog = state.renameDialog?.takeUnless { it.side == ExplorerSide.DEVICE },
            transferConflictDialog = null,
        )

    /** Вернёт `true`, если устройство [deviceId] всё ещё активно и доступно. */
    private fun isActiveDevice(deviceId: String): Boolean {
        val selected = deviceManager.selectedDeviceFlow.value
        return selected != null &&
            selected.state == DeviceState.DEVICE &&
            selected.deviceId == deviceId &&
            _state.value.activeDeviceId == deviceId
    }

    /** Нормализует device-путь в абсолютный формат без хвостовых `/`. */
    private fun normalizeDevicePath(path: String): String {
        val normalized = path.trim().ifBlank { "/" }
            .let { if (it.startsWith("/")) it else "/$it" }
            .replace(Regex("/{2,}"), "/")
            .trimEnd('/')
        return normalized.ifBlank { "/" }
    }

    /** Базовый список корней для dropdown, даже если Storage недоступен. */
    private fun defaultDeviceRoots(): List<String> = listOf(
        deviceFileService.defaultPath(),
        "/storage/emulated/0",
        "/storage/self/primary",
        "/data/local/tmp",
    ).flatMap(::toBrowsablePaths).distinct()

    /** Предпочтительный стартовый путь для device-панели. */
    private fun preferredDeviceStartPath(roots: List<String>): String {
        val normalizedRoots = roots.map(::normalizeDevicePath).distinct()
        val preferredByPriority = listOf(
            normalizeDevicePath(deviceFileService.defaultPath()),
            "/sdcard",
            "/storage/emulated/0",
            "/storage/self/primary",
            "/data/local/tmp",
        )

        return preferredByPriority.firstOrNull { candidate -> normalizedRoots.any { it == candidate } }
            ?: normalizedRoots.firstOrNull()
            ?: normalizeDevicePath(deviceFileService.defaultPath())
    }

    /**
     * Преобразует mount-point в реально просматриваемые пути для файлового менеджера.
     *
     * `df` часто возвращает системные точки (`/storage/emulated`), тогда как
     * пользовательские файлы доступны по `/storage/emulated/0` или `/sdcard`.
     */
    private fun toBrowsablePaths(rawPath: String): List<String> {
        val path = normalizeDevicePath(rawPath)
        val candidates = when (path) {
            "/" -> emptyList()
            "/storage/emulated" -> listOf("/storage/emulated/0", "/sdcard")
            "/storage/self" -> listOf("/storage/self/primary", "/sdcard")
            "/mnt/shell/emulated" -> listOf("/storage/emulated/0", "/sdcard")
            else -> listOf(path)
        }
        return candidates.map(::normalizeDevicePath).distinct()
    }

    /** Показать краткосрочный баннер обратной связи. */
    private fun showFeedback(message: String, isError: Boolean) {
        feedbackJob?.cancel()
        _state.update { it.copy(feedback = ExplorerFeedback(message = message, isError = isError)) }

        feedbackJob = scope.launch {
            delay(3_000L)
            _state.update { current ->
                if (current.feedback?.message == message) {
                    current.copy(feedback = null)
                } else {
                    current
                }
            }
        }
    }

    private fun showFeedbackResource(
        messageRes: StringResource,
        isError: Boolean,
        vararg args: Any,
    ) {
        scope.launch {
            val message = getString(messageRes, *args)
            showFeedback(message = message, isError = isError)
        }
    }
}

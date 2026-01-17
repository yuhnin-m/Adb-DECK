package com.adbdeck.feature.fileexplorer

import adbdeck.feature.file_explorer.generated.resources.Res
import adbdeck.feature.file_explorer.generated.resources.*
import com.adbdeck.core.adb.api.device.DeviceManager
import com.adbdeck.core.adb.api.monitoring.SystemMonitorClient
import com.adbdeck.core.settings.SettingsRepository
import com.adbdeck.feature.fileexplorer.service.DeviceFileService
import com.adbdeck.feature.fileexplorer.service.FileTransferService
import com.adbdeck.feature.fileexplorer.service.LocalFileService
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.lifecycle.coroutines.coroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
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
    internal val deviceManager: DeviceManager,
    internal val settingsRepository: SettingsRepository,
    internal val systemMonitorClient: SystemMonitorClient,
    internal val localFileService: LocalFileService,
    internal val deviceFileService: DeviceFileService,
    internal val fileTransferService: FileTransferService,
) : FileExplorerComponent, ComponentContext by componentContext {

    internal val scope = coroutineScope()

    internal val _state = MutableStateFlow(
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

    internal var localLoadJob: Job? = null
    internal var deviceLoadJob: Job? = null
    internal var rootsLoadJob: Job? = null
    internal var actionJob: Job? = null
    internal var transferJob: Job? = null
    internal var feedbackJob: Job? = null
    internal var actionBoundDeviceId: String? = null

    internal var localLoadRequestId: Long = 0L
    internal var deviceLoadRequestId: Long = 0L

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
}

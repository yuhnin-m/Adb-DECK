package com.adbdeck.feature.fileexplorer

import adbdeck.feature.file_explorer.generated.resources.Res
import adbdeck.feature.file_explorer.generated.resources.*
import com.adbdeck.core.adb.api.device.AdbDevice
import com.adbdeck.core.adb.api.device.DeviceState
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString

internal suspend fun DefaultFileExplorerComponent.handleDeviceChange(device: AdbDevice?) {
    when {
        device == null -> {
            cancelDeviceOperations()
            val defaultRoots = deviceFileService.defaultRoots()
            val noDeviceMessage = getString(Res.string.file_explorer_no_device_not_selected)
            _state.update { current ->
                clearDeviceScopedTransientState(
                    current.copy(
                        activeDeviceId = null,
                        deviceRoots = defaultRoots,
                        devicePanel = current.devicePanel.copy(
                            currentPath = deviceFileService.preferredStartPath(defaultRoots),
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
            val defaultRoots = deviceFileService.defaultRoots()
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
                            currentPath = deviceFileService.preferredStartPath(defaultRoots),
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
            val roots = if (isChanged) {
                deviceFileService.defaultRoots()
            } else {
                _state.value.deviceRoots.ifEmpty { deviceFileService.defaultRoots() }
            }
            val path = if (isChanged) {
                deviceFileService.preferredStartPath(roots)
            } else {
                _state.value.devicePanel.currentPath
            }
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

internal fun DefaultFileExplorerComponent.loadLocalDirectory(
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

        val errorMessage = resolveErrorMessage(
            type = FileExplorerErrorType.READ_LOCAL_DIRECTORY,
            cause = result.exceptionOrNull(),
        )
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

internal fun DefaultFileExplorerComponent.loadDeviceDirectory(
    deviceId: String,
    path: String,
    selectedPathToRestore: String?,
    fallbackToRoot: Boolean,
) {
    deviceLoadJob?.cancel()
    val requestId = ++deviceLoadRequestId
    val normalizedPath = deviceFileService.normalizePath(path)

    _state.update {
        it.copy(
            devicePanel = it.devicePanel.copy(
                currentPath = normalizedPath,
                listState = ExplorerListState.Loading,
                selectedPath = selectedPathToRestore,
            )
        )
    }

    deviceLoadJob = scope.launch {
        if (!isDeviceRequestValid(deviceId, requestId)) return@launch

        val firstResult = deviceFileService.listDirectory(deviceId, normalizedPath, adbPath())
        if (!isDeviceRequestValid(deviceId, requestId)) return@launch

        val fallbackPath = deviceFileService.preferredStartPath(_state.value.deviceRoots)
        if (firstResult.isFailure && fallbackToRoot && normalizedPath != fallbackPath) {
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
            path = normalizedPath,
            selectedPathToRestore = selectedPathToRestore,
            result = firstResult,
        )
    }
}

internal suspend fun DefaultFileExplorerComponent.applyDeviceDirectoryResult(
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

    val errorMessage = resolveErrorMessage(
        type = FileExplorerErrorType.READ_DEVICE_DIRECTORY,
        cause = result.exceptionOrNull(),
    )
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

/** Загружает список доступных root-разделов устройства через сервисный слой. */
internal fun DefaultFileExplorerComponent.loadDeviceRoots(deviceId: String) {
    rootsLoadJob?.cancel()
    rootsLoadJob = scope.launch {
        val rootsResult = deviceFileService.resolveAccessibleRoots(
            deviceId = deviceId,
            adbPath = adbPath(),
        )
        if (!isActiveDevice(deviceId)) return@launch

        rootsResult.exceptionOrNull()?.let { error ->
            showFeedback(
                message = resolveErrorMessage(
                    type = FileExplorerErrorType.READ_DEVICE_ROOTS,
                    cause = error,
                ),
                isError = true,
            )
        }

        val finalRoots = rootsResult.getOrElse { deviceFileService.defaultRoots() }
        if (!isActiveDevice(deviceId)) return@launch

        _state.update { it.copy(deviceRoots = finalRoots) }

        // Если есть валидные корни и текущий путь нерабочий/неподходящий — переходим на лучший.
        val normalizedCurrentPath = deviceFileService.normalizePath(_state.value.devicePanel.currentPath)
        val shouldSwitchPath =
            finalRoots.isNotEmpty() && (
                normalizedCurrentPath == "/" ||
                    _state.value.devicePanel.listState is ExplorerListState.Error ||
                    finalRoots.none { root ->
                        normalizedCurrentPath == root || normalizedCurrentPath.startsWith("$root/")
                    }
                )

        if (shouldSwitchPath) {
            val preferred = deviceFileService.preferredStartPath(finalRoots)
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

/** Проверка актуальности device-запроса для защиты от stale-ответов. */
internal fun DefaultFileExplorerComponent.isDeviceRequestValid(deviceId: String, requestId: Long): Boolean {
    val selected = deviceManager.selectedDeviceFlow.value
    return selected != null &&
        selected.state == DeviceState.DEVICE &&
        selected.deviceId == deviceId &&
        _state.value.activeDeviceId == deviceId &&
        requestId == deviceLoadRequestId
}

/** Вернуть deviceId активного устройства или показать feedback-ошибку. */
internal fun DefaultFileExplorerComponent.requireActiveDeviceIdForUi(): String? {
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
internal fun DefaultFileExplorerComponent.ensureActiveDeviceMatches(expectedDeviceId: String): Boolean {
    if (isActiveDevice(expectedDeviceId)) return true
    showFeedbackResource(
        messageRes = Res.string.file_explorer_error_active_device_changed,
        isError = true,
    )
    return false
}

/** Путь к adb из настроек приложения. */
internal fun DefaultFileExplorerComponent.adbPath(): String =
    settingsRepository.resolvedAdbPath()

/** Остановить все фоновые операции, связанные с device-панелью. */
internal fun DefaultFileExplorerComponent.cancelDeviceOperations() {
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
internal fun DefaultFileExplorerComponent.clearDeviceScopedTransientState(state: FileExplorerState): FileExplorerState =
    state.copy(
        deleteDialog = state.deleteDialog?.takeUnless { it.side == ExplorerSide.DEVICE },
        createDirectoryDialog = state.createDirectoryDialog?.takeUnless { it.side == ExplorerSide.DEVICE },
        renameDialog = state.renameDialog?.takeUnless { it.side == ExplorerSide.DEVICE },
        transferConflictDialog = null,
    )

/** Вернёт `true`, если устройство [deviceId] всё ещё активно и доступно. */
internal fun DefaultFileExplorerComponent.isActiveDevice(deviceId: String): Boolean {
    val selected = deviceManager.selectedDeviceFlow.value
    return selected != null &&
        selected.state == DeviceState.DEVICE &&
        selected.deviceId == deviceId &&
        _state.value.activeDeviceId == deviceId
}

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

internal fun DefaultFileExplorerComponent.loadDeviceDirectory(
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
internal fun DefaultFileExplorerComponent.loadDeviceRoots(deviceId: String) {
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
internal suspend fun DefaultFileExplorerComponent.resolveAccessibleRoots(
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
    settingsRepository.getSettings().adbPath.ifBlank { "adb" }

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

/** Нормализует device-путь в абсолютный формат без хвостовых `/`. */
internal fun DefaultFileExplorerComponent.normalizeDevicePath(path: String): String {
    val normalized = path.trim().ifBlank { "/" }
        .let { if (it.startsWith("/")) it else "/$it" }
        .replace(Regex("/{2,}"), "/")
        .trimEnd('/')
    return normalized.ifBlank { "/" }
}

/** Базовый список корней для dropdown, даже если Storage недоступен. */
internal fun DefaultFileExplorerComponent.defaultDeviceRoots(): List<String> = listOf(
    deviceFileService.defaultPath(),
    "/storage/emulated/0",
    "/storage/self/primary",
    "/data/local/tmp",
).flatMap(::toBrowsablePaths).distinct()

/** Предпочтительный стартовый путь для device-панели. */
internal fun DefaultFileExplorerComponent.preferredDeviceStartPath(roots: List<String>): String {
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
internal fun DefaultFileExplorerComponent.toBrowsablePaths(rawPath: String): List<String> {
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

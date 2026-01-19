package com.adbdeck.feature.fileexplorer

import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Выполняет короткую action-операцию с флагом [FileExplorerState.isActionRunning]. */
internal fun DefaultFileExplorerComponent.runAction(
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
                val message = resolveErrorMessage(
                    type = FileExplorerErrorType.OPERATION_FAILED,
                    cause = result.exceptionOrNull(),
                )
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
internal fun DefaultFileExplorerComponent.refreshSide(side: ExplorerSide) {
    when (side) {
        ExplorerSide.LOCAL -> onRefreshLocal()
        ExplorerSide.DEVICE -> onRefreshDevice()
    }
}

/** Получить выбранный элемент на стороне [side]. */
internal fun DefaultFileExplorerComponent.selectedItem(side: ExplorerSide): ExplorerFileItem? {
    val panel = when (side) {
        ExplorerSide.LOCAL -> _state.value.localPanel
        ExplorerSide.DEVICE -> _state.value.devicePanel
    }

    val items = (panel.listState as? ExplorerListState.Success)?.items ?: return null
    return items.firstOrNull { it.fullPath == panel.selectedPath }
}

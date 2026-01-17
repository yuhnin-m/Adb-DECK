package com.adbdeck.feature.fileexplorer

import adbdeck.feature.file_explorer.generated.resources.Res
import adbdeck.feature.file_explorer.generated.resources.*
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString

internal fun DefaultFileExplorerComponent.checkTransferConflictAndStart(
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

internal fun DefaultFileExplorerComponent.startTransfer(
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

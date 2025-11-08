package com.adbdeck.feature.fileexplorer.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.adbdeck.core.designsystem.AdbDeckTheme
import com.adbdeck.feature.fileexplorer.CreateDirectoryDialogState
import com.adbdeck.feature.fileexplorer.DeleteDialogState
import com.adbdeck.feature.fileexplorer.ExplorerFileItem
import com.adbdeck.feature.fileexplorer.ExplorerFileType
import com.adbdeck.feature.fileexplorer.ExplorerListState
import com.adbdeck.feature.fileexplorer.ExplorerPanelState
import com.adbdeck.feature.fileexplorer.ExplorerSide
import com.adbdeck.feature.fileexplorer.FileExplorerComponent
import com.adbdeck.feature.fileexplorer.FileExplorerState
import com.adbdeck.feature.fileexplorer.RenameDialogState
import com.adbdeck.feature.fileexplorer.TransferConflictDialogState
import com.adbdeck.feature.fileexplorer.TransferDirection
import com.adbdeck.feature.fileexplorer.TransferState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.jetbrains.compose.ui.tooling.preview.Preview

private val localPreviewItems = listOf(
    ExplorerFileItem("Projects", "/Users/demo/Projects", ExplorerFileType.DIRECTORY, modifiedEpochMillis = 1_740_000_000_000),
    ExplorerFileItem("notes.txt", "/Users/demo/notes.txt", ExplorerFileType.FILE, sizeBytes = 4_128, modifiedEpochMillis = 1_740_000_100_000),
)

private val devicePreviewItems = listOf(
    ExplorerFileItem("Download", "/sdcard/Download", ExplorerFileType.DIRECTORY, modifiedEpochMillis = 1_740_000_200_000),
    ExplorerFileItem("logcat.txt", "/sdcard/logcat.txt", ExplorerFileType.FILE, sizeBytes = 95_016, modifiedEpochMillis = 1_740_000_300_000),
)

/**
 * Preview-заглушка [FileExplorerComponent].
 */
private class FileExplorerPreviewComponent(
    withTransfer: Boolean,
) : FileExplorerComponent {
    override val state: StateFlow<FileExplorerState> = MutableStateFlow(
        FileExplorerState(
            localPanel = ExplorerPanelState(
                currentPath = "/Users/demo",
                listState = ExplorerListState.Success(localPreviewItems),
                selectedPath = "/Users/demo/notes.txt",
            ),
            devicePanel = ExplorerPanelState(
                currentPath = "/sdcard",
                listState = ExplorerListState.Success(devicePreviewItems),
                selectedPath = "/sdcard/logcat.txt",
            ),
            activeDeviceId = "emulator-5554",
            deviceRoots = listOf("/sdcard", "/storage/emulated/0", "/data/local/tmp"),
            transferState = if (withTransfer) {
                TransferState(
                    direction = TransferDirection.PUSH,
                    sourcePath = "/Users/demo/notes.txt",
                    targetPath = "/sdcard/notes.txt",
                    status = "Выполняется push на устройство…",
                    progress = null,
                )
            } else {
                null
            },
        )
    )

    override fun onRefreshLocal() = Unit
    override fun onLocalUp() = Unit
    override fun onOpenLocalDirectory(path: String) = Unit
    override fun onSelectLocal(path: String) = Unit
    override fun onRefreshDevice() = Unit
    override fun onDeviceUp() = Unit
    override fun onOpenDeviceDirectory(path: String) = Unit
    override fun onSelectDevice(path: String) = Unit
    override fun onSelectDeviceRoot(path: String) = Unit
    override fun onRequestDelete(side: ExplorerSide) = Unit
    override fun onConfirmDelete() = Unit
    override fun onCancelDelete() = Unit
    override fun onRequestCreateDirectory(side: ExplorerSide) = Unit
    override fun onCreateDirectoryNameChanged(value: String) = Unit
    override fun onConfirmCreateDirectory() = Unit
    override fun onCancelCreateDirectory() = Unit
    override fun onRequestRename(side: ExplorerSide) = Unit
    override fun onRenameValueChanged(value: String) = Unit
    override fun onConfirmRename() = Unit
    override fun onCancelRename() = Unit
    override fun onPushSelected() = Unit
    override fun onPullSelected() = Unit
    override fun onConfirmTransferConflict() = Unit
    override fun onCancelTransferConflict() = Unit
    override fun onCancelTransfer() = Unit
    override fun onPathCopied(path: String) = Unit
    override fun onDismissFeedback() = Unit
}

@Composable
private fun FileExplorerPreviewBody(
    isDarkTheme: Boolean,
    withTransfer: Boolean,
) {
    val component = FileExplorerPreviewComponent(withTransfer = withTransfer)
    AdbDeckTheme(isDarkTheme = isDarkTheme) {
        Surface(modifier = Modifier.fillMaxSize()) {
            FileExplorerScreen(component)
        }
    }
}

@Preview
@Composable
private fun FileExplorerLightPreview() {
    FileExplorerPreviewBody(isDarkTheme = false, withTransfer = false)
}

@Preview
@Composable
private fun FileExplorerDarkPreview() {
    FileExplorerPreviewBody(isDarkTheme = true, withTransfer = true)
}

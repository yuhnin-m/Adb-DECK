package com.adbdeck.feature.fileexplorer.ui

import adbdeck.feature.file_explorer.generated.resources.Res
import adbdeck.feature.file_explorer.generated.resources.file_explorer_action_ok
import adbdeck.feature.file_explorer.generated.resources.file_explorer_action_pull
import adbdeck.feature.file_explorer.generated.resources.file_explorer_action_push
import adbdeck.feature.file_explorer.generated.resources.file_explorer_panel_device_title
import adbdeck.feature.file_explorer.generated.resources.file_explorer_panel_local_title
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.VerticalDivider
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import com.adbdeck.core.designsystem.Dimensions
import com.adbdeck.core.ui.AdbBanner
import com.adbdeck.core.ui.AdbBannerDismissStyle
import com.adbdeck.core.ui.AdbBannerType
import com.adbdeck.feature.fileexplorer.ExplorerSide
import com.adbdeck.feature.fileexplorer.FileExplorerComponent
import org.jetbrains.compose.resources.stringResource

/**
 * Корневой экран двухпанельного File Explorer.
 */
@Composable
fun FileExplorerScreen(component: FileExplorerComponent) {
    val state by component.state.collectAsState()
    val clipboard = LocalClipboardManager.current

    Column(modifier = Modifier.fillMaxSize()) {
        Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
            ExplorerPanel(
                title = stringResource(Res.string.file_explorer_panel_local_title),
                state = state.localPanel,
                isBusy = state.isActionRunning || state.transferState != null,
                onUp = component::onLocalUp,
                onRefresh = component::onRefreshLocal,
                onOpenDirectory = component::onOpenLocalDirectory,
                onSelect = component::onSelectLocal,
                onRequestCreateDirectory = { component.onRequestCreateDirectory(ExplorerSide.LOCAL) },
                onRequestRename = { component.onRequestRename(ExplorerSide.LOCAL) },
                onRequestDelete = { component.onRequestDelete(ExplorerSide.LOCAL) },
                onCopyError = { error ->
                    clipboard.setText(AnnotatedString(error))
                    component.onPathCopied(error)
                },
                transferButtonText = stringResource(Res.string.file_explorer_action_push),
                transferButtonIcon = Icons.AutoMirrored.Outlined.ArrowForward,
                transferActionEnabled = state.activeDeviceId != null,
                onTransferAction = component::onPushSelected,
                modifier = Modifier.weight(1f),
            )

            VerticalDivider()

            ExplorerPanel(
                title = stringResource(Res.string.file_explorer_panel_device_title),
                state = state.devicePanel,
                deviceRoots = if (state.activeDeviceId != null) state.deviceRoots else emptyList(),
                isBusy = state.isActionRunning || state.transferState != null,
                onUp = component::onDeviceUp,
                onRefresh = component::onRefreshDevice,
                onOpenDirectory = component::onOpenDeviceDirectory,
                onSelect = component::onSelectDevice,
                onSelectRoot = component::onSelectDeviceRoot,
                onRequestCreateDirectory = { component.onRequestCreateDirectory(ExplorerSide.DEVICE) },
                onRequestRename = { component.onRequestRename(ExplorerSide.DEVICE) },
                onRequestDelete = { component.onRequestDelete(ExplorerSide.DEVICE) },
                onCopyError = { error ->
                    clipboard.setText(AnnotatedString(error))
                    component.onPathCopied(error)
                },
                transferButtonText = stringResource(Res.string.file_explorer_action_pull),
                transferButtonIcon = Icons.AutoMirrored.Outlined.ArrowBack,
                transferActionEnabled = state.activeDeviceId != null,
                onTransferAction = component::onPullSelected,
                modifier = Modifier.weight(1f),
            )
        }

        state.transferState?.let { transfer ->
            HorizontalDivider()
            TransferStatus(
                transfer = transfer,
                onCancelTransfer = component::onCancelTransfer,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        state.feedback?.let { feedback ->
            HorizontalDivider()
            AdbBanner(
                message = feedback.message,
                type = if (feedback.isError) AdbBannerType.ERROR else AdbBannerType.SUCCESS,
                onDismiss = component::onDismissFeedback,
                dismissStyle = AdbBannerDismissStyle.TEXT,
                dismissText = stringResource(Res.string.file_explorer_action_ok),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = Dimensions.paddingMedium,
                        vertical = Dimensions.paddingSmall,
                    ),
            )
        }
    }

    state.deleteDialog?.let {
        DeleteDialog(
            state = it,
            isRunning = state.isActionRunning,
            onConfirm = component::onConfirmDelete,
            onCancel = component::onCancelDelete,
        )
    }

    state.createDirectoryDialog?.let {
        CreateDirectoryDialog(
            state = it,
            isRunning = state.isActionRunning,
            onNameChanged = component::onCreateDirectoryNameChanged,
            onConfirm = component::onConfirmCreateDirectory,
            onCancel = component::onCancelCreateDirectory,
        )
    }

    state.renameDialog?.let {
        RenameDialog(
            state = it,
            isRunning = state.isActionRunning,
            onNameChanged = component::onRenameValueChanged,
            onConfirm = component::onConfirmRename,
            onCancel = component::onCancelRename,
        )
    }

    state.transferConflictDialog?.let {
        TransferConflictDialog(
            state = it,
            isRunning = state.isActionRunning,
            onConfirm = component::onConfirmTransferConflict,
            onCancel = component::onCancelTransferConflict,
        )
    }
}

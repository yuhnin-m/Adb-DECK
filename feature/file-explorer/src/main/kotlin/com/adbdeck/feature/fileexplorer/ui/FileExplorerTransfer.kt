package com.adbdeck.feature.fileexplorer.ui

import adbdeck.feature.file_explorer.generated.resources.Res
import adbdeck.feature.file_explorer.generated.resources.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.adbdeck.core.ui.buttons.AdbButtonSize
import com.adbdeck.core.ui.buttons.AdbButtonType
import com.adbdeck.core.ui.buttons.AdbFilledButton
import com.adbdeck.core.ui.buttons.AdbOutlinedButton
import com.adbdeck.core.ui.buttons.AdbPlainButton
import com.adbdeck.feature.fileexplorer.FileExplorerState
import com.adbdeck.feature.fileexplorer.TransferDirection
import com.adbdeck.feature.fileexplorer.TransferState
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun TransferActions(
    state: FileExplorerState,
    onPush: () -> Unit,
    onPull: () -> Unit,
    onCancelTransfer: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val localSelected = selectedItem(state.localPanel)
    val deviceSelected = selectedItem(state.devicePanel)
    val canPush = localSelected != null && state.activeDeviceId != null && state.transferState == null
    val canPull = deviceSelected != null && state.activeDeviceId != null && state.transferState == null

    Row(
        modifier = modifier.padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(Res.string.file_explorer_transfer_title),
            style = MaterialTheme.typography.titleSmall,
        )
        Spacer(Modifier.weight(1f))

        AdbFilledButton(
            onClick = onPush,
            enabled = canPush,
            text = stringResource(Res.string.file_explorer_action_push),
            leadingIcon = Icons.AutoMirrored.Outlined.ArrowForward,
            size = AdbButtonSize.MEDIUM,
        )

        AdbFilledButton(
            onClick = onPull,
            enabled = canPull,
            text = stringResource(Res.string.file_explorer_action_pull),
            leadingIcon = Icons.AutoMirrored.Outlined.ArrowBack,
            size = AdbButtonSize.MEDIUM,
        )

        if (state.transferState != null) {
            AdbOutlinedButton(
                onClick = onCancelTransfer,
                text = stringResource(Res.string.file_explorer_action_cancel),
                type = AdbButtonType.DANGER,
                size = AdbButtonSize.MEDIUM,
            )
        }
    }
}

@Composable
internal fun TransferStatus(
    transfer: TransferState,
    onCancelTransfer: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ElevatedCard(modifier = modifier.padding(10.dp)) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.SwapHoriz, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    text = when (transfer.direction) {
                        TransferDirection.PUSH -> stringResource(Res.string.file_explorer_transfer_direction_push)
                        TransferDirection.PULL -> stringResource(Res.string.file_explorer_transfer_direction_pull)
                    },
                    style = MaterialTheme.typography.labelLarge,
                )
                Spacer(Modifier.weight(1f))
                AdbPlainButton(
                    onClick = onCancelTransfer,
                    text = stringResource(Res.string.file_explorer_action_cancel),
                    size = AdbButtonSize.MEDIUM,
                    type = AdbButtonType.DANGER,
                )
            }

            Text(
                text = stringResource(Res.string.file_explorer_transfer_source, transfer.sourcePath),
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = stringResource(Res.string.file_explorer_transfer_target, transfer.targetPath),
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            if (transfer.progress == null) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            } else {
                LinearProgressIndicator(progress = { transfer.progress }, modifier = Modifier.fillMaxWidth())
            }
            Text(
                text = transfer.status,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

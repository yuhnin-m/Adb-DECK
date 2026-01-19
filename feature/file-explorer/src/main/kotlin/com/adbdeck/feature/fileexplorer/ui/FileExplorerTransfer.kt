package com.adbdeck.feature.fileexplorer.ui

import adbdeck.feature.file_explorer.generated.resources.Res
import adbdeck.feature.file_explorer.generated.resources.*
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import com.adbdeck.core.designsystem.AdbTheme
import com.adbdeck.core.designsystem.Dimensions
import com.adbdeck.core.ui.buttons.AdbButtonSize
import com.adbdeck.core.ui.buttons.AdbButtonType
import com.adbdeck.core.ui.buttons.AdbPlainButton
import com.adbdeck.core.ui.sectioncards.AdbSectionCard
import com.adbdeck.feature.fileexplorer.TransferDirection
import com.adbdeck.feature.fileexplorer.TransferState
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun TransferStatus(
    transfer: TransferState,
    onCancelTransfer: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AdbSectionCard(
        title = stringResource(
            when (transfer.direction) {
                TransferDirection.PUSH -> Res.string.file_explorer_transfer_direction_push
                TransferDirection.PULL -> Res.string.file_explorer_transfer_direction_pull
            }
        ),
        modifier = modifier.padding(Dimensions.paddingSmall),
        headerTrailing = {
            AdbPlainButton(
                onClick = onCancelTransfer,
                text = stringResource(Res.string.file_explorer_action_cancel),
                size = AdbButtonSize.MEDIUM,
                type = AdbButtonType.DANGER,
            )
        },
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Outlined.SwapHoriz,
                contentDescription = null,
                modifier = Modifier.size(Dimensions.iconSizeSmall),
                tint = AdbTheme.colorScheme.primary,
            )
            Spacer(Modifier.width(Dimensions.paddingSmall))
            Text(
                text = stringResource(
                    when (transfer.direction) {
                        TransferDirection.PUSH -> Res.string.file_explorer_transfer_direction_push
                        TransferDirection.PULL -> Res.string.file_explorer_transfer_direction_pull
                    }
                ),
                style = MaterialTheme.typography.bodySmall,
                color = AdbTheme.colorScheme.onSurfaceVariant,
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
            color = AdbTheme.colorScheme.onSurfaceVariant,
        )
    }
}

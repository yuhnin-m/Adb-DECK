package com.adbdeck.feature.settings.ui

import adbdeck.feature.settings.generated.resources.Res
import adbdeck.feature.settings.generated.resources.settings_action_checking
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.adbdeck.core.designsystem.AdbTheme
import com.adbdeck.core.designsystem.Dimensions
import com.adbdeck.core.ui.buttons.AdbButtonSize
import com.adbdeck.core.ui.buttons.AdbFilledButton
import com.adbdeck.core.ui.buttons.AdbOutlinedButton
import com.adbdeck.core.ui.textfields.AdbOutlinedTextField
import com.adbdeck.core.ui.textfields.AdbTextFieldSize
import com.adbdeck.feature.settings.ToolCheckState
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun ToolSettingBlock(
    title: String,
    description: String,
    path: String,
    placeholder: String,
    checkLabel: String,
    checkingLabel: String,
    autoDetectLabel: String?,
    isAutoDetecting: Boolean,
    autoDetectCandidates: List<String>,
    choosePathLabel: String,
    checkState: ToolCheckState,
    onPathChanged: (String) -> Unit,
    onBrowse: () -> Unit,
    onAutoDetect: (() -> Unit)?,
    onSelectAutoDetectedPath: ((String) -> Unit)?,
    onDismissAutoDetectCandidates: (() -> Unit)?,
    onOpenGuide: (() -> Unit)?,
    onCheck: () -> Unit,
) {
    val isChecking = checkState is ToolCheckState.Checking

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(Dimensions.paddingDefault),
        verticalArrangement = Arrangement.spacedBy(Dimensions.paddingSmall),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Dimensions.paddingXSmall),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AdbOutlinedTextField(
                value = path,
                onValueChange = onPathChanged,
                placeholder = placeholder,
                modifier = Modifier.weight(1f),
                size = AdbTextFieldSize.MEDIUM,
                trailingIcon = if (path.isNotBlank()) Icons.Outlined.Clear else null,
                onTrailingIconClick = if (path.isNotBlank()) {
                    { onPathChanged("") }
                } else {
                    null
                },
            )

            AdbOutlinedButton(
                onClick = onBrowse,
                text = choosePathLabel,
                leadingIcon = Icons.Outlined.FolderOpen,
                size = AdbButtonSize.MEDIUM,
            )

            AdbFilledButton(
                onClick = onCheck,
                text = if (isChecking) checkingLabel else checkLabel,
                loading = isChecking,
                enabled = !isChecking,
                size = AdbButtonSize.MEDIUM,
            )

            if (onOpenGuide != null) {
                IconButton(onClick = onOpenGuide) {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = null,
                        modifier = Modifier.size(Dimensions.iconSizeCard),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        if (!autoDetectLabel.isNullOrBlank() && onAutoDetect != null) {
            Box {
                AdbOutlinedButton(
                    onClick = onAutoDetect,
                    text = autoDetectLabel,
                    loading = isAutoDetecting,
                    enabled = !isAutoDetecting,
                    size = AdbButtonSize.MEDIUM,
                )

                DropdownMenu(
                    expanded = autoDetectCandidates.isNotEmpty(),
                    onDismissRequest = { onDismissAutoDetectCandidates?.invoke() },
                ) {
                    autoDetectCandidates.forEach { candidate ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = candidate,
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            },
                            onClick = {
                                onSelectAutoDetectedPath?.invoke(candidate)
                            },
                        )
                    }
                }
            }
        }

        ToolCheckStateRow(state = checkState)
    }
}

@Composable
private fun ToolCheckStateRow(state: ToolCheckState) {
    val successColor = AdbTheme.semanticColors.success

    when (state) {
        is ToolCheckState.Success -> Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimensions.paddingXSmall),
        ) {
            Icon(
                imageVector = Icons.Outlined.CheckCircle,
                contentDescription = null,
                modifier = Modifier.size(Dimensions.iconSizeSmall),
                tint = successColor,
            )
            Text(
                text = state.message,
                style = MaterialTheme.typography.bodySmall,
                color = successColor,
            )
        }

        is ToolCheckState.Error -> Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimensions.paddingXSmall),
        ) {
            Icon(
                imageVector = Icons.Outlined.ErrorOutline,
                contentDescription = null,
                modifier = Modifier.size(Dimensions.iconSizeSmall),
                tint = MaterialTheme.colorScheme.error,
            )
            Text(
                text = state.message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }

        is ToolCheckState.Checking -> Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimensions.paddingXSmall),
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(Dimensions.iconSizeSmall),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = stringResource(Res.string.settings_action_checking),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        ToolCheckState.Idle -> Unit
    }
}

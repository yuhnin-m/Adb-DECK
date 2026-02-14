package com.adbdeck.core.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.adbdeck.core.designsystem.Dimensions

/**
 * Унифицированная строка настройки: заголовок/описание слева, control справа.
 */
@Composable
fun AdbSettingRow(
    title: String,
    modifier: Modifier = Modifier,
    description: String? = null,
    control: @Composable () -> Unit,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimensions.paddingSmall),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (!description.isNullOrBlank()) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        control()
    }
}

/**
 * Stepper для целочисленного значения.
 */
@Composable
fun AdbIntStepper(
    value: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    minValue: Int,
    maxValue: Int,
    valueLabel: String,
    decreaseContentDescription: String,
    increaseContentDescription: String,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimensions.paddingXSmall),
    ) {
        IconButton(
            onClick = { onValueChange((value - 1).coerceAtLeast(minValue)) },
            enabled = value > minValue,
        ) {
            Icon(
                imageVector = Icons.Outlined.Remove,
                contentDescription = decreaseContentDescription,
            )
        }

        Text(
            text = valueLabel,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.width(56.dp),
            maxLines = 1,
            overflow = TextOverflow.Clip,
        )

        IconButton(
            onClick = { onValueChange((value + 1).coerceAtMost(maxValue)) },
            enabled = value < maxValue,
        ) {
            Icon(
                imageVector = Icons.Outlined.Add,
                contentDescription = increaseContentDescription,
            )
        }
    }
}

package com.adbdeck.core.ui.buttons

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.adbdeck.core.designsystem.AdbCornerRadius
import com.adbdeck.core.designsystem.AdbDeckTheme
import com.adbdeck.core.designsystem.Dimensions
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
private fun PreviewSection(
    title: String,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Dimensions.paddingSmall)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        content()
    }
}

@Composable
private fun AdbButtonsPreviewContent(isDarkTheme: Boolean) {
    AdbDeckTheme(isDarkTheme = isDarkTheme) {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(Dimensions.paddingMedium),
            ) {
                PreviewSection(title = "Filled") {
                    Row(horizontalArrangement = Arrangement.spacedBy(Dimensions.paddingSmall)) {
                        AdbFilledButton(
                            onClick = {},
                            text = "Скачать",
                            type = AdbButtonType.NEUTRAL,
                            leadingIcon = Icons.Outlined.Download,
                        )
                        AdbFilledButton(
                            onClick = {},
                            text = "Удалить",
                            type = AdbButtonType.DANGER,
                            leadingIcon = Icons.Outlined.Delete,
                        )
                        AdbFilledButton(
                            onClick = {},
                            text = "Готово",
                            type = AdbButtonType.SUCCESS,
                            trailingIcon = Icons.Outlined.Check,
                        )
                        AdbFilledButton(
                            onClick = {},
                            text = "Загрузка",
                            loading = true,
                        )
                        AdbFilledButton(
                            onClick = {},
                            enabled = false,
                            leadingIcon = Icons.Outlined.PlayArrow,
                            contentDescription = "Disabled play",
                        )
                    }
                }

                PreviewSection(title = "Outlined") {
                    Row(horizontalArrangement = Arrangement.spacedBy(Dimensions.paddingSmall)) {
                        AdbOutlinedButton(
                            onClick = {},
                            text = "Запуск",
                            leadingIcon = Icons.Outlined.PlayArrow,
                        )
                        AdbOutlinedButton(
                            onClick = {},
                            text = "Удалить",
                            type = AdbButtonType.DANGER,
                            loading = true,
                        )
                        AdbOutlinedButton(
                            onClick = {},
                            type = AdbButtonType.SUCCESS,
                            leadingIcon = Icons.Outlined.Check,
                            contentDescription = "Success icon",
                        )
                    }
                }

                PreviewSection(title = "Plain") {
                    Row(horizontalArrangement = Arrangement.spacedBy(Dimensions.paddingSmall)) {
                        AdbPlainButton(
                            onClick = {},
                            text = "Скачать",
                            leadingIcon = Icons.Outlined.Download,
                        )
                        AdbPlainButton(
                            onClick = {},
                            text = "Удалить",
                            type = AdbButtonType.DANGER,
                            enabled = false,
                        )
                        AdbPlainButton(
                            onClick = {},
                            type = AdbButtonType.SUCCESS,
                            loading = true,
                            contentDescription = "Loading success icon",
                        )
                    }
                }

                PreviewSection(title = "Размеры и радиусы") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Dimensions.paddingSmall),
                    ) {
                        AdbFilledButton(
                            onClick = {},
                            text = "L",
                            size = AdbButtonSize.LARGE,
                            cornerRadius = AdbCornerRadius.XLARGE,
                        )
                        AdbFilledButton(
                            onClick = {},
                            text = "M",
                            size = AdbButtonSize.MEDIUM,
                            cornerRadius = AdbCornerRadius.LARGE,
                        )
                        AdbFilledButton(
                            onClick = {},
                            text = "S",
                            size = AdbButtonSize.SMALL,
                            cornerRadius = AdbCornerRadius.SMALL,
                        )
                        AdbFilledButton(
                            onClick = {},
                            text = "XS",
                            size = AdbButtonSize.XSMALL,
                            cornerRadius = AdbCornerRadius.NONE,
                        )
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun AdbButtonsLightPreview() {
    AdbButtonsPreviewContent(isDarkTheme = false)
}

@Preview
@Composable
private fun AdbButtonsDarkPreview() {
    AdbButtonsPreviewContent(isDarkTheme = true)
}

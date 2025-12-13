package com.adbdeck.core.ui.textfields

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.adbdeck.core.designsystem.AdbCornerRadius
import com.adbdeck.core.designsystem.AdbDeckTheme
import com.adbdeck.core.designsystem.Dimensions
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
private fun PreviewBlock(
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
private fun AdbTextFieldsPreviewContent(isDarkTheme: Boolean) {
    AdbDeckTheme(isDarkTheme = isDarkTheme) {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(Dimensions.paddingMedium),
            ) {
                val (outlinedValue, setOutlinedValue) = remember { mutableStateOf("adb logcat") }
                val (filledValue, setFilledValue) = remember { mutableStateOf("") }

                PreviewBlock(title = "Outlined / типы / состояния") {
                    AdbOutlinedTextField(
                        value = outlinedValue,
                        onValueChange = setOutlinedValue,
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = "Поиск",
                        type = AdbTextFieldType.NEUTRAL,
                        leadingIcon = Icons.Outlined.Search,
                        trailingIcon = Icons.Outlined.Close,
                        onTrailingIconClick = { setOutlinedValue("") },
                    )

                    AdbOutlinedTextField(
                        value = "delete /system",
                        onValueChange = {},
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = "Danger",
                        type = AdbTextFieldType.DANGER,
                        supportingText = "Команда опасна",
                        readOnly = true,
                    )

                    AdbOutlinedTextField(
                        value = "com.example.app",
                        onValueChange = {},
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = "Success",
                        type = AdbTextFieldType.SUCCESS,
                        supportingText = "Пакет найден",
                        enabled = false,
                    )
                }

                PreviewBlock(title = "Filled / размеры") {
                    AdbFilledTextField(
                        value = filledValue,
                        onValueChange = setFilledValue,
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = "Large",
                        size = AdbTextFieldSize.LARGE,
                        leadingIcon = Icons.Outlined.Search,
                        cornerRadius = AdbCornerRadius.LARGE,
                    )

                    AdbFilledTextField(
                        value = "",
                        onValueChange = {},
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = "Medium",
                        size = AdbTextFieldSize.MEDIUM,
                        cornerRadius = AdbCornerRadius.MEDIUM,
                    )

                    AdbFilledTextField(
                        value = "",
                        onValueChange = {},
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = "Small",
                        size = AdbTextFieldSize.SMALL,
                        cornerRadius = AdbCornerRadius.SMALL,
                    )
                }

                PreviewBlock(title = "Plain") {
                    AdbPlainTextField(
                        value = "readonly value",
                        onValueChange = {},
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = "Plain field",
                        type = AdbTextFieldType.NEUTRAL,
                        readOnly = true,
                        trailingIcon = Icons.Outlined.Close,
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun AdbTextFieldsLightPreview() {
    AdbTextFieldsPreviewContent(isDarkTheme = false)
}

@Preview
@Composable
private fun AdbTextFieldsDarkPreview() {
    AdbTextFieldsPreviewContent(isDarkTheme = true)
}

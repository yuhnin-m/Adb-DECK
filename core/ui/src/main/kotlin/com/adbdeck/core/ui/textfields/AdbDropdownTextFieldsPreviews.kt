package com.adbdeck.core.ui.textfields

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FilterList
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

private val previewLevels = listOf(
    AdbDropdownOption("all", "All"),
    AdbDropdownOption("v", "Verbose (V)"),
    AdbDropdownOption("d", "Debug (D)"),
    AdbDropdownOption("i", "Info (I)"),
    AdbDropdownOption("w", "Warning (W)"),
    AdbDropdownOption("e", "Error (E)"),
)

@Composable
private fun PreviewGroup(
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
private fun AdbDropdownTextFieldsPreviewContent(isDarkTheme: Boolean) {
    AdbDeckTheme(isDarkTheme = isDarkTheme) {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(Dimensions.paddingMedium),
            ) {
                val (outlinedValue, setOutlinedValue) = remember { mutableStateOf("all") }
                val (filledValue, setFilledValue) = remember { mutableStateOf("d") }
                val (plainValue, setPlainValue) = remember { mutableStateOf<String?>(null) }

                PreviewGroup(title = "Outlined") {
                    AdbOutlinedDropdownTextField(
                        options = previewLevels,
                        selectedValue = outlinedValue,
                        onValueSelected = setOutlinedValue,
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = "Выберите уровень",
                        type = AdbTextFieldType.NEUTRAL,
                        leadingIcon = Icons.Outlined.FilterList,
                        cornerRadius = AdbCornerRadius.LARGE,
                    )
                }

                PreviewGroup(title = "Filled / Danger") {
                    AdbFilledDropdownTextField(
                        options = previewLevels,
                        selectedValue = filledValue,
                        onValueSelected = setFilledValue,
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = "Filled dropdown",
                        type = AdbTextFieldType.DANGER,
                        supportingText = "Внимание: опасный режим",
                        size = AdbTextFieldSize.MEDIUM,
                    )
                }

                PreviewGroup(title = "Plain / Success") {
                    AdbPlainDropdownTextField(
                        options = previewLevels,
                        selectedValue = plainValue,
                        onValueSelected = setPlainValue,
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = "Plain dropdown",
                        type = AdbTextFieldType.SUCCESS,
                        size = AdbTextFieldSize.SMALL,
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun AdbDropdownTextFieldsLightPreview() {
    AdbDropdownTextFieldsPreviewContent(isDarkTheme = false)
}

@Preview
@Composable
private fun AdbDropdownTextFieldsDarkPreview() {
    AdbDropdownTextFieldsPreviewContent(isDarkTheme = true)
}

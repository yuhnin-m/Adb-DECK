package com.adbdeck.core.ui.textfields

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.adbdeck.core.designsystem.AdbCornerRadius
import com.adbdeck.core.designsystem.AdbDeckTheme
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
private fun AdbAutocompleteTextFieldsPreviewContent(isDarkTheme: Boolean) {
    var outlinedValue by remember { mutableStateOf("") }
    var filledValue by remember { mutableStateOf("com.example.app") }

    val suggestions = remember {
        listOf(
            "com.example.app",
            "com.example.core",
            "com.google.android.gms",
            "com.android.settings",
            "ru.example.demo",
        )
    }

    AdbDeckTheme(isDarkTheme = isDarkTheme) {
        Surface {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                AdbOutlinedAutocompleteTextField(
                    value = outlinedValue,
                    onValueChange = { outlinedValue = it },
                    suggestions = suggestions,
                    onSuggestionSelected = { outlinedValue = it },
                    placeholder = "Package filter",
                    size = AdbTextFieldSize.MEDIUM,
                    cornerRadius = AdbCornerRadius.LARGE,
                    modifier = Modifier.fillMaxWidth(),
                )

                AdbFilledAutocompleteTextField(
                    value = filledValue,
                    onValueChange = { filledValue = it },
                    suggestions = suggestions,
                    onSuggestionSelected = { filledValue = it },
                    placeholder = "Package",
                    size = AdbTextFieldSize.MEDIUM,
                    cornerRadius = AdbCornerRadius.LARGE,
                    modifier = Modifier.fillMaxWidth(),
                )

                AdbPlainAutocompleteTextField(
                    value = "",
                    onValueChange = {},
                    suggestions = suggestions,
                    onSuggestionSelected = {},
                    placeholder = "Disabled",
                    enabled = false,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Preview
@Composable
private fun AdbAutocompleteTextFieldsLightPreview() {
    AdbAutocompleteTextFieldsPreviewContent(isDarkTheme = false)
}

@Preview
@Composable
private fun AdbAutocompleteTextFieldsDarkPreview() {
    AdbAutocompleteTextFieldsPreviewContent(isDarkTheme = true)
}

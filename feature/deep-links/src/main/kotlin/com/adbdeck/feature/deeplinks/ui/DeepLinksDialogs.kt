package com.adbdeck.feature.deeplinks.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import com.adbdeck.core.ui.alertdialogs.AdbAlertDialog
import com.adbdeck.core.ui.alertdialogs.AdbAlertDialogAction
import com.adbdeck.core.ui.textfields.AdbOutlinedTextField

@Composable
internal fun SaveTemplateDialog(
    name: String,
    onNameChanged: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AdbAlertDialog(
        onDismissRequest = onDismiss,
        title = "Сохранить шаблон",
        confirmAction = AdbAlertDialogAction(
            text = "Сохранить",
            onClick = onConfirm,
            enabled = name.isNotBlank(),
        ),
        dismissAction = AdbAlertDialogAction(
            text = "Отмена",
            onClick = onDismiss,
        ),
    ) {
        Text(
            text = "Введите название для текущей конфигурации:",
            style = MaterialTheme.typography.bodyMedium,
        )
        AdbOutlinedTextField(
            value = name,
            onValueChange = onNameChanged,
            placeholder = "Название шаблона",
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            trailingIcon = if (name.isNotBlank()) Icons.Outlined.Close else null,
            onTrailingIconClick = if (name.isNotBlank()) {
                { onNameChanged("") }
            } else {
                null
            },
        )
    }
}

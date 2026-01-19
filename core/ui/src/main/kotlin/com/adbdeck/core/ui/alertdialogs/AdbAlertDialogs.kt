package com.adbdeck.core.ui.alertdialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.adbdeck.core.designsystem.AdbDeckTheme
import com.adbdeck.core.ui.buttons.AdbButtonType
import com.adbdeck.core.ui.buttons.AdbFilledButton
import com.adbdeck.core.ui.buttons.AdbOutlinedButton
import org.jetbrains.compose.ui.tooling.preview.Preview

/**
 * Универсальный alert-dialog для подтверждений и форм.
 *
 * Компонент инкапсулирует единый стиль заголовка/кнопок и позволяет передавать
 * произвольный контент через слот [content].
 *
 * @param onDismissRequest Callback запроса закрытия.
 * @param title Заголовок диалога.
 * @param modifier Modifier диалога.
 * @param titleIcon Опциональная иконка рядом с заголовком.
 * @param titleIconTint Цвет иконки заголовка.
 * @param confirmAction Конфигурация подтверждающей кнопки (filled).
 * @param dismissAction Конфигурация отмены (outlined), `null` — без кнопки отмены.
 * @param hideDismissWhenBusy Скрывать кнопку отмены во время выполнения.
 * @param allowDismissWhenBusy Разрешать закрытие диалога вне кнопок во время выполнения.
 * @param contentSpacing Вертикальный отступ между элементами контента.
 * @param content Контент диалога (текст/поля ввода/доп. элементы).
 */
@Composable
fun AdbAlertDialog(
    onDismissRequest: () -> Unit,
    title: String,
    modifier: Modifier = Modifier,
    titleIcon: ImageVector? = null,
    titleIconTint: Color = MaterialTheme.colorScheme.primary,
    confirmAction: AdbAlertDialogAction,
    dismissAction: AdbAlertDialogAction? = null,
    hideDismissWhenBusy: Boolean = true,
    allowDismissWhenBusy: Boolean = false,
    contentSpacing: Dp = 8.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    val isBusy = confirmAction.loading || (dismissAction?.loading == true)
    val canDismiss = allowDismissWhenBusy || !isBusy

    AlertDialog(
        onDismissRequest = {
            if (canDismiss) {
                onDismissRequest()
            }
        },
        modifier = modifier,
        properties = DialogProperties(
            dismissOnBackPress = canDismiss,
            dismissOnClickOutside = canDismiss,
        ),
        title = {
            if (titleIcon == null) {
                Text(text = title)
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = titleIcon,
                        contentDescription = null,
                        tint = titleIconTint,
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = title)
                }
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(contentSpacing),
                content = content,
            )
        },
        confirmButton = {
            AdbFilledButton(
                onClick = confirmAction.onClick,
                text = confirmAction.text,
                enabled = confirmAction.enabled,
                loading = confirmAction.loading,
                type = confirmAction.type,
                size = confirmAction.size,
                cornerRadius = confirmAction.cornerRadius,
            )
        },
        dismissButton = {
            val shouldShowDismiss = dismissAction != null && (!hideDismissWhenBusy || !isBusy)
            if (shouldShowDismiss) {
                dismissAction?.let { action ->
                    AdbOutlinedButton(
                        onClick = action.onClick,
                        text = action.text,
                        enabled = action.enabled,
                        loading = action.loading,
                        type = action.type,
                        size = action.size,
                        cornerRadius = action.cornerRadius,
                    )
                }
            }
        },
    )
}

@Composable
private fun AdbAlertDialogPreviewContent(isDarkTheme: Boolean, loading: Boolean) {
    AdbDeckTheme(isDarkTheme = isDarkTheme) {
        Surface(modifier = Modifier.fillMaxSize()) {
            AdbAlertDialog(
                onDismissRequest = {},
                title = if (loading) "Файл уже существует" else "Удалить элемент?",
                titleIcon = if (loading) Icons.Outlined.WarningAmber else null,
                titleIconTint = MaterialTheme.colorScheme.error,
                confirmAction = AdbAlertDialogAction(
                    text = if (loading) "Перезаписать" else "Удалить",
                    onClick = {},
                    type = if (loading) AdbButtonType.DANGER else AdbButtonType.NEUTRAL,
                    loading = loading,
                ),
                dismissAction = AdbAlertDialogAction(
                    text = "Отмена",
                    onClick = {},
                ),
            ) {
                Text(
                    text = if (loading) {
                        "На целевом пути уже есть элемент с таким именем. Перезаписать?"
                    } else {
                        "Будет удалён: /sdcard/Documents/report.txt"
                    },
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Preview
@Composable
private fun AdbAlertDialogLightPreview() {
    AdbAlertDialogPreviewContent(
        isDarkTheme = false,
        loading = false,
    )
}

@Preview
@Composable
private fun AdbAlertDialogDarkPreview() {
    AdbAlertDialogPreviewContent(
        isDarkTheme = true,
        loading = true,
    )
}

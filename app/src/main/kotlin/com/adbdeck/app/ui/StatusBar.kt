package com.adbdeck.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.adbdeck.core.ui.buttons.AdbButtonSize
import com.adbdeck.core.ui.buttons.AdbOutlinedButton
import com.adbdeck.core.designsystem.Dimensions

/**
 * Нижняя строка состояния (StatusBar) главного окна ADB Deck.
 *
 * Отображает общую статусную информацию (например, количество устройств,
 * версию adb, текущую операцию и пр.).
 *
 * @param statusText Текст статуса. Если пустой — строка показывает "Готово".
 * @param historyCount Количество записей в in-memory истории команд.
 * @param isHistoryOpen Флаг, что боковая панель истории сейчас открыта.
 * @param onToggleHistory Callback открытия/закрытия панели истории.
 */
@Composable
fun StatusBar(
    statusText: String = "",
    historyCount: Int = 0,
    isHistoryOpen: Boolean = false,
    onToggleHistory: (() -> Unit)? = null,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
    ) {
        Column {
            HorizontalDivider()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(Dimensions.statusBarHeight)
                    .padding(horizontal = Dimensions.paddingDefault),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = statusText.ifBlank { "Готово" },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.weight(1f))
                onToggleHistory?.let { toggleHistory ->
                    AdbOutlinedButton(
                        onClick = toggleHistory,
                        text = if (isHistoryOpen) {
                            "Закрыть историю"
                        } else {
                            "История ($historyCount)"
                        },
                        size = AdbButtonSize.XSMALL,
                    )
                    Spacer(Modifier.width(Dimensions.paddingXSmall))
                }
                Text(
                    text = "ADB Deck v1.0",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
        } // Column
    }
}

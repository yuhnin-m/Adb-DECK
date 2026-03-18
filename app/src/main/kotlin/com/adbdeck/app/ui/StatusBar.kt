package com.adbdeck.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.adbdeck.app.APP_DISPLAY_NAME
import com.adbdeck.app.APP_VERSION
import com.adbdeck.core.designsystem.Dimensions

/**
 * Нижняя строка состояния (StatusBar) главного окна ADB Deck.
 *
 * Отображает общую статусную информацию (например, количество устройств,
 * версию adb, текущую операцию и пр.).
 *
 * @param statusText Текст статуса. Если пустой — строка показывает "Готово".
 */
@Composable
fun StatusBar(
    statusText: String = "",
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
                Text(
                    text = "$APP_DISPLAY_NAME v$APP_VERSION",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
        } // Column
    }
}

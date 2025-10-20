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
import androidx.compose.ui.unit.dp
import com.adbdeck.core.designsystem.Dimensions

/**
 * Верхняя панель (TopBar) главного окна ADB Deck.
 *
 * Отображает текущий заголовок раздела слева и опциональный trailing-контент
 * справа (обычно [DeviceBar] с выбором активного устройства).
 *
 * @param title          Заголовок текущего раздела.
 * @param trailingContent Composable-слот для правой части (например, DeviceBar).
 *                        Если `null` — правая часть остается пустой.
 */
@Composable
fun TopBar(
    title: String,
    trailingContent: @Composable (() -> Unit)? = null,
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(Dimensions.topBarHeight)
                    .padding(horizontal = Dimensions.paddingDefault),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Spacer(Modifier.width(Dimensions.paddingXSmall))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.weight(1f))
                trailingContent?.invoke()
                Spacer(Modifier.width(Dimensions.paddingXSmall))
            }
            HorizontalDivider(modifier = Modifier.fillMaxWidth())
        }
    }
}

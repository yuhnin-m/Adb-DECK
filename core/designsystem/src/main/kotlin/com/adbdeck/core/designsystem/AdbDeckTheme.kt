package com.adbdeck.core.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

/**
 * Корневая тема приложения ADB Deck.
 *
 * Оборачивает [MaterialTheme] с кастомными цветами и типографикой.
 * Поддерживает светлую и тёмную темы; переключение происходит через
 * параметр [isDarkTheme], который управляется из [AppSettings].
 *
 * @param isDarkTheme Если `true` — применяется тёмная тема, иначе светлая.
 *                    По умолчанию читает системные настройки.
 * @param content     Содержимое, к которому применяется тема.
 */
@Composable
fun AdbDeckTheme(
    isDarkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (isDarkTheme) DarkColorScheme else LightColorScheme,
        typography = AdbDeckTypography,
        content = content,
    )
}

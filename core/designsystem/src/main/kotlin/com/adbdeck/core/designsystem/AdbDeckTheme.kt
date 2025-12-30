package com.adbdeck.core.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

private val AdbDeckShapes = Shapes(
    extraSmall = RoundedCornerShape(AdbCornerRadius.SMALL.value),
    small = RoundedCornerShape(AdbCornerRadius.MEDIUM.value),
    medium = RoundedCornerShape(AdbCornerRadius.LARGE.value),
    large = RoundedCornerShape(AdbCornerRadius.XLARGE.value),
    extraLarge = RoundedCornerShape(AdbCornerRadius.XLARGE.value),
)

/**
 * Корневая тема приложения ADB Deck.
 *
 * Оборачивает [MaterialTheme] с кастомными цветами, типографикой и скруглениями.
 * Поддерживает светлую и темную темы; переключение происходит через
 * параметр [isDarkTheme], который управляется из [AppSettings].
 *
 * @param isDarkTheme Если `true` — применяется темная тема, иначе светлая.
 *                    По умолчанию читает системные настройки.
 * @param content     Содержимое, к которому применяется тема.
 */
@Composable
fun AdbDeckTheme(
    isDarkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val semanticColors = if (isDarkTheme) DarkAdbSemanticColors else LightAdbSemanticColors

    CompositionLocalProvider(
        LocalAdbSemanticColors provides semanticColors,
    ) {
        MaterialTheme(
            colorScheme = if (isDarkTheme) DarkColorScheme else LightColorScheme,
            typography = AdbDeckTypography,
            shapes = AdbDeckShapes,
            content = content,
        )
    }
}

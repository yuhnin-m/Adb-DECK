package com.adbdeck.core.designsystem

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color

/**
 * Семантические цвета приложения.
 *
 * Эти токены описывают смысл, а не конкретный UI-элемент:
 * - [info] — информационное состояние;
 * - [success] — успешное состояние;
 * - [warning] — предупреждение.
 *
 * Использование семантических цветов в feature-модулях позволяет:
 * - убрать хардкод `Color(0x...)` из экранов;
 * - централизованно корректировать палитру под light/dark тему.
 */
@Immutable
data class AdbSemanticColors(
    val info: Color,
    val success: Color,
    val warning: Color,
)

/** Семантические цвета для светлой темы. */
internal val LightAdbSemanticColors = AdbSemanticColors(
    info = AdbDeckBlue,
    success = AdbDeckGreen,
    warning = AdbDeckAmber,
)

/** Семантические цвета для темной темы. */
internal val DarkAdbSemanticColors = AdbSemanticColors(
    info = Color(0xFF90CAF9),
    success = Color(0xFF81C784),
    warning = Color(0xFFFFB74D),
)

internal val LocalAdbSemanticColors = staticCompositionLocalOf { LightAdbSemanticColors }

/**
 * Точка доступа к токенам дизайн-системы.
 */
object AdbTheme {
    /**
     * Базовая Material3 colorScheme текущей темы.
     *
     * Используется как единая точка доступа к системным цветовым ролям
     * внутри feature-модулей.
     */
    val colorScheme: ColorScheme
        @Composable
        get() = MaterialTheme.colorScheme

    /** Текущие семантические цвета в рамках [AdbDeckTheme]. */
    val semanticColors: AdbSemanticColors
        @Composable
        get() = LocalAdbSemanticColors.current
}

package com.adbdeck.core.ui.buttons

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Фиксированный тип кнопки с преднастроенной цветовой семантикой.
 */
enum class AdbButtonType {
    /** Нейтральное действие (основной сценарий). */
    NEUTRAL,

    /** Опасное действие (удаление, сброс и т.п.). */
    DANGER,

    /** Позитивное действие (подтверждение, успешный сценарий). */
    SUCCESS,
}

/**
 * Размер кнопки.
 *
 * Размеры синхронизированы по высоте с `segmented/split` компонентами.
 */
enum class AdbButtonSize(
    val height: Dp,
    val minWidth: Dp,
    val horizontalPadding: Dp,
    val iconSize: Dp,
    val loaderSize: Dp,
    val contentSpacing: Dp,
) {
    /** Крупный размер. */
    LARGE(
        height = 40.dp,
        minWidth = 80.dp,
        horizontalPadding = 14.dp,
        iconSize = 18.dp,
        loaderSize = 16.dp,
        contentSpacing = 8.dp,
    ),

    /** Средний размер. */
    MEDIUM(
        height = 34.dp,
        minWidth = 68.dp,
        horizontalPadding = 12.dp,
        iconSize = 16.dp,
        loaderSize = 14.dp,
        contentSpacing = 6.dp,
    ),

    /** Малый размер. */
    SMALL(
        height = 30.dp,
        minWidth = 56.dp,
        horizontalPadding = 10.dp,
        iconSize = 14.dp,
        loaderSize = 12.dp,
        contentSpacing = 6.dp,
    ),

    /** Очень компактный размер. */
    XSMALL(
        height = 26.dp,
        minWidth = 44.dp,
        horizontalPadding = 8.dp,
        iconSize = 12.dp,
        loaderSize = 10.dp,
        contentSpacing = 4.dp,
    ),
}

internal enum class AdbButtonStyle {
    FILLED,
    OUTLINED,
    PLAIN,
}

@Immutable
internal data class AdbButtonResolvedColors(
    val containerColor: androidx.compose.ui.graphics.Color,
    val contentColor: androidx.compose.ui.graphics.Color,
    val borderColor: androidx.compose.ui.graphics.Color,
    val disabledContainerColor: androidx.compose.ui.graphics.Color,
    val disabledContentColor: androidx.compose.ui.graphics.Color,
    val disabledBorderColor: androidx.compose.ui.graphics.Color,
    val loaderColor: androidx.compose.ui.graphics.Color,
)

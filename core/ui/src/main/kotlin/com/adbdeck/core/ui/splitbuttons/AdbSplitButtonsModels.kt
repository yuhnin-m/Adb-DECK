package com.adbdeck.core.ui.splitbuttons

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Размер split-button.
 */
enum class AdbSplitButtonSize(
    val height: Dp,
    val horizontalPadding: Dp,
    val menuPartWidth: Dp,
    val iconSize: Dp,
) {
    /** Крупный размер. */
    LARGE(
        height = 40.dp,
        horizontalPadding = 14.dp,
        menuPartWidth = 36.dp,
        iconSize = 18.dp,
    ),

    /** Средний размер. */
    MEDIUM(
        height = 34.dp,
        horizontalPadding = 12.dp,
        menuPartWidth = 32.dp,
        iconSize = 16.dp,
    ),

    /** Малый размер. */
    SMALL(
        height = 30.dp,
        horizontalPadding = 10.dp,
        menuPartWidth = 28.dp,
        iconSize = 14.dp,
    ),

    /** Очень компактный размер. */
    XSMALL(
        height = 26.dp,
        horizontalPadding = 8.dp,
        menuPartWidth = 24.dp,
        iconSize = 12.dp,
    ),
}

/**
 * Пункт выпадающего меню split-button.
 *
 * @param value Бизнес-значение пункта меню.
 * @param label Текст пункта меню.
 * @param enabled Доступность пункта меню.
 */
@Immutable
data class AdbSplitMenuItem<T>(
    val value: T,
    val label: String,
    val enabled: Boolean = true,
)

/**
 * Цвета split-button.
 */
@Immutable
data class AdbSplitButtonColors(
    val containerColor: Color,
    val contentColor: Color,
    val borderColor: Color,
    val disabledContainerColor: Color,
    val disabledContentColor: Color,
)

/**
 * Набор дефолтов для split-button.
 */
object AdbSplitButtonDefaults {
    /**
     * Цветовая схема split-button.
     */
    @Composable
    fun colors(
        containerColor: Color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor: Color = MaterialTheme.colorScheme.onSecondaryContainer,
        borderColor: Color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
        disabledContainerColor: Color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        disabledContentColor: Color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
    ): AdbSplitButtonColors {
        return AdbSplitButtonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            borderColor = borderColor,
            disabledContainerColor = disabledContainerColor,
            disabledContentColor = disabledContentColor,
        )
    }
}

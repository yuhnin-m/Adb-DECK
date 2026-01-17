package com.adbdeck.core.ui.segmentedbuttons

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Размер segmented-кнопок.
 *
 * Размеры синхронизированы с [com.adbdeck.core.ui.splitbuttons.AdbSplitButtonSize],
 * чтобы элементы управления выглядели единообразно.
 */
enum class AdbSegmentedButtonSize(
    val height: Dp,
    val horizontalPadding: Dp,
    val minWidth: Dp,
    val indicatorSize: Dp,
) {
    /** Крупный размер. */
    LARGE(
        height = 40.dp,
        horizontalPadding = 14.dp,
        minWidth = 80.dp,
        indicatorSize = 8.dp,
    ),

    /** Средний размер. */
    MEDIUM(
        height = 34.dp,
        horizontalPadding = 12.dp,
        minWidth = 68.dp,
        indicatorSize = 7.dp,
    ),

    /** Малый размер. */
    SMALL(
        height = 30.dp,
        horizontalPadding = 10.dp,
        minWidth = 56.dp,
        indicatorSize = 6.dp,
    ),

    /** Очень компактный размер. */
    XSMALL(
        height = 26.dp,
        horizontalPadding = 8.dp,
        minWidth = 44.dp,
        indicatorSize = 5.dp,
    ),
}

/**
 * Опция segmented-контрола.
 *
 * @param value Бизнес-значение опции.
 * @param label Текст сегмента.
 * @param leadingIcon Иконка сегмента (опционально).
 * @param enabled Доступность опции.
 * @param indicatorColor Опциональный цвет точки слева от подписи.
 * @param contentDescription Текст для accessibility. Если `null`, используется [label].
 */
@Immutable
data class AdbSegmentedOption<T>(
    val value: T,
    val label: String,
    val leadingIcon: ImageVector? = null,
    val enabled: Boolean = true,
    val indicatorColor: Color? = null,
    val contentDescription: String? = null,
)

/**
 * Цвета segmented-контрола.
 */
@Immutable
data class AdbSegmentedButtonColors(
    val activeContainerColor: Color,
    val activeContentColor: Color,
    val inactiveContainerColor: Color,
    val inactiveContentColor: Color,
    val borderColor: Color,
    val disabledContentColor: Color,
)

/**
 * Дефолты segmented-контрола.
 */
object AdbSegmentedButtonDefaults {
    /**
     * Цветовая схема segmented-контрола.
     */
    @Composable
    fun colors(
        activeContainerColor: Color = MaterialTheme.colorScheme.secondaryContainer,
        activeContentColor: Color = MaterialTheme.colorScheme.onSecondaryContainer,
        inactiveContainerColor: Color = MaterialTheme.colorScheme.surface,
        inactiveContentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
        borderColor: Color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
        disabledContentColor: Color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
    ): AdbSegmentedButtonColors {
        return AdbSegmentedButtonColors(
            activeContainerColor = activeContainerColor,
            activeContentColor = activeContentColor,
            inactiveContainerColor = inactiveContainerColor,
            inactiveContentColor = inactiveContentColor,
            borderColor = borderColor,
            disabledContentColor = disabledContentColor,
        )
    }
}

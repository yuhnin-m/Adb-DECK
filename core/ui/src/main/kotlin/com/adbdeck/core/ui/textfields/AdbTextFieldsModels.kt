package com.adbdeck.core.ui.textfields

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Фиксированный тип текстового поля с преднастроенной цветовой семантикой.
 */
enum class AdbTextFieldType {
    /** Нейтральное состояние. */
    NEUTRAL,

    /** Ошибочное/опасное состояние. */
    DANGER,

    /** Успешное/валидное состояние. */
    SUCCESS,
}

/**
 * Размер текстового поля.
 *
 * Размеры соотносятся с другими универсальными компонентами (`buttons`, `segmented`, `split`).
 */
enum class AdbTextFieldSize(
    val height: Dp,
    val horizontalPadding: Dp,
    val iconSize: Dp,
    val contentSpacing: Dp,
) {
    /** Крупный размер. */
    LARGE(
        height = 40.dp,
        horizontalPadding = 14.dp,
        iconSize = 18.dp,
        contentSpacing = 8.dp,
    ),

    /** Средний размер. */
    MEDIUM(
        height = 34.dp,
        horizontalPadding = 12.dp,
        iconSize = 16.dp,
        contentSpacing = 6.dp,
    ),

    /** Малый размер. */
    SMALL(
        height = 30.dp,
        horizontalPadding = 10.dp,
        iconSize = 14.dp,
        contentSpacing = 6.dp,
    ),

    /** Очень компактный размер. */
    XSMALL(
        height = 26.dp,
        horizontalPadding = 8.dp,
        iconSize = 12.dp,
        contentSpacing = 4.dp,
    ),
}

/**
 * Опция выпадающего списка для dropdown text field.
 *
 * @param value Бизнес-значение опции.
 * @param label Текст, отображаемый в поле и в меню.
 * @param enabled Доступность опции в меню.
 */
@Immutable
data class AdbDropdownOption<T>(
    val value: T,
    val label: String,
    val enabled: Boolean = true,
)

internal enum class AdbTextFieldStyle {
    FILLED,
    OUTLINED,
    PLAIN,
}

@Immutable
internal data class AdbTextFieldResolvedColors(
    val containerColor: Color,
    val borderColor: Color,
    val textColor: Color,
    val placeholderColor: Color,
    val iconColor: Color,
    val cursorColor: Color,
    val supportingColor: Color,
    val disabledContainerColor: Color,
    val disabledBorderColor: Color,
    val disabledTextColor: Color,
    val disabledPlaceholderColor: Color,
    val disabledIconColor: Color,
)

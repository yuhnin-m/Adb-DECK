package com.adbdeck.core.ui.menubuttons

import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.adbdeck.core.designsystem.AdbCornerRadius
import com.adbdeck.core.ui.buttons.AdbButtonSize
import com.adbdeck.core.ui.buttons.AdbOutlinedButton

/**
 * Пункт выпадающего меню кнопки.
 *
 * @param value Бизнес-значение опции.
 * @param label Отображаемый текст.
 * @param enabled Доступность опции в меню.
 */
@Immutable
data class AdbMenuButtonOption<T>(
    val value: T,
    val label: String,
    val enabled: Boolean = true,
)

/**
 * Кнопка с выпадающим меню на базе [AdbOutlinedButton].
 *
 * @param text Текст кнопки.
 * @param options Опции выпадающего меню.
 * @param onOptionSelected Callback выбора опции.
 * @param modifier Внешний [Modifier].
 * @param enabled Доступность кнопки.
 * @param size Размер кнопки.
 * @param cornerRadius Радиус скругления кнопки.
 * @param leadingIcon Иконка слева от текста.
 * @param trailingIcon Иконка справа от текста (по умолчанию стрелка вниз).
 * @param selectedOption Выбранное значение (для отметки в меню).
 * @param showSelectedCheckmark Показывать отметку у выбранного пункта.
 * @param contentDescription Текст для accessibility (актуально для icon-only).
 */
@Composable
fun <T> AdbOutlinedMenuButton(
    text: String,
    options: List<AdbMenuButtonOption<T>>,
    onOptionSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    size: AdbButtonSize = AdbButtonSize.MEDIUM,
    cornerRadius: AdbCornerRadius = AdbCornerRadius.MEDIUM,
    leadingIcon: ImageVector? = null,
    trailingIcon: ImageVector = Icons.Outlined.KeyboardArrowDown,
    selectedOption: T? = null,
    showSelectedCheckmark: Boolean = false,
    contentDescription: String? = null,
) {
    var expanded by remember { mutableStateOf(false) }
    val canOpenMenu = enabled && options.isNotEmpty()

    Box(modifier = modifier) {
        AdbOutlinedButton(
            onClick = { expanded = true },
            text = text,
            leadingIcon = leadingIcon,
            trailingIcon = trailingIcon,
            contentDescription = contentDescription,
            enabled = canOpenMenu,
            size = size,
            cornerRadius = cornerRadius,
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.label) },
                    enabled = option.enabled,
                    leadingIcon = if (showSelectedCheckmark && selectedOption == option.value) {
                        {
                            Icon(
                                imageVector = Icons.Outlined.Check,
                                contentDescription = null,
                            )
                        }
                    } else {
                        null
                    },
                    onClick = {
                        expanded = false
                        onOptionSelected(option.value)
                    },
                )
            }
        }
    }
}

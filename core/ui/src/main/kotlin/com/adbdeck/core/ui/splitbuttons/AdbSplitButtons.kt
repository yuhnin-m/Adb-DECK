package com.adbdeck.core.ui.splitbuttons

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.adbdeck.core.designsystem.AdbDeckTheme
import com.adbdeck.core.designsystem.Dimensions
import org.jetbrains.compose.ui.tooling.preview.Preview

/**
 * Универсальный split-button (основное действие + кнопка открытия меню).
 *
 * Подходит для сценариев "главное действие + дополнительные варианты",
 * например выбор уровня логирования через выпадающее меню.
 *
 * @param text Текст основной кнопки.
 * @param onPrimaryClick Callback основного действия (левая часть split-button).
 * @param menuItems Пункты выпадающего меню.
 * @param onMenuItemClick Callback выбора пункта меню.
 * @param modifier Modifier внешнего контейнера.
 * @param size Размер split-button.
 * @param enabled Общая доступность split-button.
 * @param menuEnabled Доступность кнопки открытия меню.
 * @param selectedMenuValue Текущее выбранное значение (для отметки в меню).
 * @param showSelectedCheckmark Показывать ли отметку у выбранного пункта.
 * @param colors Цветовая схема компонента.
 */
@Composable
fun <T> AdbSplitButton(
    text: String,
    onPrimaryClick: () -> Unit,
    menuItems: List<AdbSplitMenuItem<T>>,
    onMenuItemClick: (T) -> Unit,
    modifier: Modifier = Modifier,
    size: AdbSplitButtonSize = AdbSplitButtonSize.MEDIUM,
    enabled: Boolean = true,
    menuEnabled: Boolean = true,
    selectedMenuValue: T? = null,
    showSelectedCheckmark: Boolean = true,
    colors: AdbSplitButtonColors = AdbSplitButtonDefaults.colors(),
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        Row(
            horizontalArrangement = Arrangement.spacedBy((-1).dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SplitPrimaryPart(
                text = text,
                enabled = enabled,
                size = size,
                colors = colors,
                shape = RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp),
                onClick = onPrimaryClick,
            )

            SplitMenuPart(
                enabled = enabled && menuEnabled && menuItems.isNotEmpty(),
                size = size,
                colors = colors,
                shape = RoundedCornerShape(topEnd = 8.dp, bottomEnd = 8.dp),
                onClick = { menuExpanded = true },
            )
        }

        DropdownMenu(
            expanded = menuExpanded,
            onDismissRequest = { menuExpanded = false },
        ) {
            menuItems.forEach { item ->
                DropdownMenuItem(
                    text = { Text(item.label) },
                    enabled = item.enabled,
                    onClick = {
                        menuExpanded = false
                        onMenuItemClick(item.value)
                    },
                    leadingIcon = if (showSelectedCheckmark && item.value == selectedMenuValue) {
                        {
                            Icon(
                                imageVector = Icons.Outlined.Check,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    } else {
                        null
                    },
                )
            }
        }
    }
}

@Composable
private fun SplitPrimaryPart(
    text: String,
    enabled: Boolean,
    size: AdbSplitButtonSize,
    colors: AdbSplitButtonColors,
    shape: Shape,
    onClick: () -> Unit,
) {
    val containerColor = if (enabled) colors.containerColor else colors.disabledContainerColor
    val contentColor = if (enabled) colors.contentColor else colors.disabledContentColor

    Surface(
        shape = shape,
        color = containerColor,
        contentColor = contentColor,
        border = androidx.compose.foundation.BorderStroke(1.dp, colors.borderColor),
        modifier = Modifier
            .defaultMinSize(minHeight = size.height, minWidth = 64.dp)
            .clickable(enabled = enabled, onClick = onClick),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = size.horizontalPadding),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = text,
                style = splitTextStyle(size),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun SplitMenuPart(
    enabled: Boolean,
    size: AdbSplitButtonSize,
    colors: AdbSplitButtonColors,
    shape: Shape,
    onClick: () -> Unit,
) {
    val containerColor = if (enabled) colors.containerColor else colors.disabledContainerColor
    val contentColor = if (enabled) colors.contentColor else colors.disabledContentColor

    Surface(
        shape = shape,
        color = containerColor,
        contentColor = contentColor,
        border = androidx.compose.foundation.BorderStroke(1.dp, colors.borderColor),
        modifier = Modifier
            .defaultMinSize(minHeight = size.height, minWidth = size.menuPartWidth)
            .clickable(enabled = enabled, onClick = onClick),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(size.menuPartWidth, size.height),
        ) {
            Icon(
                imageVector = Icons.Outlined.KeyboardArrowDown,
                contentDescription = "Открыть меню",
                modifier = Modifier.size(size.iconSize),
            )
        }
    }
}

@Composable
private fun splitTextStyle(size: AdbSplitButtonSize): TextStyle {
    return when (size) {
        AdbSplitButtonSize.LARGE -> MaterialTheme.typography.labelLarge
        AdbSplitButtonSize.MEDIUM -> MaterialTheme.typography.labelMedium
        AdbSplitButtonSize.SMALL -> MaterialTheme.typography.labelSmall
        AdbSplitButtonSize.XSMALL -> MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp)
    }
}

@Composable
private fun SplitButtonsPreviewContent(isDarkTheme: Boolean) {
    AdbDeckTheme(isDarkTheme = isDarkTheme) {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(Dimensions.paddingMedium),
            ) {
                AdbSplitButton(
                    text = "Log level: All",
                    onPrimaryClick = {},
                    menuItems = listOf(
                        AdbSplitMenuItem("all", "All"),
                        AdbSplitMenuItem("v", "Verbose"),
                        AdbSplitMenuItem("d", "Debug"),
                        AdbSplitMenuItem("i", "Info"),
                        AdbSplitMenuItem("w", "Warning"),
                        AdbSplitMenuItem("e", "Error"),
                    ),
                    selectedMenuValue = "all",
                    onMenuItemClick = {},
                    size = AdbSplitButtonSize.LARGE,
                )

                AdbSplitButton(
                    text = "Mode: Compact",
                    onPrimaryClick = {},
                    menuItems = listOf(
                        AdbSplitMenuItem("compact", "Compact"),
                        AdbSplitMenuItem("full", "Full"),
                    ),
                    selectedMenuValue = "compact",
                    onMenuItemClick = {},
                    size = AdbSplitButtonSize.MEDIUM,
                )

                AdbSplitButton(
                    text = "Level: W",
                    onPrimaryClick = {},
                    menuItems = listOf(
                        AdbSplitMenuItem("all", "All"),
                        AdbSplitMenuItem("w", "Warning"),
                        AdbSplitMenuItem("e", "Error"),
                    ),
                    selectedMenuValue = "w",
                    onMenuItemClick = {},
                    size = AdbSplitButtonSize.SMALL,
                )

                AdbSplitButton(
                    text = "L: E",
                    onPrimaryClick = {},
                    menuItems = listOf(
                        AdbSplitMenuItem("all", "All"),
                        AdbSplitMenuItem("e", "Error"),
                    ),
                    selectedMenuValue = "e",
                    onMenuItemClick = {},
                    size = AdbSplitButtonSize.XSMALL,
                )
            }
        }
    }
}

@Preview
@Composable
private fun SplitButtonsLightPreview() {
    SplitButtonsPreviewContent(isDarkTheme = false)
}

@Preview
@Composable
private fun SplitButtonsDarkPreview() {
    SplitButtonsPreviewContent(isDarkTheme = true)
}

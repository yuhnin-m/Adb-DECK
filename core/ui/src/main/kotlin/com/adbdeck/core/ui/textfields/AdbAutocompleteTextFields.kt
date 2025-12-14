package com.adbdeck.core.ui.textfields

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import com.adbdeck.core.designsystem.AdbCornerRadius

/**
 * Редактируемое autocomplete-поле со стилем `filled`.
 *
 * Пользователь может как вводить текст вручную, так и выбирать
 * значение из выпадающих подсказок.
 *
 * @param value Текущее текстовое значение.
 * @param onValueChange Callback изменения текста.
 * @param suggestions Полный список подсказок.
 * @param onSuggestionSelected Callback выбора подсказки.
 * @param modifier Modifier контейнера.
 * @param placeholder Placeholder для пустого значения.
 * @param type Цветовой тип поля (`NEUTRAL/DANGER/SUCCESS`).
 * @param size Размер поля.
 * @param cornerRadius Радиус скругления.
 * @param enabled Доступность поля.
 * @param leadingIcon Иконка слева.
 * @param trailingIcon Иконка справа. Если `null`, используется стрелка меню.
 * @param onTrailingIconClick Обработчик клика по правой иконке.
 * @param supportingText Дополнительный текст под полем.
 * @param maxVisibleSuggestions Максимум видимых подсказок в меню.
 * @param menuMaxHeight Максимальная высота выпадающего меню.
 */
@Composable
fun AdbFilledAutocompleteTextField(
    value: String,
    onValueChange: (String) -> Unit,
    suggestions: List<String>,
    onSuggestionSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    type: AdbTextFieldType = AdbTextFieldType.NEUTRAL,
    size: AdbTextFieldSize = AdbTextFieldSize.MEDIUM,
    cornerRadius: AdbCornerRadius = AdbCornerRadius.MEDIUM,
    enabled: Boolean = true,
    leadingIcon: ImageVector? = null,
    trailingIcon: ImageVector? = null,
    onTrailingIconClick: (() -> Unit)? = null,
    supportingText: String? = null,
    maxVisibleSuggestions: Int = 20,
    menuMaxHeight: Dp = 280.dp,
) {
    AdbBaseAutocompleteTextField(
        style = AdbTextFieldStyle.FILLED,
        value = value,
        onValueChange = onValueChange,
        suggestions = suggestions,
        onSuggestionSelected = onSuggestionSelected,
        modifier = modifier,
        placeholder = placeholder,
        type = type,
        size = size,
        cornerRadius = cornerRadius,
        enabled = enabled,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        onTrailingIconClick = onTrailingIconClick,
        supportingText = supportingText,
        maxVisibleSuggestions = maxVisibleSuggestions,
        menuMaxHeight = menuMaxHeight,
    )
}

/**
 * Редактируемое autocomplete-поле со стилем `outlined`.
 *
 * API выровнен с [AdbFilledAutocompleteTextField] и [AdbPlainAutocompleteTextField].
 */
@Composable
fun AdbOutlinedAutocompleteTextField(
    value: String,
    onValueChange: (String) -> Unit,
    suggestions: List<String>,
    onSuggestionSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    type: AdbTextFieldType = AdbTextFieldType.NEUTRAL,
    size: AdbTextFieldSize = AdbTextFieldSize.MEDIUM,
    cornerRadius: AdbCornerRadius = AdbCornerRadius.MEDIUM,
    enabled: Boolean = true,
    leadingIcon: ImageVector? = null,
    trailingIcon: ImageVector? = null,
    onTrailingIconClick: (() -> Unit)? = null,
    supportingText: String? = null,
    maxVisibleSuggestions: Int = 20,
    menuMaxHeight: Dp = 280.dp,
) {
    AdbBaseAutocompleteTextField(
        style = AdbTextFieldStyle.OUTLINED,
        value = value,
        onValueChange = onValueChange,
        suggestions = suggestions,
        onSuggestionSelected = onSuggestionSelected,
        modifier = modifier,
        placeholder = placeholder,
        type = type,
        size = size,
        cornerRadius = cornerRadius,
        enabled = enabled,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        onTrailingIconClick = onTrailingIconClick,
        supportingText = supportingText,
        maxVisibleSuggestions = maxVisibleSuggestions,
        menuMaxHeight = menuMaxHeight,
    )
}

/**
 * Редактируемое autocomplete-поле со стилем `plain`.
 *
 * API выровнен с [AdbFilledAutocompleteTextField] и [AdbOutlinedAutocompleteTextField].
 */
@Composable
fun AdbPlainAutocompleteTextField(
    value: String,
    onValueChange: (String) -> Unit,
    suggestions: List<String>,
    onSuggestionSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    type: AdbTextFieldType = AdbTextFieldType.NEUTRAL,
    size: AdbTextFieldSize = AdbTextFieldSize.MEDIUM,
    cornerRadius: AdbCornerRadius = AdbCornerRadius.MEDIUM,
    enabled: Boolean = true,
    leadingIcon: ImageVector? = null,
    trailingIcon: ImageVector? = null,
    onTrailingIconClick: (() -> Unit)? = null,
    supportingText: String? = null,
    maxVisibleSuggestions: Int = 20,
    menuMaxHeight: Dp = 280.dp,
) {
    AdbBaseAutocompleteTextField(
        style = AdbTextFieldStyle.PLAIN,
        value = value,
        onValueChange = onValueChange,
        suggestions = suggestions,
        onSuggestionSelected = onSuggestionSelected,
        modifier = modifier,
        placeholder = placeholder,
        type = type,
        size = size,
        cornerRadius = cornerRadius,
        enabled = enabled,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        onTrailingIconClick = onTrailingIconClick,
        supportingText = supportingText,
        maxVisibleSuggestions = maxVisibleSuggestions,
        menuMaxHeight = menuMaxHeight,
    )
}

@Composable
private fun AdbBaseAutocompleteTextField(
    style: AdbTextFieldStyle,
    value: String,
    onValueChange: (String) -> Unit,
    suggestions: List<String>,
    onSuggestionSelected: (String) -> Unit,
    modifier: Modifier,
    placeholder: String?,
    type: AdbTextFieldType,
    size: AdbTextFieldSize,
    cornerRadius: AdbCornerRadius,
    enabled: Boolean,
    leadingIcon: ImageVector?,
    trailingIcon: ImageVector?,
    onTrailingIconClick: (() -> Unit)?,
    supportingText: String?,
    maxVisibleSuggestions: Int,
    menuMaxHeight: Dp,
) {
    var expanded by remember { mutableStateOf(false) }
    var anchorWidthPx by remember { mutableIntStateOf(0) }
    val suggestionsScrollState = rememberScrollState()
    val visibleLimit = maxVisibleSuggestions.coerceAtLeast(1)

    val normalizedSuggestions = remember(suggestions) {
        suggestions.asSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
            .toList()
    }

    val filteredSuggestions = remember(normalizedSuggestions, value, visibleLimit) {
        val query = value.trim()
        normalizedSuggestions
            .asSequence()
            .filter { query.isEmpty() || it.contains(query, ignoreCase = true) }
            .take(visibleLimit)
            .toList()
    }

    val fallbackTrailingIcon = if (expanded) {
        Icons.Outlined.KeyboardArrowUp
    } else {
        Icons.Outlined.KeyboardArrowDown
    }

    val resolvedTrailingIcon = trailingIcon ?: fallbackTrailingIcon
    val resolvedTrailingClick = onTrailingIconClick ?: {
        if (enabled) expanded = !expanded
    }

    LaunchedEffect(enabled, filteredSuggestions) {
        if (!enabled || filteredSuggestions.isEmpty()) {
            expanded = false
        }
    }

    val fieldModifier = Modifier
        .fillMaxWidth()
        .onGloballyPositioned { coordinates ->
            anchorWidthPx = coordinates.size.width
        }
        .onFocusChanged { focusState ->
            if (enabled && focusState.isFocused && filteredSuggestions.isNotEmpty()) {
                expanded = true
            }
        }

    val menuWidthDp = with(LocalDensity.current) { anchorWidthPx.toDp() }
    val menuHeight = remember(filteredSuggestions.size, menuMaxHeight) {
        (filteredSuggestions.size * 48).dp
            .coerceAtLeast(48.dp)
            .coerceAtMost(menuMaxHeight)
    }

    Box(modifier = modifier) {
        val onTextChanged: (String) -> Unit = { newValue ->
            onValueChange(newValue)
            if (enabled) {
                expanded = true
            }
        }

        when (style) {
            AdbTextFieldStyle.FILLED -> AdbFilledTextField(
                value = value,
                onValueChange = onTextChanged,
                modifier = fieldModifier,
                placeholder = placeholder,
                type = type,
                size = size,
                cornerRadius = cornerRadius,
                enabled = enabled,
                readOnly = false,
                leadingIcon = leadingIcon,
                trailingIcon = resolvedTrailingIcon,
                onTrailingIconClick = resolvedTrailingClick,
                supportingText = supportingText,
            )

            AdbTextFieldStyle.OUTLINED -> AdbOutlinedTextField(
                value = value,
                onValueChange = onTextChanged,
                modifier = fieldModifier,
                placeholder = placeholder,
                type = type,
                size = size,
                cornerRadius = cornerRadius,
                enabled = enabled,
                readOnly = false,
                leadingIcon = leadingIcon,
                trailingIcon = resolvedTrailingIcon,
                onTrailingIconClick = resolvedTrailingClick,
                supportingText = supportingText,
            )

            AdbTextFieldStyle.PLAIN -> AdbPlainTextField(
                value = value,
                onValueChange = onTextChanged,
                modifier = fieldModifier,
                placeholder = placeholder,
                type = type,
                size = size,
                cornerRadius = cornerRadius,
                enabled = enabled,
                readOnly = false,
                leadingIcon = leadingIcon,
                trailingIcon = resolvedTrailingIcon,
                onTrailingIconClick = resolvedTrailingClick,
                supportingText = supportingText,
            )
        }

        DropdownMenu(
            expanded = expanded && filteredSuggestions.isNotEmpty(),
            onDismissRequest = { expanded = false },
            modifier = Modifier.width(menuWidthDp),
            properties = PopupProperties(
                focusable = false,
            ),
        ) {
            Box(
                modifier = Modifier
                    .width(menuWidthDp)
                    .height(menuHeight),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight()
                        .padding(end = 8.dp)
                        .verticalScroll(suggestionsScrollState)
                        .align(Alignment.CenterStart),
                ) {
                    filteredSuggestions.forEach { suggestion ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = suggestion,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            },
                            onClick = {
                                expanded = false
                                onSuggestionSelected(suggestion)
                            },
                        )
                    }
                }

                VerticalScrollbar(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .height(menuHeight)
                        .width(8.dp),
                    adapter = rememberScrollbarAdapter(suggestionsScrollState),
                )
            }
        }
    }
}

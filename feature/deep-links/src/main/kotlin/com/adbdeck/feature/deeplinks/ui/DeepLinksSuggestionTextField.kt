package com.adbdeck.feature.deeplinks.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.focus.onFocusChanged
import com.adbdeck.core.ui.textfields.AdbOutlinedTextField
import com.adbdeck.feature.deeplinks.models.DeepLinksSuggestion

@Composable
internal fun DeepLinksSuggestionTextField(
    value: String,
    onValueChange: (String) -> Unit,
    suggestions: List<DeepLinksSuggestion>,
    onSuggestionSelected: (String) -> Unit = onValueChange,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    singleLine: Boolean = true,
    trailingIcon: ImageVector? = null,
    onTrailingIconClick: (() -> Unit)? = null,
    maxVisibleSuggestions: Int = 12,
) {
    var expanded by remember { mutableStateOf(false) }
    var anchorWidthPx by remember { mutableIntStateOf(0) }

    val normalizedSuggestions = remember(suggestions) {
        suggestions.asSequence()
            .map { suggestion ->
                suggestion.copy(
                    value = suggestion.value.trim(),
                    title = suggestion.title.trim(),
                    description = suggestion.description.trim(),
                )
            }
            .filter { suggestion -> suggestion.value.isNotEmpty() }
            .distinctBy { suggestion -> suggestion.value }
            .toList()
    }

    val visibleLimit = maxVisibleSuggestions.coerceAtLeast(1)
    val filteredSuggestions = remember(normalizedSuggestions, value, visibleLimit) {
        val query = value.trim()
        normalizedSuggestions.asSequence()
            .filter { suggestion ->
                query.isEmpty() ||
                    suggestion.value.contains(query, ignoreCase = true) ||
                    suggestion.title.contains(query, ignoreCase = true) ||
                    suggestion.description.contains(query, ignoreCase = true)
            }
            .take(visibleLimit)
            .toList()
    }

    LaunchedEffect(filteredSuggestions) {
        if (filteredSuggestions.isEmpty()) {
            expanded = false
        }
    }

    val menuWidthDp = with(LocalDensity.current) { anchorWidthPx.toDp() }

    Column(modifier = modifier) {
        AdbOutlinedTextField(
            value = value,
            onValueChange = { updated ->
                onValueChange(updated)
                if (filteredSuggestions.isNotEmpty()) {
                    expanded = true
                }
            },
            placeholder = placeholder,
            singleLine = singleLine,
            trailingIcon = trailingIcon,
            onTrailingIconClick = onTrailingIconClick,
            modifier = Modifier
                .fillMaxWidth()
                .onGloballyPositioned { coordinates ->
                    anchorWidthPx = coordinates.size.width
                }
                .onFocusChanged { focusState ->
                    if (focusState.isFocused && filteredSuggestions.isNotEmpty()) {
                        expanded = true
                    }
                },
        )

        DropdownMenu(
            expanded = expanded && filteredSuggestions.isNotEmpty(),
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .width(menuWidthDp)
                .heightIn(max = 320.dp),
            properties = PopupProperties(focusable = false),
        ) {
            filteredSuggestions.forEach { suggestion ->
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(
                                text = suggestion.title.ifBlank { suggestion.value },
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            if (suggestion.description.isNotBlank()) {
                                Text(
                                    text = suggestion.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    },
                    onClick = {
                        expanded = false
                        onSuggestionSelected(suggestion.value)
                    },
                )
            }
        }
    }
}

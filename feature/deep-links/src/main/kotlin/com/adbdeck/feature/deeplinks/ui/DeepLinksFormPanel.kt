package com.adbdeck.feature.deeplinks.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.adbdeck.core.adb.api.intents.ExtraType
import com.adbdeck.core.adb.api.intents.IntentExtra
import com.adbdeck.core.adb.api.intents.LaunchMode
import com.adbdeck.core.designsystem.Dimensions
import com.adbdeck.core.ui.buttons.AdbButtonSize
import com.adbdeck.core.ui.buttons.AdbFilledButton
import com.adbdeck.core.ui.buttons.AdbOutlinedButton
import com.adbdeck.core.ui.textfields.AdbOutlinedTextField
import com.adbdeck.core.ui.textfields.AdbTextFieldType
import com.adbdeck.feature.deeplinks.DeepLinksComponent
import com.adbdeck.feature.deeplinks.models.DeepLinksActionSuggestions
import com.adbdeck.feature.deeplinks.models.DeepLinksCategorySuggestions
import com.adbdeck.feature.deeplinks.models.DeepLinksFormUiState
import com.adbdeck.feature.deeplinks.models.DeepLinksSuggestion
import com.adbdeck.feature.deeplinks.models.packageSuggestionsToUi

@Composable
internal fun DeepLinksFormPanel(
    state: DeepLinksFormUiState,
    component: DeepLinksComponent,
    modifier: Modifier = Modifier,
) {
    val packageSuggestions = remember(state.packageSuggestions) {
        packageSuggestionsToUi(state.packageSuggestions)
    }

    Column(modifier = modifier) {
        TabRow(selectedTabIndex = if (state.mode == LaunchMode.DEEP_LINK) 0 else 1) {
            Tab(
                selected = state.mode == LaunchMode.DEEP_LINK,
                onClick = { component.onModeChanged(LaunchMode.DEEP_LINK) },
                text = { Text("Deep Link") },
            )
            Tab(
                selected = state.mode == LaunchMode.INTENT,
                onClick = { component.onModeChanged(LaunchMode.INTENT) },
                text = { Text("Intent") },
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            when (state.mode) {
                LaunchMode.DEEP_LINK -> DeepLinkFormFields(state, packageSuggestions, component)
                LaunchMode.INTENT -> IntentFormFields(state, packageSuggestions, component)
            }
        }

        HorizontalDivider()

        AdbFilledButton(
            onClick = { component.onLaunch() },
            text = if (state.isLaunching) "Запуск..." else "Запустить",
            leadingIcon = if (state.isLaunching) null else Icons.Outlined.PlayArrow,
            enabled = state.activeDeviceId != null && !state.isLaunching,
            loading = state.isLaunching,
            fullWidth = true,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        )
    }
}

@Composable
private fun DeepLinkFormFields(
    state: DeepLinksFormUiState,
    packageSuggestions: List<DeepLinksSuggestion>,
    component: DeepLinksComponent,
) {
    FormField(
        label = "URI *",
        value = state.dlUri,
        placeholder = "https://example.com/path?param=value",
        onValueChange = { component.onDlUriChanged(it) },
    )
    SuggestedFormField(
        label = "Action",
        value = state.dlAction,
        placeholder = "android.intent.action.VIEW",
        suggestions = DeepLinksActionSuggestions,
        onValueChange = { component.onDlActionChanged(it) },
    )
    SuggestedFormField(
        label = "Package (опционально)",
        value = state.dlPackage,
        placeholder = packageFieldPlaceholder(state),
        suggestions = packageSuggestions,
        onValueChange = { component.onDlPackageChanged(it) },
    )
    FormField(
        label = "Component (опционально)",
        value = state.dlComponent,
        placeholder = "com.example.myapp/.MainActivity",
        onValueChange = { component.onDlComponentChanged(it) },
    )
    SuggestedFormField(
        label = "Category (опционально)",
        value = state.dlCategory,
        placeholder = "android.intent.category.DEFAULT",
        suggestions = DeepLinksCategorySuggestions,
        onValueChange = { component.onDlCategoryChanged(it) },
    )
}

@Composable
private fun IntentFormFields(
    state: DeepLinksFormUiState,
    packageSuggestions: List<DeepLinksSuggestion>,
    component: DeepLinksComponent,
) {
    SuggestedFormField(
        label = "Action",
        value = state.itAction,
        placeholder = "android.intent.action.MAIN",
        suggestions = DeepLinksActionSuggestions,
        onValueChange = { component.onItActionChanged(it) },
    )
    FormField(
        label = "Data URI",
        value = state.itDataUri,
        placeholder = "content://... или https://...",
        onValueChange = { component.onItDataUriChanged(it) },
    )
    SuggestedFormField(
        label = "Package",
        value = state.itPackage,
        placeholder = packageFieldPlaceholder(state),
        suggestions = packageSuggestions,
        onValueChange = { component.onItPackageChanged(it) },
    )
    FormField(
        label = "Component",
        value = state.itComponent,
        placeholder = "com.example.myapp/.MainActivity",
        onValueChange = { component.onItComponentChanged(it) },
    )
    IntentFlagsField(state = state, component = component)

    HorizontalDivider()
    CategoriesEditor(
        categories = state.itCategories,
        suggestions = DeepLinksCategorySuggestions,
        onAdd = { component.onItCategoryAdd(it) },
        onRemove = { component.onItCategoryRemove(it) },
    )

    HorizontalDivider()
    ExtrasEditor(
        extras = state.itExtras,
        onAdd = { component.onItExtraAdd() },
        onRemove = { component.onItExtraRemove(it) },
        onKeyChanged = { i, k -> component.onItExtraKeyChanged(i, k) },
        onTypeChanged = { i, t -> component.onItExtraTypeChanged(i, t) },
        onValueChanged = { i, v -> component.onItExtraValueChanged(i, v) },
    )
}

@Composable
private fun IntentFlagsField(
    state: DeepLinksFormUiState,
    component: DeepLinksComponent,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Dimensions.paddingXSmall)) {
        Text(
            text = "Flags (hex/decimal)",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Dimensions.paddingSmall),
            verticalAlignment = Alignment.Top,
        ) {
            AdbOutlinedTextField(
                value = state.itFlags,
                onValueChange = { component.onItFlagsChanged(it) },
                placeholder = "0x10000000",
                modifier = Modifier.weight(1f),
                singleLine = true,
                type = if (state.itFlagsValidationMessage == null) {
                    AdbTextFieldType.NEUTRAL
                } else {
                    AdbTextFieldType.DANGER
                },
                supportingText = state.itFlagsValidationMessage ?: "Можно вводить hex (0x...) или decimal.",
                trailingIcon = if (state.itFlags.isNotBlank()) Icons.Outlined.Close else null,
                onTrailingIconClick = if (state.itFlags.isNotBlank()) {
                    { component.onItFlagsChanged("") }
                } else {
                    null
                },
            )
            AdbOutlinedButton(
                onClick = component::onShowIntentFlagsDialog,
                text = "Pick flags…",
                size = AdbButtonSize.SMALL,
            )
        }
    }
}

@Composable
private fun CategoriesEditor(
    categories: List<String>,
    suggestions: List<DeepLinksSuggestion>,
    onAdd: (String) -> Unit,
    onRemove: (Int) -> Unit,
) {
    var newCategory by remember { mutableStateOf("") }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = "Categories",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        categories.forEachIndexed { index, category ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = category,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.weight(1f),
                )
                IconButton(
                    onClick = { onRemove(index) },
                    modifier = Modifier.size(28.dp),
                ) {
                    Icon(Icons.Outlined.Close, contentDescription = "Удалить", modifier = Modifier.size(16.dp))
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            DeepLinksSuggestionTextField(
                value = newCategory,
                onValueChange = { newCategory = it },
                onSuggestionSelected = { selected ->
                    onAdd(selected)
                    newCategory = ""
                },
                suggestions = suggestions,
                placeholder = "android.intent.category.DEFAULT",
                modifier = Modifier.weight(1f),
                singleLine = true,
                trailingIcon = if (newCategory.isNotBlank()) Icons.Outlined.Close else null,
                onTrailingIconClick = if (newCategory.isNotBlank()) {
                    { newCategory = "" }
                } else {
                    null
                },
            )
            IconButton(
                onClick = {
                    if (newCategory.isNotBlank()) {
                        onAdd(newCategory.trim())
                        newCategory = ""
                    }
                },
            ) {
                Icon(Icons.Outlined.Add, contentDescription = "Добавить category")
            }
        }
    }
}

@Composable
private fun ExtrasEditor(
    extras: List<IntentExtra>,
    onAdd: () -> Unit,
    onRemove: (Int) -> Unit,
    onKeyChanged: (Int, String) -> Unit,
    onTypeChanged: (Int, ExtraType) -> Unit,
    onValueChanged: (Int, String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = "Extras",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        extras.forEachIndexed { index, extra ->
            ExtraRow(
                extra = extra,
                onRemove = { onRemove(index) },
                onKeyChanged = { onKeyChanged(index, it) },
                onTypeChanged = { onTypeChanged(index, it) },
                onValueChanged = { onValueChanged(index, it) },
            )
        }
        TextButton(
            onClick = onAdd,
            modifier = Modifier.align(Alignment.Start),
        ) {
            Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text("Добавить параметр", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun ExtraRow(
    extra: IntentExtra,
    onRemove: () -> Unit,
    onKeyChanged: (String) -> Unit,
    onTypeChanged: (ExtraType) -> Unit,
    onValueChanged: (String) -> Unit,
) {
    var typeMenuExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        AdbOutlinedTextField(
            value = extra.key,
            onValueChange = onKeyChanged,
            placeholder = "key",
            modifier = Modifier.weight(0.38f),
            singleLine = true,
            trailingIcon = if (extra.key.isNotBlank()) Icons.Outlined.Close else null,
            onTrailingIconClick = if (extra.key.isNotBlank()) {
                { onKeyChanged("") }
            } else {
                null
            },
        )

        Box(modifier = Modifier.weight(0.28f)) {
            TextButton(
                onClick = { typeMenuExpanded = true },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(extra.type.label, style = MaterialTheme.typography.bodySmall, maxLines = 1)
            }
            DropdownMenu(
                expanded = typeMenuExpanded,
                onDismissRequest = { typeMenuExpanded = false },
            ) {
                ExtraType.entries.forEach { type ->
                    DropdownMenuItem(
                        text = { Text(type.label) },
                        onClick = { typeMenuExpanded = false; onTypeChanged(type) },
                    )
                }
            }
        }

        AdbOutlinedTextField(
            value = extra.value,
            onValueChange = onValueChanged,
            placeholder = "value",
            modifier = Modifier.weight(0.38f),
            singleLine = true,
            trailingIcon = if (extra.value.isNotBlank()) Icons.Outlined.Close else null,
            onTrailingIconClick = if (extra.value.isNotBlank()) {
                { onValueChanged("") }
            } else {
                null
            },
        )

        IconButton(
            onClick = onRemove,
            modifier = Modifier.size(28.dp),
        ) {
            Icon(Icons.Outlined.Close, contentDescription = "Удалить extra", modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun SuggestedFormField(
    label: String,
    value: String,
    placeholder: String,
    suggestions: List<DeepLinksSuggestion>,
    onValueChange: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        DeepLinksSuggestionTextField(
            value = value,
            onValueChange = onValueChange,
            onSuggestionSelected = onValueChange,
            suggestions = suggestions,
            placeholder = placeholder,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            trailingIcon = if (value.isNotBlank()) Icons.Outlined.Close else null,
            onTrailingIconClick = if (value.isNotBlank()) {
                { onValueChange("") }
            } else {
                null
            },
        )
    }
}

private fun packageFieldPlaceholder(state: DeepLinksFormUiState): String = if (state.isPackageSuggestionsLoading) {
    "Загрузка пакетов..."
} else {
    "com.example.myapp"
}

@Composable
private fun FormField(
    label: String,
    value: String,
    placeholder: String,
    onValueChange: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        AdbOutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = placeholder,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            trailingIcon = if (value.isNotBlank()) Icons.Outlined.Close else null,
            onTrailingIconClick = if (value.isNotBlank()) {
                { onValueChange("") }
            } else {
                null
            },
        )
    }
}

package com.adbdeck.feature.deeplinks.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.adbdeck.core.designsystem.AdbCornerRadius
import com.adbdeck.core.designsystem.Dimensions
import com.adbdeck.core.ui.buttons.AdbButtonSize
import com.adbdeck.core.ui.buttons.AdbButtonType
import com.adbdeck.core.ui.buttons.AdbFilledButton
import com.adbdeck.core.ui.buttons.AdbOutlinedButton
import com.adbdeck.core.ui.buttons.AdbPlainButton
import com.adbdeck.core.ui.textfields.AdbOutlinedTextField
import com.adbdeck.feature.deeplinks.models.DefaultIntentFlagsSelection
import com.adbdeck.feature.deeplinks.models.FLAG_ACTIVITY_CLEAR_TASK
import com.adbdeck.feature.deeplinks.models.FLAG_ACTIVITY_CLEAR_TOP
import com.adbdeck.feature.deeplinks.models.FLAG_ACTIVITY_MULTIPLE_TASK
import com.adbdeck.feature.deeplinks.models.FLAG_ACTIVITY_NEW_TASK
import com.adbdeck.feature.deeplinks.models.FLAG_ACTIVITY_NO_HISTORY
import com.adbdeck.feature.deeplinks.models.IntentFlagBadge
import com.adbdeck.feature.deeplinks.models.IntentFlagDefinitions
import com.adbdeck.feature.deeplinks.models.IntentFlagDefinitionsByKey
import com.adbdeck.feature.deeplinks.models.IntentFlagDefinition
import com.adbdeck.feature.deeplinks.models.IntentFlagGroup
import com.adbdeck.feature.deeplinks.models.IntentFlagPreset
import com.adbdeck.feature.deeplinks.models.IntentFlagPresets
import com.adbdeck.feature.deeplinks.models.computeIntentFlagsMask
import com.adbdeck.feature.deeplinks.models.formatIntentFlagsMask

private data class IntentFlagsValidation(
    val blockingMessages: List<String> = emptyList(),
    val warningMessages: List<String> = emptyList(),
    val suggestAutoAddNewTask: Boolean = false,
)

private val SectionShape = RoundedCornerShape(AdbCornerRadius.MEDIUM.value)
private val BadgeShape = RoundedCornerShape(AdbCornerRadius.XLARGE.value)
private val FlagsListMaxHeight = 420.dp

@Composable
internal fun IntentFlagsDialog(
    initialSelectedFlagKeys: Set<String>,
    onApply: (Set<String>) -> Unit,
    onDismiss: () -> Unit,
) {
    var selectedFlagKeys by remember(initialSelectedFlagKeys) {
        mutableStateOf(initialSelectedFlagKeys.toSet())
    }
    var searchQuery by remember { mutableStateOf("") }
    var showAdvanced by remember { mutableStateOf(false) }
    var showRisky by remember { mutableStateOf(false) }
    var showUriPermissions by remember { mutableStateOf(false) }

    val currentMask = remember(selectedFlagKeys) {
        computeIntentFlagsMask(selectedFlagKeys)
    }
    val validation = remember(selectedFlagKeys) {
        validateIntentFlagsSelection(selectedFlagKeys)
    }
    val selectedPreset = remember(selectedFlagKeys) {
        IntentFlagPresets.firstOrNull { preset -> preset.flagKeys == selectedFlagKeys }
    }

    val commonFlags = remember(searchQuery) {
        filterFlagsByQuery(IntentFlagGroup.COMMON, searchQuery)
    }
    val advancedFlags = remember(searchQuery) {
        filterFlagsByQuery(IntentFlagGroup.ADVANCED, searchQuery)
    }
    val riskyFlags = remember(searchQuery) {
        filterFlagsByQuery(IntentFlagGroup.RISKY, searchQuery)
    }
    val uriFlags = remember(searchQuery) {
        filterFlagsByQuery(IntentFlagGroup.URI_PERMISSIONS, searchQuery)
    }
    val hasVisibleFlags = remember(commonFlags, advancedFlags, riskyFlags, uriFlags) {
        commonFlags.isNotEmpty() || advancedFlags.isNotEmpty() || riskyFlags.isNotEmpty() || uriFlags.isNotEmpty()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(Dimensions.paddingXSmall)) {
                Text(
                    text = "Intent flags",
                    style = MaterialTheme.typography.titleLarge,
                )
                Text(
                    text = "Выберите флаги Activity/URI для запуска intent.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Dimensions.paddingSmall)) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.32f),
                    shape = SectionShape,
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                horizontal = Dimensions.paddingMedium,
                                vertical = Dimensions.paddingSmall,
                            ),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Current mask: ${formatIntentFlagsMask(currentMask)}",
                            style = MaterialTheme.typography.labelLarge,
                            fontFamily = FontFamily.Monospace,
                        )
                        Text(
                            text = "Выбрано: ${selectedFlagKeys.size}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                AdbOutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = "Поиск по имени флага",
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = if (searchQuery.isNotBlank()) Icons.Outlined.Close else null,
                    onTrailingIconClick = if (searchQuery.isNotBlank()) {
                        { searchQuery = "" }
                    } else {
                        null
                    },
                )

                PresetsRow(
                    presets = IntentFlagPresets,
                    selectedPresetId = selectedPreset?.id,
                    selectedPresetDescription = selectedPreset?.description,
                    onPresetSelected = { preset -> selectedFlagKeys = preset.flagKeys.toSet() },
                )

                if (validation.suggestAutoAddNewTask) {
                    ValidationMessageCard(
                        message = "CLEAR_TASK без NEW_TASK. Для корректного поведения добавьте NEW_TASK.",
                        isBlocking = true,
                        actionText = "Авто-добавить",
                        onAction = { selectedFlagKeys = selectedFlagKeys + FLAG_ACTIVITY_NEW_TASK },
                    )
                }

                validation.blockingMessages.forEach { message ->
                    ValidationMessageCard(
                        message = message,
                        isBlocking = true,
                    )
                }

                validation.warningMessages.forEach { message ->
                    ValidationMessageCard(
                        message = message,
                        isBlocking = false,
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = FlagsListMaxHeight)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(Dimensions.paddingSmall),
                ) {
                    if (!hasVisibleFlags) {
                        Text(
                            text = "По запросу ничего не найдено.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    FlagsSection(
                        title = "Common (Safe)",
                        flags = commonFlags,
                        selectedFlagKeys = selectedFlagKeys,
                        onToggle = { key ->
                            selectedFlagKeys = selectedFlagKeys.toggleFlag(key)
                        },
                    )

                    CollapsibleFlagsSection(
                        title = "Advanced",
                        flags = advancedFlags,
                        expanded = showAdvanced,
                        onExpandedChange = { showAdvanced = it },
                        selectedFlagKeys = selectedFlagKeys,
                        onToggle = { key ->
                            selectedFlagKeys = selectedFlagKeys.toggleFlag(key)
                        },
                    )

                    CollapsibleFlagsSection(
                        title = "URI permissions (Advanced)",
                        flags = uriFlags,
                        expanded = showUriPermissions,
                        onExpandedChange = { showUriPermissions = it },
                        selectedFlagKeys = selectedFlagKeys,
                        onToggle = { key ->
                            selectedFlagKeys = selectedFlagKeys.toggleFlag(key)
                        },
                    )

                    CollapsibleFlagsSection(
                        title = "Risky",
                        flags = riskyFlags,
                        expanded = showRisky,
                        onExpandedChange = { showRisky = it },
                        selectedFlagKeys = selectedFlagKeys,
                        onToggle = { key ->
                            selectedFlagKeys = selectedFlagKeys.toggleFlag(key)
                        },
                        headerColor = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(Dimensions.paddingSmall)) {
                AdbPlainButton(
                    onClick = { selectedFlagKeys = DefaultIntentFlagsSelection },
                    text = "Reset",
                    size = AdbButtonSize.SMALL,
                )
                AdbOutlinedButton(
                    onClick = onDismiss,
                    text = "Cancel",
                    size = AdbButtonSize.SMALL,
                )
                AdbFilledButton(
                    onClick = { onApply(selectedFlagKeys) },
                    text = "Apply",
                    size = AdbButtonSize.SMALL,
                    enabled = validation.blockingMessages.isEmpty(),
                )
            }
        },
    )
}

@Composable
private fun ValidationMessageCard(
    message: String,
    isBlocking: Boolean,
    actionText: String? = null,
    onAction: (() -> Unit)? = null,
) {
    val containerColor = if (isBlocking) {
        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
    }
    val contentColor = if (isBlocking) {
        MaterialTheme.colorScheme.onErrorContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        color = containerColor,
        shape = SectionShape,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = Dimensions.paddingSmall,
                    vertical = Dimensions.paddingSmall,
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimensions.paddingSmall),
        ) {
            Icon(
                imageVector = Icons.Outlined.WarningAmber,
                contentDescription = null,
                tint = if (isBlocking) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary,
            )

            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = contentColor,
                modifier = Modifier.weight(1f),
            )

            if (!actionText.isNullOrBlank() && onAction != null) {
                AdbOutlinedButton(
                    onClick = onAction,
                    text = actionText,
                    type = if (isBlocking) AdbButtonType.DANGER else AdbButtonType.NEUTRAL,
                    size = AdbButtonSize.XSMALL,
                )
            }
        }
    }
}

@Composable
private fun PresetsRow(
    presets: List<IntentFlagPreset>,
    selectedPresetId: String?,
    selectedPresetDescription: String?,
    onPresetSelected: (IntentFlagPreset) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Dimensions.paddingXSmall)) {
        Text(
            text = "Presets",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        LazyRow(horizontalArrangement = Arrangement.spacedBy(Dimensions.paddingSmall)) {
            items(presets, key = { preset -> preset.id }) { preset ->
                val isSelected = selectedPresetId == preset.id
                if (isSelected) {
                    AdbFilledButton(
                        onClick = { onPresetSelected(preset) },
                        text = preset.title,
                        size = AdbButtonSize.SMALL,
                    )
                } else {
                    AdbOutlinedButton(
                        onClick = { onPresetSelected(preset) },
                        text = preset.title,
                        size = AdbButtonSize.SMALL,
                    )
                }
            }
        }

        if (!selectedPresetDescription.isNullOrBlank()) {
            Text(
                text = selectedPresetDescription,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun FlagsSection(
    title: String,
    flags: List<IntentFlagDefinition>,
    selectedFlagKeys: Set<String>,
    onToggle: (String) -> Unit,
) {
    if (flags.isEmpty()) return

    Column(verticalArrangement = Arrangement.spacedBy(Dimensions.paddingSmall)) {
        SectionHeader(
            title = title,
            count = flags.size,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        flags.forEach { flag ->
            IntentFlagRow(
                flag = flag,
                checked = selectedFlagKeys.contains(flag.key),
                onToggle = { onToggle(flag.key) },
            )
        }
    }
}

@Composable
private fun CollapsibleFlagsSection(
    title: String,
    flags: List<IntentFlagDefinition>,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    selectedFlagKeys: Set<String>,
    onToggle: (String) -> Unit,
    headerColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    if (flags.isEmpty()) return

    Column(verticalArrangement = Arrangement.spacedBy(Dimensions.paddingSmall)) {
        SectionHeader(
            title = title,
            count = flags.size,
            expanded = expanded,
            color = headerColor,
            onClick = { onExpandedChange(!expanded) },
        )

        if (expanded) {
            flags.forEach { flag ->
                IntentFlagRow(
                    flag = flag,
                    checked = selectedFlagKeys.contains(flag.key),
                    onToggle = { onToggle(flag.key) },
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    count: Int,
    color: Color,
    expanded: Boolean? = null,
    onClick: (() -> Unit)? = null,
) {
    val marker = when (expanded) {
        null -> ""
        true -> "▼ "
        false -> "▶ "
    }

    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f),
        shape = SectionShape,
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) {
                    Modifier.clickable(onClick = onClick)
                } else {
                    Modifier
                },
            ),
    ) {
        Text(
            text = "$marker$title ($count)",
            style = MaterialTheme.typography.labelMedium,
            color = color,
            modifier = Modifier.padding(
                horizontal = Dimensions.paddingSmall,
                vertical = Dimensions.paddingXSmall,
            ),
        )
    }
}

@Composable
private fun IntentFlagRow(
    flag: IntentFlagDefinition,
    checked: Boolean,
    onToggle: () -> Unit,
) {
    val isRisky = flag.badge == IntentFlagBadge.RISKY
    val containerColor = if (isRisky) {
        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.36f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.32f)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = containerColor,
                shape = SectionShape,
            )
            .clickable(onClick = onToggle)
            .padding(
                horizontal = Dimensions.paddingSmall,
                vertical = Dimensions.paddingSmall,
            ),
        verticalAlignment = Alignment.Top,
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = { onToggle() },
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(top = Dimensions.paddingXSmall),
            verticalArrangement = Arrangement.spacedBy(Dimensions.paddingXSmall),
        ) {
            Text(
                text = flag.label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                fontFamily = FontFamily.Monospace,
            )
            Text(
                text = flag.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(Modifier.width(Dimensions.paddingSmall))
        FlagBadge(flag.badge)
    }
}

@Composable
private fun FlagBadge(badge: IntentFlagBadge) {
    val (text, containerColor, contentColor) = when (badge) {
        IntentFlagBadge.SAFE -> Triple(
            "Safe",
            MaterialTheme.colorScheme.tertiaryContainer,
            MaterialTheme.colorScheme.onTertiaryContainer,
        )

        IntentFlagBadge.ADVANCED -> Triple(
            "Advanced",
            MaterialTheme.colorScheme.secondaryContainer,
            MaterialTheme.colorScheme.onSecondaryContainer,
        )

        IntentFlagBadge.RISKY -> Triple(
            "Risky",
            MaterialTheme.colorScheme.errorContainer,
            MaterialTheme.colorScheme.onErrorContainer,
        )
    }

    Surface(
        color = containerColor,
        shape = BadgeShape,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = contentColor,
            modifier = Modifier.padding(
                horizontal = Dimensions.paddingSmall,
                vertical = Dimensions.paddingXSmall,
            ),
        )
    }
}

private fun filterFlagsByQuery(group: IntentFlagGroup, query: String): List<IntentFlagDefinition> {
    val normalizedQuery = query.trim()
    return IntentFlagDefinitions
        .asSequence()
        .filter { definition -> definition.group == group }
        .filter { definition ->
            normalizedQuery.isEmpty() ||
                definition.label.contains(normalizedQuery, ignoreCase = true) ||
                definition.description.contains(normalizedQuery, ignoreCase = true)
        }
        .toList()
}

private fun Set<String>.toggleFlag(key: String): Set<String> = if (contains(key)) {
    this - key
} else {
    this + key
}

private fun validateIntentFlagsSelection(selectedFlagKeys: Set<String>): IntentFlagsValidation {
    val blocking = mutableListOf<String>()
    val warnings = mutableListOf<String>()

    val hasNewTask = selectedFlagKeys.contains(FLAG_ACTIVITY_NEW_TASK)
    val hasClearTask = selectedFlagKeys.contains(FLAG_ACTIVITY_CLEAR_TASK)
    val hasMultipleTask = selectedFlagKeys.contains(FLAG_ACTIVITY_MULTIPLE_TASK)
    val hasNoHistory = selectedFlagKeys.contains(FLAG_ACTIVITY_NO_HISTORY)
    val hasClearTop = selectedFlagKeys.contains(FLAG_ACTIVITY_CLEAR_TOP)

    val suggestAutoAddNewTask = hasClearTask && !hasNewTask

    if (hasClearTask && !hasNewTask) {
        blocking += "CLEAR_TASK имеет смысл только вместе с NEW_TASK."
    }
    if (hasMultipleTask && !hasNewTask) {
        warnings += "MULTIPLE_TASK обычно используют вместе с NEW_TASK."
    }
    if (hasNoHistory && hasClearTop) {
        warnings += "Комбинация NO_HISTORY + CLEAR_TOP может дать неожиданный back stack."
    }
    if (hasNewTask) {
        warnings += "NEW_TASK меняет поведение back stack: возврат может работать не как обычно."
    }
    if (selectedFlagKeys.any { key ->
            IntentFlagDefinitionsByKey[key]?.badge == IntentFlagBadge.RISKY
        }
    ) {
        warnings += "Выбраны рискованные флаги: они могут сильно изменить back stack и recents."
    }

    return IntentFlagsValidation(
        blockingMessages = blocking,
        warningMessages = warnings,
        suggestAutoAddNewTask = suggestAutoAddNewTask,
    )
}

package com.adbdeck.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ClearAll
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import com.adbdeck.core.designsystem.AdbDeckGreen
import com.adbdeck.core.designsystem.Dimensions
import com.adbdeck.core.process.ProcessHistoryEntry
import com.adbdeck.core.ui.EmptyView
import com.adbdeck.core.ui.buttons.AdbButtonSize
import com.adbdeck.core.ui.buttons.AdbButtonType
import com.adbdeck.core.ui.buttons.AdbPlainButton
import com.adbdeck.core.ui.selection.AdbMultiSelectionState
import com.adbdeck.core.ui.selection.clearSelection
import com.adbdeck.core.ui.selection.onItemSelectionRequested
import com.adbdeck.core.ui.selection.retainVisible
import com.adbdeck.core.ui.selection.selectAll
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Нижняя панель истории команд.
 *
 * Поведение выделения строк соответствует Logcat:
 * - click: одиночное выделение;
 * - Shift+click: диапазон;
 * - Ctrl/Cmd+click: добавить/убрать строку.
 */
@Composable
fun ProcessHistoryPanel(
    entries: List<ProcessHistoryEntry>,
    onClose: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val clipboard = LocalClipboardManager.current
    val visibleEntries = remember(entries) { entries.asReversed() }
    val visibleIds = remember(visibleEntries) { visibleEntries.map { it.id } }
    val listFocusRequester = remember { FocusRequester() }

    var selectionState by remember { mutableStateOf(AdbMultiSelectionState<Long>()) }
    var selectionModifiers by remember { mutableStateOf(HistorySelectionModifiers()) }

    LaunchedEffect(visibleIds) {
        selectionState = selectionState.retainVisible(visibleIds = visibleIds)
    }

    fun copySelected() {
        if (selectionState.selectedIds.isEmpty()) return
        val text = visibleEntries
            .asSequence()
            .filter { entry -> entry.id in selectionState.selectedIds }
            .map { entry -> entry.commandText }
            .joinToString(separator = "\n")
        if (text.isNotBlank()) {
            clipboard.setText(AnnotatedString(text))
        }
    }

    fun selectAll() {
        if (visibleIds.isEmpty()) return
        selectionState = selectionState.selectAll(visibleIds = visibleIds)
    }

    fun clearSelection() {
        selectionState = selectionState.clearSelection()
    }

    fun onSelectionRequested(
        entryId: Long,
        additiveSelection: Boolean,
        rangeSelection: Boolean,
    ) {
        selectionState = selectionState.onItemSelectionRequested(
            itemId = entryId,
            visibleIds = visibleIds,
            additiveSelection = additiveSelection,
            rangeSelection = rangeSelection,
        )
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Dimensions.paddingSmall, vertical = Dimensions.paddingXSmall),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimensions.paddingXSmall),
        ) {
            Text(
                text = "История команд",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "Выбрано: ${selectionState.selectedIds.size}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            AdbPlainButton(
                onClick = onClose,
                leadingIcon = Icons.Outlined.Close,
                size = AdbButtonSize.XSMALL,
                contentDescription = "Закрыть историю",
            )
        }

        HorizontalDivider()

        Row(
            modifier = Modifier
                .fillMaxSize(),
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .focusRequester(listFocusRequester)
                    .focusable()
                    .onPreviewKeyEvent { event ->
                        selectionModifiers = selectionModifiers.copy(
                            shiftPressed = event.isShiftPressed,
                            additivePressed = event.isCtrlPressed || event.isMetaPressed,
                        )

                        if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false

                        val primaryModifier = event.isCtrlPressed || event.isMetaPressed
                        when {
                            primaryModifier && event.key == Key.C -> {
                                copySelected()
                                true
                            }

                            primaryModifier && event.key == Key.A -> {
                                selectAll()
                                true
                            }

                            event.key == Key.Escape -> {
                                clearSelection()
                                true
                            }

                            else -> false
                        }
                    },
            ) {
                if (visibleEntries.isEmpty()) {
                    EmptyView(message = "История пуста.")
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        items(
                            items = visibleEntries,
                            key = { item -> item.id },
                        ) { entry ->
                            ProcessHistoryRow(
                                entry = entry,
                                selected = entry.id in selectionState.selectedIds,
                                onClick = {
                                    listFocusRequester.requestFocus()
                                    onSelectionRequested(
                                        entryId = entry.id,
                                        additiveSelection = selectionModifiers.additivePressed,
                                        rangeSelection = selectionModifiers.shiftPressed,
                                    )
                                },
                            )
                        }
                    }
                }
            }

            VerticalDivider()

            Column(
                modifier = Modifier
                    .width(Dimensions.paddingLarge + Dimensions.paddingSmall)
                    .fillMaxHeight()
                    .padding(vertical = Dimensions.paddingXSmall),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Dimensions.paddingXSmall),
            ) {
                AdbPlainButton(
                    onClick = ::copySelected,
                    leadingIcon = Icons.Outlined.ContentCopy,
                    enabled = selectionState.selectedIds.isNotEmpty(),
                    size = AdbButtonSize.SMALL,
                    contentDescription = "Копировать выбранные команды",
                )
                AdbPlainButton(
                    onClick = {
                        clearSelection()
                        onClear()
                    },
                    leadingIcon = Icons.Outlined.ClearAll,
                    type = AdbButtonType.DANGER,
                    enabled = visibleEntries.isNotEmpty(),
                    size = AdbButtonSize.SMALL,
                    contentDescription = "Очистить историю",
                )
            }
        }
    }
}

@Composable
private fun ProcessHistoryRow(
    entry: ProcessHistoryEntry,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val selectionColor = if (selected) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
    } else {
        MaterialTheme.colorScheme.surface
    }
    val resultText = entry.resultText()
    val resultColor = if (entry.isSuccess) {
        AdbDeckGreen
    } else {
        MaterialTheme.colorScheme.error
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(selectionColor)
            .clickable(onClick = onClick)
            .padding(horizontal = Dimensions.paddingSmall, vertical = Dimensions.paddingXSmall),
    ) {
        Text(
            text = "${entry.shortTime()}: ${entry.commandText}",
            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = Dimensions.paddingXSmall),
        )
        Text(
            text = resultText,
            style = MaterialTheme.typography.labelSmall,
            color = resultColor,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth(),
        )
    }
    HorizontalDivider(
        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.16f),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Dimensions.paddingSmall),
    )
}

private data class HistorySelectionModifiers(
    val additivePressed: Boolean = false,
    val shiftPressed: Boolean = false,
)

private fun ProcessHistoryEntry.shortTime(): String =
    TIME_FORMATTER.format(Instant.ofEpochMilli(timestampEpochMs).atZone(ZoneId.systemDefault()))

private fun ProcessHistoryEntry.resultText(): String =
    shortError?.takeIf { it.isNotBlank() }
        ?: "exit=${exitCode ?: "n/a"}"

private val TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")

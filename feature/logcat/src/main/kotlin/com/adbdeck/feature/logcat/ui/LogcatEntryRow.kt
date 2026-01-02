package com.adbdeck.feature.logcat.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import adbdeck.feature.logcat.generated.resources.Res
import adbdeck.feature.logcat.generated.resources.logcat_context_select_all_filtered
import com.adbdeck.core.adb.api.logcat.LogcatEntry
import com.adbdeck.core.adb.api.logcat.LogcatLevel
import com.adbdeck.core.designsystem.Dimensions
import com.adbdeck.core.i18n.AdbCommonStringRes
import com.adbdeck.feature.logcat.LogcatDisplayMode
import com.adbdeck.feature.logcat.LogcatFontFamily
import com.adbdeck.feature.logcat.LogcatState
import org.jetbrains.compose.resources.stringResource

// Вертикальные отступы строк лога — намеренно меньше стандартных для плотного вывода
private val COMPACT_ROW_VERTICAL_PADDING = 1.dp
private val FULL_ROW_VERTICAL_PADDING = 2.dp
private val FULL_ROW_HEADER_SPACING = 6.dp

/**
 * Строка лога с поддержкой выделения и контекстного меню действий.
 *
 * Контекстное меню открывается по правому клику (desktop) или long-press
 * и позволяет скопировать строку, скопировать текущее выделение
 * или выделить все отфильтрованные записи.
 */
@Composable
@OptIn(ExperimentalComposeUiApi::class)
internal fun LogEntryRow(
    entry: LogcatEntry,
    state: LogcatState,
    selected: Boolean,
    onClick: () -> Unit,
    hasAnySelection: Boolean,
    onCopyLine: () -> Unit,
    onCopySelected: () -> Unit,
    onSelectAll: () -> Unit,
) {
    var isContextMenuExpanded by remember(entry.id) { mutableStateOf(false) }
    val selectionColor = if (selected) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
    } else {
        Color.Transparent
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(selectionColor)
            .onPointerEvent(PointerEventType.Press) { event ->
                if (event.buttons.isSecondaryPressed) {
                    onClick()
                    isContextMenuExpanded = true
                }
            }
            .pointerInput(entry.id) {
                detectTapGestures(
                    onTap = { onClick() },
                    onLongPress = {
                        onClick()
                        isContextMenuExpanded = true
                    },
                )
            },
    ) {
        when (state.displayMode) {
            LogcatDisplayMode.COMPACT -> CompactRow(entry = entry, state = state)
            LogcatDisplayMode.FULL -> FullRow(entry = entry, state = state)
        }

        DropdownMenu(
            expanded = isContextMenuExpanded,
            onDismissRequest = { isContextMenuExpanded = false },
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(AdbCommonStringRes.actionCopyLine)) },
                onClick = {
                    isContextMenuExpanded = false
                    onCopyLine()
                },
            )
            if (hasAnySelection) {
                DropdownMenuItem(
                    text = { Text(stringResource(AdbCommonStringRes.actionCopySelected)) },
                    onClick = {
                        isContextMenuExpanded = false
                        onCopySelected()
                    },
                )
            }
            DropdownMenuItem(
                text = { Text(stringResource(Res.string.logcat_context_select_all_filtered)) },
                onClick = {
                    isContextMenuExpanded = false
                    onSelectAll()
                },
            )
        }
    }
}

// ── Режимы отображения строки ─────────────────────────────────────────────────

/**
 * Компактная строка: `[L] [timestamp] tag: message` — одна строка моноширинного текста.
 */
@Composable
private fun CompactRow(entry: LogcatEntry, state: LogcatState) {
    val color = if (state.coloredLevels) levelColor(entry.level) else MaterialTheme.colorScheme.onSurface
    val ts = remember(entry.id, state.showDate, state.showTime, state.showMillis) {
        buildTimestamp(entry, state)
    }
    val fontFamily = remember(state.fontFamily) { state.fontFamily.toComposeFontFamily() }

    Text(
        text = buildString {
            append(entry.level.code)
            append(' ')
            if (ts.isNotEmpty()) {
                append(ts)
                append(' ')
            }
            append(entry.tag)
            append(": ")
            append(entry.message)
        },
        style = MaterialTheme.typography.bodySmall.copy(
            fontFamily = fontFamily,
            fontSize = state.fontSizeSp.sp,
        ),
        color = color,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Dimensions.paddingSmall, vertical = COMPACT_ROW_VERTICAL_PADDING),
    )
}

/**
 * Полная строка: шапка с метаданными + тело сообщения.
 */
@Composable
private fun FullRow(entry: LogcatEntry, state: LogcatState) {
    val lColor = levelColor(entry.level)
    val textColor = if (state.coloredLevels) lColor else MaterialTheme.colorScheme.onSurface
    val ts = remember(entry.id, state.showDate, state.showTime, state.showMillis) {
        buildTimestamp(entry, state)
    }
    val metaText = buildString {
        if (ts.isNotEmpty()) {
            append(ts)
            append("  ")
        }
        if (entry.pid.isNotEmpty()) {
            append(entry.pid)
            append('/')
            append(entry.tid)
        }
    }

    val logFont = remember(state.fontFamily) { state.fontFamily.toComposeFontFamily() }
    val logFontSize = state.fontSizeSp.sp
    val metaFontSize = (state.fontSizeSp - 1).coerceAtLeast(8).sp

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Dimensions.paddingSmall, vertical = FULL_ROW_VERTICAL_PADDING),
    ) {
        // Шапка: timestamp PID/TID [L] tag
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(FULL_ROW_HEADER_SPACING),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (metaText.isNotEmpty()) {
                Text(
                    text = metaText,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = logFont,
                        fontSize = metaFontSize,
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
            Surface(
                color = if (state.coloredLevels) lColor.copy(alpha = 0.18f)
                else MaterialTheme.colorScheme.surfaceVariant,
                shape = MaterialTheme.shapes.extraSmall,
            ) {
                Text(
                    text = " ${entry.level.code} ",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = logFont,
                        fontSize = metaFontSize,
                    ),
                    color = if (state.coloredLevels) lColor else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = entry.tag,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = logFont,
                    fontSize = metaFontSize,
                ),
                color = textColor,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        // Тело сообщения
        Text(
            text = entry.message,
            style = MaterialTheme.typography.bodySmall.copy(
                fontFamily = logFont,
                fontSize = logFontSize,
            ),
            color = MaterialTheme.colorScheme.onSurface,
        )

        HorizontalDivider(
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f),
        )
    }
}

// ── Вспомогательные функции ───────────────────────────────────────────────────

/**
 * Возвращает цвет для каждого уровня лога.
 */
@Composable
private fun levelColor(level: LogcatLevel): Color = when (level) {
    LogcatLevel.VERBOSE -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
    LogcatLevel.DEBUG   -> MaterialTheme.colorScheme.onSurface
    LogcatLevel.INFO    -> Color(0xFF4CAF50)
    LogcatLevel.WARNING -> Color(0xFFFF9800)
    LogcatLevel.ERROR   -> Color(0xFFF44336)
    LogcatLevel.FATAL   -> Color(0xFFE91E63)
    LogcatLevel.SILENT  -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
    LogcatLevel.UNKNOWN -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
}

/**
 * Строит строку метки времени из полей записи в соответствии с флагами отображения.
 */
private fun buildTimestamp(entry: LogcatEntry, state: LogcatState): String = buildString {
    if (state.showDate && entry.date.isNotEmpty()) {
        append(entry.date)
        if (state.showTime) append(' ')
    }
    if (state.showTime && entry.time.isNotEmpty()) {
        append(entry.time)
        if (state.showMillis && entry.millis.isNotEmpty()) {
            append('.')
            append(entry.millis)
        }
    }
}

/**
 * Конвертирует [LogcatFontFamily] в Compose [FontFamily].
 */
private fun LogcatFontFamily.toComposeFontFamily(): FontFamily = when (this) {
    LogcatFontFamily.MONOSPACE  -> FontFamily.Monospace
    LogcatFontFamily.SANS_SERIF -> FontFamily.SansSerif
    LogcatFontFamily.SERIF      -> FontFamily.Serif
    LogcatFontFamily.DEFAULT    -> FontFamily.Default
}

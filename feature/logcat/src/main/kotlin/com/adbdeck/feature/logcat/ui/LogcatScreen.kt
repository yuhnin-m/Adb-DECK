package com.adbdeck.feature.logcat.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.adbdeck.feature.logcat.LogcatFontFamily
import com.adbdeck.core.adb.api.logcat.LogcatEntry
import com.adbdeck.core.adb.api.logcat.LogcatLevel
import com.adbdeck.core.designsystem.Dimensions
import com.adbdeck.feature.logcat.LogcatComponent
import com.adbdeck.feature.logcat.LogcatDisplayMode
import com.adbdeck.feature.logcat.LogcatState
import kotlinx.coroutines.launch

/**
 * Экран Logcat — потоковый просмотр `adb logcat` с фильтрами и настройками.
 *
 * Компоновка:
 * - [LogcatToolbar]   — управление потоком + переключение режимов отображения
 * - [FilterBar]       — текстовые фильтры + выбор минимального уровня лога
 * - [LogList]         — LazyColumn записей с автоскроллом и FAB «вниз»
 * - [LogcatStatusBar] — индикатор потока, активное устройство, счетчик строк
 *
 * @param component Компонент Logcat.
 */
@Composable
fun LogcatScreen(component: LogcatComponent) {
    val state by component.state.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        LogcatToolbar(state = state, component = component)
        HorizontalDivider()
        FilterBar(state = state, component = component)
        HorizontalDivider()
        LogList(
            state = state,
            component = component,
            modifier = Modifier.weight(1f),
        )
        HorizontalDivider()
        LogcatStatusBar(state = state)
    }
}

// ── Toolbar ──────────────────────────────────────────────────────────────────

/**
 * Панель управления: Start/Stop/Clear + переключатели режима отображения.
 */
@Composable
private fun LogcatToolbar(
    state: LogcatState,
    component: LogcatComponent,
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Dimensions.paddingSmall, vertical = Dimensions.paddingXSmall),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimensions.paddingXSmall),
        ) {
            // ── Start / Stop ───────────────────────────────────────
            if (!state.isRunning) {
                OutlinedButton(onClick = component::onStart) {
                    Icon(
                        imageVector = Icons.Outlined.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(Dimensions.paddingXSmall))
                    Text("Start", style = MaterialTheme.typography.labelMedium)
                }
            } else {
                OutlinedButton(onClick = component::onStop) {
                    Icon(
                        imageVector = Icons.Outlined.Stop,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.error,
                    )
                    Spacer(Modifier.width(Dimensions.paddingXSmall))
                    Text(
                        text = "Stop",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }

            // ── Clear ──────────────────────────────────────────────
            OutlinedButton(onClick = component::onClear) {
                Text("Clear", style = MaterialTheme.typography.labelMedium)
            }

            Spacer(Modifier.weight(1f))

            // ── Font size ──────────────────────────────────────────
            Text(
                text = "A",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            IconButton(
                onClick = { component.onFontSizeChanged(state.fontSizeSp - 1) },
                modifier = Modifier.size(24.dp),
                enabled = state.fontSizeSp > 8,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Remove,
                    contentDescription = "Уменьшить шрифт",
                    modifier = Modifier.size(14.dp),
                )
            }
            Text(
                text = "${state.fontSizeSp}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.width(20.dp),
            )
            IconButton(
                onClick = { component.onFontSizeChanged(state.fontSizeSp + 1) },
                modifier = Modifier.size(24.dp),
                enabled = state.fontSizeSp < 24,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Add,
                    contentDescription = "Увеличить шрифт",
                    modifier = Modifier.size(14.dp),
                )
            }
            Text(
                text = "A",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 14.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.width(Dimensions.paddingSmall))

            // ── Mode ───────────────────────────────────────────────
            Text(
                text = "Mode:",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            FilterChip(
                selected = state.displayMode == LogcatDisplayMode.COMPACT,
                onClick = { component.onDisplayModeChanged(LogcatDisplayMode.COMPACT) },
                label = { Text("Compact", style = MaterialTheme.typography.labelSmall) },
            )
            FilterChip(
                selected = state.displayMode == LogcatDisplayMode.FULL,
                onClick = { component.onDisplayModeChanged(LogcatDisplayMode.FULL) },
                label = { Text("Full", style = MaterialTheme.typography.labelSmall) },
            )

            Spacer(Modifier.width(Dimensions.paddingSmall))

            // ── Display toggles ────────────────────────────────────
            FilterChip(
                selected = state.showDate,
                onClick = component::onToggleShowDate,
                label = { Text("Date", style = MaterialTheme.typography.labelSmall) },
            )
            FilterChip(
                selected = state.showTime,
                onClick = component::onToggleShowTime,
                label = { Text("Time", style = MaterialTheme.typography.labelSmall) },
            )
            FilterChip(
                selected = state.showMillis,
                onClick = component::onToggleShowMillis,
                label = { Text("ms", style = MaterialTheme.typography.labelSmall) },
            )
            FilterChip(
                selected = state.coloredLevels,
                onClick = component::onToggleColoredLevels,
                label = { Text("Colors", style = MaterialTheme.typography.labelSmall) },
            )
        }
    }
}

// ── Filter bar ───────────────────────────────────────────────────────────────

/**
 * Панель фильтрации: текстовые поля + выбор минимального уровня лога.
 */
@Composable
private fun FilterBar(
    state: LogcatState,
    component: LogcatComponent,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Dimensions.paddingSmall, vertical = Dimensions.paddingXSmall),
        verticalArrangement = Arrangement.spacedBy(Dimensions.paddingXSmall),
    ) {
        // ── Text filters ───────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Dimensions.paddingSmall),
        ) {
            CompactTextField(
                value = state.searchQuery,
                onValueChange = component::onSearchChanged,
                placeholder = "Search…",
                modifier = Modifier.weight(1f),
            )
            CompactTextField(
                value = state.tagFilter,
                onValueChange = component::onTagFilterChanged,
                placeholder = "Tag filter",
                modifier = Modifier.weight(1f),
            )
            CompactTextField(
                value = state.packageFilter,
                onValueChange = component::onPackageFilterChanged,
                placeholder = "Package filter",
                modifier = Modifier.weight(1f),
            )
        }

        // ── Level filter ───────────────────────────────────────────
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimensions.paddingXSmall),
        ) {
            Text(
                text = "Level:",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            // "All" chip — deselects the level filter
            LevelChip(
                label = "All",
                selected = state.levelFilter == null,
                levelColor = MaterialTheme.colorScheme.onSurface,
                onClick = { component.onLevelFilterChanged(null) },
            )

            val levels = listOf(
                LogcatLevel.VERBOSE,
                LogcatLevel.DEBUG,
                LogcatLevel.INFO,
                LogcatLevel.WARNING,
                LogcatLevel.ERROR,
                LogcatLevel.FATAL,
            )
            levels.forEach { level ->
                LevelChip(
                    label = level.code.toString(),
                    selected = state.levelFilter == level,
                    levelColor = levelColor(level),
                    onClick = {
                        component.onLevelFilterChanged(
                            if (state.levelFilter == level) null else level
                        )
                    },
                )
            }
        }
    }
}

/**
 * Компактное текстовое поле для фильтров с кнопкой сброса.
 */
@Composable
private fun CompactTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        placeholder = {
            Text(
                text = placeholder,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
            )
        },
        trailingIcon = if (value.isNotEmpty()) {
            {
                IconButton(
                    onClick = { onValueChange("") },
                    modifier = Modifier.size(20.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "Clear",
                        modifier = Modifier.size(14.dp),
                    )
                }
            }
        } else null,
        singleLine = true,
        textStyle = MaterialTheme.typography.bodySmall,
    )
}

/**
 * Чип выбора уровня лога.
 *
 * @param label      Метка чипа (буква уровня или «All»).
 * @param selected   Выбран ли этот уровень.
 * @param levelColor Цвет уровня для подсветки метки.
 * @param onClick    Обработчик нажатия.
 */
@Composable
private fun LevelChip(
    label: String,
    selected: Boolean,
    levelColor: Color,
    onClick: () -> Unit,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = levelColor,
            )
        },
        leadingIcon = if (label != "All") {
            {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(levelColor, CircleShape),
                )
            }
        } else null,
    )
}

// ── Log list ─────────────────────────────────────────────────────────────────

/**
 * Область отображения записей лога с автоскроллом.
 *
 * Автоскролл:
 * - Включен → прокручивает вниз при каждом новом пакете строк.
 * - Пользователь прокрутил вверх → автоскролл выключается.
 * - Пользователь вернулся вниз → автоскролл снова включается.
 * - FAB «стрелка вниз» появляется, когда автоскролл отключен.
 */
@Composable
private fun LogList(
    state: LogcatState,
    component: LogcatComponent,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    val currentAutoScroll by rememberUpdatedState(state.autoScroll)
    val currentEntries by rememberUpdatedState(state.filteredEntries)

    // Пользователь прокрутил список до самого низа?
    val isAtBottom by remember {
        derivedStateOf {
            val info = listState.layoutInfo
            val lastVisible = info.visibleItemsInfo.lastOrNull()?.index ?: -1
            val total = info.totalItemsCount
            total == 0 || lastVisible >= total - 1
        }
    }

    // Прокрутка вниз при поступлении новых строк (totalLineCount растет монотонно)
    LaunchedEffect(state.totalLineCount) {
        if (currentAutoScroll && currentEntries.isNotEmpty()) {
            listState.scrollToItem(currentEntries.size - 1)
        }
    }

    // Пользователь прокрутил вверх → приостановить автоскролл
    LaunchedEffect(listState.isScrollInProgress) {
        if (listState.isScrollInProgress && !isAtBottom) {
            component.onAutoScrollChanged(false)
        }
    }

    // Пользователь вернулся к низу → возобновить автоскролл
    LaunchedEffect(isAtBottom) {
        if (isAtBottom && !currentAutoScroll) {
            component.onAutoScrollChanged(true)
        }
    }

    Box(modifier = modifier) {
        if (state.filteredEntries.isEmpty()) {
            EmptyLogState(state = state)
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
            ) {
                items(
                    items = state.filteredEntries,
                    key = { entry -> entry.id },
                ) { entry ->
                    LogEntryRow(entry = entry, state = state)
                }
            }
        }

        // FAB «прокрутить вниз» — показывается, когда автоскролл отключен
        if (!state.autoScroll && state.filteredEntries.isNotEmpty()) {
            SmallFloatingActionButton(
                onClick = {
                    component.onAutoScrollChanged(true)
                    scope.launch { listState.scrollToItem(state.filteredEntries.size - 1) }
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(Dimensions.paddingDefault),
            ) {
                Icon(
                    imageVector = Icons.Outlined.KeyboardArrowDown,
                    contentDescription = "Прокрутить вниз",
                )
            }
        }
    }
}

// ── Empty / error states ─────────────────────────────────────────────────────

@Composable
private fun EmptyLogState(state: LogcatState) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Dimensions.paddingSmall),
        ) {
            when {
                state.error != null -> {
                    Text(
                        text = "⚠  ${state.error}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }

                state.activeDeviceId == null -> {
                    Text(
                        text = "Устройство не выбрано",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline,
                    )
                    Text(
                        text = "Выберите устройство в верхней панели, затем нажмите Start",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline,
                    )
                }

                state.isRunning && state.entries.isEmpty() -> {
                    Text(
                        text = "Ожидание вывода logcat…",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline,
                    )
                }

                state.entries.isNotEmpty() -> {
                    // Есть данные, но фильтр ничего не пропустил
                    Text(
                        text = "Нет записей, соответствующих фильтру",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline,
                    )
                }

                else -> {
                    Text(
                        text = "Нажмите Start для начала захвата logcat",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline,
                    )
                }
            }
        }
    }
}

// ── Log entry rows ───────────────────────────────────────────────────────────

@Composable
private fun LogEntryRow(entry: LogcatEntry, state: LogcatState) {
    when (state.displayMode) {
        LogcatDisplayMode.COMPACT -> CompactRow(entry = entry, state = state)
        LogcatDisplayMode.FULL -> FullRow(entry = entry, state = state)
    }
}

/**
 * Компактная строка: `[L] [timestamp] tag: message` — одна строка моноширинного текста.
 */
@Composable
private fun CompactRow(entry: LogcatEntry, state: LogcatState) {
    val color = if (state.coloredLevels) levelColor(entry.level) else MaterialTheme.colorScheme.onSurface
    val ts = buildTimestamp(entry, state)

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
            fontFamily = state.fontFamily.toComposeFontFamily(),
            fontSize = state.fontSizeSp.sp,
        ),
        color = color,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Dimensions.paddingSmall, vertical = 1.dp),
    )
}

/**
 * Полная строка: шапка с метаданными + тело сообщения.
 */
@Composable
private fun FullRow(entry: LogcatEntry, state: LogcatState) {
    val lColor = levelColor(entry.level)
    val textColor = if (state.coloredLevels) lColor else MaterialTheme.colorScheme.onSurface
    val ts = buildTimestamp(entry, state)
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

    val logFont = state.fontFamily.toComposeFontFamily()
    val logFontSize = state.fontSizeSp.sp

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Dimensions.paddingSmall, vertical = 2.dp),
    ) {
        // Шапка: timestamp PID/TID [L] tag
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (metaText.isNotEmpty()) {
                Text(
                    text = metaText,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = logFont,
                        fontSize = (state.fontSizeSp - 1).coerceAtLeast(8).sp,
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
                        fontSize = (state.fontSizeSp - 1).coerceAtLeast(8).sp,
                    ),
                    color = if (state.coloredLevels) lColor else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = entry.tag,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = logFont,
                    fontSize = (state.fontSizeSp - 1).coerceAtLeast(8).sp,
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

// ── Status bar ───────────────────────────────────────────────────────────────

/**
 * Строка состояния потока logcat.
 *
 * Показывает: индикатор (зеленый/серый), статус, устройство, счетчик строк, ошибку.
 */
@Composable
private fun LogcatStatusBar(state: LogcatState) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(Dimensions.statusBarHeight)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(horizontal = Dimensions.paddingDefault),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimensions.paddingSmall),
    ) {
        // Индикатор состояния
        val dotColor = when {
            state.isRunning -> Color(0xFF4CAF50)
            state.error != null -> MaterialTheme.colorScheme.error
            else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)
        }
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(dotColor, CircleShape),
        )

        Text(
            text = when {
                state.isRunning -> "Streaming"
                state.error != null -> "Error"
                else -> "Stopped"
            },
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        state.activeDeviceId?.let { id ->
            Text("│", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
            Text(
                text = id,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Spacer(Modifier.weight(1f))

        if (!state.autoScroll) {
            Text(
                text = "↑ Пауза",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }

        if (state.totalLineCount > 0) {
            val hasFilter = state.levelFilter != null ||
                    state.searchQuery.isNotBlank() ||
                    state.tagFilter.isNotBlank() ||
                    state.packageFilter.isNotBlank()
            Text(
                text = if (hasFilter) {
                    "${state.filteredEntries.size} / ${state.totalLineCount} строк"
                } else {
                    "${state.totalLineCount} строк"
                },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (state.error != null) {
            Text(
                text = state.error,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
        }
    }
}

// ── Helpers ──────────────────────────────────────────────────────────────────

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
    LogcatFontFamily.MONOSPACE -> FontFamily.Monospace
    LogcatFontFamily.SANS_SERIF -> FontFamily.SansSerif
    LogcatFontFamily.SERIF -> FontFamily.Serif
    LogcatFontFamily.DEFAULT -> FontFamily.Default
}

/**
 * Возвращает цвет для каждого уровня лога.
 */
@Composable
private fun levelColor(level: LogcatLevel): Color = when (level) {
    LogcatLevel.VERBOSE -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
    LogcatLevel.DEBUG -> MaterialTheme.colorScheme.onSurface
    LogcatLevel.INFO -> Color(0xFF4CAF50)
    LogcatLevel.WARNING -> Color(0xFFFF9800)
    LogcatLevel.ERROR -> Color(0xFFF44336)
    LogcatLevel.FATAL -> Color(0xFFE91E63)
    LogcatLevel.SILENT -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
    LogcatLevel.UNKNOWN -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
}

package com.adbdeck.feature.logcat.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import adbdeck.feature.logcat.generated.resources.Res
import adbdeck.feature.logcat.generated.resources.*
import com.adbdeck.feature.logcat.LogcatFontFamily
import com.adbdeck.core.adb.api.logcat.LogcatEntry
import com.adbdeck.core.adb.api.logcat.LogcatLevel
import com.adbdeck.core.designsystem.AdbCornerRadius
import com.adbdeck.core.designsystem.Dimensions
import com.adbdeck.core.i18n.AdbCommonStringRes
import com.adbdeck.core.ui.buttons.AdbButtonSize
import com.adbdeck.core.ui.buttons.AdbButtonType
import com.adbdeck.core.ui.buttons.AdbFilledButton
import com.adbdeck.core.ui.buttons.AdbOutlinedButton
import com.adbdeck.core.ui.textfields.AdbOutlinedAutocompleteTextField
import com.adbdeck.core.ui.textfields.AdbOutlinedTextField
import com.adbdeck.core.ui.textfields.AdbTextFieldSize
import com.adbdeck.core.ui.textfields.AdbTextFieldType
import com.adbdeck.core.ui.splitbuttons.AdbSplitButton
import com.adbdeck.core.ui.splitbuttons.AdbSplitMenuItem
import com.adbdeck.core.ui.splitbuttons.AdbSplitButtonSize
import com.adbdeck.feature.logcat.LogcatComponent
import com.adbdeck.feature.logcat.LogcatDisplayMode
import com.adbdeck.feature.logcat.LogcatError
import com.adbdeck.feature.logcat.LogcatState
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

/**
 * Экран Logcat — потоковый просмотр `adb logcat` с фильтрами и панелью настроек.
 *
 * Компоновка:
 * - [LogcatToolbar] управление потоком, выбор уровня логирования, кнопка настроек;
 * - [LogcatSettingsPanel] дочерняя панель с параметрами отображения;
 * - [FilterBar] текстовые фильтры;
 * - [LogList] LazyColumn записей с автоскроллом и FAB «вниз»;
 * - [LogcatStatusBar] индикатор потока, активное устройство, счетчик строк.
 *
 * @param component Компонент Logcat.
 */
@Composable
fun LogcatScreen(component: LogcatComponent) {
    val state by component.state.collectAsState()
    var isSettingsOpen by remember { mutableStateOf(false) }
    var selectedEntryIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var selectionAnchorId by remember { mutableStateOf<Long?>(null) }
    val clipboard = LocalClipboardManager.current

    LaunchedEffect(state.filteredEntries) {
        val visibleIds = state.filteredEntries.asSequence().map { it.id }.toHashSet()
        selectedEntryIds = selectedEntryIds.filterTo(mutableSetOf()) { it in visibleIds }
        if (selectionAnchorId != null && selectionAnchorId !in visibleIds) {
            selectionAnchorId = null
        }
    }

    fun clearSelection() {
        selectedEntryIds = emptySet()
        selectionAnchorId = null
    }

    fun copySelectedEntries() {
        if (selectedEntryIds.isEmpty()) return
        val text = state.filteredEntries
            .asSequence()
            .filter { it.id in selectedEntryIds }
            .map { it.raw }
            .joinToString(separator = "\n")
        if (text.isNotBlank()) {
            clipboard.setText(AnnotatedString(text))
        }
    }

    fun copySingleEntry(raw: String) {
        if (raw.isBlank()) return
        clipboard.setText(AnnotatedString(raw))
    }

    fun selectAllFilteredEntries() {
        if (state.filteredEntries.isEmpty()) return
        selectedEntryIds = state.filteredEntries.map { it.id }.toSet()
        selectionAnchorId = state.filteredEntries.first().id
    }

    fun onEntrySelectionRequested(
        entryId: Long,
        addToSelection: Boolean,
        selectRange: Boolean,
    ) {
        val visibleIds = state.filteredEntries.map { it.id }
        if (visibleIds.isEmpty()) return

        if (selectRange && selectionAnchorId != null) {
            val anchorIndex = visibleIds.indexOf(selectionAnchorId)
            val targetIndex = visibleIds.indexOf(entryId)
            if (anchorIndex != -1 && targetIndex != -1) {
                val rangeIds = if (anchorIndex <= targetIndex) {
                    visibleIds.subList(anchorIndex, targetIndex + 1)
                } else {
                    visibleIds.subList(targetIndex, anchorIndex + 1)
                }
                selectedEntryIds = if (addToSelection) {
                    selectedEntryIds + rangeIds
                } else {
                    rangeIds.toSet()
                }
                return
            }
        }

        selectedEntryIds = when {
            addToSelection && entryId in selectedEntryIds -> selectedEntryIds - entryId
            addToSelection -> selectedEntryIds + entryId
            else -> setOf(entryId)
        }
        selectionAnchorId = entryId
    }

    Column(modifier = Modifier.fillMaxSize()) {
        LogcatToolbar(
            state = state,
            component = component,
            selectedCount = selectedEntryIds.size,
            onCopySelected = ::copySelectedEntries,
            onResetSelection = ::clearSelection,
            onClearAndResetSelection = {
                clearSelection()
                component.onClear()
            },
            isSettingsOpen = isSettingsOpen,
            onToggleSettings = { isSettingsOpen = !isSettingsOpen },
        )
        HorizontalDivider()

        Row(modifier = Modifier.weight(1f)) {
            Column(modifier = Modifier.weight(1f)) {
                FilterBar(state = state, component = component)
                HorizontalDivider()
                LogList(
                    state = state,
                    component = component,
                    selectedEntryIds = selectedEntryIds,
                    onEntrySelectionRequested = ::onEntrySelectionRequested,
                    onCopySelected = ::copySelectedEntries,
                    onCopyLine = ::copySingleEntry,
                    onSelectAll = ::selectAllFilteredEntries,
                    onClearSelection = ::clearSelection,
                    modifier = Modifier.weight(1f),
                )
            }

            if (isSettingsOpen) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(1.dp)
                        .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
                )

                LogcatSettingsPanel(
                    state = state,
                    component = component,
                    modifier = Modifier
                        .width(340.dp)
                        .fillMaxHeight(),
                )
            }
        }

        HorizontalDivider()
        LogcatStatusBar(state = state)
    }
}

// ── Toolbar ──────────────────────────────────────────────────────────────────

/**
 * Основная панель управления logcat.
 *
 * Содержит:
 * - запуск/остановку потока;
 * - очистку буфера;
 * - split-button выбора минимального уровня;
 * - кнопку открытия панели настроек.
 */
@Composable
private fun LogcatToolbar(
    state: LogcatState,
    component: LogcatComponent,
    selectedCount: Int,
    onCopySelected: () -> Unit,
    onResetSelection: () -> Unit,
    onClearAndResetSelection: () -> Unit,
    isSettingsOpen: Boolean,
    onToggleSettings: () -> Unit,
) {
    val levelMenuItems = rememberLogcatLevelMenuItems()
    val levelButtonText = stringResource(
        Res.string.logcat_toolbar_level_button,
        state.levelFilter?.code ?: stringResource(Res.string.logcat_level_all_short),
    )

    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Dimensions.paddingSmall, vertical = Dimensions.paddingDefault),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimensions.paddingXSmall),
        ) {
            // ── Start / Stop ───────────────────────────────────────
            if (!state.isRunning) {
                AdbFilledButton(
                    onClick = component::onStart,
                    text = stringResource(AdbCommonStringRes.actionStart),
                    type = AdbButtonType.NEUTRAL,
                    size = AdbButtonSize.MEDIUM,
                    cornerRadius = AdbCornerRadius.LARGE,
                    leadingIcon = Icons.Outlined.PlayArrow,
                )
            } else {
                AdbFilledButton(
                    onClick = component::onStop,
                    text = stringResource(AdbCommonStringRes.actionStop),
                    type = AdbButtonType.DANGER,
                    size = AdbButtonSize.MEDIUM,
                    cornerRadius = AdbCornerRadius.LARGE,
                    leadingIcon = Icons.Outlined.Stop,
                )
            }

            // ── Clear ──────────────────────────────────────────────
            AdbOutlinedButton(
                onClick = onClearAndResetSelection,
                text = stringResource(AdbCommonStringRes.actionClear),
                type = AdbButtonType.NEUTRAL,
                size = AdbButtonSize.MEDIUM,
                cornerRadius = AdbCornerRadius.LARGE,
            )

            if (selectedCount > 0) {
                AdbOutlinedButton(
                    onClick = onCopySelected,
                    text = stringResource(Res.string.logcat_toolbar_copy_selected, selectedCount),
                    type = AdbButtonType.SUCCESS,
                    size = AdbButtonSize.MEDIUM,
                    cornerRadius = AdbCornerRadius.LARGE,
                )

                AdbOutlinedButton(
                    onClick = onResetSelection,
                    text = stringResource(AdbCommonStringRes.actionClearSelection),
                    type = AdbButtonType.NEUTRAL,
                    size = AdbButtonSize.MEDIUM,
                    cornerRadius = AdbCornerRadius.LARGE,
                    leadingIcon = Icons.Outlined.Close,
                )
            }

            Spacer(Modifier.weight(1f))

            AdbSplitButton(
                text = levelButtonText,
                onPrimaryClick = { component.onLevelFilterChanged(null) },
                menuItems = levelMenuItems,
                selectedMenuValue = state.levelFilter,
                onMenuItemClick = { selected ->
                    component.onLevelFilterChanged(
                        if (selected != null && state.levelFilter == selected) null else selected
                    )
                },
                size = AdbSplitButtonSize.MEDIUM,
                cornerRadius = AdbCornerRadius.XLARGE
            )

            AdbOutlinedButton(
                onClick = onToggleSettings,
                text = if (isSettingsOpen) {
                    stringResource(AdbCommonStringRes.actionCloseSettings)
                } else {
                    stringResource(AdbCommonStringRes.actionSettings)
                },
                type = AdbButtonType.NEUTRAL,
                size = AdbButtonSize.MEDIUM,
                cornerRadius = AdbCornerRadius.LARGE,
                leadingIcon = Icons.Outlined.Settings,
            )
        }
    }
}

/**
 * Пункты меню выбора минимального уровня логирования.
 */
@Composable
private fun rememberLogcatLevelMenuItems(): List<AdbSplitMenuItem<LogcatLevel?>> {
    val allLabel = stringResource(Res.string.logcat_level_all_short)
    val verboseLabel = stringResource(Res.string.logcat_level_verbose)
    val debugLabel = stringResource(Res.string.logcat_level_debug)
    val infoLabel = stringResource(Res.string.logcat_level_info)
    val warningLabel = stringResource(Res.string.logcat_level_warning)
    val errorLabel = stringResource(Res.string.logcat_level_error)
    val fatalLabel = stringResource(Res.string.logcat_level_fatal)

    return remember(
        allLabel,
        verboseLabel,
        debugLabel,
        infoLabel,
        warningLabel,
        errorLabel,
        fatalLabel,
    ) {
        listOf(
            AdbSplitMenuItem(
                value = null,
                label = allLabel,
            ),
            AdbSplitMenuItem(
                value = LogcatLevel.VERBOSE,
                label = verboseLabel,
            ),
            AdbSplitMenuItem(
                value = LogcatLevel.DEBUG,
                label = debugLabel,
            ),
            AdbSplitMenuItem(
                value = LogcatLevel.INFO,
                label = infoLabel,
            ),
            AdbSplitMenuItem(
                value = LogcatLevel.WARNING,
                label = warningLabel,
            ),
            AdbSplitMenuItem(
                value = LogcatLevel.ERROR,
                label = errorLabel,
            ),
            AdbSplitMenuItem(
                value = LogcatLevel.FATAL,
                label = fatalLabel,
            ),
        )
    }
}

// ── Filter bar ───────────────────────────────────────────────────────────────

/**
 * Панель фильтрации: только текстовые фильтры.
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
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Dimensions.paddingSmall),
        ) {
            CompactTextField(
                value = state.searchQuery,
                onValueChange = component::onSearchChanged,
                placeholder = stringResource(AdbCommonStringRes.placeholderSearch),
                modifier = Modifier.weight(1f),
            )
            CompactTextField(
                value = state.tagFilter,
                onValueChange = component::onTagFilterChanged,
                placeholder = stringResource(Res.string.logcat_filter_tag_placeholder),
                modifier = Modifier.weight(1f),
            )
            CompactTextField(
                value = state.packageFilter,
                onValueChange = component::onPackageFilterChanged,
                placeholder = if (state.isPackageSuggestionsLoading) {
                    stringResource(Res.string.logcat_filter_package_loading_placeholder)
                } else {
                    stringResource(Res.string.logcat_filter_package_placeholder)
                },
                suggestions = state.packageSuggestions,
                modifier = Modifier.weight(1f),
            )
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
    suggestions: List<String> = emptyList(),
    modifier: Modifier = Modifier,
) {
    if (suggestions.isEmpty()) {
        AdbOutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = modifier,
            placeholder = placeholder,
            type = AdbTextFieldType.NEUTRAL,
            size = AdbTextFieldSize.MEDIUM,
            cornerRadius = AdbCornerRadius.LARGE,
            leadingIcon = null,
            trailingIcon = if (value.isNotEmpty()) Icons.Outlined.Close else null,
            onTrailingIconClick = if (value.isNotEmpty()) {
                { onValueChange("") }
            } else {
                null
            },
            singleLine = true,
        )
    } else {
        AdbOutlinedAutocompleteTextField(
            value = value,
            onValueChange = onValueChange,
            suggestions = suggestions,
            onSuggestionSelected = onValueChange,
            modifier = modifier,
            placeholder = placeholder,
            type = AdbTextFieldType.NEUTRAL,
            size = AdbTextFieldSize.MEDIUM,
            cornerRadius = AdbCornerRadius.LARGE,
            trailingIcon = if (value.isNotEmpty()) Icons.Outlined.Close else null,
            onTrailingIconClick = if (value.isNotEmpty()) {
                { onValueChange("") }
            } else {
                null
            },
            maxVisibleSuggestions = 40,
        )
    }
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
    selectedEntryIds: Set<Long>,
    onEntrySelectionRequested: (entryId: Long, addToSelection: Boolean, selectRange: Boolean) -> Unit,
    onCopySelected: () -> Unit,
    onCopyLine: (String) -> Unit,
    onSelectAll: () -> Unit,
    onClearSelection: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val listFocusRequester = remember { FocusRequester() }
    var selectionModifiers by remember { mutableStateOf(LogSelectionModifiers()) }

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

    Box(
        modifier = modifier
            .focusRequester(listFocusRequester)
            .focusable()
            .onPreviewKeyEvent { event ->
                selectionModifiers = selectionModifiers.copy(
                    shiftPressed = event.isShiftPressed,
                    additivePressed = event.isCtrlPressed || event.isMetaPressed,
                )

                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false

                val isPrimaryModifier = event.isCtrlPressed || event.isMetaPressed
                when {
                    isPrimaryModifier && event.key == Key.C -> {
                        onCopySelected()
                        true
                    }

                    isPrimaryModifier && event.key == Key.A -> {
                        onSelectAll()
                        true
                    }

                    event.key == Key.Escape -> {
                        onClearSelection()
                        true
                    }

                    else -> false
                }
            },
    ) {
        if (state.filteredEntries.isEmpty()) {
            EmptyLogState(state = state)
        } else {
            Box(modifier = Modifier.fillMaxSize()) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(end = 10.dp),
                ) {
                    items(
                        items = state.filteredEntries,
                        key = { entry -> entry.id },
                    ) { entry ->
                        LogEntryRow(
                            entry = entry,
                            state = state,
                            selected = entry.id in selectedEntryIds,
                            hasAnySelection = selectedEntryIds.isNotEmpty(),
                            onClick = {
                                listFocusRequester.requestFocus()
                                onEntrySelectionRequested(
                                    entry.id,
                                    selectionModifiers.additivePressed,
                                    selectionModifiers.shiftPressed,
                                )
                            },
                            onCopyLine = { onCopyLine(entry.raw) },
                            onCopySelected = onCopySelected,
                            onSelectAll = onSelectAll,
                        )
                    }
                }

                VerticalScrollbar(
                    adapter = rememberScrollbarAdapter(listState),
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .fillMaxHeight(),
                )
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
                    contentDescription = stringResource(Res.string.logcat_content_desc_scroll_down),
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
                    val errorMessage = localizeLogcatError(state.error)
                    Text(
                        text = stringResource(
                            Res.string.logcat_empty_error_format,
                            errorMessage,
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }

                state.activeDeviceId == null -> {
                    Text(
                        text = stringResource(Res.string.logcat_empty_no_device_title),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline,
                    )
                    Text(
                        text = stringResource(Res.string.logcat_empty_no_device_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline,
                    )
                }

                state.isRunning && state.entries.isEmpty() -> {
                    Text(
                        text = stringResource(Res.string.logcat_empty_waiting_stream),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline,
                    )
                }

                state.entries.isNotEmpty() -> {
                    // Есть данные, но фильтр ничего не пропустил
                    Text(
                        text = stringResource(Res.string.logcat_empty_no_results),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline,
                    )
                }

                else -> {
                    Text(
                        text = stringResource(Res.string.logcat_empty_start_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline,
                    )
                }
            }
        }
    }
}

// ── Helpers ──────────────────────────────────────────────────────────────────

@Composable
private fun localizeLogcatError(error: LogcatError?): String {
    if (error == null) return ""

    return when (error) {
        LogcatError.NoActiveDevice -> stringResource(Res.string.logcat_error_no_active_device)
        is LogcatError.DeviceUnavailable -> stringResource(
            Res.string.logcat_error_device_unavailable,
            error.deviceId,
            error.deviceStateRaw,
        )
        is LogcatError.StreamFailure -> {
            val details = error.details.orEmpty().trim()
            if (details.isNotEmpty()) {
                stringResource(Res.string.logcat_error_stream_with_reason, details)
            } else {
                stringResource(Res.string.logcat_error_stream_generic)
            }
        }
    }
}

private data class LogSelectionModifiers(
    val shiftPressed: Boolean = false,
    val additivePressed: Boolean = false,
)

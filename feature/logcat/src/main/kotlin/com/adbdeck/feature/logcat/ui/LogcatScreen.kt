package com.adbdeck.feature.logcat.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ClearAll
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.ImportExport
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material.icons.outlined.Tune
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import adbdeck.feature.logcat.generated.resources.Res
import adbdeck.feature.logcat.generated.resources.*
import androidx.compose.animation.core.snap
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.unit.Constraints
import com.adbdeck.core.adb.api.logcat.LogcatLevel
import com.adbdeck.core.designsystem.AdbCornerRadius
import com.adbdeck.core.designsystem.Dimensions
import com.adbdeck.core.i18n.AdbCommonStringRes
import com.adbdeck.core.ui.buttons.AdbButtonSize
import com.adbdeck.core.ui.buttons.AdbButtonType
import com.adbdeck.core.ui.buttons.AdbFilledButton
import com.adbdeck.core.ui.buttons.AdbOutlinedButton
import com.adbdeck.core.ui.filedialogs.HostFileDialogFilter
import com.adbdeck.core.ui.filedialogs.HostFileSelectionMode
import com.adbdeck.core.ui.filedialogs.OpenFileDialogConfig
import com.adbdeck.core.ui.filedialogs.SaveFileDialogConfig
import com.adbdeck.core.ui.filedialogs.SaveFileExtensionPolicy
import com.adbdeck.core.ui.filedialogs.showOpenFileDialog
import com.adbdeck.core.ui.filedialogs.showSaveFileDialog
import com.adbdeck.core.ui.menubuttons.AdbMenuButtonOption
import com.adbdeck.core.ui.menubuttons.AdbOutlinedMenuButton
import com.adbdeck.core.ui.selection.AdbMultiSelectionState
import com.adbdeck.core.ui.selection.clearSelection
import com.adbdeck.core.ui.selection.onItemSelectionRequested
import com.adbdeck.core.ui.selection.retainVisible
import com.adbdeck.core.ui.selection.selectAll
import com.adbdeck.core.ui.textfields.AdbOutlinedAutocompleteTextField
import com.adbdeck.core.ui.textfields.AdbOutlinedTextField
import com.adbdeck.core.ui.textfields.AdbTextFieldSize
import com.adbdeck.core.ui.textfields.AdbTextFieldType
import com.adbdeck.feature.logcat.LogcatComponent
import com.adbdeck.feature.logcat.LogcatError
import com.adbdeck.feature.logcat.LogcatState
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
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
    var selectionState by remember { mutableStateOf(AdbMultiSelectionState<Long>()) }
    val clipboard = LocalClipboardManager.current

    LaunchedEffect(state.filteredEntries) {
        selectionState = selectionState.retainVisible(
            visibleIds = state.filteredEntries.map { entry -> entry.id },
        )
    }

    fun clearSelection() {
        selectionState = selectionState.clearSelection()
    }

    fun copySelectedEntries() {
        if (selectionState.selectedIds.isEmpty()) return
        val text = state.filteredEntries
            .asSequence()
            .filter { it.id in selectionState.selectedIds }
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
        selectionState = selectionState.selectAll(
            visibleIds = state.filteredEntries.map { entry -> entry.id },
        )
    }

    fun onEntrySelectionRequested(
        entryId: Long,
        addToSelection: Boolean,
        selectRange: Boolean,
    ) {
        selectionState = selectionState.onItemSelectionRequested(
            itemId = entryId,
            visibleIds = state.filteredEntries.map { entry -> entry.id },
            additiveSelection = addToSelection,
            rangeSelection = selectRange,
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        LogcatToolbar(
            state = state,
            component = component,
            selectedCount = selectionState.selectedIds.size,
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
                    selectedEntryIds = selectionState.selectedIds,
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

private enum class LogcatTransferAction {
    IMPORT,
    EXPORT,
}

private enum class LogcatToolbarMeasureSlot {
    EXPANDED_PROBE,
    FINAL_ROW,
}

private val LOGCAT_EXPORT_FILE_TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HHmmss")
private val LOGCAT_TOOLBAR_COMPACT_MENU_WIDTH = 52.dp

// ── Toolbar ──────────────────────────────────────────────────────────────────

/**
 * Основная панель управления logcat.
 *
 * Содержит:
 * - запуск/остановку потока;
 * - импорт/экспорт логов;
 * - очистку буфера;
 * - выбор минимального уровня логирования;
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
    val startLabel = stringResource(AdbCommonStringRes.actionStart)
    val stopLabel = stringResource(AdbCommonStringRes.actionStop)
    val clearLabel = stringResource(AdbCommonStringRes.actionClear)
    val clearSelectionLabel = stringResource(AdbCommonStringRes.actionClearSelection)
    val settingsLabel = stringResource(AdbCommonStringRes.actionSettings)
    val closeSettingsLabel = stringResource(AdbCommonStringRes.actionCloseSettings)
    val levelButtonText = stringResource(
        Res.string.logcat_toolbar_level_button,
        state.levelFilter?.code ?: stringResource(Res.string.logcat_level_all_short),
    )
    val transferButtonLabel = stringResource(Res.string.logcat_toolbar_import_export)
    val transferImportLabel = stringResource(Res.string.logcat_toolbar_import)
    val transferExportLabel = stringResource(Res.string.logcat_toolbar_export)
    val importDialogTitle = stringResource(Res.string.logcat_import_dialog_title)
    val exportDialogTitle = stringResource(Res.string.logcat_export_dialog_title)
    val fileFilterDescription = stringResource(Res.string.logcat_file_filter_description)
    val closeFileLabel = stringResource(Res.string.logcat_toolbar_close_file)
    val transferOptions = remember(transferImportLabel, transferExportLabel) {
        listOf(
            AdbMenuButtonOption(
                value = LogcatTransferAction.IMPORT,
                label = transferImportLabel,
            ),
            AdbMenuButtonOption(
                value = LogcatTransferAction.EXPORT,
                label = transferExportLabel,
            ),
        )
    }

    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
    ) {
        SubcomposeLayout(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Dimensions.paddingSmall, vertical = Dimensions.paddingDefault),
        ) { constraints ->
            val expandedProbe = subcompose(LogcatToolbarMeasureSlot.EXPANDED_PROBE) {
                LogcatToolbarActionsRow(
                    compact = false,
                    withFlexibleSpacer = false,
                    state = state,
                    component = component,
                    selectedCount = selectedCount,
                    onCopySelected = onCopySelected,
                    onResetSelection = onResetSelection,
                    onClearAndResetSelection = onClearAndResetSelection,
                    isSettingsOpen = isSettingsOpen,
                    onToggleSettings = onToggleSettings,
                    levelMenuItems = levelMenuItems,
                    levelButtonText = levelButtonText,
                    transferButtonLabel = transferButtonLabel,
                    transferOptions = transferOptions,
                    importDialogTitle = importDialogTitle,
                    exportDialogTitle = exportDialogTitle,
                    fileFilterDescription = fileFilterDescription,
                    closeFileLabel = closeFileLabel,
                    startLabel = startLabel,
                    stopLabel = stopLabel,
                    clearLabel = clearLabel,
                    clearSelectionLabel = clearSelectionLabel,
                    settingsLabel = settingsLabel,
                    closeSettingsLabel = closeSettingsLabel,
                )
            }.map { measurable ->
                measurable.measure(Constraints())
            }

            val expandedRequiredWidth = expandedProbe.maxOfOrNull { it.width } ?: 0
            val useCompact = expandedRequiredWidth > constraints.maxWidth

            val finalRow = subcompose(LogcatToolbarMeasureSlot.FINAL_ROW) {
                LogcatToolbarActionsRow(
                    compact = useCompact,
                    withFlexibleSpacer = true,
                    state = state,
                    component = component,
                    selectedCount = selectedCount,
                    onCopySelected = onCopySelected,
                    onResetSelection = onResetSelection,
                    onClearAndResetSelection = onClearAndResetSelection,
                    isSettingsOpen = isSettingsOpen,
                    onToggleSettings = onToggleSettings,
                    levelMenuItems = levelMenuItems,
                    levelButtonText = levelButtonText,
                    transferButtonLabel = transferButtonLabel,
                    transferOptions = transferOptions,
                    importDialogTitle = importDialogTitle,
                    exportDialogTitle = exportDialogTitle,
                    fileFilterDescription = fileFilterDescription,
                    closeFileLabel = closeFileLabel,
                    startLabel = startLabel,
                    stopLabel = stopLabel,
                    clearLabel = clearLabel,
                    clearSelectionLabel = clearSelectionLabel,
                    settingsLabel = settingsLabel,
                    closeSettingsLabel = closeSettingsLabel,
                )
            }.map { measurable ->
                measurable.measure(constraints)
            }

            val layoutWidth = finalRow.maxOfOrNull { it.width } ?: constraints.minWidth
            val layoutHeight = finalRow.maxOfOrNull { it.height } ?: 0
            layout(layoutWidth, layoutHeight) {
                finalRow.forEach { placeable ->
                    placeable.placeRelative(0, 0)
                }
            }
        }
    }
}

/**
 * Пункты меню выбора минимального уровня логирования.
 */
@Composable
private fun rememberLogcatLevelMenuItems(): List<AdbMenuButtonOption<LogcatLevel?>> {
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
            AdbMenuButtonOption(
                value = null,
                label = allLabel,
            ),
            AdbMenuButtonOption(
                value = LogcatLevel.VERBOSE,
                label = verboseLabel,
            ),
            AdbMenuButtonOption(
                value = LogcatLevel.DEBUG,
                label = debugLabel,
            ),
            AdbMenuButtonOption(
                value = LogcatLevel.INFO,
                label = infoLabel,
            ),
            AdbMenuButtonOption(
                value = LogcatLevel.WARNING,
                label = warningLabel,
            ),
            AdbMenuButtonOption(
                value = LogcatLevel.ERROR,
                label = errorLabel,
            ),
            AdbMenuButtonOption(
                value = LogcatLevel.FATAL,
                label = fatalLabel,
            ),
        )
    }
}

@Composable
private fun LogcatToolbarActionsRow(
    compact: Boolean,
    withFlexibleSpacer: Boolean,
    state: LogcatState,
    component: LogcatComponent,
    selectedCount: Int,
    onCopySelected: () -> Unit,
    onResetSelection: () -> Unit,
    onClearAndResetSelection: () -> Unit,
    isSettingsOpen: Boolean,
    onToggleSettings: () -> Unit,
    levelMenuItems: List<AdbMenuButtonOption<LogcatLevel?>>,
    levelButtonText: String,
    transferButtonLabel: String,
    transferOptions: List<AdbMenuButtonOption<LogcatTransferAction>>,
    importDialogTitle: String,
    exportDialogTitle: String,
    fileFilterDescription: String,
    closeFileLabel: String,
    startLabel: String,
    stopLabel: String,
    clearLabel: String,
    clearSelectionLabel: String,
    settingsLabel: String,
    closeSettingsLabel: String,
) {
    val settingsButtonText = if (isSettingsOpen) closeSettingsLabel else settingsLabel
    val copySelectedLabel = stringResource(Res.string.logcat_toolbar_copy_selected, selectedCount)
    val compactMenuModifier = if (compact) Modifier.width(LOGCAT_TOOLBAR_COMPACT_MENU_WIDTH) else Modifier

    val primaryActionText = when {
        state.isFileMode -> closeFileLabel
        state.isRunning -> stopLabel
        else -> startLabel
    }
    val primaryActionIcon = when {
        state.isFileMode -> Icons.Outlined.Close
        state.isRunning -> Icons.Outlined.Stop
        else -> Icons.Outlined.PlayArrow
    }
    val primaryActionType = when {
        state.isRunning && !state.isFileMode -> AdbButtonType.DANGER
        else -> AdbButtonType.NEUTRAL
    }
    val primaryAction: () -> Unit = when {
        state.isFileMode -> component::onCloseImportedFile
        state.isRunning -> component::onStop
        else -> component::onStart
    }

    Row(
        modifier = if (withFlexibleSpacer) Modifier.fillMaxWidth() else Modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimensions.paddingXSmall),
    ) {
        AdbFilledButton(
            onClick = primaryAction,
            text = if (compact) null else primaryActionText,
            contentDescription = if (compact) primaryActionText else null,
            type = primaryActionType,
            size = AdbButtonSize.MEDIUM,
            cornerRadius = AdbCornerRadius.LARGE,
            leadingIcon = primaryActionIcon,
        )

        AdbOutlinedMenuButton(
            text = if (compact) "" else transferButtonLabel,
            options = transferOptions,
            onOptionSelected = { action ->
                when (action) {
                    LogcatTransferAction.IMPORT -> {
                        val selectedPath = showLogcatImportDialog(
                            title = importDialogTitle,
                            filterDescription = fileFilterDescription,
                        )
                        if (selectedPath != null) {
                            component.onImportFromFile(selectedPath)
                        }
                    }

                    LogcatTransferAction.EXPORT -> {
                        // По требованию: сначала стоп потока, затем выбор пути сохранения.
                        component.onStop()
                        val selectedPath = showLogcatExportDialog(
                            title = exportDialogTitle,
                            defaultFileName = defaultLogcatExportFileName(),
                            filterDescription = fileFilterDescription,
                        )
                        if (selectedPath != null) {
                            component.onExportToFile(selectedPath)
                        }
                    }
                }
            },
            contentDescription = if (compact) transferButtonLabel else null,
            size = AdbButtonSize.MEDIUM,
            cornerRadius = AdbCornerRadius.LARGE,
            leadingIcon = Icons.Outlined.ImportExport,
            modifier = compactMenuModifier,
        )

        AdbOutlinedButton(
            onClick = onClearAndResetSelection,
            text = if (compact) null else clearLabel,
            contentDescription = if (compact) clearLabel else null,
            type = AdbButtonType.NEUTRAL,
            size = AdbButtonSize.MEDIUM,
            cornerRadius = AdbCornerRadius.LARGE,
            leadingIcon = Icons.Outlined.ClearAll,
        )

        if (selectedCount > 0) {
            AdbOutlinedButton(
                onClick = onCopySelected,
                text = if (compact) null else copySelectedLabel,
                contentDescription = if (compact) copySelectedLabel else null,
                type = AdbButtonType.SUCCESS,
                size = AdbButtonSize.MEDIUM,
                cornerRadius = AdbCornerRadius.LARGE,
                leadingIcon = Icons.Outlined.ContentCopy,
            )

            AdbOutlinedButton(
                onClick = onResetSelection,
                text = if (compact) null else clearSelectionLabel,
                contentDescription = if (compact) clearSelectionLabel else null,
                type = AdbButtonType.NEUTRAL,
                size = AdbButtonSize.MEDIUM,
                cornerRadius = AdbCornerRadius.LARGE,
                leadingIcon = Icons.Outlined.Close,
            )
        }

        if (withFlexibleSpacer) {
            Spacer(Modifier.weight(1f))
        }

        AdbOutlinedMenuButton(
            text = if (compact) "" else levelButtonText,
            options = levelMenuItems,
            selectedOption = state.levelFilter,
            showSelectedCheckmark = true,
            onOptionSelected = { selected ->
                component.onLevelFilterChanged(
                    if (selected != null && state.levelFilter == selected) null else selected
                )
            },
            contentDescription = if (compact) levelButtonText else null,
            size = AdbButtonSize.MEDIUM,
            cornerRadius = AdbCornerRadius.XLARGE,
            leadingIcon = Icons.Outlined.Tune,
            modifier = compactMenuModifier,
        )

        AdbOutlinedButton(
            onClick = onToggleSettings,
            text = if (compact) null else settingsButtonText,
            contentDescription = if (compact) settingsButtonText else null,
            type = AdbButtonType.NEUTRAL,
            size = AdbButtonSize.MEDIUM,
            cornerRadius = AdbCornerRadius.LARGE,
            leadingIcon = Icons.Outlined.Settings,
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
                placeholder = if (state.isPackageSuggestionsLoading && !state.isFileMode) {
                    stringResource(Res.string.logcat_filter_package_loading_placeholder)
                } else {
                    stringResource(Res.string.logcat_filter_package_placeholder)
                },
                suggestions = if (state.isFileMode) emptyList() else state.packageSuggestions,
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
@OptIn(ExperimentalFoundationApi::class)
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
                        val itemAnimationModifier = if (state.smoothStreamAnimation) {
                            // Оставляем только перемещение без fade, чтобы избежать "моргания" viewport.
                            Modifier.animateItem(
                                fadeInSpec = snap(),
                                fadeOutSpec = snap(),
                            )
                        } else {
                            Modifier
                        }

                        LogEntryRow(
                            entry = entry,
                            state = state,
                            selected = entry.id in selectedEntryIds,
                            modifier = itemAnimationModifier,
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

                state.isFileMode -> {
                    Text(
                        text = stringResource(Res.string.logcat_empty_file_mode),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline,
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

private data class LogSelectionModifiers(
    val shiftPressed: Boolean = false,
    val additivePressed: Boolean = false,
)

/**
 * Диалог выбора лог-файла для импорта.
 */
private fun showLogcatImportDialog(
    title: String,
    filterDescription: String,
): String? = showOpenFileDialog(
    OpenFileDialogConfig(
        title = title,
        selectionMode = HostFileSelectionMode.FILES_ONLY,
        filters = listOf(
            HostFileDialogFilter(
                description = filterDescription,
                extensions = listOf("logcat", "txt", "log", "json"),
            ),
        ),
        isAcceptAllFileFilterUsed = true,
    ),
)

/**
 * Диалог выбора пути сохранения экспортируемого лог-файла.
 */
private fun showLogcatExportDialog(
    title: String,
    defaultFileName: String,
    filterDescription: String,
): String? = showSaveFileDialog(
    SaveFileDialogConfig(
        title = title,
        defaultFileName = defaultFileName,
        filters = listOf(
            HostFileDialogFilter(
                description = filterDescription,
                extensions = listOf("logcat", "txt", "log"),
            ),
        ),
        isAcceptAllFileFilterUsed = true,
        extensionPolicy = SaveFileExtensionPolicy.AppendIfMissing(defaultExtension = "logcat"),
    ),
)

/**
 * Имя файла экспорта по умолчанию в формате `adbdeck-logcat_yyyy-MM-dd_HHmmss.logcat`.
 */
private fun defaultLogcatExportFileName(): String {
    val timestamp = LocalDateTime.now().format(LOGCAT_EXPORT_FILE_TIME_FORMATTER)
    return "adbdeck-logcat_${timestamp}.logcat"
}

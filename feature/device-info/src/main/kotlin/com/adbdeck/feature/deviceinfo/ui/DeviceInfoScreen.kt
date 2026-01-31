package com.adbdeck.feature.deviceinfo.ui

import adbdeck.feature.device_info.generated.resources.Res
import adbdeck.feature.device_info.generated.resources.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.compose.ui.unit.dp
import com.adbdeck.core.designsystem.AdbCornerRadius
import com.adbdeck.core.designsystem.AdbTheme
import com.adbdeck.core.designsystem.Dimensions
import com.adbdeck.core.i18n.AdbCommonStringRes
import com.adbdeck.core.ui.AdbBanner
import com.adbdeck.core.ui.AdbBannerType
import com.adbdeck.core.ui.EmptyView
import com.adbdeck.core.ui.buttons.AdbButtonSize
import com.adbdeck.core.ui.buttons.AdbButtonType
import com.adbdeck.core.ui.buttons.AdbOutlinedButton
import com.adbdeck.core.ui.selection.AdbMultiSelectionState
import com.adbdeck.core.ui.selection.clearSelection
import com.adbdeck.core.ui.selection.onItemSelectionRequested
import com.adbdeck.core.ui.selection.retainVisible
import com.adbdeck.core.ui.selection.selectAll
import com.adbdeck.feature.deviceinfo.DeviceInfoComponent
import com.adbdeck.feature.deviceinfo.DeviceInfoRow
import com.adbdeck.feature.deviceinfo.DeviceInfoSection
import com.adbdeck.feature.deviceinfo.DeviceInfoSectionKind
import com.adbdeck.feature.deviceinfo.DeviceInfoSectionLoadState
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

/**
 * Экран подробной информации об устройстве и системе.
 */
@Composable
fun DeviceInfoScreen(component: DeviceInfoComponent) {
    val state by component.state.collectAsState()
    val clipboard = LocalClipboardManager.current

    var selectionState by remember { mutableStateOf(AdbMultiSelectionState<String>()) }
    var selectionModifiers by remember { mutableStateOf(DeviceInfoSelectionModifiers()) }

    val sectionTitles = rememberSectionTitles()
    val selectedRowIdsBySection = remember(selectionState.selectedIds) {
        groupSelectionBySection(selectionState.selectedIds)
    }
    val selectableRows = remember(state.sections) { flattenSelectableRows(state.sections) }
    val exportDialogTitle = stringResource(Res.string.device_info_export_dialog_title)
    val exportDialogFilter = stringResource(Res.string.device_info_export_dialog_filter)

    LaunchedEffect(selectableRows) {
        selectionState = selectionState.retainVisible(
            visibleIds = selectableRows.map { row -> row.id },
        )
    }

    fun copyText(value: String) {
        if (value.isBlank()) return
        clipboard.setText(AnnotatedString(value))
    }

    fun clearSelection() {
        selectionState = selectionState.clearSelection()
    }

    fun copySelectedRows() {
        val rows = selectableRows.filter { it.id in selectionState.selectedIds }
        if (rows.isEmpty()) return
        copyText(rows.joinToString(separator = "\n") { "${it.key}: ${it.value}" })
    }

    fun copySectionRows(section: DeviceInfoSectionKind) {
        val title = sectionTitles[section].orEmpty()
        val sectionRows = selectableRows
            .asSequence()
            .filter { it.section == section }
            .map { DeviceInfoRow(id = it.id, key = it.key, value = it.value) }
            .toList()
        val text = formatSectionForCopy(title = title, rows = sectionRows)
        copyText(text)
    }

    fun copyAllSections() {
        val text = state.sections.joinToString(separator = "\n\n") { section ->
            val title = sectionTitles[section.kind].orEmpty()
            val rows = (section.state as? DeviceInfoSectionLoadState.Success)?.rows.orEmpty()
            formatSectionForCopy(title = title, rows = rows)
        }.trim()

        copyText(text)
    }

    fun selectAllRows() {
        if (selectableRows.isEmpty()) return
        selectionState = selectionState.selectAll(
            visibleIds = selectableRows.map { row -> row.id },
        )
    }

    fun onRowSelectionRequested(
        rowId: String,
        addToSelection: Boolean,
        selectRange: Boolean,
    ) {
        selectionState = selectionState.onItemSelectionRequested(
            itemId = rowId,
            visibleIds = selectableRows.map { row -> row.id },
            additiveSelection = addToSelection,
            rangeSelection = selectRange,
        )
    }

    val hasAnyRows by remember(selectableRows) {
        derivedStateOf { selectableRows.isNotEmpty() }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            DeviceInfoToolbar(
                isDeviceAvailable = state.isDeviceAvailable,
                isRefreshing = state.isRefreshing,
                isExporting = state.isExportingJson,
                selectedCount = selectionState.selectedIds.size,
                hasAnyRows = hasAnyRows,
                lastUpdatedAtMillis = state.lastUpdatedAtMillis,
                onRefresh = component::onRefresh,
                onCopyAll = ::copyAllSections,
                onCopySelected = ::copySelectedRows,
                onClearSelection = ::clearSelection,
                onExportJson = {
                    val deviceId = state.activeDeviceId ?: return@DeviceInfoToolbar
                    val path = showSaveJsonDialog(
                        deviceId = deviceId,
                        dialogTitle = exportDialogTitle,
                        dialogFilterDescription = exportDialogFilter,
                    ) ?: return@DeviceInfoToolbar
                    component.onExportJson(path)
                },
            )

            HorizontalDivider()

            if (!state.isDeviceAvailable) {
                EmptyView(
                    message = stringResource(
                        Res.string.device_info_empty_no_device_message,
                        stringResource(Res.string.device_info_empty_no_device_title),
                        stringResource(Res.string.device_info_empty_no_device_subtitle),
                    ),
                )
            } else {
                val listState = rememberLazyListState()
                val scope = rememberCoroutineScope()
                val listFocusRequester = remember { FocusRequester() }

                Row(modifier = Modifier.weight(1f)) {
                    DeviceInfoMiniNavigation(
                        sections = state.sections,
                        sectionTitles = sectionTitles,
                        onSectionClick = { sectionIndex ->
                            scope.launch {
                                listState.animateScrollToItem(sectionIndex)
                            }
                        },
                    )

                    VerticalDivider(
                        modifier = Modifier.fillMaxHeight(),
                        color = AdbTheme.colorScheme.outline.copy(alpha = 0.2f),
                    )

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

                                val hasPrimaryModifier = event.isCtrlPressed || event.isMetaPressed
                                when {
                                    hasPrimaryModifier && event.key == Key.C -> {
                                        copySelectedRows()
                                        true
                                    }

                                    hasPrimaryModifier && event.key == Key.A -> {
                                        selectAllRows()
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
                        LazyColumn(
                            state = listState,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = Dimensions.paddingDefault, vertical = Dimensions.paddingSmall)
                                .padding(end = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(Dimensions.paddingDefault),
                        ) {
                            itemsIndexed(
                                items = state.sections,
                                key = { _, section -> section.kind.id },
                            ) { _, section ->
                                DeviceInfoSectionTable(
                                    section = section,
                                    title = sectionTitles[section.kind].orEmpty(),
                                    selectedRowIds = selectedRowIdsBySection[section.kind].orEmpty(),
                                    hasAnySelection = selectionState.selectedIds.isNotEmpty(),
                                    onRowClick = { rowId ->
                                        listFocusRequester.requestFocus()
                                        onRowSelectionRequested(
                                            rowId = rowId,
                                            addToSelection = selectionModifiers.additivePressed,
                                            selectRange = selectionModifiers.shiftPressed,
                                        )
                                    },
                                    onCopySelected = ::copySelectedRows,
                                    onCopyAll = ::copyAllSections,
                                    onCopySection = { copySectionRows(section.kind) },
                                    onCopyKey = ::copyText,
                                    onCopyValue = ::copyText,
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
            }
        }

        state.feedback?.let { feedback ->
            AdbBanner(
                message = feedback.message,
                type = if (feedback.isError) AdbBannerType.ERROR else AdbBannerType.SUCCESS,
                onDismiss = component::onDismissFeedback,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(Dimensions.paddingDefault),
            )
        }
    }
}

@Composable
private fun DeviceInfoToolbar(
    isDeviceAvailable: Boolean,
    isRefreshing: Boolean,
    isExporting: Boolean,
    selectedCount: Int,
    hasAnyRows: Boolean,
    lastUpdatedAtMillis: Long?,
    onRefresh: () -> Unit,
    onCopyAll: () -> Unit,
    onCopySelected: () -> Unit,
    onClearSelection: () -> Unit,
    onExportJson: () -> Unit,
) {
    Surface(color = AdbTheme.colorScheme.surface, tonalElevation = 1.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Dimensions.paddingSmall, vertical = Dimensions.paddingXSmall),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimensions.paddingSmall),
        ) {
            AdbOutlinedButton(
                onClick = onRefresh,
                enabled = isDeviceAvailable,
                loading = isRefreshing,
                text = stringResource(AdbCommonStringRes.actionRefresh),
                leadingIcon = Icons.Outlined.Refresh,
                type = AdbButtonType.NEUTRAL,
                size = AdbButtonSize.MEDIUM,
                cornerRadius = AdbCornerRadius.MEDIUM,
            )

            AdbOutlinedButton(
                onClick = onCopyAll,
                enabled = isDeviceAvailable && hasAnyRows,
                text = stringResource(Res.string.device_info_toolbar_copy_all),
                leadingIcon = Icons.Outlined.ContentCopy,
                type = AdbButtonType.SUCCESS,
                size = AdbButtonSize.MEDIUM,
                cornerRadius = AdbCornerRadius.MEDIUM,
            )

            if (selectedCount > 0) {
                AdbOutlinedButton(
                    onClick = onCopySelected,
                    text = stringResource(Res.string.device_info_context_copy_selected) + " ($selectedCount)",
                    type = AdbButtonType.SUCCESS,
                    size = AdbButtonSize.MEDIUM,
                    cornerRadius = AdbCornerRadius.MEDIUM,
                )

                AdbOutlinedButton(
                    onClick = onClearSelection,
                    text = stringResource(AdbCommonStringRes.actionClearSelection),
                    type = AdbButtonType.NEUTRAL,
                    size = AdbButtonSize.MEDIUM,
                    cornerRadius = AdbCornerRadius.MEDIUM,
                )
            }

            AdbOutlinedButton(
                onClick = onExportJson,
                enabled = isDeviceAvailable && hasAnyRows,
                loading = isExporting,
                text = stringResource(Res.string.device_info_toolbar_export_json),
                leadingIcon = Icons.Outlined.FileDownload,
                type = AdbButtonType.NEUTRAL,
                size = AdbButtonSize.MEDIUM,
                cornerRadius = AdbCornerRadius.MEDIUM,
            )

            Spacer(modifier = Modifier.weight(1f))

            val updatedAtText = if (lastUpdatedAtMillis != null) {
                stringResource(
                    Res.string.device_info_toolbar_last_updated,
                    formatTimestamp(lastUpdatedAtMillis),
                )
            } else {
                stringResource(Res.string.device_info_toolbar_never_updated)
            }

            Text(
                text = updatedAtText,
                style = MaterialTheme.typography.bodySmall,
                color = AdbTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun DeviceInfoMiniNavigation(
    sections: List<DeviceInfoSection>,
    sectionTitles: Map<DeviceInfoSectionKind, String>,
    onSectionClick: (index: Int) -> Unit,
) {
    Column(
        modifier = Modifier
            .width(176.dp)
            .fillMaxHeight()
            .background(AdbTheme.colorScheme.surface)
            .padding(Dimensions.paddingSmall),
        verticalArrangement = Arrangement.spacedBy(Dimensions.paddingXSmall),
    ) {
        Text(
            text = stringResource(Res.string.device_info_nav_title),
            style = MaterialTheme.typography.labelMedium,
            color = AdbTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = Dimensions.paddingSmall),
        )

        sections.forEachIndexed { index, section ->
            DeviceInfoMiniNavigationItem(
                section = section,
                title = sectionTitles[section.kind].orEmpty(),
                onClick = { onSectionClick(index) },
            )
        }
    }
}

@Composable
private fun rememberSectionTitles(): Map<DeviceInfoSectionKind, String> {
    val overviewTitle = stringResource(Res.string.device_info_section_overview)
    val buildTitle = stringResource(Res.string.device_info_section_build)
    val displayTitle = stringResource(Res.string.device_info_section_display)
    val cpuRamTitle = stringResource(Res.string.device_info_section_cpu_ram)
    val batteryTitle = stringResource(Res.string.device_info_section_battery)
    val networkTitle = stringResource(Res.string.device_info_section_network)
    val cellularTitle = stringResource(Res.string.device_info_section_cellular)
    val modemTitle = stringResource(Res.string.device_info_section_modem)
    val imsRcsTitle = stringResource(Res.string.device_info_section_ims_rcs)
    val storageTitle = stringResource(Res.string.device_info_section_storage)
    val securityTitle = stringResource(Res.string.device_info_section_security)
    val systemTitle = stringResource(Res.string.device_info_section_system)

    return remember(
        overviewTitle,
        buildTitle,
        displayTitle,
        cpuRamTitle,
        batteryTitle,
        networkTitle,
        cellularTitle,
        modemTitle,
        imsRcsTitle,
        storageTitle,
        securityTitle,
        systemTitle,
    ) {
        mapOf(
            DeviceInfoSectionKind.OVERVIEW to overviewTitle,
            DeviceInfoSectionKind.BUILD to buildTitle,
            DeviceInfoSectionKind.DISPLAY to displayTitle,
            DeviceInfoSectionKind.CPU_RAM to cpuRamTitle,
            DeviceInfoSectionKind.BATTERY to batteryTitle,
            DeviceInfoSectionKind.NETWORK to networkTitle,
            DeviceInfoSectionKind.CELLULAR to cellularTitle,
            DeviceInfoSectionKind.MODEM to modemTitle,
            DeviceInfoSectionKind.IMS_RCS to imsRcsTitle,
            DeviceInfoSectionKind.STORAGE to storageTitle,
            DeviceInfoSectionKind.SECURITY to securityTitle,
            DeviceInfoSectionKind.SYSTEM to systemTitle,
        )
    }
}

private fun flattenSelectableRows(sections: List<DeviceInfoSection>): List<SelectableRow> {
    return sections.flatMap { section ->
        val rows = (section.state as? DeviceInfoSectionLoadState.Success)?.rows.orEmpty()
        rows.map { row ->
            SelectableRow(
                id = row.id,
                section = section.kind,
                key = row.key,
                value = row.value,
            )
        }
    }
}

private fun formatSectionForCopy(
    title: String,
    rows: List<DeviceInfoRow>,
): String {
    if (rows.isEmpty()) return ""

    return buildString {
        append('[')
        append(title)
        append(']')
        append('\n')

        rows.forEachIndexed { index, row ->
            append(row.key)
            append(": ")
            append(row.value)
            if (index != rows.lastIndex) append('\n')
        }
    }
}

private fun formatTimestamp(epochMillis: Long): String {
    return Instant.ofEpochMilli(epochMillis)
        .atZone(ZoneId.systemDefault())
        .toLocalDateTime()
        .format(TIMESTAMP_FORMATTER)
}

private fun showSaveJsonDialog(
    deviceId: String,
    dialogTitle: String,
    dialogFilterDescription: String,
): String? {
    val safeDeviceId = deviceId.replace(Regex("[^a-zA-Z0-9._-]+"), "_")
    val chooser = JFileChooser(File(System.getProperty("user.home"))).apply {
        this.dialogTitle = dialogTitle
        fileFilter = FileNameExtensionFilter("$dialogFilterDescription (*.json)", "json")
        selectedFile = File("device-info-$safeDeviceId.json")
    }

    if (chooser.showSaveDialog(null) != JFileChooser.APPROVE_OPTION) return null
    return chooser.selectedFile?.absolutePath
}

private data class SelectableRow(
    val id: String,
    val section: DeviceInfoSectionKind,
    val key: String,
    val value: String,
)

private data class DeviceInfoSelectionModifiers(
    val shiftPressed: Boolean = false,
    val additivePressed: Boolean = false,
)

@Composable
private fun DeviceInfoMiniNavigationItem(
    section: DeviceInfoSection,
    title: String,
    onClick: () -> Unit,
) {
    val sectionColor = when (section.state) {
        DeviceInfoSectionLoadState.Loading -> AdbTheme.semanticColors.info
        is DeviceInfoSectionLoadState.Success -> AdbTheme.semanticColors.success
        is DeviceInfoSectionLoadState.Error -> AdbTheme.colorScheme.error
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = AdbTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                shape = RoundedCornerShape(AdbCornerRadius.MEDIUM.value),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = Dimensions.paddingSmall, vertical = Dimensions.paddingXSmall),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .width(6.dp)
                .height(18.dp)
                .background(
                    color = sectionColor,
                    shape = RoundedCornerShape(999.dp),
                )
        )

        Text(
            text = title,
            style = MaterialTheme.typography.bodySmall,
            color = AdbTheme.colorScheme.onSurface,
            modifier = Modifier
                .weight(1f)
                .padding(start = Dimensions.paddingSmall)
                .padding(vertical = 2.dp)
        )
    }
}

private fun groupSelectionBySection(selectedRowIds: Set<String>): Map<DeviceInfoSectionKind, Set<String>> {
    if (selectedRowIds.isEmpty()) return emptyMap()

    val bySectionId = DeviceInfoSectionKind.entries.associateBy { it.id }
    val grouped = mutableMapOf<DeviceInfoSectionKind, MutableSet<String>>()

    selectedRowIds.forEach { rowId ->
        val sectionId = rowId.substringBefore(':', missingDelimiterValue = "")
        val sectionKind = bySectionId[sectionId] ?: return@forEach
        grouped.getOrPut(sectionKind) { mutableSetOf() } += rowId
    }

    return grouped.mapValues { (_, value) -> value.toSet() }
}

private val TIMESTAMP_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

package com.adbdeck.feature.filesystem.ui

import adbdeck.feature.file_system.generated.resources.Res
import adbdeck.feature.file_system.generated.resources.*
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.adbdeck.core.adb.api.monitoring.storage.StorageCategory
import com.adbdeck.core.adb.api.monitoring.storage.StorageSummary
import com.adbdeck.core.ui.EmptyView
import com.adbdeck.core.ui.ErrorView
import com.adbdeck.core.ui.LoadingView
import com.adbdeck.core.utils.formatKb
import com.adbdeck.feature.filesystem.CleanupOption
import com.adbdeck.feature.filesystem.CleanupState
import com.adbdeck.feature.filesystem.CleanupStatus
import com.adbdeck.feature.filesystem.ContentAnalysis
import com.adbdeck.feature.filesystem.ContentAnalysisState
import com.adbdeck.feature.filesystem.FileSystemComponent
import com.adbdeck.feature.filesystem.FileSystemListState
import com.adbdeck.feature.filesystem.FileSystemPartitionItem
import org.jetbrains.compose.resources.stringResource

/**
 * Экран информации о файловых системах устройства.
 */
@Composable
fun FileSystemScreen(component: FileSystemComponent) {
    val state by component.state.collectAsState()
    val refreshContentDescription = stringResource(Res.string.file_system_toolbar_refresh_content_desc)
    val cleanupContentDescription = stringResource(Res.string.file_system_toolbar_cleanup_content_desc)
    val emptyNoDevice = stringResource(Res.string.file_system_empty_no_device)
    val emptyNoData = stringResource(Res.string.file_system_empty_no_data)

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            IconButton(onClick = component::onRefresh) {
                Icon(Icons.Outlined.Refresh, contentDescription = refreshContentDescription)
            }

            FilledIconButton(
                onClick = component::onOpenCleanup,
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                ),
            ) {
                Icon(
                    imageVector = Icons.Filled.CleaningServices,
                    contentDescription = cleanupContentDescription,
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }

        HorizontalDivider()

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) {
            when (val listState = state.listState) {
                is FileSystemListState.NoDevice -> EmptyView(message = emptyNoDevice)
                is FileSystemListState.Loading -> LoadingView()
                is FileSystemListState.Error -> ErrorView(message = listState.message, onRetry = component::onRefresh)
                is FileSystemListState.Success -> {
                    val relevant = listState.partitions.filter { it.partition.isRelevant }
                    if (relevant.isEmpty()) {
                        EmptyView(message = emptyNoData)
                    } else {
                        FileSystemContent(
                            partitions = relevant,
                            summary = listState.summary,
                            contentAnalysis = state.contentAnalysis,
                            onOpenPartition = component::onOpenPartition,
                        )
                    }
                }
            }
        }
    }

    if (state.cleanup.isDialogOpen) {
        CleanupDialog(
            cleanupState = state.cleanup,
            onDismiss = component::onDismissCleanup,
            onToggleOption = component::onToggleCleanupOption,
            onStartCleanup = component::onStartCleanup,
            onCancelCleanup = component::onCancelCleanup,
            onCopyLog = component::onCopyCleanupLog,
        )
    }

    if (state.cleanup.isConfirmDialogOpen) {
        CleanupConfirmDialog(
            onConfirm = component::onConfirmCleanup,
            onDismiss = component::onDismissCleanupConfirm,
        )
    }
}

@Composable
private fun FileSystemContent(
    partitions: List<FileSystemPartitionItem>,
    summary: StorageSummary,
    contentAnalysis: ContentAnalysisState,
    onOpenPartition: (String) -> Unit,
) {
    val (appSpecificPartitions, devicePartitionsRaw) = partitions.partition { isAppSpecificPartition(it.partition) }
    val devicePartitions = remember(devicePartitionsRaw) {
        devicePartitionsRaw.sortedWith(
            compareBy<FileSystemPartitionItem>({ it.partition.usedPercent }, { it.partition.mountPoint }),
        )
    }
    val appPartitions = remember(appSpecificPartitions) {
        appSpecificPartitions.sortedBy { it.partition.mountPoint }
    }

    val deviceSectionTitle = stringResource(Res.string.file_system_section_device_title)
    val appSectionTitle = stringResource(Res.string.file_system_section_app_title)
    val deviceSectionSubtitle = stringResource(
        Res.string.file_system_section_device_subtitle,
        devicePartitions.size,
    )
    val appSectionSubtitle = stringResource(
        Res.string.file_system_section_app_subtitle,
        appPartitions.size,
    )

    val listState = rememberLazyListState()

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(end = 10.dp),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item(key = "summary") {
                FileSystemSummaryCard(summary = summary)
            }

            item(key = "content_analysis") {
                ContentAnalysisCard(state = contentAnalysis)
            }

            if (devicePartitions.isNotEmpty()) {
                item(key = "device_section") {
                    FileSystemSectionHeader(
                        title = deviceSectionTitle,
                        subtitle = deviceSectionSubtitle,
                    )
                }
                items(
                    items = devicePartitions,
                    key = { it.partition.mountPoint + "|" + it.partition.filesystem },
                ) { item ->
                    FileSystemPartitionRow(
                        item = item,
                        onOpenPartition = onOpenPartition,
                    )
                }
            }

            if (appPartitions.isNotEmpty()) {
                item(key = "app_section") {
                    FileSystemSectionHeader(
                        title = appSectionTitle,
                        subtitle = appSectionSubtitle,
                    )
                }
                items(
                    items = appPartitions,
                    key = { it.partition.mountPoint + "|" + it.partition.filesystem },
                ) { item ->
                    FileSystemPartitionRow(
                        item = item,
                        onOpenPartition = onOpenPartition,
                    )
                }
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

@Composable
private fun ContentAnalysisCard(state: ContentAnalysisState) {
    val title = stringResource(Res.string.file_system_analysis_title)

    Surface(
        shape = RoundedCornerShape(12.dp),
        tonalElevation = 3.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            when (state) {
                is ContentAnalysisState.Idle,
                is ContentAnalysisState.Loading,
                -> {
                    Text(
                        text = stringResource(Res.string.file_system_analysis_loading),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                is ContentAnalysisState.Error -> {
                    Text(
                        text = stringResource(Res.string.file_system_analysis_error_title),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                    Text(
                        text = state.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                is ContentAnalysisState.Success -> {
                    ContentAnalysisRows(state.analysis)
                }
            }
        }
    }
}

@Composable
private fun ContentAnalysisRows(analysis: ContentAnalysis) {
    val rows = remember(analysis) {
        listOf(
            Pair(Res.string.file_system_analysis_app_size, analysis.appSizeKb),
            Pair(Res.string.file_system_analysis_app_data_size, analysis.appDataSizeKb),
            Pair(Res.string.file_system_analysis_app_cache_size, analysis.appCacheSizeKb),
            Pair(Res.string.file_system_analysis_photos_size, analysis.photosSizeKb),
            Pair(Res.string.file_system_analysis_videos_size, analysis.videosSizeKb),
            Pair(Res.string.file_system_analysis_audio_size, analysis.audioSizeKb),
            Pair(Res.string.file_system_analysis_downloads_size, analysis.downloadsSizeKb),
            Pair(Res.string.file_system_analysis_other_size, analysis.otherSizeKb),
            Pair(Res.string.file_system_analysis_system_size, analysis.systemSizeKb),
            Pair(Res.string.file_system_analysis_data_free, analysis.dataFreeKb),
            Pair(Res.string.file_system_analysis_data_total, analysis.dataTotalKb),
        ).filter { it.second != null }
    }

    if (rows.isEmpty()) {
        Text(
            text = stringResource(Res.string.file_system_analysis_empty),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        rows.forEach { (labelRes, valueKb) ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = stringResource(labelRes),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = valueKb?.formatKb().orEmpty(),
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.SemiBold,
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

@Composable
private fun FileSystemSectionHeader(
    title: String,
    subtitle: String,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun FileSystemSummaryCard(summary: StorageSummary) {
    val summaryTitle = stringResource(Res.string.file_system_summary_title)
    val statUsed = stringResource(Res.string.file_system_stat_used)
    val statFree = stringResource(Res.string.file_system_stat_free)
    val statTotal = stringResource(Res.string.file_system_stat_total)
    val statPercent = stringResource(Res.string.file_system_stat_percent)

    Surface(
        shape = RoundedCornerShape(12.dp),
        tonalElevation = 3.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = summaryTitle,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            LinearProgressIndicator(
                progress = { summary.usedPercent / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                color = progressColor(summary.usedPercent),
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                FileSystemStat(statUsed, summary.usedKb.formatKb(), MaterialTheme.colorScheme.primary)
                FileSystemStat(statFree, summary.freeKb.formatKb(), MaterialTheme.colorScheme.secondary)
                FileSystemStat(statTotal, summary.totalKb.formatKb(), MaterialTheme.colorScheme.onSurface)
                FileSystemStat(statPercent, "${summary.usedPercent}%", progressColor(summary.usedPercent))
            }
        }
    }
}

@Composable
private fun progressColor(usedPercent: Int) = when {
    usedPercent >= 90 -> MaterialTheme.colorScheme.error
    usedPercent >= 75 -> MaterialTheme.colorScheme.tertiary
    else -> MaterialTheme.colorScheme.primary
}

@Composable
private fun FileSystemStat(label: String, value: String, color: androidx.compose.ui.graphics.Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.SemiBold,
            ),
            color = color,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun FileSystemPartitionRow(
    item: FileSystemPartitionItem,
    onOpenPartition: (String) -> Unit,
) {
    val partition = item.partition

    val categorySystem = stringResource(Res.string.file_system_category_system)
    val categoryData = stringResource(Res.string.file_system_category_data)
    val categoryExternal = stringResource(Res.string.file_system_category_external)
    val categoryOther = stringResource(Res.string.file_system_category_other)
    val usedFormat = stringResource(Res.string.file_system_used_format, partition.usedKb.formatKb())
    val freeFormat = stringResource(Res.string.file_system_free_format, partition.freeKb.formatKb())
    val totalFormat = stringResource(Res.string.file_system_total_format, partition.totalKb.formatKb())
    val openLabel = stringResource(Res.string.file_system_partition_open)

    val categoryColor = when (partition.category) {
        StorageCategory.SYSTEM -> MaterialTheme.colorScheme.secondary
        StorageCategory.DATA -> MaterialTheme.colorScheme.primary
        StorageCategory.EXTERNAL -> MaterialTheme.colorScheme.tertiary
        StorageCategory.OTHER -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val categoryLabel = when (partition.category) {
        StorageCategory.SYSTEM -> categorySystem
        StorageCategory.DATA -> categoryData
        StorageCategory.EXTERNAL -> categoryExternal
        StorageCategory.OTHER -> categoryOther
    }

    Surface(
        shape = RoundedCornerShape(8.dp),
        tonalElevation = 1.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = partition.mountPoint,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Medium,
                    ),
                    modifier = Modifier.weight(1f),
                )

                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = categoryColor.copy(alpha = 0.15f),
                ) {
                    Text(
                        text = categoryLabel,
                        style = MaterialTheme.typography.labelSmall.copy(color = categoryColor),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                }

                Spacer(Modifier.width(8.dp))

                TextButton(
                    onClick = {
                        item.openPath?.let(onOpenPartition)
                    },
                    enabled = item.openPath != null,
                ) {
                    Text(text = openLabel)
                }
            }

            Text(
                text = partition.filesystem,
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp,
            )

            LinearProgressIndicator(
                progress = { partition.usedPercent / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp),
                color = progressColor(partition.usedPercent),
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = usedFormat,
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = freeFormat,
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = totalFormat,
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "${partition.usedPercent}%",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.SemiBold,
                    ),
                    color = progressColor(partition.usedPercent),
                )
            }
        }
    }
}

@Composable
private fun CleanupDialog(
    cleanupState: CleanupState,
    onDismiss: () -> Unit,
    onToggleOption: (CleanupOption) -> Unit,
    onStartCleanup: () -> Unit,
    onCancelCleanup: () -> Unit,
    onCopyLog: () -> Unit,
) {
    val title = stringResource(Res.string.file_system_cleanup_title)
    val subtitle = stringResource(Res.string.file_system_cleanup_subtitle)
    val cleanSelected = stringResource(Res.string.file_system_cleanup_clean_selected)
    val cancel = stringResource(Res.string.file_system_cleanup_cancel)
    val close = stringResource(Res.string.file_system_cleanup_close)
    val copy = stringResource(Res.string.file_system_cleanup_copy)
    val logTitle = stringResource(Res.string.file_system_cleanup_log_title)
    val logEmpty = stringResource(Res.string.file_system_cleanup_log_empty)

    val tempLabel = stringResource(Res.string.file_system_cleanup_option_temp)
    val downloadsLabel = stringResource(Res.string.file_system_cleanup_option_downloads)
    val appCacheLabel = stringResource(Res.string.file_system_cleanup_option_app_cache)

    val statusLabel = when (cleanupState.status) {
        CleanupStatus.IDLE -> stringResource(Res.string.file_system_cleanup_status_idle)
        CleanupStatus.RUNNING -> stringResource(Res.string.file_system_cleanup_status_running)
        CleanupStatus.SUCCESS -> stringResource(Res.string.file_system_cleanup_status_success)
        CleanupStatus.ERROR -> stringResource(Res.string.file_system_cleanup_status_error)
    }

    val logScrollState = rememberScrollState()

    Dialog(
        onDismissRequest = {
            if (!cleanupState.running) {
                onDismiss()
            }
        },
        properties = DialogProperties(
            dismissOnBackPress = !cleanupState.running,
            dismissOnClickOutside = !cleanupState.running,
        ),
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .heightIn(max = 680.dp),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                CleanupOptionRow(
                    title = tempLabel,
                    checked = cleanupState.selectedOptions.contains(CleanupOption.TEMP),
                    enabled = !cleanupState.running,
                    onToggle = { onToggleOption(CleanupOption.TEMP) },
                )
                CleanupOptionRow(
                    title = downloadsLabel,
                    checked = cleanupState.selectedOptions.contains(CleanupOption.DOWNLOADS),
                    enabled = !cleanupState.running,
                    onToggle = { onToggleOption(CleanupOption.DOWNLOADS) },
                )
                CleanupOptionRow(
                    title = appCacheLabel,
                    checked = cleanupState.selectedOptions.contains(CleanupOption.APP_CACHE),
                    enabled = !cleanupState.running,
                    onToggle = { onToggleOption(CleanupOption.APP_CACHE) },
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onStartCleanup,
                        enabled = !cleanupState.running && cleanupState.selectedOptions.isNotEmpty(),
                    ) {
                        Text(cleanSelected)
                    }
                    OutlinedButton(
                        onClick = onCancelCleanup,
                        enabled = cleanupState.running,
                    ) {
                        Text(cancel)
                    }
                    OutlinedButton(onClick = onCopyLog) {
                        Text(copy)
                    }
                    TextButton(
                        onClick = onDismiss,
                        enabled = !cleanupState.running,
                    ) {
                        Text(close)
                    }
                }

                Text(
                    text = statusLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = when (cleanupState.status) {
                        CleanupStatus.ERROR -> MaterialTheme.colorScheme.error
                        CleanupStatus.SUCCESS -> MaterialTheme.colorScheme.primary
                        CleanupStatus.RUNNING -> MaterialTheme.colorScheme.tertiary
                        CleanupStatus.IDLE -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )

                LinearProgressIndicator(
                    progress = { cleanupState.progress.coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp),
                )

                Text(
                    text = logTitle,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outlineVariant,
                            shape = RoundedCornerShape(8.dp),
                        )
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                        .padding(8.dp),
                ) {
                    Text(
                        text = cleanupState.log.ifBlank { logEmpty },
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(end = 10.dp)
                            .verticalScroll(logScrollState),
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                        color = MaterialTheme.colorScheme.onSurface,
                    )

                    VerticalScrollbar(
                        adapter = rememberScrollbarAdapter(logScrollState),
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .fillMaxHeight(),
                    )
                }
            }
        }
    }
}

@Composable
private fun CleanupOptionRow(
    title: String,
    checked: Boolean,
    enabled: Boolean,
    onToggle: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(
                value = checked,
                enabled = enabled,
                onValueChange = { onToggle() },
            )
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked = checked,
            enabled = enabled,
            onCheckedChange = { onToggle() },
        )
        Spacer(Modifier.width(8.dp))
        Text(text = title, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun CleanupConfirmDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.file_system_cleanup_confirm_title)) },
        text = { Text(stringResource(Res.string.file_system_cleanup_confirm_message)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(Res.string.file_system_cleanup_clean_selected))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.file_system_cleanup_cancel))
            }
        },
    )
}

private val packageSegmentRegex = Regex("^[a-zA-Z][a-zA-Z0-9_]*(?:\\.[a-zA-Z0-9_]+){1,}$")

private fun isAppSpecificPartition(item: com.adbdeck.core.adb.api.monitoring.storage.StoragePartition): Boolean {
    val mount = item.mountPoint.trim()
    if (mount.isBlank()) return false

    val normalized = mount.lowercase()
    if (normalized.contains("/android/data/") || normalized.contains("/android/obb/")) return true

    if (normalized.startsWith("/data/user/") ||
        normalized.startsWith("/data/user_de/") ||
        normalized.startsWith("/data_mirror/") ||
        normalized.startsWith("/mnt/installer/") ||
        normalized.startsWith("/mnt/androidwritable/")
    ) {
        return mount.split('/').any(::isLikelyPackageSegment)
    }

    val candidateRoots = normalized.startsWith("/data/") ||
        normalized.startsWith("/mnt/") ||
        normalized.startsWith("/storage/")
    if (!candidateRoots) return false

    if (normalized == "/data" ||
        normalized.startsWith("/data/media") ||
        normalized == "/sdcard" ||
        normalized.startsWith("/storage/emulated") ||
        normalized.startsWith("/storage/self") ||
        normalized.startsWith("/storage/enc_emulated") ||
        normalized.startsWith("/mnt/runtime")
    ) {
        return false
    }

    return mount.split('/').any(::isLikelyPackageSegment)
}

private fun isLikelyPackageSegment(segment: String): Boolean = packageSegmentRegex.matches(segment)

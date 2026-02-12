package com.adbdeck.feature.systemmonitor.ui

import adbdeck.feature.system_monitor.generated.resources.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.adbdeck.core.adb.api.monitoring.storage.StorageCategory
import com.adbdeck.core.adb.api.monitoring.storage.StoragePartition
import com.adbdeck.core.adb.api.monitoring.storage.StorageSummary
import com.adbdeck.core.ui.EmptyView
import com.adbdeck.core.ui.ErrorView
import com.adbdeck.core.ui.LoadingView
import com.adbdeck.core.utils.formatKb
import com.adbdeck.feature.systemmonitor.storage.StorageComponent
import com.adbdeck.feature.systemmonitor.storage.StorageListState
import org.jetbrains.compose.resources.stringResource

/**
 * Composable вкладки «Storage» в System Monitor.
 *
 * ## Структура
 * ```
 * Column {
 *   StorageToolbar(...)        // Refresh button
 *   HorizontalDivider
 *   LazyColumn {
 *     StorageSummaryCard(...)  // Общая сводка + прогресс-бар
 *     Разделы [StoragePartitionRow per relevant partition]
 *   }
 * }
 * ```
 *
 * @param component Компонент вкладки Storage.
 */
@Composable
fun StorageScreen(component: StorageComponent) {
    val state by component.state.collectAsState()
    val storageTitle = stringResource(Res.string.system_monitor_storage_title_filesystems)
    val refreshContentDescription = stringResource(Res.string.system_monitor_toolbar_refresh_content_desc)
    val emptyNoDevice = stringResource(Res.string.system_monitor_empty_no_device)
    val emptyNoStorageData = stringResource(Res.string.system_monitor_empty_storage_no_data)

    Column(modifier = Modifier.fillMaxSize()) {
        // ── Toolbar ───────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Outlined.Storage,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.width(8.dp))
            Text(
                storageTitle,
                style = MaterialTheme.typography.titleSmall,
            )
            Spacer(Modifier.weight(1f))
            IconButton(onClick = component::onRefresh) {
                Icon(Icons.Outlined.Refresh, contentDescription = refreshContentDescription)
            }
        }
        HorizontalDivider()

        // ── Контент ───────────────────────────────────────────────────
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when (val listState = state.listState) {
                is StorageListState.NoDevice ->
                    EmptyView(message = emptyNoDevice)

                is StorageListState.Loading ->
                    LoadingView()

                is StorageListState.Error ->
                    ErrorView(message = listState.message, onRetry = component::onRefresh)

                is StorageListState.Success -> {
                    val relevant = listState.partitions.filter { it.isRelevant }
                    if (relevant.isEmpty()) {
                        EmptyView(message = emptyNoStorageData)
                    } else {
                        StorageContent(
                            summary    = listState.summary,
                            partitions = relevant,
                        )
                    }
                }
            }
        }
    }
}

// ── Основной контент ──────────────────────────────────────────────────────────

@Composable
private fun StorageContent(
    summary: StorageSummary,
    partitions: List<StoragePartition>,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Сводная карточка
        item(key = "summary") {
            StorageSummaryCard(summary = summary)
        }

        // Список разделов по категориям
        val grouped = partitions.sortedWith(
            compareBy({ it.category.ordinal }, { it.mountPoint })
        )
        items(grouped, key = { it.mountPoint }) { partition ->
            StoragePartitionRow(partition = partition)
        }
    }
}

// ── Сводная карточка ──────────────────────────────────────────────────────────

@Composable
private fun StorageSummaryCard(summary: StorageSummary) {
    val summaryTitle = stringResource(Res.string.system_monitor_storage_summary_title)
    val statUsed = stringResource(Res.string.system_monitor_storage_stat_used)
    val statFree = stringResource(Res.string.system_monitor_storage_stat_free)
    val statTotal = stringResource(Res.string.system_monitor_storage_stat_total)
    val statPercent = stringResource(Res.string.system_monitor_storage_stat_percent)
    Surface(
        shape          = RoundedCornerShape(12.dp),
        tonalElevation = 3.dp,
        modifier       = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                summaryTitle,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            // Прогресс-бар
            LinearProgressIndicator(
                progress = { summary.usedPercent / 100f },
                modifier = Modifier.fillMaxWidth().height(8.dp),
                color    = progressColor(summary.usedPercent),
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )
            // Строка значений
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                StorageStat(statUsed, summary.usedKb.formatKb(), MaterialTheme.colorScheme.primary)
                StorageStat(statFree, summary.freeKb.formatKb(), MaterialTheme.colorScheme.secondary)
                StorageStat(statTotal, summary.totalKb.formatKb(), MaterialTheme.colorScheme.onSurface)
                StorageStat(statPercent, "${summary.usedPercent}%", progressColor(summary.usedPercent))
            }
        }
    }
}

@Composable
private fun progressColor(usedPercent: Int) = when {
    usedPercent >= 90 -> MaterialTheme.colorScheme.error
    usedPercent >= 75 -> MaterialTheme.colorScheme.tertiary
    else              -> MaterialTheme.colorScheme.primary
}

@Composable
private fun StorageStat(label: String, value: String, color: androidx.compose.ui.graphics.Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text  = value,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.SemiBold,
            ),
            color = color,
        )
        Text(
            text  = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// ── Строка раздела ────────────────────────────────────────────────────────────

@Composable
private fun StoragePartitionRow(partition: StoragePartition) {
    val categorySystem = stringResource(Res.string.system_monitor_storage_category_system)
    val categoryData = stringResource(Res.string.system_monitor_storage_category_data)
    val categoryExternal = stringResource(Res.string.system_monitor_storage_category_external)
    val categoryOther = stringResource(Res.string.system_monitor_storage_category_other)
    val usedFormat = stringResource(Res.string.system_monitor_storage_used_format, partition.usedKb.formatKb())
    val freeFormat = stringResource(Res.string.system_monitor_storage_free_format, partition.freeKb.formatKb())
    val totalFormat = stringResource(Res.string.system_monitor_storage_total_format, partition.totalKb.formatKb())
    val categoryColor = when (partition.category) {
        StorageCategory.SYSTEM   -> MaterialTheme.colorScheme.secondary
        StorageCategory.DATA     -> MaterialTheme.colorScheme.primary
        StorageCategory.EXTERNAL -> MaterialTheme.colorScheme.tertiary
        StorageCategory.OTHER    -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val categoryLabel = when (partition.category) {
        StorageCategory.SYSTEM   -> categorySystem
        StorageCategory.DATA     -> categoryData
        StorageCategory.EXTERNAL -> categoryExternal
        StorageCategory.OTHER    -> categoryOther
    }

    Surface(
        shape          = RoundedCornerShape(8.dp),
        tonalElevation = 1.dp,
        modifier       = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            // Заголовок: mount point + category chip
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text     = partition.mountPoint,
                    style    = MaterialTheme.typography.bodyMedium.copy(
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
                        text     = categoryLabel,
                        style    = MaterialTheme.typography.labelSmall.copy(color = categoryColor),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                }
            }
            // Файловая система
            Text(
                text  = partition.filesystem,
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp,
            )
            // Прогресс-бар
            LinearProgressIndicator(
                progress  = { partition.usedPercent / 100f },
                modifier  = Modifier.fillMaxWidth().height(4.dp),
                color     = progressColor(partition.usedPercent),
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )
            // Значения
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
                    text  = "${partition.usedPercent}%",
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

package com.adbdeck.feature.filesystem.ui

import adbdeck.feature.file_system.generated.resources.Res
import adbdeck.feature.file_system.generated.resources.file_system_category_data
import adbdeck.feature.file_system.generated.resources.file_system_category_external
import adbdeck.feature.file_system.generated.resources.file_system_category_other
import adbdeck.feature.file_system.generated.resources.file_system_category_system
import adbdeck.feature.file_system.generated.resources.file_system_empty_no_data
import adbdeck.feature.file_system.generated.resources.file_system_empty_no_device
import adbdeck.feature.file_system.generated.resources.file_system_free_format
import adbdeck.feature.file_system.generated.resources.file_system_section_app_subtitle
import adbdeck.feature.file_system.generated.resources.file_system_section_app_title
import adbdeck.feature.file_system.generated.resources.file_system_section_device_subtitle
import adbdeck.feature.file_system.generated.resources.file_system_section_device_title
import adbdeck.feature.file_system.generated.resources.file_system_stat_free
import adbdeck.feature.file_system.generated.resources.file_system_stat_percent
import adbdeck.feature.file_system.generated.resources.file_system_stat_total
import adbdeck.feature.file_system.generated.resources.file_system_stat_used
import adbdeck.feature.file_system.generated.resources.file_system_summary_title
import adbdeck.feature.file_system.generated.resources.file_system_title_filesystems
import adbdeck.feature.file_system.generated.resources.file_system_toolbar_refresh_content_desc
import adbdeck.feature.file_system.generated.resources.file_system_total_format
import adbdeck.feature.file_system.generated.resources.file_system_used_format
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import com.adbdeck.feature.filesystem.FileSystemComponent
import com.adbdeck.feature.filesystem.FileSystemListState
import org.jetbrains.compose.resources.stringResource

/**
 * Экран информации о файловых системах устройства.
 *
 * @param component Компонент экрана.
 */
@Composable
fun FileSystemScreen(component: FileSystemComponent) {
    val state by component.state.collectAsState()
    val title = stringResource(Res.string.file_system_title_filesystems)
    val refreshContentDescription = stringResource(Res.string.file_system_toolbar_refresh_content_desc)
    val emptyNoDevice = stringResource(Res.string.file_system_empty_no_device)
    val emptyNoData = stringResource(Res.string.file_system_empty_no_data)

    Column(modifier = Modifier.fillMaxSize()) {
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
                text = title,
                style = MaterialTheme.typography.titleSmall,
            )
            Spacer(Modifier.weight(1f))
            IconButton(onClick = component::onRefresh) {
                Icon(Icons.Outlined.Refresh, contentDescription = refreshContentDescription)
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
                    val relevant = listState.partitions.filter { it.isRelevant }
                    if (relevant.isEmpty()) {
                        EmptyView(message = emptyNoData)
                    } else {
                        FileSystemContent(
                            partitions = relevant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FileSystemContent(
    partitions: List<StoragePartition>,
) {
    val (appSpecificPartitions, devicePartitionsRaw) = partitions.partition(::isAppSpecificPartition)
    val devicePartitions = devicePartitionsRaw.sortedWith(compareBy({ it.category.ordinal }, { it.mountPoint }))
    val appPartitions = appSpecificPartitions.sortedBy { it.mountPoint }
    val summarySource = if (devicePartitions.isNotEmpty()) devicePartitions else partitions
    val summary = StorageSummary(
        totalKb = summarySource.sumOf { it.totalKb },
        usedKb = summarySource.sumOf { it.usedKb },
        freeKb = summarySource.sumOf { it.freeKb },
    )

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

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item(key = "summary") {
            FileSystemSummaryCard(summary = summary)
        }

        if (devicePartitions.isNotEmpty()) {
            item(key = "device_section") {
                FileSystemSectionHeader(
                    title = deviceSectionTitle,
                    subtitle = deviceSectionSubtitle,
                )
            }
            items(devicePartitions, key = { it.mountPoint }) { partition ->
                FileSystemPartitionRow(partition = partition)
            }
        }

        if (appPartitions.isNotEmpty()) {
            item(key = "app_section") {
                FileSystemSectionHeader(
                    title = appSectionTitle,
                    subtitle = appSectionSubtitle,
                )
            }
            items(appPartitions, key = { it.mountPoint }) { partition ->
                FileSystemPartitionRow(partition = partition)
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
private fun FileSystemPartitionRow(partition: StoragePartition) {
    val categorySystem = stringResource(Res.string.file_system_category_system)
    val categoryData = stringResource(Res.string.file_system_category_data)
    val categoryExternal = stringResource(Res.string.file_system_category_external)
    val categoryOther = stringResource(Res.string.file_system_category_other)
    val usedFormat = stringResource(Res.string.file_system_used_format, partition.usedKb.formatKb())
    val freeFormat = stringResource(Res.string.file_system_free_format, partition.freeKb.formatKb())
    val totalFormat = stringResource(Res.string.file_system_total_format, partition.totalKb.formatKb())

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
                horizontalArrangement = Arrangement.SpaceBetween,
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

private val packageSegmentRegex = Regex("^[a-zA-Z][a-zA-Z0-9_]*(?:\\.[a-zA-Z0-9_]+){1,}$")

private fun isAppSpecificPartition(partition: StoragePartition): Boolean {
    val mount = partition.mountPoint.trim()
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

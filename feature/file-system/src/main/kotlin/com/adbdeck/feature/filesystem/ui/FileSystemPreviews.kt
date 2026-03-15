package com.adbdeck.feature.filesystem.ui

import androidx.compose.runtime.Composable
import com.adbdeck.core.adb.api.monitoring.storage.StoragePartition
import com.adbdeck.core.adb.api.monitoring.storage.StorageSummary
import com.adbdeck.core.designsystem.AdbDeckTheme
import com.adbdeck.feature.filesystem.CleanupOption
import com.adbdeck.feature.filesystem.ContentAnalysis
import com.adbdeck.feature.filesystem.ContentAnalysisState
import com.adbdeck.feature.filesystem.FileSystemComponent
import com.adbdeck.feature.filesystem.FileSystemListState
import com.adbdeck.feature.filesystem.FileSystemPartitionItem
import com.adbdeck.feature.filesystem.FileSystemState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.jetbrains.compose.ui.tooling.preview.Preview

private val samplePartitions = listOf(
    FileSystemPartitionItem(
        partition = StoragePartition(
            filesystem = "/dev/block/sda1",
            totalKb = 52_428_800L,
            usedKb = 10_485_760L,
            freeKb = 41_943_040L,
            usedPercent = 20,
            mountPoint = "/system",
        ),
        openPath = null,
    ),
    FileSystemPartitionItem(
        partition = StoragePartition(
            filesystem = "/dev/block/sda2",
            totalKb = 104_857_600L,
            usedKb = 73_400_320L,
            freeKb = 31_457_280L,
            usedPercent = 70,
            mountPoint = "/data",
        ),
        openPath = null,
    ),
    FileSystemPartitionItem(
        partition = StoragePartition(
            filesystem = "/dev/fuse",
            totalKb = 52_428_800L,
            usedKb = 20_971_520L,
            freeKb = 31_457_280L,
            usedPercent = 40,
            mountPoint = "/sdcard",
        ),
        openPath = "/sdcard",
    ),
)

private class PreviewFileSystemComponent : FileSystemComponent {
    override val state: StateFlow<FileSystemState> = MutableStateFlow(
        FileSystemState(
            listState = FileSystemListState.Success(
                partitions = samplePartitions,
                summary = StorageSummary(
                    totalKb = samplePartitions.sumOf { it.partition.totalKb },
                    usedKb = samplePartitions.sumOf { it.partition.usedKb },
                    freeKb = samplePartitions.sumOf { it.partition.freeKb },
                ),
            ),
            contentAnalysis = ContentAnalysisState.Success(
                ContentAnalysis(
                    appSizeKb = 3_200_000,
                    appDataSizeKb = 12_500_000,
                    appCacheSizeKb = 840_000,
                    photosSizeKb = 6_100_000,
                    videosSizeKb = 2_300_000,
                    audioSizeKb = 450_000,
                    downloadsSizeKb = 910_000,
                    otherSizeKb = 1_200_000,
                    systemSizeKb = 18_000_000,
                    dataFreeKb = 42_000_000,
                    dataTotalKb = 84_000_000,
                ),
            ),
        ),
    )

    override fun onRefresh() = Unit
    override fun onOpenCleanup() = Unit
    override fun onDismissCleanup() = Unit
    override fun onToggleCleanupOption(option: CleanupOption) = Unit
    override fun onStartCleanup() = Unit
    override fun onConfirmCleanup() = Unit
    override fun onDismissCleanupConfirm() = Unit
    override fun onCancelCleanup() = Unit
    override fun onCopyCleanupLog() = Unit
    override fun onOpenPartition(path: String) = Unit
}

@Preview
@Composable
private fun FileSystemScreenPreview() {
    AdbDeckTheme(isDarkTheme = false) {
        FileSystemScreen(component = PreviewFileSystemComponent())
    }
}

@Preview
@Composable
private fun FileSystemScreenDarkPreview() {
    AdbDeckTheme(isDarkTheme = true) {
        FileSystemScreen(component = PreviewFileSystemComponent())
    }
}

package com.adbdeck.feature.filesystem.ui

import androidx.compose.runtime.Composable
import com.adbdeck.core.adb.api.monitoring.storage.StoragePartition
import com.adbdeck.core.adb.api.monitoring.storage.StorageSummary
import com.adbdeck.core.designsystem.AdbDeckTheme
import com.adbdeck.feature.filesystem.FileSystemComponent
import com.adbdeck.feature.filesystem.FileSystemListState
import com.adbdeck.feature.filesystem.FileSystemState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.jetbrains.compose.ui.tooling.preview.Preview

private val samplePartitions = listOf(
    StoragePartition(
        filesystem = "/dev/block/sda1",
        totalKb = 52_428_800L,
        usedKb = 10_485_760L,
        freeKb = 41_943_040L,
        usedPercent = 20,
        mountPoint = "/system",
    ),
    StoragePartition(
        filesystem = "/dev/block/sda2",
        totalKb = 104_857_600L,
        usedKb = 73_400_320L,
        freeKb = 31_457_280L,
        usedPercent = 70,
        mountPoint = "/data",
    ),
    StoragePartition(
        filesystem = "/dev/fuse",
        totalKb = 52_428_800L,
        usedKb = 20_971_520L,
        freeKb = 31_457_280L,
        usedPercent = 40,
        mountPoint = "/sdcard",
    ),
)

private class PreviewFileSystemComponent : FileSystemComponent {
    override val state: StateFlow<FileSystemState> = MutableStateFlow(
        FileSystemState(
            listState = FileSystemListState.Success(
                partitions = samplePartitions,
                summary = StorageSummary(
                    totalKb = samplePartitions.sumOf { it.totalKb },
                    usedKb = samplePartitions.sumOf { it.usedKb },
                    freeKb = samplePartitions.sumOf { it.freeKb },
                ),
            ),
        ),
    )

    override fun onRefresh() = Unit
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

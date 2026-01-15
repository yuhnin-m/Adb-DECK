package com.adbdeck.feature.fileexplorer.ui

import com.adbdeck.feature.fileexplorer.ExplorerFileItem
import com.adbdeck.feature.fileexplorer.ExplorerListState
import com.adbdeck.feature.fileexplorer.ExplorerPanelState
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

internal fun selectedItem(panel: ExplorerPanelState): ExplorerFileItem? {
    val items = (panel.listState as? ExplorerListState.Success)?.items ?: return null
    return items.firstOrNull { it.fullPath == panel.selectedPath }
}

internal fun currentRootForPath(roots: List<String>, currentPath: String): String? {
    val normalizedPath = currentPath.trimEnd('/').ifBlank { "/" }
    return roots
        .map { it.trimEnd('/').ifBlank { "/" } }
        .filter { root -> normalizedPath == root || normalizedPath.startsWith("$root/") }
        .maxByOrNull { it.length }
        ?: roots.firstOrNull()
}

private val dateFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

internal fun formatEpochMillis(epochMillis: Long): String =
    dateFormatter.format(Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()))

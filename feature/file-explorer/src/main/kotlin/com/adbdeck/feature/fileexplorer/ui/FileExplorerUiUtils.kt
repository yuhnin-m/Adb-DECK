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
    val normalizedPath = normalizePathForCompare(currentPath)
    var bestMatch: String? = null
    for (root in roots) {
        val normalizedRoot = normalizePathForCompare(root)
        if (normalizedPath == normalizedRoot || normalizedPath.startsWith("$normalizedRoot/")) {
            if (bestMatch == null || normalizedRoot.length > bestMatch.length) {
                bestMatch = normalizedRoot
            }
        }
    }
    return bestMatch ?: roots.firstOrNull()
}

private val dateFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

internal fun formatEpochMillis(epochMillis: Long): String =
    dateFormatter.format(Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()))

private fun normalizePathForCompare(path: String): String =
    path.trimEnd('/').ifBlank { "/" }

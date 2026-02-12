package com.adbdeck.feature.systemmonitor.processes

import com.adbdeck.core.adb.api.monitoring.process.ProcessInfo
import kotlinx.coroutines.flow.update

internal fun DefaultProcessesComponent.handleSearchChanged(query: String) {
    _state.update { current ->
        val processes = (current.listState as? ProcessListState.Success)?.processes ?: emptyList()
        current.copy(
            searchQuery = query,
            filteredProcesses = applyFilters(processes, query, current.sortField),
        )
    }
}

internal fun DefaultProcessesComponent.handleSortFieldChanged(field: ProcessSortField) {
    _state.update { current ->
        val processes = (current.listState as? ProcessListState.Success)?.processes ?: emptyList()
        current.copy(
            sortField = field,
            filteredProcesses = applyFilters(processes, current.searchQuery, field),
        )
    }
}

internal fun applyFilters(
    processes: List<ProcessInfo>,
    query: String,
    sortField: ProcessSortField,
): List<ProcessInfo> {
    val q = query.trim()
    val filtered = if (q.isEmpty()) {
        processes
    } else {
        processes.filter { p ->
            p.name.contains(q, ignoreCase = true) ||
                p.packageName.contains(q, ignoreCase = true) ||
                p.user.contains(q, ignoreCase = true) ||
                p.pid.toString() == q
        }
    }

    return when (sortField) {
        ProcessSortField.CPU -> filtered.sortedByDescending { it.cpuPercent }
        ProcessSortField.MEMORY -> filtered.sortedByDescending { it.rssKb }
        ProcessSortField.NAME -> filtered.sortedBy { it.displayName.lowercase() }
        ProcessSortField.PID -> filtered.sortedBy { it.pid }
    }
}

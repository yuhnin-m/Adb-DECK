package com.adbdeck.feature.systemmonitor.ui

import adbdeck.feature.system_monitor.generated.resources.Res
import adbdeck.feature.system_monitor.generated.resources.system_monitor_empty_no_device
import adbdeck.feature.system_monitor.generated.resources.system_monitor_empty_processes_not_found
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.adbdeck.core.adb.api.monitoring.process.ProcessInfo
import com.adbdeck.core.ui.AdbBanner
import com.adbdeck.core.ui.AdbBannerType
import com.adbdeck.core.ui.EmptyView
import com.adbdeck.core.ui.ErrorView
import com.adbdeck.core.ui.LoadingView
import com.adbdeck.feature.systemmonitor.processes.ProcessActionFeedback
import com.adbdeck.feature.systemmonitor.processes.ProcessDetailState
import com.adbdeck.feature.systemmonitor.processes.ProcessListState
import com.adbdeck.feature.systemmonitor.processes.ProcessSortField
import com.adbdeck.feature.systemmonitor.processes.ProcessesComponent
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import org.jetbrains.compose.resources.stringResource

/**
 * Composable вкладки «Processes» в System Monitor.
 */
@Composable
fun ProcessesScreen(component: ProcessesComponent) {
    Column(modifier = Modifier.fillMaxSize()) {
        ProcessesToolbarHost(component = component)
        HorizontalDivider()
        ProcessesMainContent(
            component = component,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        )
        ProcessesFeedbackHost(component = component)
    }
}

private data class ProcessesToolbarUiState(
    val isMonitoring: Boolean,
    val searchQuery: String,
    val sortField: ProcessSortField,
)

private data class ProcessesListUiState(
    val listState: ProcessListState,
    val filteredProcesses: List<ProcessInfo>,
    val selectedProcess: ProcessInfo?,
    val isActionRunning: Boolean,
)

private data class ProcessesDetailUiState(
    val selectedProcess: ProcessInfo?,
    val detailState: ProcessDetailState,
    val isActionRunning: Boolean,
)

@Composable
private fun ProcessesToolbarHost(component: ProcessesComponent) {
    val initialUi = remember(component) {
        val initial = component.state.value
        ProcessesToolbarUiState(
            isMonitoring = initial.isMonitoring,
            searchQuery = initial.searchQuery,
            sortField = initial.sortField,
        )
    }
    val toolbarState by remember(component) {
        component.state
            .map {
                ProcessesToolbarUiState(
                    isMonitoring = it.isMonitoring,
                    searchQuery = it.searchQuery,
                    sortField = it.sortField,
                )
            }
            .distinctUntilChanged()
    }.collectAsState(initial = initialUi)

    ProcessesToolbar(
        isMonitoring = toolbarState.isMonitoring,
        searchQuery = toolbarState.searchQuery,
        sortField = toolbarState.sortField,
        onStartMonitoring = component::onStartMonitoring,
        onStopMonitoring = component::onStopMonitoring,
        onRefresh = component::onRefresh,
        onSearchChanged = component::onSearchChanged,
        onSortFieldChanged = component::onSortFieldChanged,
    )
}

@Composable
private fun ProcessesMainContent(
    component: ProcessesComponent,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
        ) {
            MetricsChartHost(component = component)
            ProcessListHost(
                component = component,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            )
        }

        ProcessDetailHost(component = component)
    }
}

@Composable
private fun MetricsChartHost(component: ProcessesComponent) {
    val initialHistory = remember(component) { component.state.value.history }
    val history by remember(component) {
        component.state
            .map { it.history }
            .distinctUntilChanged()
    }.collectAsState(initial = initialHistory)

    if (history.isNotEmpty()) {
        MetricsChartPanel(history = history)
        HorizontalDivider()
    }
}

@Composable
private fun ProcessListHost(
    component: ProcessesComponent,
    modifier: Modifier = Modifier,
) {
    val noDeviceMessage = stringResource(Res.string.system_monitor_empty_no_device)
    val processesNotFoundMessage = stringResource(Res.string.system_monitor_empty_processes_not_found)
    val initialUi = remember(component) {
        val initial = component.state.value
        ProcessesListUiState(
            listState = initial.listState,
            filteredProcesses = initial.filteredProcesses,
            selectedProcess = initial.selectedProcess,
            isActionRunning = initial.isActionRunning,
        )
    }
    val uiState by remember(component) {
        component.state
            .map {
                ProcessesListUiState(
                    listState = it.listState,
                    filteredProcesses = it.filteredProcesses,
                    selectedProcess = it.selectedProcess,
                    isActionRunning = it.isActionRunning,
                )
            }
            .distinctUntilChanged()
    }.collectAsState(initial = initialUi)

    Box(modifier = modifier) {
        when (val listState = uiState.listState) {
            is ProcessListState.NoDevice -> EmptyView(message = noDeviceMessage)
            is ProcessListState.Loading -> LoadingView()
            is ProcessListState.Error -> {
                ErrorView(
                    message = listState.message,
                    onRetry = component::onRefresh,
                )
            }
            is ProcessListState.Success -> {
                if (uiState.filteredProcesses.isEmpty()) {
                    EmptyView(message = processesNotFoundMessage)
                } else {
                    ProcessList(
                        processes = uiState.filteredProcesses,
                        selectedProcess = uiState.selectedProcess,
                        isActionRunning = uiState.isActionRunning,
                        onSelectProcess = component::onSelectProcess,
                        onKillProcess = component::onKillProcess,
                    )
                }
            }
        }
    }
}

@Composable
private fun ProcessDetailHost(component: ProcessesComponent) {
    val initialUi = remember(component) {
        val initial = component.state.value
        ProcessesDetailUiState(
            selectedProcess = initial.selectedProcess,
            detailState = initial.detailState,
            isActionRunning = initial.isActionRunning,
        )
    }
    val uiState by remember(component) {
        component.state
            .map {
                ProcessesDetailUiState(
                    selectedProcess = it.selectedProcess,
                    detailState = it.detailState,
                    isActionRunning = it.isActionRunning,
                )
            }
            .distinctUntilChanged()
    }.collectAsState(initial = initialUi)

    val selected = uiState.selectedProcess ?: return

    VerticalDivider()
    ProcessDetailPanel(
        process = selected,
        detailState = uiState.detailState,
        isActionRunning = uiState.isActionRunning,
        onClose = component::onClearSelection,
        onKill = component::onKillProcess,
        onForceStop = component::onForceStopApp,
        onOpenPackageDetails = component::onOpenPackageDetails,
        modifier = Modifier
            .width(360.dp)
            .fillMaxHeight(),
    )
}

@Composable
private fun ProcessesFeedbackHost(component: ProcessesComponent) {
    val initialFeedback = remember(component) { component.state.value.actionFeedback }
    val feedback by remember(component) {
        component.state
            .map { it.actionFeedback }
            .distinctUntilChanged()
    }.collectAsState(initial = initialFeedback)

    feedback?.let { banner ->
        RenderProcessesFeedback(
            feedback = banner,
            onDismiss = component::onDismissFeedback,
        )
    }
}

@Composable
private fun RenderProcessesFeedback(
    feedback: ProcessActionFeedback,
    onDismiss: () -> Unit,
) {
    AdbBanner(
        message = feedback.message,
        type = if (feedback.isError) AdbBannerType.ERROR else AdbBannerType.SUCCESS,
        onDismiss = onDismiss,
        modifier = Modifier.fillMaxWidth(),
    )
}

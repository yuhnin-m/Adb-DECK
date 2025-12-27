package com.adbdeck.feature.systemmonitor.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.adbdeck.core.adb.api.monitoring.process.ProcessInfo
import com.adbdeck.core.adb.api.monitoring.process.ProcessState
import com.adbdeck.core.adb.api.monitoring.SystemSnapshot
import com.adbdeck.core.ui.AdbBanner
import com.adbdeck.core.ui.AdbBannerType
import com.adbdeck.core.ui.EmptyView
import com.adbdeck.core.ui.ErrorView
import com.adbdeck.core.ui.LoadingView
import com.adbdeck.core.utils.formatKb
import com.adbdeck.feature.systemmonitor.processes.ProcessListState
import com.adbdeck.feature.systemmonitor.processes.ProcessSortField
import com.adbdeck.feature.systemmonitor.processes.ProcessesComponent

/**
 * Composable вкладки «Processes» в System Monitor.
 *
 * ## Структура
 * ```
 * Column {
 *   ProcessesToolbar(...)       // Мониторинг | Обновить | Поиск | Сортировка
 *   HorizontalDivider
 *   Row(weight 1f) {
 *     Column(weight 1f) {
 *       MetricsChartPanel(...)  // CPU + RAM графики
 *       HorizontalDivider
 *       ProcessList(...)        // LazyColumn со строками процессов
 *     }
 *     if (selectedProcess != null) {
 *       VerticalDivider
 *       ProcessDetailPanel(...) // Детали выбранного процесса
 *     }
 *   }
 *   AdbBanner(...)              // Снэкбар обратной связи (если есть)
 * }
 * ```
 *
 * @param component Компонент вкладки Processes.
 */
@Composable
fun ProcessesScreen(component: ProcessesComponent) {
    val state by component.state.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        // ── Toolbar ───────────────────────────────────────────────────
        ProcessesToolbar(
            isMonitoring = state.isMonitoring,
            searchQuery  = state.searchQuery,
            sortField    = state.sortField,
            onStartMonitoring = component::onStartMonitoring,
            onStopMonitoring  = component::onStopMonitoring,
            onRefresh         = component::onRefresh,
            onSearchChanged   = component::onSearchChanged,
            onSortFieldChanged = component::onSortFieldChanged,
        )
        HorizontalDivider()

        // ── Основная область (список + детали) ────────────────────────
        Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
            // Левая колонка: графики + список процессов
            Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                // Мини-графики CPU и RAM (показываем только если есть история)
                if (state.history.isNotEmpty()) {
                    MetricsChartPanel(history = state.history)
                    HorizontalDivider()
                }

                // Список процессов
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    when (val listState = state.listState) {
                        is ProcessListState.NoDevice ->
                            EmptyView(message = "Устройство не подключено")

                        is ProcessListState.Loading ->
                            LoadingView()

                        is ProcessListState.Error ->
                            ErrorView(
                                message = listState.message,
                                onRetry = component::onRefresh,
                            )

                        is ProcessListState.Success -> {
                            if (state.filteredProcesses.isEmpty()) {
                                EmptyView(message = "Процессы не найдены")
                            } else {
                                ProcessList(
                                    processes        = state.filteredProcesses,
                                    selectedProcess  = state.selectedProcess,
                                    isActionRunning  = state.isActionRunning,
                                    onSelectProcess  = component::onSelectProcess,
                                    onKillProcess    = component::onKillProcess,
                                )
                            }
                        }
                    }
                }
            }

            // Панель деталей (если выбран процесс)
            if (state.selectedProcess != null) {
                VerticalDivider()
                ProcessDetailPanel(
                    process     = state.selectedProcess!!,
                    detailState = state.detailState,
                    isActionRunning = state.isActionRunning,
                    onClose     = component::onClearSelection,
                    onKill      = component::onKillProcess,
                    onForceStop = component::onForceStopApp,
                    onOpenPackageDetails = component::onOpenPackageDetails,
                    modifier    = Modifier.width(360.dp).fillMaxHeight(),
                )
            }
        }

        // ── Feedback снэкбар ──────────────────────────────────────────
        state.actionFeedback?.let { feedback ->
            AdbBanner(
                message   = feedback.message,
                type = if (feedback.isError) AdbBannerType.ERROR else AdbBannerType.SUCCESS,
                onDismiss = component::onDismissFeedback,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

// ── Toolbar ───────────────────────────────────────────────────────────────────

@Composable
private fun ProcessesToolbar(
    isMonitoring: Boolean,
    searchQuery: String,
    sortField: ProcessSortField,
    onStartMonitoring: () -> Unit,
    onStopMonitoring: () -> Unit,
    onRefresh: () -> Unit,
    onSearchChanged: (String) -> Unit,
    onSortFieldChanged: (ProcessSortField) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Кнопка Start/Stop Monitoring
        if (isMonitoring) {
            Button(
                onClick = onStopMonitoring,
                colors  = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor   = MaterialTheme.colorScheme.onErrorContainer,
                ),
            ) {
                Icon(Icons.Outlined.Stop, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Stop")
            }
        } else {
            Button(onClick = onStartMonitoring) {
                Icon(Icons.Outlined.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Monitor")
            }
        }

        // Refresh
        IconButton(onClick = onRefresh) {
            Icon(Icons.Outlined.Refresh, contentDescription = "Обновить")
        }

        // Кнопки сортировки
        SortButtons(
            currentSort = sortField,
            onSortChanged = onSortFieldChanged,
        )

        Spacer(Modifier.weight(1f))

        // Поиск
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChanged,
            modifier = Modifier.width(220.dp).height(46.dp),
            placeholder = { Text("Поиск процессов...", fontSize = 13.sp) },
            leadingIcon = {
                Icon(Icons.Outlined.Search, contentDescription = null, modifier = Modifier.size(18.dp))
            },
            trailingIcon = if (searchQuery.isNotEmpty()) {
                {
                    IconButton(onClick = { onSearchChanged("") }, modifier = Modifier.size(18.dp)) {
                        Icon(Icons.Outlined.Clear, contentDescription = "Очистить")
                    }
                }
            } else null,
            singleLine = true,
            textStyle = LocalTextStyle.current.copy(fontSize = 13.sp),
        )
    }
}

@Composable
private fun SortButtons(
    currentSort: ProcessSortField,
    onSortChanged: (ProcessSortField) -> Unit,
) {
    val fields = listOf(
        ProcessSortField.CPU    to "CPU",
        ProcessSortField.MEMORY to "MEM",
        ProcessSortField.NAME   to "Name",
        ProcessSortField.PID    to "PID",
    )
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        fields.forEach { (field, label) ->
            val isSelected = currentSort == field
            FilterChip(
                selected = isSelected,
                onClick  = { onSortChanged(field) },
                label    = { Text(label, fontSize = 12.sp) },
                modifier = Modifier.height(30.dp),
            )
        }
    }
}

// ── Графики CPU / RAM ─────────────────────────────────────────────────────────

@Composable
private fun MetricsChartPanel(history: List<SystemSnapshot>) {
    val cpuValues = history.map { it.cpuPercent }
    val ramValues = history.map { it.ramPercent }

    val latestCpu = cpuValues.lastOrNull() ?: 0f
    val latestRam = ramValues.lastOrNull() ?: 0f
    val totalRam  = history.lastOrNull()?.totalRamKb ?: 0L
    val usedRam   = history.lastOrNull()?.usedRamKb ?: 0L

    val cpuColor = MaterialTheme.colorScheme.primary
    val ramColor = MaterialTheme.colorScheme.tertiary

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(90.dp)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // CPU-график
        MetricCard(
            label       = "CPU",
            valueText   = "%.1f%%".format(latestCpu),
            values      = cpuValues,
            lineColor   = cpuColor,
            modifier    = Modifier.weight(1f).fillMaxHeight(),
        )
        // RAM-график
        MetricCard(
            label     = "RAM",
            valueText = "${usedRam.formatKb()} / ${totalRam.formatKb()}",
            values    = ramValues,
            lineColor = ramColor,
            modifier  = Modifier.weight(1f).fillMaxHeight(),
        )
    }
}

@Composable
private fun MetricCard(
    label: String,
    valueText: String,
    values: List<Float>,
    lineColor: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape    = RoundedCornerShape(8.dp),
        tonalElevation = 2.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Метка + значение
            Column(modifier = Modifier.width(90.dp)) {
                Text(
                    text  = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text  = valueText,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = FontFamily.Monospace,
                    ),
                    color = lineColor,
                )
            }
            // Линейный график
            LineChart(
                values    = values,
                lineColor = lineColor,
                modifier  = Modifier.weight(1f).fillMaxHeight(),
            )
        }
    }
}

/**
 * Компактный линейный график на Compose Canvas.
 *
 * Рисует линию + закрашенную область под ней без внешних библиотек.
 * Используется для отображения истории CPU% и RAM%.
 *
 * @param values    Список значений 0f–100f.
 * @param lineColor Цвет линии.
 */
@Composable
private fun LineChart(
    values: List<Float>,
    lineColor: Color,
    modifier: Modifier = Modifier,
) {
    val fillColor = lineColor.copy(alpha = 0.15f)

    Canvas(modifier = modifier) {
        if (values.size < 2) return@Canvas
        drawLineChart(
            values    = values,
            lineColor = lineColor,
            fillColor = fillColor,
        )
    }
}

private fun DrawScope.drawLineChart(
    values: List<Float>,
    lineColor: Color,
    fillColor: Color,
) {
    val w = size.width
    val h = size.height
    val n = values.size

    fun xOf(i: Int) = w * i / (n - 1)
    fun yOf(v: Float) = h * (1f - v / 100f)

    // Заливка под графиком
    val fillPath = Path().apply {
        moveTo(xOf(0), h)
        values.forEachIndexed { i, v ->
            lineTo(xOf(i), yOf(v))
        }
        lineTo(xOf(n - 1), h)
        close()
    }
    drawPath(fillPath, color = fillColor)

    // Линия графика
    val linePath = Path().apply {
        values.forEachIndexed { i, v ->
            val x = xOf(i)
            val y = yOf(v)
            if (i == 0) moveTo(x, y) else lineTo(x, y)
        }
    }
    drawPath(
        path  = linePath,
        color = lineColor,
        style = Stroke(
            width     = 1.5.dp.toPx(),
            cap       = StrokeCap.Round,
            join      = StrokeJoin.Round,
        ),
    )

    // Точка последнего значения
    val lastX = xOf(n - 1)
    val lastY = yOf(values.last())
    drawCircle(color = lineColor, radius = 3.dp.toPx(), center = Offset(lastX, lastY))
}

// ── Список процессов ──────────────────────────────────────────────────────────

@Composable
private fun ProcessList(
    processes: List<ProcessInfo>,
    selectedProcess: ProcessInfo?,
    isActionRunning: Boolean,
    onSelectProcess: (ProcessInfo) -> Unit,
    onKillProcess: (ProcessInfo) -> Unit,
) {
    val listState = rememberLazyListState()

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
    ) {
        // Заголовок таблицы
        item(key = "header") {
            ProcessListHeader()
            HorizontalDivider()
        }
        items(processes, key = { it.pid }) { process ->
            ProcessRow(
                process         = process,
                isSelected      = process.pid == selectedProcess?.pid,
                isActionRunning = isActionRunning,
                onClick         = { onSelectProcess(process) },
                onKill          = { onKillProcess(process) },
            )
            HorizontalDivider(thickness = 0.5.dp)
        }
    }
}

@Composable
private fun ProcessListHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(28.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("PID",  style = headerStyle(), modifier = Modifier.width(52.dp))
        Text("Name", style = headerStyle(), modifier = Modifier.weight(1f))
        Text("User", style = headerStyle(), modifier = Modifier.width(80.dp))
        Text("CPU%", style = headerStyle(), modifier = Modifier.width(54.dp))
        Text("RSS",  style = headerStyle(), modifier = Modifier.width(70.dp))
        Spacer(Modifier.width(36.dp))
    }
}

@Composable
private fun headerStyle() = MaterialTheme.typography.labelSmall.copy(
    color = MaterialTheme.colorScheme.onSurfaceVariant,
)

@Composable
private fun ProcessRow(
    process: ProcessInfo,
    isSelected: Boolean,
    isActionRunning: Boolean,
    onClick: () -> Unit,
    onKill: () -> Unit,
) {
    val bgColor = if (isSelected)
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
    else
        Color.Transparent

    val stateColor = when (process.state) {
        ProcessState.RUNNING  -> MaterialTheme.colorScheme.primary
        ProcessState.ZOMBIE   -> MaterialTheme.colorScheme.error
        ProcessState.STOPPED  -> MaterialTheme.colorScheme.tertiary
        else                  -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(36.dp)
            .background(bgColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // PID
        Text(
            text     = process.pid.toString(),
            style    = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
            color    = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(52.dp),
        )
        // Name (display name + state badge)
        Row(
            modifier            = Modifier.weight(1f),
            verticalAlignment   = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text     = process.displayName,
                style    = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
            // State chip
            ProcessStateChip(process.state, stateColor)
        }
        // User
        Text(
            text     = process.user.take(12),
            style    = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
            color    = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.width(80.dp),
        )
        // CPU%
        Text(
            text     = "%.1f".format(process.cpuPercent),
            style    = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
            color    = if (process.cpuPercent > 50f) MaterialTheme.colorScheme.error
                       else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.width(54.dp),
        )
        // RSS
        Text(
            text     = process.rssKb.formatKb(),
            style    = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
            color    = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.width(70.dp),
        )
        // Kill button
        IconButton(
            onClick  = onKill,
            enabled  = !isActionRunning,
            modifier = Modifier.size(28.dp),
        ) {
            Icon(
                imageVector        = Icons.Outlined.Close,
                contentDescription = "Kill process",
                modifier           = Modifier.size(14.dp),
                tint               = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ProcessStateChip(state: ProcessState, color: Color) {
    if (state == ProcessState.SLEEPING || state == ProcessState.UNKNOWN) return
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = color.copy(alpha = 0.15f),
        modifier = Modifier.wrapContentSize(),
    ) {
        Text(
            text     = state.symbol.toString(),
            style    = MaterialTheme.typography.labelSmall.copy(
                fontFamily = FontFamily.Monospace,
                color      = color,
            ),
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
        )
    }
}

package com.adbdeck.feature.systemmonitor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.adbdeck.core.adb.api.monitoring.ProcessDetails
import com.adbdeck.core.adb.api.monitoring.ProcessInfo
import com.adbdeck.core.ui.LoadingView
import com.adbdeck.core.utils.formatKb
import com.adbdeck.feature.systemmonitor.processes.ProcessDetailState

/**
 * Боковая панель деталей выбранного процесса.
 *
 * Отображается справа от списка процессов при выборе строки.
 * Содержит:
 * - Заголовок (имя, PID, кнопки действий)
 * - Базовые метрики из списка (CPU%, RSS, VSZ)
 * - Детальную разбивку памяти из /proc + dumpsys (если загружено)
 * - Системную информацию (user, ppid, threads, cmdline)
 *
 * @param process     Выбранный процесс (базовые данные из списка).
 * @param detailState Состояние загрузки детальной информации.
 * @param onClose     Снять выделение / закрыть панель.
 * @param onKill      Убить процесс (kill -9).
 * @param onForceStop Force-stop Android-приложения.
 * @param modifier    Modifier для контейнера панели.
 */
@Composable
fun ProcessDetailPanel(
    process: ProcessInfo,
    detailState: ProcessDetailState,
    isActionRunning: Boolean,
    onClose: () -> Unit,
    onKill: (ProcessInfo) -> Unit,
    onForceStop: (ProcessInfo) -> Unit,
    onOpenPackageDetails: (ProcessInfo) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surface),
    ) {
        // ── Заголовок панели ──────────────────────────────────────────
        DetailHeader(
            process     = process,
            isActionRunning = isActionRunning,
            onClose     = onClose,
            onKill      = { onKill(process) },
            onForceStop = { onForceStop(process) },
            onOpenPackageDetails = { onOpenPackageDetails(process) },
        )
        HorizontalDivider()

        // ── Скроллируемое содержимое ──────────────────────────────────
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Базовые метрики (всегда доступны из ProcessInfo)
            DetailSection(title = "Метрики") {
                InfoRow("CPU",      "%.2f%%".format(process.cpuPercent))
                InfoRow("MEM%",     "%.2f%%".format(process.memPercent))
                InfoRow("RSS",      process.rssKb.formatKb())
                InfoRow("VSZ",      process.vszKb.formatKb())
                InfoRow("State",    "${process.state.symbol} — ${process.state.displayName}")
            }

            // Детальная информация (из /proc + dumpsys)
            when (detailState) {
                is ProcessDetailState.Loading ->
                    LoadingView(modifier = Modifier.fillMaxWidth().height(120.dp))

                is ProcessDetailState.Error ->
                    Text(
                        text  = "Детали недоступны: ${detailState.message}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )

                is ProcessDetailState.Success -> {
                    val d = detailState.details
                    // Разбивка памяти
                    if (d.pssKb > 0L || d.ussKb > 0L || d.heapSizeKb > 0L) {
                        DetailSection(title = "Память (детально)") {
                            if (d.pssKb > 0L)           InfoRow("PSS",          d.pssKb.formatKb())
                            if (d.ussKb > 0L)           InfoRow("USS",          d.ussKb.formatKb())
                            if (d.heapSizeKb > 0L)      InfoRow("Heap size",    d.heapSizeKb.formatKb())
                            if (d.heapAllocKb > 0L)     InfoRow("Heap alloc",   d.heapAllocKb.formatKb())
                            if (d.heapFreeKb > 0L)      InfoRow("Heap free",    d.heapFreeKb.formatKb())
                            if (d.nativeHeapSizeKb > 0L) InfoRow("Native heap", d.nativeHeapSizeKb.formatKb())
                            if (d.nativeHeapAllocKb > 0L) InfoRow("Native alloc", d.nativeHeapAllocKb.formatKb())
                        }
                    }
                    // Системная информация
                    DetailSection(title = "Процесс") {
                        InfoRow("PID",     d.pid.toString())
                        InfoRow("PPID",    d.ppid.toString())
                        InfoRow("Threads", d.threads.toString())
                        if (d.user.isNotEmpty()) InfoRow("User", d.user)
                        if (d.openFiles > 0)     InfoRow("Open files", d.openFiles.toString())
                    }
                    // Командная строка
                    if (d.cmdline.isNotBlank()) {
                        DetailSection(title = "Командная строка") {
                            Text(
                                text     = d.cmdline,
                                style    = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize   = 11.sp,
                                ),
                                color    = MaterialTheme.colorScheme.onSurface,
                                maxLines = 4,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }

                is ProcessDetailState.Idle -> Unit // не должно появляться при открытой панели
            }

            // Системная инфо всегда (из ProcessInfo)
            DetailSection(title = "Идентификация") {
                InfoRow("PID",  process.pid.toString())
                InfoRow("PPID", process.ppid.takeIf { it > 0 }?.toString() ?: "—")
                InfoRow("User", process.user.ifEmpty { "—" })
                if (process.packageName.isNotEmpty()) {
                    InfoRow("Package", process.packageName)
                }
            }
        }
    }
}

// ── Заголовок панели ──────────────────────────────────────────────────────────

@Composable
private fun DetailHeader(
    process: ProcessInfo,
    isActionRunning: Boolean,
    onClose: () -> Unit,
    onKill: () -> Unit,
    onForceStop: () -> Unit,
    onOpenPackageDetails: () -> Unit,
) {
    Column {
        // Верхняя строка: имя + кнопка закрыть
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .padding(start = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text     = process.displayName,
                style    = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onClose) {
                Icon(Icons.Outlined.Close, contentDescription = "Закрыть")
            }
        }
        // PID + кнопки действий
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text  = "PID ${process.pid}",
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.weight(1f))
            // Kill button
            OutlinedButton(
                onClick = onKill,
                enabled = !isActionRunning,
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                modifier = Modifier.height(30.dp),
            ) {
                Icon(Icons.Outlined.Delete, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text("Kill", fontSize = 12.sp)
            }
            // Force Stop (только для пакетов)
            if (process.looksLikePackage) {
                OutlinedButton(
                    onClick = onForceStop,
                    enabled = !isActionRunning,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.height(30.dp),
                ) {
                    Icon(Icons.Outlined.StopCircle, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Force Stop", fontSize = 12.sp)
                }

                OutlinedButton(
                    onClick = onOpenPackageDetails,
                    enabled = !isActionRunning,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.height(30.dp),
                ) {
                    Icon(Icons.Outlined.OpenInNew, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("В Packages", fontSize = 12.sp)
                }
            }
        }
    }
}

// ── Вспомогательные composable ────────────────────────────────────────────────

@Composable
private fun DetailSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text  = title.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
        )
        Surface(
            shape          = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
            tonalElevation = 1.dp,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                content()
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text     = label,
            style    = MaterialTheme.typography.bodySmall,
            color    = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(
            text     = value,
            style    = MaterialTheme.typography.bodySmall.copy(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Medium,
            ),
            color    = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1.5f),
        )
    }
}

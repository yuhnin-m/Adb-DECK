package com.adbdeck.feature.systemmonitor.ui

import adbdeck.feature.system_monitor.generated.resources.*
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
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
import com.adbdeck.core.adb.api.monitoring.process.ProcessDetails
import com.adbdeck.core.adb.api.monitoring.process.ProcessInfo
import com.adbdeck.core.designsystem.AdbCornerRadius
import com.adbdeck.core.ui.buttons.AdbButtonSize
import com.adbdeck.core.ui.buttons.AdbButtonType
import com.adbdeck.core.ui.buttons.AdbFilledButton
import com.adbdeck.core.ui.buttons.AdbOutlinedButton
import com.adbdeck.core.ui.LoadingView
import com.adbdeck.core.utils.formatKb
import com.adbdeck.feature.systemmonitor.processes.ProcessDetailState
import org.jetbrains.compose.resources.stringResource

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
    val sectionMetrics = stringResource(Res.string.system_monitor_detail_section_metrics)
    val labelCpu = stringResource(Res.string.system_monitor_detail_label_cpu)
    val labelMemPercent = stringResource(Res.string.system_monitor_detail_label_mem_percent)
    val labelRss = stringResource(Res.string.system_monitor_detail_label_rss)
    val labelVsz = stringResource(Res.string.system_monitor_detail_label_vsz)
    val labelState = stringResource(Res.string.system_monitor_detail_label_state)
    val sectionMemoryDetailed = stringResource(Res.string.system_monitor_detail_section_memory_detailed)
    val labelPss = stringResource(Res.string.system_monitor_detail_label_pss)
    val labelUss = stringResource(Res.string.system_monitor_detail_label_uss)
    val labelHeapSize = stringResource(Res.string.system_monitor_detail_label_heap_size)
    val labelHeapAlloc = stringResource(Res.string.system_monitor_detail_label_heap_alloc)
    val labelHeapFree = stringResource(Res.string.system_monitor_detail_label_heap_free)
    val labelNativeHeap = stringResource(Res.string.system_monitor_detail_label_native_heap)
    val labelNativeAlloc = stringResource(Res.string.system_monitor_detail_label_native_alloc)
    val sectionProcess = stringResource(Res.string.system_monitor_detail_section_process)
    val labelPid = stringResource(Res.string.system_monitor_detail_label_pid)
    val labelPpid = stringResource(Res.string.system_monitor_detail_label_ppid)
    val labelThreads = stringResource(Res.string.system_monitor_detail_label_threads)
    val labelUser = stringResource(Res.string.system_monitor_detail_label_user)
    val labelOpenFiles = stringResource(Res.string.system_monitor_detail_label_open_files)
    val sectionCommandLine = stringResource(Res.string.system_monitor_detail_section_command_line)
    val sectionIdentification = stringResource(Res.string.system_monitor_detail_section_identification)
    val labelPackage = stringResource(Res.string.system_monitor_detail_label_package)

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
            DetailSection(title = sectionMetrics) {
                InfoRow(labelCpu, "%.2f%%".format(process.cpuPercent))
                InfoRow(labelMemPercent, "%.2f%%".format(process.memPercent))
                InfoRow(labelRss, process.rssKb.formatKb())
                InfoRow(labelVsz, process.vszKb.formatKb())
                InfoRow(labelState, "${process.state.symbol} — ${process.state.displayName}")
            }

            // Детальная информация (из /proc + dumpsys)
            when (detailState) {
                is ProcessDetailState.Loading ->
                    LoadingView(modifier = Modifier.fillMaxWidth().height(120.dp))

                is ProcessDetailState.Error ->
                    Text(
                        text = stringResource(
                            Res.string.system_monitor_detail_unavailable_format,
                            detailState.message,
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )

                is ProcessDetailState.Success -> {
                    val d = detailState.details
                    // Разбивка памяти
                    if (d.pssKb > 0L || d.ussKb > 0L || d.heapSizeKb > 0L) {
                        DetailSection(title = sectionMemoryDetailed) {
                            if (d.pssKb > 0L) InfoRow(labelPss, d.pssKb.formatKb())
                            if (d.ussKb > 0L) InfoRow(labelUss, d.ussKb.formatKb())
                            if (d.heapSizeKb > 0L) InfoRow(labelHeapSize, d.heapSizeKb.formatKb())
                            if (d.heapAllocKb > 0L) InfoRow(labelHeapAlloc, d.heapAllocKb.formatKb())
                            if (d.heapFreeKb > 0L) InfoRow(labelHeapFree, d.heapFreeKb.formatKb())
                            if (d.nativeHeapSizeKb > 0L) InfoRow(labelNativeHeap, d.nativeHeapSizeKb.formatKb())
                            if (d.nativeHeapAllocKb > 0L) InfoRow(labelNativeAlloc, d.nativeHeapAllocKb.formatKb())
                        }
                    }
                    // Системная информация
                    DetailSection(title = sectionProcess) {
                        InfoRow(labelPid, d.pid.toString())
                        InfoRow(labelPpid, d.ppid.toString())
                        InfoRow(labelThreads, d.threads.toString())
                        if (d.user.isNotEmpty()) InfoRow(labelUser, d.user)
                        if (d.openFiles > 0) InfoRow(labelOpenFiles, d.openFiles.toString())
                    }
                    // Командная строка
                    if (d.cmdline.isNotBlank()) {
                        DetailSection(title = sectionCommandLine) {
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
            DetailSection(title = sectionIdentification) {
                InfoRow(labelPid, process.pid.toString())
                InfoRow(labelPpid, process.ppid.takeIf { it > 0 }?.toString() ?: "—")
                InfoRow(labelUser, process.user.ifEmpty { "—" })
                if (process.packageName.isNotEmpty()) {
                    InfoRow(labelPackage, process.packageName)
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
    val closeDescription = stringResource(Res.string.system_monitor_detail_close)
    val pidLabel = stringResource(Res.string.system_monitor_detail_pid_format, process.pid)
    val actionsTitle = stringResource(Res.string.system_monitor_detail_actions_title)
    val killLabel = stringResource(Res.string.system_monitor_detail_action_kill)
    val forceStopLabel = stringResource(Res.string.system_monitor_detail_action_force_stop)
    val openPackagesLabel = stringResource(Res.string.system_monitor_detail_action_open_packages)
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
                Icon(Icons.Outlined.Close, contentDescription = closeDescription)
            }
        }

        // PID процесса
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .padding(bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = pidLabel,
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        // Действия в отдельном вертикальном блоке
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .padding(bottom = 8.dp),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
            ),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = actionsTitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                AdbOutlinedButton(
                    onClick = onKill,
                    text = killLabel,
                    leadingIcon = Icons.Outlined.Delete,
                    type = AdbButtonType.DANGER,
                    size = AdbButtonSize.SMALL,
                    cornerRadius = AdbCornerRadius.MEDIUM,
                    enabled = !isActionRunning,
                    fullWidth = true,
                )

                if (process.looksLikePackage) {
                    AdbFilledButton(
                        onClick = onForceStop,
                        text = forceStopLabel,
                        leadingIcon = Icons.Outlined.StopCircle,
                        type = AdbButtonType.DANGER,
                        size = AdbButtonSize.SMALL,
                        cornerRadius = AdbCornerRadius.MEDIUM,
                        enabled = !isActionRunning,
                        fullWidth = true,
                    )

                    AdbFilledButton(
                        onClick = onOpenPackageDetails,
                        text = openPackagesLabel,
                        leadingIcon = Icons.Outlined.OpenInNew,
                        size = AdbButtonSize.SMALL,
                        cornerRadius = AdbCornerRadius.MEDIUM,
                        enabled = !isActionRunning,
                        fullWidth = true,
                    )
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

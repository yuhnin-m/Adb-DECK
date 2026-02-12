package com.adbdeck.feature.systemmonitor.ui

import adbdeck.feature.system_monitor.generated.resources.Res
import adbdeck.feature.system_monitor.generated.resources.system_monitor_metric_cpu
import adbdeck.feature.system_monitor.generated.resources.system_monitor_metric_ram
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.unit.dp
import com.adbdeck.core.adb.api.monitoring.SystemSnapshot
import com.adbdeck.core.utils.formatKb
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun MetricsChartPanel(history: List<SystemSnapshot>) {
    val cpuValues = history.map { it.cpuPercent }
    val ramValues = history.map { it.ramPercent }
    val cpuLabel = stringResource(Res.string.system_monitor_metric_cpu)
    val ramLabel = stringResource(Res.string.system_monitor_metric_ram)

    val latestCpu = cpuValues.lastOrNull() ?: 0f
    val totalRam = history.lastOrNull()?.totalRamKb ?: 0L
    val usedRam = history.lastOrNull()?.usedRamKb ?: 0L

    val cpuColor = MaterialTheme.colorScheme.primary
    val ramColor = MaterialTheme.colorScheme.tertiary

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(90.dp)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        MetricCard(
            label = cpuLabel,
            valueText = "%.1f%%".format(latestCpu),
            values = cpuValues,
            lineColor = cpuColor,
            modifier = Modifier.weight(1f).fillMaxHeight(),
        )
        MetricCard(
            label = ramLabel,
            valueText = "${usedRam.formatKb()} / ${totalRam.formatKb()}",
            values = ramValues,
            lineColor = ramColor,
            modifier = Modifier.weight(1f).fillMaxHeight(),
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
        shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
        ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.width(90.dp)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = valueText,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = FontFamily.Monospace,
                    ),
                    color = lineColor,
                )
            }
            LineChart(
                values = values,
                lineColor = lineColor,
                modifier = Modifier.weight(1f).fillMaxHeight(),
            )
        }
    }
}

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
            values = values,
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

    val fillPath = Path().apply {
        moveTo(xOf(0), h)
        values.forEachIndexed { i, v ->
            lineTo(xOf(i), yOf(v))
        }
        lineTo(xOf(n - 1), h)
        close()
    }
    drawPath(fillPath, color = fillColor)

    val linePath = Path().apply {
        values.forEachIndexed { i, v ->
            val x = xOf(i)
            val y = yOf(v)
            if (i == 0) moveTo(x, y) else lineTo(x, y)
        }
    }
    drawPath(
        path = linePath,
        color = lineColor,
        style = Stroke(
            width = 1.5.dp.toPx(),
            cap = StrokeCap.Round,
            join = StrokeJoin.Round,
        ),
    )

    val lastX = xOf(n - 1)
    val lastY = yOf(values.last())
    drawCircle(color = lineColor, radius = 3.dp.toPx(), center = Offset(lastX, lastY))
}

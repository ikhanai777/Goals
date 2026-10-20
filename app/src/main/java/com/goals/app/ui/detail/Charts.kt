package com.goals.app.ui.detail

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.goals.app.ui.theme.GoalsTheme
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

/** 26-week grid of daily totals, one column per week, shaded by share of the daily target. */
@Composable
fun Heatmap(
    totals: Map<LocalDate, Double>,
    target: Double,
    today: LocalDate,
    weekStart: DayOfWeek,
    description: String,
    modifier: Modifier = Modifier,
) {
    val weeks = 26
    val firstDay = today.with(TemporalAdjusters.previousOrSame(weekStart)).minusWeeks((weeks - 1).toLong())
    val primary = MaterialTheme.colorScheme.primary
    val track = GoalsTheme.extra.track
    Canvas(
        modifier
            .fillMaxWidth()
            .height(112.dp)
            .semantics { contentDescription = description },
    ) {
        val gap = 3.dp.toPx()
        val cell = minOf((size.width - gap * (weeks - 1)) / weeks, (size.height - gap * 6) / 7)
        val radius = CornerRadius(2.dp.toPx())
        for (w in 0 until weeks) {
            for (d in 0 until 7) {
                val date = firstDay.plusDays((w * 7 + d).toLong())
                if (date.isAfter(today)) continue
                val share = ((totals[date] ?: 0.0) / target).coerceIn(0.0, 1.0).toFloat()
                val color = if (share == 0f) track else primary.copy(alpha = 0.25f + 0.75f * share)
                drawRoundRect(color, Offset(w * (cell + gap), d * (cell + gap)), Size(cell, cell), radius)
            }
        }
    }
}

/** Bars for recent periods, with the target as a dashed slate-blue line. */
@Composable
fun BarChart(
    values: List<Double>,
    labels: List<String>,
    target: Double,
    description: String,
    modifier: Modifier = Modifier,
) {
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val track = GoalsTheme.extra.track
    val max = maxOf(values.maxOrNull() ?: 0.0, target) * 1.1
    Column(modifier.semantics { contentDescription = description }) {
        Canvas(Modifier.fillMaxWidth().height(120.dp)) {
            val gap = 6.dp.toPx()
            val barWidth = (size.width - gap * (values.size - 1)) / values.size
            drawLine(track, Offset(0f, size.height), Offset(size.width, size.height), 1.dp.toPx())
            values.forEachIndexed { i, v ->
                val h = if (max == 0.0) 0f else (v / max * size.height).toFloat()
                val color = if (v >= target) primary else primary.copy(alpha = 0.45f)
                drawRoundRect(
                    color,
                    Offset(i * (barWidth + gap), size.height - h),
                    Size(barWidth, h),
                    CornerRadius(3.dp.toPx()),
                )
            }
            if (max > 0) {
                val y = (size.height - target / max * size.height).toFloat()
                drawLine(
                    secondary, Offset(0f, y), Offset(size.width, y), 1.5.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx())),
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Row(Modifier.fillMaxWidth()) {
            labels.forEachIndexed { i, label ->
                if (i > 0) Spacer(Modifier.width(6.dp))
                Text(
                    label,
                    style = MaterialTheme.typography.labelSmall,
                    color = GoalsTheme.extra.muted,
                    textAlign = TextAlign.Center,
                    softWrap = false,
                    overflow = TextOverflow.Visible,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

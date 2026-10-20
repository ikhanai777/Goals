package com.goals.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.MoreTime
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.goals.app.data.GoalType
import com.goals.app.domain.badge
import com.goals.app.domain.formatProgress
import com.goals.app.ui.GoalItem
import com.goals.app.ui.theme.AreaColors
import com.goals.app.ui.theme.GoalsTheme
import com.goals.app.ui.theme.NumericStyle

@Composable
fun ProgressBar(
    fraction: Float,
    modifier: Modifier = Modifier,
    height: Dp = 4.dp,
    color: Color = MaterialTheme.colorScheme.primary,
) {
    val animated by animateFloatAsState(fraction.coerceIn(0f, 1f), tween(200), label = "progress")
    Box(
        modifier
            .height(height)
            .clip(RoundedCornerShape(50))
            .background(GoalsTheme.extra.track),
    ) {
        Box(
            Modifier
                .fillMaxHeight()
                .fillMaxWidth(animated)
                .clip(RoundedCornerShape(50))
                .background(color),
        )
    }
}

@Composable
fun ProgressRing(
    fraction: Float,
    modifier: Modifier = Modifier,
    size: Dp = 28.dp,
    stroke: Dp = 3.dp,
    color: Color = MaterialTheme.colorScheme.primary,
    content: @Composable () -> Unit = {},
) {
    val animated by animateFloatAsState(fraction.coerceIn(0f, 1f), tween(200), label = "ring")
    val track = GoalsTheme.extra.track
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val w = stroke.toPx()
            val inset = w / 2
            val arcSize = Size(this.size.width - w, this.size.height - w)
            drawArc(track, 0f, 360f, false, Offset(inset, inset), arcSize, style = Stroke(w))
            drawArc(color, -90f, 360f * animated, false, Offset(inset, inset), arcSize, style = Stroke(w, cap = StrokeCap.Round))
        }
        content()
    }
}

/** Round checkbox for CHECK goals, "+" for the other types. */
@Composable
fun LogControl(item: GoalItem, onLog: () -> Unit, modifier: Modifier = Modifier) {
    val haptics = LocalHapticFeedback.current
    val scheme = MaterialTheme.colorScheme
    val met = item.progress.met
    val description = "${item.goal.title}, ${item.goal.formatProgress(item.progress.value)}, " +
        when {
            item.goal.type == GoalType.CHECK && met -> "mark not done"
            item.goal.type == GoalType.CHECK -> "mark done"
            else -> "log progress"
        }
    Box(
        modifier
            .size(48.dp)
            .clip(CircleShape)
            .clickable(role = Role.Button) {
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onLog()
            }
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        when {
            item.goal.type == GoalType.CHECK && met -> Box(
                Modifier.size(24.dp).clip(CircleShape).background(scheme.primary),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Rounded.Check, null, Modifier.size(16.dp), tint = scheme.onPrimary) }
            item.progress.skipped -> Box(
                Modifier.size(24.dp).clip(CircleShape).border(1.5.dp, scheme.secondary, CircleShape),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Rounded.SkipNext, null, Modifier.size(14.dp), tint = scheme.secondary) }
            item.goal.type == GoalType.CHECK -> Box(
                Modifier.size(24.dp).clip(CircleShape).border(1.5.dp, scheme.outline, CircleShape),
            )
            else -> Box(
                Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(if (met) scheme.primary else scheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    when {
                        met -> Icons.Rounded.Check
                        item.goal.type == GoalType.DURATION -> Icons.Rounded.MoreTime
                        else -> Icons.Rounded.Add
                    },
                    null,
                    Modifier.size(18.dp),
                    tint = if (met) scheme.onPrimary else scheme.onPrimaryContainer,
                )
            }
        }
    }
}

@Composable
fun AreaBar(colorIndex: Int?, modifier: Modifier = Modifier) {
    Box(
        modifier
            .width(4.dp)
            .fillMaxHeight()
            .clip(RoundedCornerShape(2.dp))
            .background(colorIndex?.let { AreaColors[it % AreaColors.size] } ?: Color.Transparent),
    )
}

@Composable
fun StreakBadge(streak: Int) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            Icons.Rounded.LocalFireDepartment, null,
            Modifier.size(12.dp), tint = MaterialTheme.colorScheme.secondary,
        )
        Text(
            "$streak",
            style = NumericStyle.copy(fontSize = MaterialTheme.typography.labelSmall.fontSize),
            color = MaterialTheme.colorScheme.secondary,
        )
    }
}

/** The compact Today row: area bar, icon, title, value, progress bar, streak and log control. */
@Composable
fun GoalRowContent(
    item: GoalItem,
    showStreak: Boolean,
    onOpen: () -> Unit,
    onLog: () -> Unit,
    modifier: Modifier = Modifier,
    meta: String? = null,
) {
    val scheme = MaterialTheme.colorScheme
    val p = item.progress
    val barColor = when {
        p.overdue -> scheme.error
        p.met -> GoalsTheme.extra.success
        else -> scheme.primary
    }
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = GoalsTheme.rowHeight)
            .clip(MaterialTheme.shapes.small)
            .background(scheme.surfaceContainerLow)
            .clickable(onClick = onOpen)
            .padding(start = 0.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AreaBar(item.area?.colorIndex, Modifier.height(GoalsTheme.rowHeight - 16.dp))
        Spacer(Modifier.width(10.dp))
        Icon(
            iconFor(item.goal.icon), null, Modifier.size(20.dp),
            tint = if (p.done) GoalsTheme.extra.muted else scheme.onSurface,
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f).padding(vertical = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    item.goal.title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (p.done) GoalsTheme.extra.muted else scheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(8.dp))
                Text(item.goal.formatProgress(p.value), style = NumericStyle, color = GoalsTheme.extra.muted)
            }
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                ProgressBar(p.fraction, Modifier.weight(1f), color = barColor)
                if (showStreak && p.currentStreak >= 2) {
                    Spacer(Modifier.width(8.dp))
                    StreakBadge(p.currentStreak)
                }
            }
            if (meta != null && GoalsTheme.rowHeight > 48.dp) {
                Spacer(Modifier.height(4.dp))
                Text(
                    meta,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (p.overdue) scheme.error else GoalsTheme.extra.muted,
                    maxLines = 1,
                )
            }
        }
        Spacer(Modifier.width(4.dp))
        LogControl(item, onLog)
    }
}

/** [GoalRowContent] with swipe right to log and swipe left to skip. Swipes snap back after acting. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeGoalRow(
    item: GoalItem,
    showStreak: Boolean,
    onOpen: () -> Unit,
    onLog: () -> Unit,
    onQuickLog: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier,
    meta: String? = null,
) {
    val haptics = LocalHapticFeedback.current
    val state = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            when (value) {
                SwipeToDismissBoxValue.StartToEnd -> { haptics.performHapticFeedback(HapticFeedbackType.LongPress); onQuickLog() }
                SwipeToDismissBoxValue.EndToStart -> { haptics.performHapticFeedback(HapticFeedbackType.LongPress); onSkip() }
                SwipeToDismissBoxValue.Settled -> Unit
            }
            false
        },
    )
    SwipeToDismissBox(
        state = state,
        modifier = modifier.semantics {
            customActions = listOf(
                CustomAccessibilityAction("Log") { onQuickLog(); true },
                CustomAccessibilityAction("Skip") { onSkip(); true },
            )
        },
        enableDismissFromStartToEnd = !(item.goal.type == GoalType.CHECK && item.progress.met),
        enableDismissFromEndToStart = !item.progress.done,
        backgroundContent = {
            val toEnd = state.dismissDirection == SwipeToDismissBoxValue.StartToEnd
            val scheme = MaterialTheme.colorScheme
            Row(
                Modifier
                    .fillMaxSize()
                    .clip(MaterialTheme.shapes.small)
                    .background(if (toEnd) scheme.primary else scheme.secondary)
                    .padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = if (toEnd) Arrangement.Start else Arrangement.End,
            ) {
                val tint = if (toEnd) scheme.onPrimary else scheme.onSecondary
                Icon(if (toEnd) Icons.Rounded.Check else Icons.Rounded.SkipNext, null, Modifier.size(20.dp), tint = tint)
                Spacer(Modifier.width(6.dp))
                Text(if (toEnd) "Log" else "Skip", style = MaterialTheme.typography.labelMedium, color = tint)
            }
        },
    ) {
        GoalRowContent(item, showStreak, onOpen, onLog, meta = meta)
    }
}

@Composable
fun SectionHeader(
    title: String,
    count: String?,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurface,
    expanded: Boolean? = null,
    onToggle: (() -> Unit)? = null,
) {
    Row(
        modifier
            .fillMaxWidth()
            .then(if (onToggle != null) Modifier.clickable(onClick = onToggle) else Modifier)
            .padding(top = 16.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = MaterialTheme.typography.titleSmall, color = color)
        if (count != null) {
            Spacer(Modifier.width(6.dp))
            Text(count, style = NumericStyle, color = GoalsTheme.extra.muted)
        }
        Spacer(Modifier.weight(1f))
        if (expanded != null) {
            Icon(
                Icons.Rounded.ExpandMore, if (expanded) "Collapse" else "Expand",
                Modifier.size(20.dp).rotate(if (expanded) 180f else 0f),
                tint = GoalsTheme.extra.muted,
            )
        }
    }
}

@Composable
fun CadenceBadge(item: GoalItem) {
    Box(
        Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .padding(horizontal = 5.dp, vertical = 1.dp),
    ) {
        Text(
            item.goal.cadence.badge(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
        )
    }
}

@Composable
fun StatTile(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier
            .clip(MaterialTheme.shapes.small)
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(horizontal = 10.dp, vertical = 8.dp),
    ) {
        Text(value, style = MaterialTheme.typography.titleMedium.copy(fontFeatureSettings = "tnum"))
        Text(label, style = MaterialTheme.typography.labelSmall, color = GoalsTheme.extra.muted, maxLines = 1)
    }
}

@Composable
fun EmptyState(text: String, action: String?, onAction: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxWidth().padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text, style = MaterialTheme.typography.bodyMedium, color = GoalsTheme.extra.muted)
        if (action != null) {
            androidx.compose.material3.TextButton(onClick = onAction) { Text(action) }
        }
    }
}

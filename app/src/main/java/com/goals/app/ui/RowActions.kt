package com.goals.app.ui

/** What a goal row can do: open its detail, tap the log control, swipe right to log, swipe left to skip. */
data class RowActions(
    val onOpen: (GoalItem) -> Unit,
    val onLog: (GoalItem) -> Unit,
    val onQuickLog: (GoalItem) -> Unit,
    val onSkip: (GoalItem) -> Unit,
)

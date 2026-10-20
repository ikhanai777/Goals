package com.goals.app.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Today
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.goals.app.ui.theme.GoalsTheme
import com.goals.app.ui.components.glow
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.goals.app.ui.components.ConfirmDialog
import com.goals.app.ui.components.LogDialog
import com.goals.app.ui.detail.GoalDetailScreen
import com.goals.app.ui.editor.GoalEditorScreen
import com.goals.app.ui.editor.ProjectEditorScreen
import com.goals.app.ui.editor.QuickAddContent
import com.goals.app.ui.goals.GoalsScreen
import com.goals.app.ui.project.ProjectDetailScreen
import com.goals.app.ui.settings.SettingsScreen
import com.goals.app.ui.today.TodayScreen
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.time.LocalDate

private data class Tab(val route: String, val label: String, val icon: ImageVector, val selectedIcon: ImageVector)

private val Tabs = listOf(
    Tab("today", "Today", Icons.Outlined.Today, Icons.Rounded.Today),
    Tab("goals", "Goals", Icons.Outlined.Flag, Icons.Rounded.Flag),
    Tab("settings", "Settings", Icons.Outlined.Settings, Icons.Rounded.Settings),
)

/** Where a log dialog was opened from: a row tap logs for today, "Add past entry" lets the user pick the date. */
private data class LogRequest(val goalId: String, val allowDate: Boolean)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalsAppRoot(
    vm: GoalsViewModel,
    state: AppState,
    versionName: String,
    openGoalId: Flow<String>,
) {
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val route = backStack?.destination?.route
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var todayFilter by rememberSaveable { mutableStateOf(TodayFilter.TODAY) }
    var quickAdd by remember { mutableStateOf<GoalDraft?>(null) }
    var editorDraft by remember { mutableStateOf<GoalDraft?>(null) }
    var logRequest by remember { mutableStateOf<LogRequest?>(null) }
    var addMenu by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf<(() -> Unit)?>(null) }

    LaunchedEffect(Unit) {
        vm.messages.collect { message ->
            val result = snackbar.showSnackbar(
                message.text,
                actionLabel = if (message.undo != null) "Undo" else null,
                duration = SnackbarDuration.Short,
            )
            if (result == SnackbarResult.ActionPerformed) message.undo?.invoke()
        }
    }
    LaunchedEffect(Unit) { openGoalId.collect { nav.navigate("goal/$it") } }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri?.let(vm::exportTo)
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let(vm::importFrom)
    }

    val rows = RowActions(
        onOpen = { nav.navigate("goal/${it.goal.id}") },
        onLog = { item -> if (!vm.tapLog(item)) logRequest = LogRequest(item.goal.id, allowDate = false) },
        onQuickLog = vm::quickLog,
        onSkip = vm::skip,
    )

    fun newGoal(projectId: String? = null) {
        quickAdd = GoalDraft(startDate = state.today, projectId = projectId)
    }

    fun openEditor(draft: GoalDraft) {
        editorDraft = draft
        nav.navigate("edit-goal")
    }

    val topLevel = route in Tabs.map { it.route }
    Scaffold(
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onBackground,
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            if (topLevel) {
                AppNavigationBar(route) { target ->
                    nav.navigate(target) {
                        popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            }
        },
        floatingActionButton = {
            if (route == "today" || route == "goals") {
                Box {
                    AddButton(
                        onClick = { if (route == "goals") addMenu = true else newGoal() },
                        onLongClick = { addMenu = true },
                    )
                    DropdownMenu(addMenu, onDismissRequest = { addMenu = false }) {
                        DropdownMenuItem(
                            text = { Text("Goal") },
                            leadingIcon = { Icon(Icons.Rounded.CheckCircle, null) },
                            onClick = { addMenu = false; newGoal() },
                        )
                        DropdownMenuItem(
                            text = { Text("Project") },
                            leadingIcon = { Icon(Icons.Rounded.Folder, null) },
                            onClick = { addMenu = false; nav.navigate("edit-project") },
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(nav, startDestination = "today", modifier = Modifier.padding(padding)) {
            composable("today") {
                TodayScreen(
                    state, todayFilter, { todayFilter = it }, rows,
                    onToggleMilestone = vm::toggleMilestone,
                    onOpenProject = { nav.navigate("project/$it") },
                    onAdd = { newGoal() },
                )
            }
            composable("goals") {
                GoalsScreen(
                    state,
                    onOpenGoal = { nav.navigate("goal/$it") },
                    onOpenProject = { nav.navigate("project/$it") },
                    onAdd = { newGoal() },
                )
            }
            composable("settings") {
                SettingsScreen(
                    state.settings, versionName, vm::updateSettings,
                    onExport = { exportLauncher.launch("goals-backup-${LocalDate.now()}.json") },
                    onImport = { importLauncher.launch(arrayOf("application/json", "text/plain", "*/*")) },
                    onClearAll = vm::clearAll,
                )
            }
            composable("goal/{id}") { entry ->
                val item = entry.arguments?.getString("id")?.let(state::goal)
                if (item == null) {
                    if (state.loaded) LaunchedEffect(Unit) { nav.popBackStack() }
                    return@composable
                }
                GoalDetailScreen(
                    state, item,
                    onBack = { nav.popBackStack() },
                    onEdit = { openEditor(GoalDraft.from(item)) },
                    onLog = { if (!vm.tapLog(item)) logRequest = LogRequest(item.goal.id, allowDate = false) },
                    onAddPastEntry = { logRequest = LogRequest(item.goal.id, allowDate = true) },
                    onDeleteEntry = vm::deleteEntry,
                    onDuplicate = { vm.duplicate(item) },
                    onArchive = { vm.archive(item, it) },
                    onDelete = {
                        confirmDelete = {
                            nav.popBackStack()
                            vm.deleteGoal(item)
                        }
                    },
                    onOpenProject = { nav.navigate("project/$it") },
                )
            }
            composable("project/{id}") { entry ->
                val item = entry.arguments?.getString("id")?.let(state::project)
                if (item == null) {
                    if (state.loaded) LaunchedEffect(Unit) { nav.popBackStack() }
                    return@composable
                }
                ProjectDetailScreen(
                    state, item, rows,
                    onBack = { nav.popBackStack() },
                    onEdit = { nav.navigate("edit-project?id=${item.project.id}") },
                    onAddGoal = { newGoal(item.project.id) },
                    onArchive = { vm.archiveProject(item.project, it) },
                    onDelete = {
                        confirmDelete = {
                            nav.popBackStack()
                            vm.deleteProject(item.project)
                        }
                    },
                    onSaveMilestone = vm::saveMilestone,
                    onToggleMilestone = vm::toggleMilestone,
                    onDeleteMilestone = vm::deleteMilestone,
                )
            }
            composable("edit-goal") {
                val draft = editorDraft
                if (draft == null) {
                    LaunchedEffect(Unit) { nav.popBackStack() }
                    return@composable
                }
                GoalEditorScreen(
                    state, draft,
                    onBack = { nav.popBackStack() },
                    onSave = {
                        vm.saveGoal(it)
                        nav.popBackStack()
                    },
                )
            }
            composable("edit-project?id={id}") { entry ->
                val id = entry.arguments?.getString("id")
                ProjectEditorScreen(
                    state, id?.let(state::project)?.project,
                    onBack = { nav.popBackStack() },
                    onSave = {
                        vm.saveProject(it)
                        nav.popBackStack()
                        if (id == null) nav.navigate("project/${it.id}")
                    },
                )
            }
        }
    }

    quickAdd?.let { draft ->
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(onDismissRequest = { quickAdd = null }, sheetState = sheetState) {
            QuickAddContent(
                draft, state.today,
                onChange = { quickAdd = it },
                onMoreOptions = {
                    scope.launch { sheetState.hide() }.invokeOnCompletion {
                        quickAdd = null
                        openEditor(draft)
                    }
                },
                onCreate = {
                    vm.saveGoal(draft)
                    scope.launch { sheetState.hide() }.invokeOnCompletion { quickAdd = null }
                },
            )
        }
    }

    logRequest?.let { request ->
        val item = state.goal(request.goalId)
        if (item == null) {
            logRequest = null
        } else {
            LogDialog(
                item, state.today, request.allowDate,
                onDismiss = { logRequest = null },
                onConfirm = { value, date ->
                    logRequest = null
                    vm.log(item, value, date)
                },
            )
        }
    }

    confirmDelete?.let { action ->
        ConfirmDialog(
            "Delete?", "This permanently deletes it and its history.", "Delete",
            onDismiss = { confirmDelete = null },
        ) {
            confirmDelete = null
            action()
        }
    }
}

@Composable
fun AppNavigationBar(route: String?, onNavigate: (String) -> Unit) {
    val style = GoalsTheme.style
    NavigationBar(
        containerColor = if (style.fireIce) Color(0xEE0F1724) else MaterialTheme.colorScheme.background,
        tonalElevation = 0.dp,
    ) {
        Tabs.forEach { tab ->
            val selected = route == tab.route
            NavigationBarItem(
                selected = selected,
                onClick = { onNavigate(tab.route) },
                icon = { Icon(if (selected) tab.selectedIcon else tab.icon, null) },
                label = { Text(tab.label, style = MaterialTheme.typography.labelMedium) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = if (style.fireIce) style.ice else MaterialTheme.colorScheme.primary,
                    selectedTextColor = if (style.fireIce) Color.White else MaterialTheme.colorScheme.primary,
                    indicatorColor = if (style.fireIce) style.fire.copy(alpha = 0.22f) else MaterialTheme.colorScheme.primaryContainer,
                ),
            )
        }
    }
}

/** Cobalt add button: tap for the default action, long-press for the Goal / Project menu. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AddButton(onClick: () -> Unit, onLongClick: () -> Unit) {
    val style = GoalsTheme.style
    Surface(
        shape = MaterialTheme.shapes.large,
        color = if (style.fireIce) Color.Transparent else MaterialTheme.colorScheme.primary,
        contentColor = if (style.fireIce) Color.White else MaterialTheme.colorScheme.onPrimary,
        shadowElevation = if (style.fireIce) 0.dp else 2.dp,
        modifier = Modifier
            .then(if (style.fireIce) Modifier.glow(style.fire, 12.dp, spread = 10.dp, strength = 1.4f) else Modifier)
            .size(52.dp)
            .then(
                if (style.fireIce) {
                    Modifier.background(
                        Brush.linearGradient(listOf(Color(0xFF7FE0FF), style.ice, style.fire, Color(0xFFFF3D00))),
                        MaterialTheme.shapes.large,
                    )
                } else {
                    Modifier
                },
            )
            .semantics { contentDescription = "Add" }
            .combinedClickable(role = Role.Button, onClick = onClick, onLongClick = onLongClick),
    ) {
        Box(contentAlignment = Alignment.Center) { Icon(Icons.Rounded.Add, null) }
    }
}

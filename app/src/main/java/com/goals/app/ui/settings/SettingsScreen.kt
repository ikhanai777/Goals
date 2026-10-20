package com.goals.app.ui.settings

import android.os.Build
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.goals.app.data.AppSettings
import com.goals.app.data.Density
import com.goals.app.data.ThemeMode
import com.goals.app.data.ThemeStyle
import com.goals.app.ui.components.ConfirmDialog
import com.goals.app.ui.components.TimePickerModal
import com.goals.app.ui.theme.GoalsTheme
import java.time.DayOfWeek

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: AppSettings,
    versionName: String,
    onUpdate: ((AppSettings) -> AppSettings) -> Unit,
    onExport: () -> Unit,
    onImport: () -> Unit,
    onClearAll: () -> Unit,
) {
    var confirmClear by remember { mutableStateOf(false) }
    var confirmImport by remember { mutableStateOf(false) }
    var pickingReminder by remember { mutableStateOf(false) }

    Scaffold(

        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onBackground,
        contentWindowInsets = WindowInsets(0),
        topBar = {
            TopAppBar(
                title = { Text("Settings", style = MaterialTheme.typography.titleLarge) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(bottom = 32.dp),
        ) {
            Group("General")
            Choice(
                "Week starts on", settings.weekStart,
                listOf(DayOfWeek.MONDAY, DayOfWeek.SUNDAY), { it.name.lowercase().replaceFirstChar(Char::titlecase) },
            ) { v -> onUpdate { it.copy(weekStart = v) } }
            Choice(
                "Day starts at", settings.dayStartHour, (0..6).toList(),
                { if (it == 0) "Midnight" else "%d:00 AM".format(it) },
            ) { v -> onUpdate { it.copy(dayStartHour = v) } }

            Group("Appearance")
            Choice(
                "Style", settings.style, ThemeStyle.entries,
                { if (it == ThemeStyle.FIRE_ICE) "Fire & Ice" else "Classic" },
            ) { v -> onUpdate { it.copy(style = v) } }
            val classic = settings.style == ThemeStyle.CLASSIC
            if (classic) {
                Choice(
                    "Theme", settings.theme, ThemeMode.entries,
                    { it.name.lowercase().replaceFirstChar(Char::titlecase) },
                ) { v -> onUpdate { it.copy(theme = v) } }
            }
            Choice(
                "Density", settings.density, Density.entries,
                { it.name.lowercase().replaceFirstChar(Char::titlecase) },
            ) { v -> onUpdate { it.copy(density = v) } }
            if (classic && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                Toggle("Dynamic color", "Use wallpaper colors instead of cobalt", settings.dynamicColor) { v ->
                    onUpdate { it.copy(dynamicColor = v) }
                }
            }
            Toggle("Show streaks", null, settings.showStreaks) { v -> onUpdate { it.copy(showStreaks = v) } }

            Group("Notifications")
            Toggle("Reminders", "Per-goal reminders with Done and Snooze actions", settings.notificationsEnabled) { v ->
                onUpdate { it.copy(notificationsEnabled = v) }
            }
            Item(
                "Default reminder time",
                "%02d:%02d".format(settings.defaultReminderMinutes / 60, settings.defaultReminderMinutes % 60),
            ) { pickingReminder = true }

            Group("Data")
            Item("Export", "Save all data as JSON", onClick = onExport)
            Item("Import", "Replace all data from a JSON export") { confirmImport = true }
            Item("Clear all data", null, color = MaterialTheme.colorScheme.error) { confirmClear = true }

            Group("About")
            Item("Version", versionName) {}
            Item("Privacy", "All data stays on this device. No account, no analytics.") {}
        }
    }
    if (confirmClear) {
        ConfirmDialog(
            "Clear all data?", "Every goal, project, entry and reminder will be deleted. This can't be undone.",
            "Clear", onDismiss = { confirmClear = false },
        ) { confirmClear = false; onClearAll() }
    }
    if (confirmImport) {
        ConfirmDialog(
            "Replace all data?", "Importing replaces everything currently in the app with the file's contents.",
            "Choose file", onDismiss = { confirmImport = false },
        ) { confirmImport = false; onImport() }
    }
    if (pickingReminder) {
        TimePickerModal(
            settings.defaultReminderMinutes / 60, settings.defaultReminderMinutes % 60,
            onDismiss = { pickingReminder = false },
        ) { h, m -> pickingReminder = false; onUpdate { it.copy(defaultReminderMinutes = h * 60 + m) } }
    }
}

@Composable
private fun Group(title: String) {
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.padding(top = 8.dp))
    Text(
        title,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 4.dp),
    )
}

@Composable
private fun Item(title: String, subtitle: String?, color: Color = Color.Unspecified, onClick: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        Text(title, style = MaterialTheme.typography.bodyMedium, color = if (color == Color.Unspecified) MaterialTheme.colorScheme.onSurface else color)
        if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = GoalsTheme.extra.muted)
    }
}

@Composable
private fun Toggle(title: String, subtitle: String?, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable { onChange(!checked) }.padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium)
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = GoalsTheme.extra.muted)
        }
        Spacer(Modifier.width(12.dp))
        Switch(checked, onChange)
    }
}

@Composable
private fun <T> Choice(title: String, value: T, options: List<T>, label: (T) -> String, onPick: (T) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Row(
        Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable { open = true }.padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Column {
            Text(label(value), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
            DropdownMenu(open, onDismissRequest = { open = false }) {
                options.forEach { option ->
                    DropdownMenuItem(text = { Text(label(option)) }, onClick = { open = false; onPick(option) })
                }
            }
        }
    }
}

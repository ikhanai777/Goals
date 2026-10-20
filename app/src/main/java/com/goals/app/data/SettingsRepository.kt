package com.goals.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.DayOfWeek

enum class ThemeMode { SYSTEM, LIGHT, DARK }

enum class Density { COMFORTABLE, COMPACT }

/** Visual style. FIRE_ICE is a dark-only style with glowing fire and ice accents. */
enum class ThemeStyle { CLASSIC, FIRE_ICE }

data class AppSettings(
    val weekStart: DayOfWeek = DayOfWeek.MONDAY,
    /** Hour (0–23) at which a new day begins; entries before it count toward the previous day. */
    val dayStartHour: Int = 0,
    val theme: ThemeMode = ThemeMode.SYSTEM,
    val style: ThemeStyle = ThemeStyle.CLASSIC,
    val density: Density = Density.COMFORTABLE,
    val dynamicColor: Boolean = false,
    val showStreaks: Boolean = true,
    val notificationsEnabled: Boolean = true,
    /** Minutes after midnight used to prefill new reminders. */
    val defaultReminderMinutes: Int = 9 * 60,
)

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {
    private object Keys {
        val weekStart = stringPreferencesKey("week_start")
        val dayStartHour = intPreferencesKey("day_start_hour")
        val theme = stringPreferencesKey("theme")
        val style = stringPreferencesKey("style")
        val density = stringPreferencesKey("density")
        val dynamicColor = booleanPreferencesKey("dynamic_color")
        val showStreaks = booleanPreferencesKey("show_streaks")
        val notifications = booleanPreferencesKey("notifications")
        val defaultReminder = intPreferencesKey("default_reminder")
    }

    val settings: Flow<AppSettings> = context.dataStore.data.map(::read)

    suspend fun update(transform: (AppSettings) -> AppSettings) {
        context.dataStore.edit { p ->
            val next = transform(read(p))
            p[Keys.weekStart] = next.weekStart.name
            p[Keys.dayStartHour] = next.dayStartHour
            p[Keys.theme] = next.theme.name
            p[Keys.style] = next.style.name
            p[Keys.density] = next.density.name
            p[Keys.dynamicColor] = next.dynamicColor
            p[Keys.showStreaks] = next.showStreaks
            p[Keys.notifications] = next.notificationsEnabled
            p[Keys.defaultReminder] = next.defaultReminderMinutes
        }
    }

    private fun read(p: Preferences): AppSettings {
        val d = AppSettings()
        return AppSettings(
            weekStart = p[Keys.weekStart]?.let(DayOfWeek::valueOf) ?: d.weekStart,
            dayStartHour = p[Keys.dayStartHour] ?: d.dayStartHour,
            theme = p[Keys.theme]?.let(ThemeMode::valueOf) ?: d.theme,
            style = p[Keys.style]?.let(ThemeStyle::valueOf) ?: d.style,
            density = p[Keys.density]?.let(Density::valueOf) ?: d.density,
            dynamicColor = p[Keys.dynamicColor] ?: d.dynamicColor,
            showStreaks = p[Keys.showStreaks] ?: d.showStreaks,
            notificationsEnabled = p[Keys.notifications] ?: d.notificationsEnabled,
            defaultReminderMinutes = p[Keys.defaultReminder] ?: d.defaultReminderMinutes,
        )
    }
}

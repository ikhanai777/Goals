# Goals — Android App Specification

**Version:** 1.0 (draft)
**Date:** 2026-10-20
**Platform:** Android 8.0+ (API 26+), phones first, tablet-adaptive
**Working name:** Goals

---

## 1. Product summary

Goals is a compact, offline-first Android app for setting projects and goals and tracking progress on them on yearly, monthly, weekly, daily, or custom cadences. It is built for people who want structure without clutter: one tap to log progress, one screen to see where they stand, and a short form to create anything.

### 1.1 Design principles

1. **Compact by default.** Dense but readable lists. No hero banners or decorative cards. Every pixel shows status or takes an action.
2. **One tap to log.** The most common action, marking progress, never takes more than one tap from the Today screen.
3. **Progressive disclosure.** Creating a goal needs a title and a cadence. Everything else (units, reminders, milestones, color) sits under "More options".
4. **Calm palette.** Slate blue, cobalt blue, black, and white. Color carries meaning (progress, state); it is not decoration.
5. **Offline-first and private.** All data is stored on the device. Sync and backup are opt-in.

### 1.2 Target users

- Individuals tracking personal goals (fitness, reading, savings, learning).
- Freelancers and students tracking projects with milestones and deadlines.
- Habit builders who want daily and weekly streaks without a gamified UI.

### 1.3 Out of scope (v1)

- Team or shared goals, social feeds, leaderboards.
- AI coaching or suggestions.
- iOS and web clients.

---

## 2. Core concepts and terminology

| Concept | Definition |
|---|---|
| **Area** | Optional top-level grouping (e.g. Health, Work, Finance). Has a name and an icon. |
| **Project** | A container with a start date, an optional due date, and milestones. It holds goals and tasks. Progress is the roll-up of its children. |
| **Goal** | A measurable target tracked on a cadence. It can belong to a project or stand alone. |
| **Tracker** | The cadence and measurement rule attached to a goal (what counts and how often). |
| **Period** | One instance of a cadence (e.g. "Week 43, 2026"). Progress resets each period; history is kept. |
| **Entry** | A single logged unit of progress, with a timestamp and an optional note. |
| **Milestone** | A dated checkpoint inside a project. |
| **Streak** | The number of consecutive periods in which the target was met. |

### 2.1 Goal types

| Type | Example | Measurement | Logging control |
|---|---|---|---|
| **Check (yes/no)** | "Meditate" | Done or not done per period | Checkbox |
| **Count** | "Drink 8 glasses of water" | Integer toward a target | Stepper (− / +) with long-press for custom amount |
| **Quantity** | "Run 20 km per week" | Decimal value with a unit | Number input |
| **Duration** | "Study 5 h per week" | Time | Start/stop timer or manual minutes |
| **Milestone / %** | "Finish thesis draft" | 0–100 % or a checklist of sub-steps | Slider or checklist |
| **Limit (reduce)** | "Under 2 coffees per day" | Stay at or below a ceiling | Stepper; turns to warning state past the limit |

### 2.2 Cadences (tracker options)

| Cadence | Period boundary | Notes |
|---|---|---|
| **Daily** | Local midnight (configurable "day starts at" hour, e.g. 4 AM for night owls) | Can be restricted to specific weekdays (Mon–Fri). |
| **Weekly** | Week start day from settings (Mon or Sun) | Target can be "X times per week" or a total amount. |
| **Monthly** | 1st of month | Target can be "X times per month" or a total amount. |
| **Yearly** | Jan 1, or a custom fiscal year start | Shows month-by-month breakdown. |
| **Custom** | User-defined | Options: every N days, every N weeks on chosen days, specific dates, or a one-off date range (e.g. "90-day challenge"). |
| **No cadence** | Single deadline | For one-off goals ("Pass driving test by Mar 1"). |

**Cascading targets (optional).** A yearly goal can auto-split into monthly, weekly, or daily sub-targets (e.g. "Read 24 books/year" becomes "2/month"). The user can accept the split or edit each sub-target. Progress logged on a sub-target rolls up to its parent.

---

## 3. Visual design system

### 3.1 Color palette

Core brand colors:

| Token | Light theme | Dark theme | Use |
|---|---|---|---|
| `primary` (Cobalt) | `#0047AB` | `#5B8DEF` | Primary buttons, FAB, active tab, progress fill, links |
| `onPrimary` | `#FFFFFF` | `#0A0F1A` | Text and icons on primary |
| `secondary` (Slate blue) | `#6A5ACD` | `#A79BF0` | Secondary progress, chips, selected filters, streak accents |
| `onSecondary` | `#FFFFFF` | `#0A0F1A` | Text and icons on secondary |
| `background` | `#FFFFFF` | `#0A0B0F` (near-black) | App background |
| `surface` | `#F5F6FA` | `#14161C` | List rows, sheets, cards |
| `surfaceVariant` | `#E6E8F0` | `#1F222B` | Progress track, dividers, input fields |
| `onBackground` / `onSurface` | `#0A0B0F` | `#F2F3F7` | Primary text |
| `onSurfaceMuted` | `#5B6070` | `#9AA0B0` | Secondary text, captions, metadata |
| `outline` | `#D3D6E0` | `#2C303B` | 1 dp borders and dividers |

Semantic colors (used sparingly, only for state):

| Token | Light | Dark | Use |
|---|---|---|---|
| `success` | `#1F7A4D` | `#5CC38E` | Target met, completed |
| `warning` | `#B26A00` | `#F0B44C` | Behind pace, limit exceeded |
| `error` | `#B3261E` | `#F2B8B5` | Validation errors, overdue |

Rules:

- Cobalt is the single action color. Only one cobalt filled button per screen.
- Slate blue is for secondary emphasis: chips, streaks, the "planned" portion of progress bars.
- No gradients and no drop shadows heavier than elevation 1. Separation comes from `surface` vs. `background` and 1 dp outlines.
- All text and color pairs meet WCAG AA (4.5:1 for body text, 3:1 for large text and UI glyphs) in both themes. Cobalt `#0047AB` on white is about 8.4:1; slate blue `#6A5ACD` on white is about 5.3:1.
- Material You dynamic color is **off** by default to keep the brand palette, with an opt-in toggle in Settings.
- Users can tag an Area with one of 8 muted accent colors, shown only as a 4 dp leading bar on rows, never as a fill.

### 3.2 Typography

Font: **Inter** (bundled), falling back to Roboto.

| Style | Size / line height | Weight | Use |
|---|---|---|---|
| `titleLarge` | 20 / 28 sp | SemiBold | Screen titles |
| `titleMedium` | 16 / 22 sp | SemiBold | Section headers, goal title in detail |
| `bodyMedium` | 14 / 20 sp | Regular | Row titles, body text |
| `bodySmall` | 12 / 16 sp | Regular | Metadata, captions |
| `labelMedium` | 12 / 16 sp | Medium | Chips, buttons, tabs |
| `numeric` | 14 sp, tabular figures | Medium | Progress values ("12 / 20 km") |

Respect system font scaling up to 200 %. Rows grow in height; they do not truncate numbers.

### 3.3 Spacing, shape, density

- 4 dp base grid. Standard padding: 16 dp screen edge, 12 dp inside rows, 8 dp between inline elements.
- **Compact list row:** 56 dp tall (single line plus meta), 48 dp minimum touch target on the log control.
- Corner radius: 8 dp for rows and cards, 12 dp for bottom sheets, full pill for chips and progress bars.
- Progress bar: 4 dp tall in lists, 8 dp in detail view.
- Icons: Material Symbols Rounded, 20 dp in rows, 24 dp in the app bar.
- Density setting: **Comfortable** (56 dp rows) or **Compact** (48 dp rows, hides secondary meta line).

### 3.4 Motion

- 150–200 ms standard easing for state changes. Progress fills animate from the previous value.
- Completing a period target gives a short check-mark morph and a light haptic tick. No confetti.
- Respect the system "Remove animations" setting.

---

## 4. Information architecture and navigation

Bottom navigation bar with 4 destinations (56 dp height, labels always shown):

1. **Today**: everything due in the current day, week, and month periods.
2. **Goals**: all goals and projects, filterable.
3. **Calendar**: month or week view of entries, deadlines, and milestones.
4. **Insights**: stats, streaks, and history.

Settings is reached from the avatar/overflow icon in the top app bar on any tab.

A single **FAB (cobalt, "+")** appears on Today and Goals. Tap: quick-add goal sheet. Long-press: choose Goal, Project, or Quick log.

```
Today ── Goal row ── tap ──▶ Goal detail ── edit ──▶ Edit goal sheet
  │          └─ tap log control ──▶ logs entry inline (no navigation)
  └── FAB ──▶ Quick-add sheet ── "More options" ──▶ Full editor

Goals ── Project row ──▶ Project detail ──▶ Goal detail
```

---

## 5. Screens

### 5.1 Onboarding (first launch only, max 3 screens, skippable)

1. **Welcome:** one sentence and a "Get started" button.
2. **Pick starters (optional):** 6–8 template chips (Read books, Exercise, Save money, Drink water, Learn a language, Ship a project). Selecting a chip pre-fills a goal.
3. **Preferences:** week start day, day-start hour, reminder permission prompt (Android 13+ `POST_NOTIFICATIONS`).

### 5.2 Today

Purpose: answer "what do I need to do now?" and allow logging without leaving the screen.

Layout, top to bottom:

- **Top app bar (compact, 56 dp):** date ("Tue, Oct 20"), a small overall ring showing today's completion %, and overflow menu.
- **Period segmented control:** `Day · Week · Month · Year` (default Day). Filters which periods are shown.
- **Sections** (collapsible headers with count, e.g. "Daily · 3/5"):
  - Overdue (only if any; `error`-tinted header)
  - Daily
  - This week
  - This month
  - Upcoming milestones (next 7 days)
- **Goal row (compact):**
  ```
  ▌[icon] Read 30 minutes                    [ 20/30 m ]  (+)
  ▌       ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━  🔥 12
  ```
  - Leading 4 dp Area color bar, 20 dp icon, title (1 line, ellipsis).
  - Right side: numeric progress and a log control matched to the goal type (checkbox, + stepper, timer play button).
  - Bottom: 4 dp progress bar (cobalt fill, `surfaceVariant` track); streak count in slate blue if ≥ 2.
  - **Swipe right:** complete or log the default increment. **Swipe left:** skip this period (keeps the streak if the user allows skips) or snooze to later today.
  - Long-press: multi-select for bulk complete, move, or archive.
- Completed rows move to a collapsed "Done (n)" group at the bottom of each section.
- **Empty state:** one line of text ("Nothing due today") and a text button "Add a goal".

### 5.3 Goals (library)

- **Search** field in the app bar (expands on tap).
- **Filter chips** row (horizontally scrollable, slate blue when selected): All · Projects · Daily · Weekly · Monthly · Yearly · Custom · Archived. A second chip opens an Area filter.
- **Sort** menu: Due date, Progress, Recently updated, Name, Manual (drag handle).
- **List:** projects appear as expandable rows showing their child goals indented by 16 dp. Each row shows title, cadence badge ("W", "M", "Y", "D", "C"), progress %, and due date if any.
- Toggle in the overflow menu between **List** and **Grouped by Area** views.

### 5.4 Goal detail

- **Header:** title, Area chip, cadence label ("Weekly · 3× per week"), edit icon, overflow menu (Duplicate, Archive, Delete, Share progress as image).
- **Current period card:** large progress ring (96 dp) or bar, value vs. target, pace indicator ("On track", "2 behind pace" in `warning`), and a primary log button.
- **Stats strip** (4 compact tiles in one row): Current streak · Best streak · Completion rate · Total logged.
- **History:**
  - Daily goals: GitHub-style heatmap of the last 26 weeks (cobalt intensity scale).
  - Weekly and monthly goals: bar chart of the last 12 periods with the target line in slate blue.
  - Yearly goals: 12-month bar chart plus cumulative line against the ideal pace line.
- **Entries list:** date, value, and note. Swipe to edit or delete. "Add past entry" text button for backfilling.
- **Notes:** free-text "Why this matters" field, collapsed by default.

### 5.5 Project detail

- Header: title, date range, overall % (roll-up), days remaining.
- **Tabs (compact, 40 dp):** Overview · Goals · Milestones · Notes.
- **Overview:** progress bar, milestone timeline (horizontal, dots on a line, completed in cobalt, upcoming in slate blue outline), and the next three actions.
- **Goals:** child goals using the same compact rows as Today.
- **Milestones:** checklist with due dates, drag to reorder.
- **Roll-up rule** (set per project): average of child goal progress (default), weighted by user-set weights, or milestones completed / total.

### 5.6 Create and edit goal

**Quick-add bottom sheet** (default; fits on one screen without scrolling):

1. Title field (autofocus, keyboard open).
2. Cadence chips: `Daily · Weekly · Monthly · Yearly · Custom · Once`.
3. Type chips: `Check · Count · Amount · Time · %`.
4. Target field (hidden for Check): number and unit (unit is free text with suggestions such as km, pages, $, min).
5. "More options ›" text button.
6. Primary button: **Create**.

**More options** (full-screen editor, grouped sections with dividers):

- **Schedule:** start date, end date or "no end", active weekdays, custom recurrence builder (every N days/weeks, on chosen days, specific dates).
- **Target behavior:** "at least" or "at most" (limit), times per period vs. total amount, default log increment, allow partial credit, allowed skips per period.
- **Cascade (yearly and monthly only):** toggle "Split into sub-targets", choose granularity, preview the generated targets.
- **Organization:** Area, Project, icon picker (≈200 Material Symbols), tags.
- **Reminders:** see §6.
- **Notes:** motivation and description.

Validation: title required (max 80 chars); target > 0; end date ≥ start date. Errors appear inline under the field in `error` color.

### 5.7 Calendar

- Toggle between **Month** (default) and **Week** views.
- Month cells show up to 3 dots (cobalt = entries logged, slate blue = milestone, `error` = missed deadline).
- Tapping a day opens a bottom sheet with that day's entries and due items, and lets the user log for that date.

### 5.8 Insights

- **Range selector:** Week · Month · Year · All.
- **Summary row:** goals completed, average completion rate, longest active streak.
- **Completion trend:** line chart, cobalt line on a `surfaceVariant` grid.
- **By Area:** horizontal bar list.
- **Top streaks** and **Needs attention** (goals below 50 % pace), each with a maximum of 5 rows.
- **Year in review** card (shown in December and January).

Charts use cobalt for "actual" and slate blue for "target/planned". Every chart has a text alternative for TalkBack (e.g. "Completion rate 78 percent, up 6 points from last month").

### 5.9 Settings

- **General:** week starts on, day starts at (hour), default cadence, default reminder time.
- **Appearance:** theme (System / Light / Dark), density (Comfortable / Compact), dynamic color (off by default), show streaks.
- **Notifications:** master toggle, daily summary time, quiet hours.
- **Data:** export (JSON, CSV), import (JSON), backup to Google Drive (opt-in), clear all data (with confirmation).
- **Security:** app lock with biometric or device PIN.
- **Areas & tags:** manage, rename, reorder, set colors.
- **About:** version, licenses, privacy policy.

---

## 6. Reminders and notifications

- **Per-goal reminders:** one or more times per day on chosen days, or "N days before the deadline" for one-off goals and milestones.
- **Smart nudge (optional):** if a weekly or monthly goal falls behind pace, send one reminder on the next suitable day. At most one nudge per goal per period.
- **Daily summary** (optional): one notification at a chosen time listing the number of items due.
- **Actionable notifications:** buttons "Done", "+1" (or the default increment), and "Snooze 1 h". Logging from a notification does not open the app.
- Implemented with `WorkManager` for summaries and `AlarmManager.setExactAndAllowWhileIdle` for user-set times, falling back to inexact alarms if `SCHEDULE_EXACT_ALARM` is denied. Reschedule on boot, time-zone change, and time change.
- Notification channels: Reminders, Daily summary, Nudges, Timers (ongoing timer for duration goals).

---

## 7. Home screen widgets (Jetpack Glance)

| Widget | Size | Content |
|---|---|---|
| **Today list** | 4×2, resizable | Up to 5 due goals with tap-to-log controls |
| **Single goal** | 2×1 | Progress ring, value, and +1 button for one pinned goal |
| **Streak** | 1×1 | Streak count for one goal |

Widgets follow the same palette, with a white or near-black background matching the system theme.

---

## 8. Data model

All IDs are UUID strings. Timestamps are stored in UTC epoch milliseconds. Dates are stored as ISO `LocalDate` strings and interpreted in the user's current time zone.

```kotlin
enum class GoalType { CHECK, COUNT, QUANTITY, DURATION, PERCENT, LIMIT }
enum class CadenceKind { DAILY, WEEKLY, MONTHLY, YEARLY, CUSTOM, ONCE }
enum class TargetMode { TIMES_PER_PERIOD, TOTAL_AMOUNT }
enum class RollUp { AVERAGE, WEIGHTED, MILESTONES }

data class Area(
    val id: String, val name: String, val icon: String,
    val colorIndex: Int, val sortOrder: Int,
)

data class Project(
    val id: String, val areaId: String?, val title: String, val notes: String?,
    val startDate: LocalDate, val dueDate: LocalDate?,
    val rollUp: RollUp, val archived: Boolean,
    val createdAt: Long, val updatedAt: Long,
)

data class Goal(
    val id: String, val projectId: String?, val areaId: String?,
    val parentGoalId: String?,          // set for cascaded sub-targets
    val title: String, val icon: String, val notes: String?,
    val type: GoalType, val target: Double?, val unit: String?,
    val targetMode: TargetMode, val defaultIncrement: Double,
    val weight: Double = 1.0, val allowedSkipsPerPeriod: Int = 0,
    val archived: Boolean, val sortOrder: Int,
    val createdAt: Long, val updatedAt: Long,
)

data class Tracker(                     // 1:1 with Goal
    val goalId: String, val cadence: CadenceKind,
    val interval: Int = 1,              // every N days/weeks for CUSTOM
    val weekdays: Set<DayOfWeek>?,      // active days
    val specificDates: List<LocalDate>?,
    val startDate: LocalDate, val endDate: LocalDate?,
)

data class Entry(
    val id: String, val goalId: String, val value: Double,
    val loggedFor: LocalDate,           // the day the progress counts toward
    val createdAt: Long, val note: String?,
    val isSkip: Boolean = false,
)

data class Milestone(
    val id: String, val projectId: String, val title: String,
    val dueDate: LocalDate?, val completedAt: Long?, val sortOrder: Int,
)

data class Reminder(
    val id: String, val goalId: String?, val milestoneId: String?,
    val time: LocalTime?, val weekdays: Set<DayOfWeek>?,
    val daysBefore: Int?, val enabled: Boolean,
)
```

**Derived values** (computed, not stored): current period bounds, period progress, pace, streaks, and completion rate. Cache them in an in-memory `StateFlow` per goal and recompute when entries change.

**Pace formula:** `expected = target × (elapsed days in period / total days in period)`. The goal is "behind pace" when `actual < expected × 0.9`.

**Streak rule:** a period counts toward the streak when its target is met, or it is skipped within the allowed skips. The current period never breaks a streak until it ends.

---

## 9. Technical architecture

| Layer | Choice |
|---|---|
| Language | Kotlin 2.x |
| UI | Jetpack Compose, Material 3 with a custom color scheme (§3.1) |
| Architecture | MVVM + unidirectional data flow; single-activity; feature modules (`:feature:today`, `:feature:goals`, `:feature:calendar`, `:feature:insights`, `:feature:settings`) over `:core:data`, `:core:domain`, `:core:ui` |
| DI | Hilt |
| Navigation | Navigation Compose with type-safe routes |
| Persistence | Room (SQLite), with migrations covered by tests |
| Preferences | DataStore (Proto) |
| Async | Coroutines + Flow |
| Background | WorkManager, AlarmManager |
| Widgets | Jetpack Glance |
| Charts | Vico (Compose) or custom Canvas for the heatmap |
| Backup | Google Drive App Data folder via the Drive REST API (opt-in) |
| Testing | JUnit5 + Turbine for domain/view models, Room in-memory tests, Compose UI tests, Paparazzi screenshot tests for light/dark and both densities |
| Min / target SDK | 26 / latest stable |

**Performance targets**

- Cold start under 1 s on a mid-range device (e.g. Pixel 6a). Use a Baseline Profile.
- Today screen renders 100 goals at 60 fps (lazy list with stable keys).
- APK under 10 MB.

---

## 10. Accessibility

- All touch targets at least 48 × 48 dp.
- Every log control has a content description stating the goal, the current value, and the action ("Read 30 minutes, 20 of 30 minutes, add 5 minutes").
- Progress is never conveyed by color alone; numbers are always shown next to bars.
- Full TalkBack support with custom actions on rows (Log, Skip, Open) in place of swipe gestures.
- Dynamic font scaling to 200 % without clipping; layouts tested at 130 % and 200 %.
- Reduced motion respected (§3.4).

---

## 11. Privacy and security

- No account required. No analytics by default; an opt-in, anonymous crash reporting toggle only.
- All data stays on the device unless Drive backup is enabled. Backups are encrypted with a user passphrase.
- Optional biometric app lock; the app content is hidden in the Recents screen when the lock is on (`FLAG_SECURE`).
- Export lets users take all their data with them in JSON or CSV.

---

## 12. Release scope

### MVP (v1.0)

- Goals with Check, Count, Quantity, and Duration types.
- Daily, Weekly, Monthly, Yearly, and Once cadences.
- Projects with milestones and average roll-up.
- Today, Goals, Goal detail, Project detail, Quick-add, Settings.
- Per-goal reminders with actionable notifications.
- Light and dark themes, both densities.
- JSON export and import.

### v1.1

- Custom cadences (every N days/weeks, specific dates).
- Cascading yearly → monthly/weekly targets.
- Calendar and Insights tabs.
- Today list and Single goal widgets.
- Limit and Percent goal types.

### v1.2

- Google Drive backup, CSV export, app lock.
- Smart nudges, daily summary.
- Weighted and milestone roll-ups, streak widget, year in review.

---

## 13. Acceptance criteria (MVP)

1. A user can create a daily Check goal from the FAB in at most 3 taps plus typing the title.
2. Logging progress from Today takes exactly 1 tap and updates the progress bar within 100 ms.
3. Periods roll over correctly at the configured day-start hour and week-start day, including across DST changes and time-zone changes.
4. Streaks compute correctly for daily, weekly, monthly, and yearly goals, including allowed skips (covered by unit tests).
5. A reminder fires within 1 minute of its scheduled time (exact alarm permitted) and survives device reboot.
6. Tapping "Done" on a notification logs the entry without opening the app, and Today reflects it on next open.
7. All screens pass the Accessibility Scanner with no errors in light and dark themes.
8. Exporting to JSON and importing it into a fresh install restores all areas, projects, goals, entries, milestones, and reminders.

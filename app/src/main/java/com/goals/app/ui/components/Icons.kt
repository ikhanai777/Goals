package com.goals.app.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AttachMoney
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Brush
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.automirrored.rounded.DirectionsBike
import androidx.compose.material.icons.automirrored.rounded.DirectionsRun
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.LocalDrink
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.RocketLaunch
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.SelfImprovement
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Work
import androidx.compose.material.icons.rounded.HealthAndSafety
import androidx.compose.ui.graphics.vector.ImageVector

/** Icon keys stored on goals and areas, in picker order. */
val GoalIcons: List<Pair<String, ImageVector>> = listOf(
    "flag" to Icons.Rounded.Flag,
    "star" to Icons.Rounded.Star,
    "book" to Icons.AutoMirrored.Rounded.MenuBook,
    "run" to Icons.AutoMirrored.Rounded.DirectionsRun,
    "fitness" to Icons.Rounded.FitnessCenter,
    "bike" to Icons.AutoMirrored.Rounded.DirectionsBike,
    "water" to Icons.Rounded.LocalDrink,
    "meditate" to Icons.Rounded.SelfImprovement,
    "sleep" to Icons.Rounded.Bedtime,
    "food" to Icons.Rounded.Restaurant,
    "savings" to Icons.Rounded.Savings,
    "money" to Icons.Rounded.AttachMoney,
    "school" to Icons.Rounded.School,
    "language" to Icons.Rounded.Language,
    "code" to Icons.Rounded.Code,
    "write" to Icons.Rounded.Edit,
    "music" to Icons.Rounded.MusicNote,
    "art" to Icons.Rounded.Brush,
    "work" to Icons.Rounded.Work,
    "launch" to Icons.Rounded.RocketLaunch,
    "health" to Icons.Rounded.HealthAndSafety,
    "heart" to Icons.Rounded.Favorite,
    "spa" to Icons.Rounded.Spa,
    "person" to Icons.Rounded.Person,
)

private val byKey = GoalIcons.toMap()

fun iconFor(key: String): ImageVector = byKey[key] ?: Icons.Rounded.Flag

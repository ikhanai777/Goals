package com.goals.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.goals.app.R
import com.goals.app.data.Density
import com.goals.app.data.ThemeMode

/** Colors outside the Material scheme: semantic states and the muted secondary text tone. */
@Immutable
data class ExtraColors(
    val success: Color,
    val warning: Color,
    val muted: Color,
    val track: Color,
)

private val LightScheme = lightColorScheme(
    primary = Color(0xFF0047AB),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDCE6F7),
    onPrimaryContainer = Color(0xFF00296A),
    secondary = Color(0xFF6A5ACD),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE7E3FA),
    onSecondaryContainer = Color(0xFF2C2170),
    background = Color.White,
    onBackground = Color(0xFF0A0B0F),
    surface = Color.White,
    onSurface = Color(0xFF0A0B0F),
    surfaceVariant = Color(0xFFE6E8F0),
    onSurfaceVariant = Color(0xFF5B6070),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF5F6FA),
    surfaceContainer = Color(0xFFF5F6FA),
    surfaceContainerHigh = Color(0xFFEEF0F5),
    surfaceContainerHighest = Color(0xFFE6E8F0),
    outline = Color(0xFFD3D6E0),
    outlineVariant = Color(0xFFE6E8F0),
    error = Color(0xFFB3261E),
    onError = Color.White,
)

private val DarkScheme = darkColorScheme(
    primary = Color(0xFF5B8DEF),
    onPrimary = Color(0xFF0A0F1A),
    primaryContainer = Color(0xFF16305C),
    onPrimaryContainer = Color(0xFFD6E2FA),
    secondary = Color(0xFFA79BF0),
    onSecondary = Color(0xFF0A0F1A),
    secondaryContainer = Color(0xFF2E2860),
    onSecondaryContainer = Color(0xFFE7E3FA),
    background = Color(0xFF0A0B0F),
    onBackground = Color(0xFFF2F3F7),
    surface = Color(0xFF0A0B0F),
    onSurface = Color(0xFFF2F3F7),
    surfaceVariant = Color(0xFF1F222B),
    onSurfaceVariant = Color(0xFF9AA0B0),
    surfaceContainerLowest = Color(0xFF0A0B0F),
    surfaceContainerLow = Color(0xFF14161C),
    surfaceContainer = Color(0xFF14161C),
    surfaceContainerHigh = Color(0xFF1A1D24),
    surfaceContainerHighest = Color(0xFF1F222B),
    outline = Color(0xFF2C303B),
    outlineVariant = Color(0xFF1F222B),
    error = Color(0xFFF2B8B5),
    onError = Color(0xFF601410),
)

private val LightExtra = ExtraColors(
    success = Color(0xFF1F7A4D), warning = Color(0xFFB26A00),
    muted = Color(0xFF5B6070), track = Color(0xFFE6E8F0),
)
private val DarkExtra = ExtraColors(
    success = Color(0xFF5CC38E), warning = Color(0xFFF0B44C),
    muted = Color(0xFF9AA0B0), track = Color(0xFF1F222B),
)

/** Muted accents an Area can carry, shown only as a thin leading bar. */
val AreaColors = listOf(
    Color(0xFF0047AB), Color(0xFF6A5ACD), Color(0xFF2E8B87), Color(0xFFB0793A),
    Color(0xFF8A5A9E), Color(0xFF4F7A3A), Color(0xFFB0525E), Color(0xFF5B6070),
)

val Inter = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
)

private val AppTypography = Typography(
    titleLarge = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 28.sp),
    titleMedium = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 22.sp),
    titleSmall = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp),
    bodyLarge = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 22.sp),
    bodyMedium = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp),
    labelLarge = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp),
    labelMedium = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp),
    labelSmall = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 14.sp),
)

/** Tabular figures so progress values don't jitter as they change. */
val NumericStyle = TextStyle(
    fontFamily = Inter, fontWeight = FontWeight.Medium, fontSize = 13.sp, lineHeight = 18.sp,
    fontFeatureSettings = "tnum",
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(8.dp),
    large = RoundedCornerShape(12.dp),
    extraLarge = RoundedCornerShape(12.dp),
)

val LocalExtraColors = staticCompositionLocalOf { LightExtra }
val LocalRowHeight = staticCompositionLocalOf<Dp> { 56.dp }

object GoalsTheme {
    val extra: ExtraColors @Composable get() = LocalExtraColors.current
    val rowHeight: Dp @Composable get() = LocalRowHeight.current
}

@Composable
fun GoalsTheme(
    theme: ThemeMode = ThemeMode.SYSTEM,
    density: Density = Density.COMFORTABLE,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val dark = when (theme) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val scheme: ColorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        dark -> DarkScheme
        else -> LightScheme
    }
    CompositionLocalProvider(
        LocalExtraColors provides if (dark) DarkExtra else LightExtra,
        LocalRowHeight provides if (density == Density.COMPACT) 48.dp else 56.dp,
    ) {
        MaterialTheme(colorScheme = scheme, typography = AppTypography, shapes = AppShapes, content = content)
    }
}

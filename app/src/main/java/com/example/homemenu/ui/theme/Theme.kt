package com.example.homemenu.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.homemenu.R
import com.example.homemenu.domain.model.ThemeMode

val Lora = FontFamily(
    Font(R.font.lora, FontWeight.Normal),
    Font(R.font.lora, FontWeight.SemiBold),
    Font(R.font.lora, FontWeight.Bold),
)
val Nunito = FontFamily(
    Font(R.font.nunito, FontWeight.Normal),
    Font(R.font.nunito, FontWeight.Medium),
    Font(R.font.nunito, FontWeight.SemiBold),
    Font(R.font.nunito, FontWeight.Bold),
)

private fun heading(size: Int, line: Int, weight: FontWeight = FontWeight.SemiBold) =
    TextStyle(fontFamily = Lora, fontWeight = weight, fontSize = size.sp, lineHeight = line.sp)

private fun body(size: Int, line: Int, weight: FontWeight = FontWeight.Normal) =
    TextStyle(fontFamily = Nunito, fontWeight = weight, fontSize = size.sp, lineHeight = line.sp)

val AppTypography = Typography(
    displaySmall = heading(34, 42),
    headlineLarge = heading(30, 38),
    headlineMedium = heading(26, 34),
    headlineSmall = heading(22, 30),
    titleLarge = heading(20, 28),
    titleMedium = body(16, 24, FontWeight.Bold),
    titleSmall = body(14, 20, FontWeight.Bold),
    bodyLarge = body(16, 24),
    bodyMedium = body(14, 20),
    bodySmall = body(12, 16),
    labelLarge = body(14, 20, FontWeight.SemiBold),
    labelMedium = body(12, 16, FontWeight.SemiBold),
    labelSmall = body(11, 16, FontWeight.SemiBold),
)

val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(24.dp),
)

fun isDarkMode(mode: ThemeMode, systemDark: Boolean): Boolean = when (mode) {
    ThemeMode.SYSTEM -> systemDark
    ThemeMode.LIGHT -> false
    ThemeMode.DARK -> true
}

/** Whether the effective theme is dark (for system bar icons). LEGACY follows [mode]; other themes decide themselves. */
fun appIsDark(mode: ThemeMode, themeId: String?, customColors: String?, systemDark: Boolean): Boolean =
    ThemePalettes.resolve(themeId, customColors)?.dark ?: isDarkMode(mode, systemDark)

/** Full Material 3 colour scheme from a palette; every on-colour is checked for 4.5:1 contrast. */
fun ThemePalette.toColorScheme(): ColorScheme {
    fun c(v: Long) = Color(v)
    val base = if (dark) darkColorScheme() else lightColorScheme()
    val tertiaryContainer = ColorMath.blend(tertiary, surface, 0.25)
    return base.copy(
        primary = c(primary),
        onPrimary = c(onColor(primary)),
        primaryContainer = c(primaryContainer),
        onPrimaryContainer = c(onColor(primaryContainer)),
        inversePrimary = c(primaryContainer),
        secondary = c(secondary),
        onSecondary = c(onColor(secondary)),
        secondaryContainer = c(secondaryContainer),
        onSecondaryContainer = c(onColor(secondaryContainer)),
        tertiary = c(tertiary),
        onTertiary = c(onColor(tertiary)),
        tertiaryContainer = c(tertiaryContainer),
        onTertiaryContainer = c(onColor(tertiaryContainer)),
        background = c(background),
        onBackground = c(onColor(background)),
        surface = c(surface),
        onSurface = c(onColor(surface)),
        surfaceVariant = c(surfaceVariant),
        onSurfaceVariant = c(onVariant(surfaceVariant)),
        surfaceTint = c(primary),
        inverseSurface = c(text),
        inverseOnSurface = c(background),
        outline = c(outline),
        outlineVariant = c(outlineVariant),
        surfaceBright = c(surface),
        surfaceDim = c(surfaceHighest),
        surfaceContainerLowest = c(surface),
        surfaceContainerLow = c(surfaceLow),
        surfaceContainer = c(surfaceContainer),
        surfaceContainerHigh = c(surfaceHigh),
        surfaceContainerHighest = c(surfaceHighest),
    )
}

/**
 * App theme. [themeId] = AppThemeId name from user_settings (null / LEGACY = original look that
 * follows [mode]); [customColors] = CustomAppearance JSON, used when themeId is CUSTOM.
 */
@Composable
fun HomeMenuTheme(
    mode: ThemeMode = ThemeMode.SYSTEM,
    themeId: String? = null,
    customColors: String? = null,
    content: @Composable () -> Unit,
) {
    val systemDark = isSystemInDarkTheme()
    val palette = remember(themeId, customColors) { ThemePalettes.resolve(themeId, customColors) }
    val scheme = remember(palette, mode, systemDark) {
        palette?.toColorScheme() ?: if (isDarkMode(mode, systemDark)) DarkColors else LightColors
    }
    MaterialTheme(
        colorScheme = scheme,
        typography = AppTypography,
        shapes = AppShapes,
        content = content,
    )
}

/** True when the current MaterialTheme is dark (based on background luminance), for any theme. */
@Composable
fun isAppDark(): Boolean = MaterialTheme.colorScheme.background.luminance() < 0.5f

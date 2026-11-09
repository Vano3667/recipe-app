package com.example.homemenu.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// Brand palette (see DESIGN.md for RAL / Pantone / NCS analogues)
val MintCream = Color(0xFFF5FFFA)        // background, light
val RoseDawn = Color(0xFFC0796B)         // Pantone 16-1522 Rose Dawn ≈ RAL 3015
val Terracotta = Color(0xFFA65A4E)       // accessible accent (white text 5.0:1)
val RoseContainer = Color(0xFFF7DCD5)
val Cream = Color(0xFFF3E6D3)
val Beige = Color(0xFF8A6F55)
val Cocoa = Color(0xFF3B2E2A)            // text on light
val CocoaNight = Color(0xFF221B19)       // background, dark
val CocoaSurface = Color(0xFF2D2421)
val CocoaSurfaceHigh = Color(0xFF3A2F2B)
val RoseNight = Color(0xFFF2A99A)
val CreamText = Color(0xFFF2E7E2)

val LightColors = lightColorScheme(
    primary = Terracotta,
    onPrimary = Color.White,
    primaryContainer = RoseContainer,
    onPrimaryContainer = Color(0xFF3E140E),
    secondary = Beige,
    onSecondary = Color.White,
    secondaryContainer = Cream,
    onSecondaryContainer = Color(0xFF2E2115),
    tertiary = Color(0xFF5E7F6E),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFD6EEE2),
    onTertiaryContainer = Color(0xFF13261C),
    background = MintCream,
    onBackground = Cocoa,
    surface = Color(0xFFFFFBF8),
    onSurface = Cocoa,
    surfaceVariant = Color(0xFFF1E6E1),
    onSurfaceVariant = Color(0xFF5E4D47),
    surfaceContainer = Color(0xFFFBF3EF),
    surfaceContainerHigh = Color(0xFFF6ECE7),
    surfaceContainerLow = Color(0xFFFFF8F5),
    outline = Color(0xFF8E7A73),
    outlineVariant = Color(0xFFE0D2CC),
    error = Color(0xFFB3261E),
    onError = Color.White,
)

val DarkColors = darkColorScheme(
    primary = RoseNight,
    onPrimary = Color(0xFF4A1E16),
    primaryContainer = Color(0xFF6E3A30),
    onPrimaryContainer = Color(0xFFFFDAD3),
    secondary = Color(0xFFDCC3A6),
    onSecondary = Color(0xFF3D2D1C),
    secondaryContainer = Color(0xFF544331),
    onSecondaryContainer = Color(0xFFF6E2C9),
    tertiary = Color(0xFFA9CFBA),
    onTertiary = Color(0xFF15372A),
    tertiaryContainer = Color(0xFF2E4D3F),
    onTertiaryContainer = Color(0xFFC5EBD6),
    background = CocoaNight,
    onBackground = CreamText,
    surface = CocoaSurface,
    onSurface = CreamText,
    surfaceVariant = Color(0xFF4A3D38),
    onSurfaceVariant = Color(0xFFD9C6BF),
    surfaceContainer = CocoaSurface,
    surfaceContainerHigh = CocoaSurfaceHigh,
    surfaceContainerLow = Color(0xFF29201E),
    outline = Color(0xFFA2908A),
    outlineVariant = Color(0xFF4F423D),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
)

/** Tag chip colors: (container, content) for light and dark themes. Contrast ≥ 4.5:1. */
fun tagColors(hint: String, dark: Boolean): Pair<Color, Color> {
    val light = when (hint) {
        "mint" -> Color(0xFFD3F1E4) to Color(0xFF1D4636)
        "sage" -> Color(0xFFDDE7D2) to Color(0xFF2E3F22)
        "coral" -> Color(0xFFFFD9CF) to Color(0xFF6A2414)
        "lavender" -> Color(0xFFE6DDF5) to Color(0xFF3A2A5C)
        "terracotta" -> Color(0xFFF2CDBE) to Color(0xFF5C2112)
        "peach" -> Color(0xFFFFE3CC) to Color(0xFF5E3313)
        "rose" -> Color(0xFFF9D3DA) to Color(0xFF5E1B2A)
        "pink" -> Color(0xFFFCDDEB) to Color(0xFF5A1A3A)
        "taupe" -> Color(0xFFE5DBD3) to Color(0xFF3D3029)
        else -> Color(0xFFF3E6D3) to Color(0xFF3D2E1E) // cream
    }
    if (!dark) return light
    // Dark: swap roles — deep container with the light tint as text.
    return Color(
        red = light.second.red * 0.9f, green = light.second.green * 0.9f, blue = light.second.blue * 0.9f,
    ) to light.first
}

val TagPalette = listOf("mint", "sage", "coral", "lavender", "terracotta", "peach", "rose", "pink", "cream", "taupe")

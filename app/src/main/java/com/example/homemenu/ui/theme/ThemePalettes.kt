package com.example.homemenu.ui.theme

import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt

/*
 * Pure Kotlin (no Compose / Android): theme ids, palettes, custom colours and colour math.
 * Colours are ARGB values stored as Long (0xFFRRGGBB), the same form Compose `Color(Long)` takes.
 */

/** Colour themes. Stored by [name] in `user_settings.themeId`. LEGACY = the original v1 look that follows ThemeMode. */
enum class AppThemeId(val title: String, val description: String) {
    LEGACY("Классическая", "Исходное оформление, светлое или тёмное по настройке выше"),
    LIGHT_MINIMAL("Светлая", "Белый фон, спокойный синий акцент"),
    WARM_CREAM("Кремовая", "Тёплый кремовый фон, карамельные акценты"),
    NATURAL_GREEN("Зелёная", "Мягкая зелень и светлый шалфей"),
    MODERN_DARK("Тёмная", "Графитовый фон, голубой акцент"),
    WARM_TERRACOTTA("Терракотовая", "Терракота и пыльная роза"),
    CONTEMPORARY_CONTRAST("Контрастная", "Чёрное на белом, жёлтый акцент"),
    CUSTOM("Свои цвета", "Настройте цвета сами");

    companion object {
        /** Unknown or missing values fall back to LEGACY, so old databases keep their look. */
        fun parse(value: String?): AppThemeId = entries.firstOrNull { it.name == value } ?: LEGACY

        /** The six predefined themes (not LEGACY, not CUSTOM). */
        val presets: List<AppThemeId> get() = entries.filter { it != LEGACY && it != CUSTOM }
    }
}

/** All colour roles of a theme. On-colours are derived with [onColor] so text stays readable. */
data class ThemePalette(
    val dark: Boolean,
    val background: Long,
    val surface: Long,
    val surfaceVariant: Long,
    val surfaceLow: Long,
    val surfaceContainer: Long,
    val surfaceHigh: Long,
    val surfaceHighest: Long,
    val primary: Long,
    val primaryContainer: Long,
    val secondary: Long,
    val secondaryContainer: Long,
    val tertiary: Long,
    val text: Long,
    val textVariant: Long,
    val outline: Long,
    val outlineVariant: Long,
) {
    /** Text colour for content on [bg]: the theme text if readable (4.5:1), otherwise white or near-black. */
    fun onColor(bg: Long): Long = ColorMath.readableOn(bg, text)

    /** Secondary text on [bg]: [textVariant] if readable, otherwise the main on-colour. */
    fun onVariant(bg: Long): Long =
        if (ColorMath.contrast(textVariant, bg) >= ColorMath.MIN_TEXT_CONTRAST) textVariant else onColor(bg)
}

/** Colour roles the user can change in the custom theme. */
enum class CustomRole(val title: String) {
    BACKGROUND("Основной фон"),
    SECONDARY_BACKGROUND("Дополнительный фон"),
    SURFACE("Поверхности и панели"),
    CARD("Карточки"),
    BUTTON("Кнопки и заголовки"),
    PRIMARY("Основной акцент"),
    SECONDARY("Дополнительный акцент"),
    TEXT("Текст"),
    TEXT_SECONDARY("Второстепенный текст"),
}

/**
 * Custom theme: a preset [base] plus user overrides. Stored as JSON in `user_settings.customColors`:
 * `{"base":"NATURAL_GREEN","BACKGROUND":"#102030",...}`. Unknown keys and bad values are ignored.
 */
data class CustomAppearance(
    val base: AppThemeId = AppThemeId.LIGHT_MINIMAL,
    val colors: Map<CustomRole, Long> = emptyMap(),
) {
    fun withColor(role: CustomRole, color: Long) = copy(colors = colors + (role to (color or ColorMath.OPAQUE)))

    fun encode(): String {
        val map = linkedMapOf("base" to base.name)
        CustomRole.entries.forEach { role -> colors[role]?.let { map[role.name] = ColorMath.toHex(it) } }
        return json.encodeToString(MapSerializer(String.serializer(), String.serializer()), map)
    }

    companion object {
        private val json = Json { ignoreUnknownKeys = true }

        fun decode(value: String?): CustomAppearance {
            if (value.isNullOrBlank()) return CustomAppearance()
            val map = try {
                json.decodeFromString(MapSerializer(String.serializer(), String.serializer()), value)
            } catch (e: Exception) {
                return CustomAppearance()
            }
            val base = AppThemeId.parse(map["base"]).takeIf { it in AppThemeId.presets } ?: AppThemeId.LIGHT_MINIMAL
            val colors = CustomRole.entries.mapNotNull { role -> map[role.name]?.let(ColorMath::parseHex)?.let { role to it } }.toMap()
            return CustomAppearance(base, colors)
        }
    }
}

object ThemePalettes {
    val LIGHT_MINIMAL = ThemePalette(
        dark = false,
        background = 0xFFF8F9FA, surface = 0xFFFFFFFF, surfaceVariant = 0xFFEEF0F2,
        surfaceLow = 0xFFF3F5F7, surfaceContainer = 0xFFEFF1F3, surfaceHigh = 0xFFE9ECEF, surfaceHighest = 0xFFE3E6EA,
        primary = 0xFF2F5D8A, primaryContainer = 0xFFD6E4F2,
        secondary = 0xFF5F6B76, secondaryContainer = 0xFFE3E8EC,
        tertiary = 0xFF3F6F5A,
        text = 0xFF1D2329, textVariant = 0xFF4A545E,
        outline = 0xFF7A848E, outlineVariant = 0xFFD5DADF,
    )

    val WARM_CREAM = ThemePalette(
        dark = false,
        background = 0xFFFBF5EA, surface = 0xFFFFFBF3, surfaceVariant = 0xFFF1E7D6,
        surfaceLow = 0xFFFAF2E5, surfaceContainer = 0xFFF6EDDD, surfaceHigh = 0xFFF2E7D5, surfaceHighest = 0xFFEDE1CD,
        primary = 0xFF8A5A2B, primaryContainer = 0xFFF3DFC4,
        secondary = 0xFF75654F, secondaryContainer = 0xFFEDE3D2,
        tertiary = 0xFF5E7044,
        text = 0xFF3A2E22, textVariant = 0xFF5F5142,
        outline = 0xFF8C7D6B, outlineVariant = 0xFFE2D6C3,
    )

    val NATURAL_GREEN = ThemePalette(
        dark = false,
        background = 0xFFF3F7F1, surface = 0xFFFAFCF8, surfaceVariant = 0xFFE1EADC,
        surfaceLow = 0xFFEEF4EB, surfaceContainer = 0xFFE9F0E5, surfaceHigh = 0xFFE3EBDF, surfaceHighest = 0xFFDDE6D8,
        primary = 0xFF2E6B3F, primaryContainer = 0xFFCDE8D2,
        secondary = 0xFF56694F, secondaryContainer = 0xFFDCE7D6,
        tertiary = 0xFF6E5F30,
        text = 0xFF1B2A1E, textVariant = 0xFF4A5A4C,
        outline = 0xFF76867A, outlineVariant = 0xFFD2DECF,
    )

    val MODERN_DARK = ThemePalette(
        dark = true,
        background = 0xFF121418, surface = 0xFF1A1D22, surfaceVariant = 0xFF2A2F36,
        surfaceLow = 0xFF16191D, surfaceContainer = 0xFF1E2227, surfaceHigh = 0xFF252A30, surfaceHighest = 0xFF2D3239,
        primary = 0xFF8AB4F8, primaryContainer = 0xFF23436F,
        secondary = 0xFFB8C4D2, secondaryContainer = 0xFF37414C,
        tertiary = 0xFF9BD3B5,
        text = 0xFFE6E8EB, textVariant = 0xFFB9BFC7,
        outline = 0xFF8A929C, outlineVariant = 0xFF3A4048,
    )

    val WARM_TERRACOTTA = ThemePalette(
        dark = false,
        background = 0xFFFBF1EC, surface = 0xFFFFF8F4, surfaceVariant = 0xFFF2DFD6,
        surfaceLow = 0xFFF9ECE6, surfaceContainer = 0xFFF5E5DE, surfaceHigh = 0xFFF1DED5, surfaceHighest = 0xFFECD6CC,
        primary = 0xFFA4472F, primaryContainer = 0xFFF6D3C6,
        secondary = 0xFF7D5A4C, secondaryContainer = 0xFFEEDDD5,
        tertiary = 0xFF5F6B42,
        text = 0xFF3B211A, textVariant = 0xFF63463C,
        outline = 0xFF937468, outlineVariant = 0xFFE4CBC0,
    )

    val CONTEMPORARY_CONTRAST = ThemePalette(
        dark = false,
        background = 0xFFFFFFFF, surface = 0xFFFFFFFF, surfaceVariant = 0xFFEDEDED,
        surfaceLow = 0xFFF7F7F7, surfaceContainer = 0xFFF2F2F2, surfaceHigh = 0xFFEBEBEB, surfaceHighest = 0xFFE4E4E4,
        primary = 0xFF111111, primaryContainer = 0xFFFFD60A,
        secondary = 0xFF3D3D3D, secondaryContainer = 0xFFE6E6E6,
        tertiary = 0xFF0050C8,
        text = 0xFF0A0A0A, textVariant = 0xFF3A3A3A,
        outline = 0xFF4D4D4D, outlineVariant = 0xFFBDBDBD,
    )

    /** Light colours of the original theme (Color.kt LightColors), used only for the preview card. */
    val LEGACY_PREVIEW = ThemePalette(
        dark = false,
        background = 0xFFF5FFFA, surface = 0xFFFFFBF8, surfaceVariant = 0xFFF1E6E1,
        surfaceLow = 0xFFFFF8F5, surfaceContainer = 0xFFFBF3EF, surfaceHigh = 0xFFF6ECE7, surfaceHighest = 0xFFF1E6E1,
        primary = 0xFFA65A4E, primaryContainer = 0xFFF7DCD5,
        secondary = 0xFF8A6F55, secondaryContainer = 0xFFF3E6D3,
        tertiary = 0xFF5E7F6E,
        text = 0xFF3B2E2A, textVariant = 0xFF5E4D47,
        outline = 0xFF8E7A73, outlineVariant = 0xFFE0D2CC,
    )

    /** Quick swatches in the colour picker. */
    val SWATCHES: List<Long> = listOf(
        0xFFFFFFFF, 0xFFF8F9FA, 0xFFFBF5EA, 0xFFF3F7F1, 0xFFFBF1EC, 0xFF121418,
        0xFF2F5D8A, 0xFF2E6B3F, 0xFFA4472F, 0xFF8A5A2B, 0xFF6B4FA0, 0xFFC2185B,
        0xFFFFD60A, 0xFF8AB4F8, 0xFF1D2329, 0xFF0A0A0A,
    )

    fun preset(id: AppThemeId): ThemePalette? = when (id) {
        AppThemeId.LIGHT_MINIMAL -> LIGHT_MINIMAL
        AppThemeId.WARM_CREAM -> WARM_CREAM
        AppThemeId.NATURAL_GREEN -> NATURAL_GREEN
        AppThemeId.MODERN_DARK -> MODERN_DARK
        AppThemeId.WARM_TERRACOTTA -> WARM_TERRACOTTA
        AppThemeId.CONTEMPORARY_CONTRAST -> CONTEMPORARY_CONTRAST
        AppThemeId.LEGACY, AppThemeId.CUSTOM -> null
    }

    /** The base preset with the user's overrides applied; dependent colours are derived. */
    fun custom(appearance: CustomAppearance): ThemePalette {
        var p = preset(appearance.base) ?: LIGHT_MINIMAL
        val o = appearance.colors
        o[CustomRole.BACKGROUND]?.let { p = p.copy(background = it, dark = ColorMath.luminance(it) < 0.25) }
        o[CustomRole.SECONDARY_BACKGROUND]?.let { p = p.copy(surfaceVariant = it, surfaceLow = it) }
        o[CustomRole.SURFACE]?.let { p = p.copy(surface = it, surfaceContainer = it) }
        o[CustomRole.CARD]?.let { p = p.copy(surfaceHigh = it, surfaceHighest = it) }
        o[CustomRole.BUTTON]?.let { p = p.copy(primary = it) }
        o[CustomRole.PRIMARY]?.let { p = p.copy(primaryContainer = it) }
        o[CustomRole.SECONDARY]?.let { p = p.copy(secondary = it, secondaryContainer = ColorMath.blend(it, p.surface, 0.35)) }
        o[CustomRole.TEXT]?.let {
            p = p.copy(text = it, outline = ColorMath.blend(it, p.background, 0.55), outlineVariant = ColorMath.blend(it, p.background, 0.2))
        }
        o[CustomRole.TEXT_SECONDARY]?.let { p = p.copy(textVariant = it) }
        return p
    }

    /** null = LEGACY: the caller uses the original light/dark schemes. */
    fun resolve(themeId: String?, customColors: String?): ThemePalette? {
        val id = AppThemeId.parse(themeId)
        return if (id == AppThemeId.CUSTOM) custom(CustomAppearance.decode(customColors)) else preset(id)
    }

    /** Palette shown on a theme's preview card. */
    fun previewOf(id: AppThemeId, saved: CustomAppearance): ThemePalette = when (id) {
        AppThemeId.LEGACY -> LEGACY_PREVIEW
        AppThemeId.CUSTOM -> custom(saved)
        else -> preset(id) ?: LIGHT_MINIMAL
    }

    fun roleColor(p: ThemePalette, role: CustomRole): Long = when (role) {
        CustomRole.BACKGROUND -> p.background
        CustomRole.SECONDARY_BACKGROUND -> p.surfaceVariant
        CustomRole.SURFACE -> p.surface
        CustomRole.CARD -> p.surfaceHigh
        CustomRole.BUTTON -> p.primary
        CustomRole.PRIMARY -> p.primaryContainer
        CustomRole.SECONDARY -> p.secondary
        CustomRole.TEXT -> p.text
        CustomRole.TEXT_SECONDARY -> p.textVariant
    }

    /** Readability problems of a palette, as user-facing Russian messages. Empty = fine. */
    fun contrastWarnings(p: ThemePalette): List<String> {
        val min = ColorMath.MIN_TEXT_CONTRAST
        val out = mutableListOf<String>()
        if (ColorMath.contrast(p.text, p.background) < min) out += "Текст плохо читается на основном фоне — подставим чёрный или белый."
        if (ColorMath.contrast(p.text, p.surfaceHigh) < min) out += "Текст плохо читается на карточках — подставим чёрный или белый."
        if (ColorMath.contrast(p.textVariant, p.surfaceVariant) < min) out += "Второстепенный текст слишком бледный."
        if (ColorMath.contrast(p.onColor(p.primary), p.primary) < min) out += "Надписи на кнопках плохо читаются: выберите цвет темнее или светлее."
        if (ColorMath.contrast(p.primary, p.background) < 3.0) out += "Кнопки почти сливаются с фоном."
        return out
    }
}

/** WCAG contrast, hex and HSV helpers on ARGB Long colours. */
object ColorMath {
    const val OPAQUE = 0xFF000000L
    const val WHITE = 0xFFFFFFFFL
    const val NEAR_BLACK = 0xFF111111L
    const val MIN_TEXT_CONTRAST = 4.5

    fun red(c: Long): Int = ((c shr 16) and 0xFF).toInt()
    fun green(c: Long): Int = ((c shr 8) and 0xFF).toInt()
    fun blue(c: Long): Int = (c and 0xFF).toInt()

    fun rgb(r: Int, g: Int, b: Int): Long =
        OPAQUE or (r.coerceIn(0, 255).toLong() shl 16) or (g.coerceIn(0, 255).toLong() shl 8) or b.coerceIn(0, 255).toLong()

    /** WCAG 2.x relative luminance, 0..1. */
    fun luminance(c: Long): Double {
        fun ch(v: Int): Double {
            val s = v / 255.0
            return if (s <= 0.03928) s / 12.92 else ((s + 0.055) / 1.055).pow(2.4)
        }
        return 0.2126 * ch(red(c)) + 0.7152 * ch(green(c)) + 0.0722 * ch(blue(c))
    }

    /** WCAG contrast ratio, 1..21. */
    fun contrast(a: Long, b: Long): Double {
        val la = luminance(a)
        val lb = luminance(b)
        return (max(la, lb) + 0.05) / (min(la, lb) + 0.05)
    }

    /** [preferred] if it reaches 4.5:1 on [bg], otherwise whichever of white / near-black contrasts more. */
    fun readableOn(bg: Long, preferred: Long): Long {
        if (contrast(preferred, bg) >= MIN_TEXT_CONTRAST) return preferred
        return if (contrast(WHITE, bg) >= contrast(NEAR_BLACK, bg)) WHITE else NEAR_BLACK
    }

    /** Mix: [t] = weight of [a] (0..1). */
    fun blend(a: Long, b: Long, t: Double): Long {
        val k = t.coerceIn(0.0, 1.0)
        fun mix(x: Int, y: Int) = (x * k + y * (1 - k)).roundToInt()
        return rgb(mix(red(a), red(b)), mix(green(a), green(b)), mix(blue(a), blue(b)))
    }

    fun toHex(c: Long): String = "#%06X".format(c and 0xFFFFFFL)

    /** "#RRGGBB" or "RRGGBB" (case-insensitive) -> opaque colour; anything else -> null. */
    fun parseHex(s: String): Long? {
        val h = s.trim().removePrefix("#")
        if (h.length != 6 || !h.all { it in '0'..'9' || it.lowercaseChar() in 'a'..'f' }) return null
        return OPAQUE or h.toLong(16)
    }

    /** [hue 0..360, saturation 0..1, value 0..1]. */
    fun toHsv(c: Long): FloatArray {
        val r = red(c) / 255f
        val g = green(c) / 255f
        val b = blue(c) / 255f
        val mx = maxOf(r, g, b)
        val mn = minOf(r, g, b)
        val d = mx - mn
        var h = when {
            d == 0f -> 0f
            mx == r -> 60f * (((g - b) / d) % 6f)
            mx == g -> 60f * (((b - r) / d) + 2f)
            else -> 60f * (((r - g) / d) + 4f)
        }
        if (h < 0f) h += 360f
        val s = if (mx == 0f) 0f else d / mx
        return floatArrayOf(h, s, mx)
    }

    fun fromHsv(h: Float, s: Float, v: Float): Long {
        val hh = ((h % 360f) + 360f) % 360f
        val ss = s.coerceIn(0f, 1f)
        val vv = v.coerceIn(0f, 1f)
        val c = vv * ss
        val x = c * (1f - abs((hh / 60f) % 2f - 1f))
        val m = vv - c
        val (r, g, b) = when {
            hh < 60f -> Triple(c, x, 0f)
            hh < 120f -> Triple(x, c, 0f)
            hh < 180f -> Triple(0f, c, x)
            hh < 240f -> Triple(0f, x, c)
            hh < 300f -> Triple(x, 0f, c)
            else -> Triple(c, 0f, x)
        }
        fun ch(f: Float) = ((f + m) * 255f).roundToInt()
        return rgb(ch(r), ch(g), ch(b))
    }
}

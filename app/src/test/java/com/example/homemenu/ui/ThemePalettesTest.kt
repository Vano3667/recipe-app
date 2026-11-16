package com.example.homemenu.ui

import com.example.homemenu.ui.theme.AppThemeId
import com.example.homemenu.ui.theme.ColorMath
import com.example.homemenu.ui.theme.CustomAppearance
import com.example.homemenu.ui.theme.CustomRole
import com.example.homemenu.ui.theme.ThemePalettes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class ThemePalettesTest {

    @Test fun sixPresetThemes() {
        assertEquals(6, AppThemeId.presets.size)
        AppThemeId.presets.forEach { assertNotNull(it.name, ThemePalettes.preset(it)) }
        assertNull(ThemePalettes.preset(AppThemeId.LEGACY))
        assertNull(ThemePalettes.preset(AppThemeId.CUSTOM))
    }

    @Test fun presetsAreReadable() {
        AppThemeId.presets.forEach { id ->
            val p = ThemePalettes.preset(id)!!
            listOf(
                "background" to p.background, "surface" to p.surface, "surfaceVariant" to p.surfaceVariant,
                "surfaceHigh" to p.surfaceHigh, "surfaceHighest" to p.surfaceHighest,
                "primary" to p.primary, "primaryContainer" to p.primaryContainer,
                "secondary" to p.secondary, "secondaryContainer" to p.secondaryContainer, "tertiary" to p.tertiary,
            ).forEach { (role, bg) ->
                val ratio = ColorMath.contrast(p.onColor(bg), bg)
                assertTrue("$id $role contrast $ratio", ratio >= 4.5)
            }
            assertTrue("$id text on background", ColorMath.contrast(p.text, p.background) >= 4.5)
            assertTrue("$id secondary text", ColorMath.contrast(p.textVariant, p.surfaceVariant) >= 4.5)
            assertTrue("$id no warnings", ThemePalettes.contrastWarnings(p).isEmpty())
        }
    }

    @Test fun onlyModernDarkIsDark() {
        AppThemeId.presets.forEach { id ->
            assertEquals(id.name, id == AppThemeId.MODERN_DARK, ThemePalettes.preset(id)!!.dark)
        }
    }

    @Test fun resolveFallsBackToLegacy() {
        assertNull(ThemePalettes.resolve(null, null))
        assertNull(ThemePalettes.resolve("LEGACY", null))
        assertNull(ThemePalettes.resolve("SOMETHING_OLD", null))
        assertEquals(ThemePalettes.WARM_CREAM, ThemePalettes.resolve("WARM_CREAM", null))
        assertEquals(AppThemeId.LEGACY, AppThemeId.parse(null))
    }

    @Test fun customColoursRoundTrip() {
        val custom = CustomAppearance(AppThemeId.NATURAL_GREEN)
            .withColor(CustomRole.BACKGROUND, 0xFF102030)
            .withColor(CustomRole.TEXT, 0xFFEEEEEE)
            .withColor(CustomRole.BUTTON, 0xFF8AB4F8)
        val decoded = CustomAppearance.decode(custom.encode())
        assertEquals(custom, decoded)
        val p = ThemePalettes.resolve("CUSTOM", custom.encode())!!
        assertEquals(0xFF102030, p.background)
        assertEquals(0xFFEEEEEE, p.text)
        assertEquals(0xFF8AB4F8, p.primary)
        assertTrue(p.dark)
        // Roles that were not changed come from the base theme.
        assertEquals(ThemePalettes.NATURAL_GREEN.secondary, p.secondary)
    }

    @Test fun badCustomJsonIsIgnored() {
        assertEquals(CustomAppearance(), CustomAppearance.decode(null))
        assertEquals(CustomAppearance(), CustomAppearance.decode(""))
        assertEquals(CustomAppearance(), CustomAppearance.decode("not json"))
        val weird = CustomAppearance.decode("{\"base\":\"CUSTOM\",\"TEXT\":\"zzz\",\"UNKNOWN\":\"#000000\",\"CARD\":\"#abcdef\"}")
        assertEquals(AppThemeId.LIGHT_MINIMAL, weird.base)
        assertEquals(mapOf(CustomRole.CARD to 0xFFABCDEF), weird.colors)
    }

    @Test fun unreadableCustomTextIsWarnedAndReplacedOnScreen() {
        val custom = CustomAppearance().withColor(CustomRole.BACKGROUND, 0xFFFFFFFF).withColor(CustomRole.TEXT, 0xFFF0F0F0)
        val p = ThemePalettes.custom(custom)
        assertFalse(ThemePalettes.contrastWarnings(p).isEmpty())
        assertTrue(ColorMath.contrast(p.onColor(p.background), p.background) >= 4.5)
    }

    @Test fun hexParsing() {
        assertEquals(0xFFA4472F, ColorMath.parseHex("#A4472F"))
        assertEquals(0xFFA4472F, ColorMath.parseHex("a4472f"))
        assertNull(ColorMath.parseHex("#12345"))
        assertNull(ColorMath.parseHex("#GG0000"))
        assertEquals("#A4472F", ColorMath.toHex(0xFFA4472F))
    }

    @Test fun hsvRoundTrip() {
        listOf(0xFFA4472F, 0xFF2F5D8A, 0xFFFFFFFF, 0xFF000000, 0xFF8AB4F8, 0xFFFFD60A, 0xFF777777).forEach { c ->
            val hsv = ColorMath.toHsv(c)
            val back = ColorMath.fromHsv(hsv[0], hsv[1], hsv[2])
            assertTrue(ColorMath.toHex(c) + " -> " + ColorMath.toHex(back), abs(ColorMath.red(c) - ColorMath.red(back)) <= 1)
            assertTrue(abs(ColorMath.green(c) - ColorMath.green(back)) <= 1)
            assertTrue(abs(ColorMath.blue(c) - ColorMath.blue(back)) <= 1)
        }
    }

    @Test fun contrastExtremes() {
        assertEquals(21.0, ColorMath.contrast(ColorMath.WHITE, 0xFF000000), 0.01)
        assertEquals(1.0, ColorMath.contrast(0xFF777777, 0xFF777777), 0.001)
    }
}

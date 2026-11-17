package com.example.homemenu.domain

import com.example.homemenu.domain.backup.BackupAnalyzer
import com.example.homemenu.domain.backup.BackupData
import com.example.homemenu.domain.backup.BackupIngredient
import com.example.homemenu.domain.backup.BackupJson
import com.example.homemenu.domain.backup.BackupManifest
import com.example.homemenu.domain.backup.BackupMenuEntry
import com.example.homemenu.domain.backup.BackupRecipe
import com.example.homemenu.domain.backup.BackupSettings
import com.example.homemenu.domain.backup.BackupStep
import com.example.homemenu.domain.backup.BackupTag
import com.example.homemenu.domain.backup.DuplicateInfo
import com.example.homemenu.domain.backup.ImportReport
import com.example.homemenu.domain.backup.IncompatibleBackupException
import com.example.homemenu.domain.backup.ManifestFile
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test

/** Task 4: pure parts of the ZIP/JSON import pipeline (no Android, no database). */
class BackupAnalyzerTest {

    private val data = BackupData(
        tags = listOf(BackupTag("Суп", "green")),
        recipes = listOf(
            BackupRecipe(
                id = 1, name = "Борщ", photo = "a.jpg",
                ingredients = listOf(BackupIngredient("Свёкла", amount = 1.0, unit = "PCS"), BackupIngredient("Соль", amount = 1.0, unit = "PINCH")),
                steps = listOf(BackupStep("Сварить", "b.jpg")),
                tags = listOf("Суп"),
            ),
            BackupRecipe(
                id = 2, name = "Чай",
                ingredients = listOf(BackupIngredient("Чай", amount = 1.0, unit = "PCS")),
                steps = listOf(BackupStep("Заварить")),
                tags = listOf("суп ", "Напиток"),
            ),
            BackupRecipe(id = 3, name = "   "),
        ),
        menu = listOf(
            BackupMenuEntry("2026-11-09", "LUNCH", 1, 2),
            BackupMenuEntry("2026-11-09", "LUNCH", 2, 2, position = 1),
            BackupMenuEntry("2026-11-09", "DINNER", 99, 1),
        ),
    )

    @Test fun `key ignores case, yo, outer and repeated spaces`() {
        assertThat(BackupAnalyzer.key("  Ёжик   в  Тумане ")).isEqualTo("ежик в тумане")
        assertThat(BackupAnalyzer.key("БОРЩ")).isEqualTo(BackupAnalyzer.key("борщ"))
    }

    @Test fun `referenced photos include recipe and step photos`() {
        assertThat(BackupAnalyzer.referencedPhotos(data)).containsExactly("a.jpg", "b.jpg")
    }

    @Test fun `analyze counts, duplicates and warnings`() {
        val p = BackupAnalyzer.analyze(
            data,
            existingByKey = mapOf("борщ" to 50L),
            availablePhotos = setOf("a.jpg"),
            sourceKind = "zip",
        )
        assertThat(p.sourceKind).isEqualTo("zip")
        assertThat(p.recipes).isEqualTo(2)
        assertThat(p.ingredients).isEqualTo(3)
        assertThat(p.steps).isEqualTo(2)
        assertThat(p.tags).isEqualTo(2)
        assertThat(p.menuEntries).isEqualTo(2)
        assertThat(p.photosReferenced).isEqualTo(2)
        assertThat(p.photosAvailable).isEqualTo(1)
        assertThat(p.missingPhotos).containsExactly("b.jpg")
        assertThat(p.duplicates).containsExactly(DuplicateInfo(1, "Борщ", 50))
        assertThat(p.newRecipes).isEqualTo(1)
        assertThat(p.hasSettings).isFalse()
        assertThat(p.warnings).hasSize(3)
    }

    @Test fun `analyze of a clean file has no warnings`() {
        val clean = data.copy(recipes = data.recipes.take(2).map { it.copy(photo = null, steps = emptyList()) }, menu = data.menu.take(2))
        val p = BackupAnalyzer.analyze(clean, emptyMap(), emptySet(), "json", extraWarnings = emptyList())
        assertThat(p.warnings).isEmpty()
        assertThat(p.duplicates).isEmpty()
        assertThat(p.newRecipes).isEqualTo(2)
    }

    @Test fun `extra warnings are kept`() {
        val p = BackupAnalyzer.analyze(BackupData(), emptyMap(), emptySet(), "zip", extraWarnings = listOf("x"))
        assertThat(p.warnings).containsExactly("x")
    }

    @Test fun `image signatures`() {
        fun head(vararg b: Int) = ByteArray(16).also { arr -> b.forEachIndexed { i, v -> arr[i] = v.toByte() } }
        assertThat(BackupAnalyzer.isImage(head(0xFF, 0xD8, 0xFF))).isTrue()
        assertThat(BackupAnalyzer.isImage(head(0x89, 0x50, 0x4E, 0x47))).isTrue()
        assertThat(BackupAnalyzer.isImage(head(0x52, 0x49, 0x46, 0x46, 0, 0, 0, 0, 0x57, 0x45, 0x42, 0x50))).isTrue()
        assertThat(BackupAnalyzer.isImage(head(0x47, 0x49, 0x46, 0x38))).isTrue()
        assertThat(BackupAnalyzer.isImage(head(0, 0, 0, 0, 0x66, 0x74, 0x79, 0x70))).isTrue()
        assertThat(BackupAnalyzer.isImage(head())).isFalse()
        assertThat(BackupAnalyzer.isImage("<html>hello</html>".toByteArray())).isFalse()
        assertThat(BackupAnalyzer.isImage(byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte()))).isFalse()
    }

    @Test fun `photo names cannot escape the photos folder`() {
        assertThat(BackupAnalyzer.safePhotoName("p_1.jpg")).isEqualTo("p_1.jpg")
        assertThat(BackupAnalyzer.safePhotoName("photos/p_1.jpg")).isEqualTo("p_1.jpg")
        assertThat(BackupAnalyzer.safePhotoName("../../etc/passwd")).isEqualTo("passwd")
        assertThat(BackupAnalyzer.safePhotoName("C:\\dir\\p.jpg")).isEqualTo("p.jpg")
        assertThat(BackupAnalyzer.safePhotoName("..")).isNull()
        assertThat(BackupAnalyzer.safePhotoName("a..b.jpg")).isNull()
        assertThat(BackupAnalyzer.safePhotoName("")).isNull()
        assertThat(BackupAnalyzer.safePhotoName("photos/")).isNull()
        assertThat(BackupAnalyzer.safePhotoName("x".repeat(129))).isNull()
    }

    @Test fun `import report summary`() {
        val r = ImportReport(3, 1, 0, 2, 4, 1, emptyList(), emptyList(), verified = true)
        assertThat(r.summary()).isEqualTo("Добавлено рецептов: 3, обновлено: 1. Фото: 2. Блюд в меню: 4 (пропущено повторов: 1).")
    }

    @Test fun `manifest round trip`() {
        val m = BackupManifest(
            appVersion = "1.1.0", exportedAt = 42, recipes = 2, menuEntries = 3, photos = 1,
            files = listOf(ManifestFile("recipes.json", 100, "ab".repeat(32)), ManifestFile("photos/a.jpg", 5, "cd".repeat(32))),
        )
        assertThat(BackupJson.decodeManifest(BackupJson.encodeManifest(m))).isEqualTo(m)
    }

    @Test fun `broken manifest is rejected`() {
        assertThrows(IncompatibleBackupException::class.java) { BackupJson.decodeManifest("not a json") }
    }

    @Test fun `v2 round trip keeps dish order, settings and embedded images`() {
        val full = data.copy(
            appVersion = "1.1.0",
            settings = BackupSettings("LIGHT", themeId = "NATURAL_GREEN", customColors = "{}"),
            images = mapOf("a.jpg" to "AAEC", "b.jpg" to "AwQF"),
        )
        val back = BackupJson.decode(BackupJson.encode(full))
        assertThat(back).isEqualTo(full)
        assertThat(back.menu.map { it.position }).containsExactly(0, 1, 0).inOrder()
        assertThat(back.recipes.first().ingredients.map { it.product }).containsExactly("Свёкла", "Соль").inOrder()
    }
}

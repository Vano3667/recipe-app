package com.example.homemenu.domain

import com.example.homemenu.domain.backup.BACKUP_FORMAT_VERSION
import com.example.homemenu.domain.backup.BackupData
import com.example.homemenu.domain.backup.BackupIngredient
import com.example.homemenu.domain.backup.BackupJson
import com.example.homemenu.domain.backup.BackupMenuEntry
import com.example.homemenu.domain.backup.BackupProduct
import com.example.homemenu.domain.backup.BackupRecipe
import com.example.homemenu.domain.backup.BackupSettings
import com.example.homemenu.domain.backup.BackupStep
import com.example.homemenu.domain.backup.BackupTag
import com.example.homemenu.domain.backup.IncompatibleBackupException
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test

class BackupJsonTest {
    private val sample = BackupData(
        exportedAt = 1_700_000_000_000,
        products = listOf(BackupProduct("Сыр бурата", "G", "DAIRY")),
        tags = listOf(BackupTag("Летнее", "yellow")),
        recipes = listOf(
            BackupRecipe(
                id = 7, name = "Салат с буратой \"для души\"", photo = "p_7.jpg", cookingTimeMin = 15,
                difficulty = "EASY", category = "LUNCH", cuisine = "Итальянская", calories = 320, servings = 2,
                ingredients = listOf(BackupIngredient("Сыр бурата", "G", "DAIRY", 125.0, "G"), BackupIngredient("Соль", amount = 1.0, unit = "PINCH")),
                steps = listOf(BackupStep("Нарезать", "s_1.jpg"), BackupStep("Смешать")),
                notes = "Многострочная\nзаметка 🍅", tags = listOf("Летнее"), createdAt = 123,
            ),
        ),
        menu = listOf(BackupMenuEntry("2026-11-09", "LUNCH", 7, 3)),
        settings = BackupSettings("DARK"),
    )

    @Test fun `round trip keeps all data`() {
        val text = BackupJson.encode(sample)
        assertThat(text).contains("\"formatVersion\": $BACKUP_FORMAT_VERSION")
        assertThat(BackupJson.decode(text)).isEqualTo(sample)
    }

    @Test fun `empty backup round trips`() {
        assertThat(BackupJson.decode(BackupJson.encode(BackupData()))).isEqualTo(BackupData())
    }

    @Test fun `unknown fields are ignored`() {
        val text = """{"formatVersion":1,"futureField":true,"recipes":[{"id":1,"name":"Чай","extra":5}]}"""
        assertThat(BackupJson.decode(text).recipes.single().name).isEqualTo("Чай")
    }

    @Test fun `newer version is rejected`() {
        val e = assertThrows(IncompatibleBackupException::class.java) { BackupJson.decode("""{"formatVersion":99}""") }
        assertThat(e.message).contains("99")
    }

    @Test fun `zero version is rejected`() {
        assertThrows(IncompatibleBackupException::class.java) { BackupJson.decode("""{"formatVersion":0}""") }
    }

    @Test fun `missing version is rejected`() {
        assertThrows(IncompatibleBackupException::class.java) { BackupJson.decode("""{"recipes":[]}""") }
    }

    @Test fun `malformed json is rejected`() {
        assertThrows(IncompatibleBackupException::class.java) { BackupJson.decode("not a json") }
        assertThrows(IncompatibleBackupException::class.java) { BackupJson.decode("") }
    }

    @Test fun `wrong field types are rejected`() {
        assertThrows(IncompatibleBackupException::class.java) {
            BackupJson.decode("""{"formatVersion":1,"recipes":[{"id":"abc","name":1}]}""")
        }
    }
}

package com.example.homemenu.domain

import com.example.homemenu.domain.backup.BackupData
import com.example.homemenu.domain.backup.BackupJson
import com.google.common.truth.Truth.assertThat
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import org.junit.Assert.assertThrows
import org.junit.Test

/** Task 6 / user check 23.2: the real JSON file that failed to import on the user's phone. */
class FixtureJsonDecodeTest {

    @Test fun `fixture is the original unmodified file`() {
        val f = UserFixture.file()
        assertThat(f.length()).isEqualTo(UserFixture.SIZE_BYTES)
        val text = UserFixture.text()
        assertThat(text).contains("\"amount\": null")
        assertThat(text).contains("\"cookingTimeMin\": null")
    }

    @Test fun `root cause - a strict reader rejects explicit nulls in the fixture`() {
        val strict = Json { ignoreUnknownKeys = true }
        assertThrows(SerializationException::class.java) {
            strict.decodeFromString(BackupData.serializer(), UserFixture.text())
        }
    }

    @Test fun `fixture decodes completely`() {
        val data = BackupJson.decode(UserFixture.text())
        assertThat(data.version).isEqualTo(1)
        assertThat(data.recipes).hasSize(UserFixture.RECIPES)
        assertThat(data.recipes.sumOf { it.ingredients.size }).isEqualTo(UserFixture.INGREDIENTS)
        assertThat(data.recipes.sumOf { it.steps.size }).isEqualTo(UserFixture.STEPS)
        assertThat(data.recipes.map { it.id }).containsExactlyElementsIn((1L..140L).toList()).inOrder()
    }

    @Test fun `first recipe keeps names amounts units order and tags`() {
        val r = BackupJson.decode(UserFixture.text()).recipes.first()
        assertThat(r.name).isEqualTo("Сырники")
        assertThat(r.calories).isEqualTo(443)
        assertThat(r.servings).isEqualTo(1)
        // explicit nulls -> defaults
        assertThat(r.cookingTimeMin).isEqualTo(0)
        assertThat(r.difficulty).isEqualTo("EASY")
        assertThat(r.category).isEqualTo("DINNER")
        assertThat(r.ingredients.map { it.product })
            .containsExactly("яйцо", "рисовой муки", "брикетного творога 5%", "масла гхи", "подсластитель по вкусу").inOrder()
        assertThat(r.ingredients.map { it.amount }).containsExactly(1.0, 30.0, 180.0, 2.0, 0.0).inOrder()
        assertThat(r.ingredients.map { it.unit }).containsExactly("PCS", "G", "G", "G", "TO_TASTE").inOrder()
        assertThat(r.ingredients.last().productUnit).isEqualTo("G") // was null
        assertThat(r.ingredients.map { it.department }).containsExactly("DAIRY", "GRAINS", "FISH", "SAUCES", "PANTRY").inOrder()
        assertThat(r.steps).hasSize(6)
        assertThat(r.steps.first().text).isEqualTo("Смешайте в миске творог, муку, яйцо и подсластитель.")
        assertThat(r.tags).containsExactly("Неделя 1", "Понедельник", "Завтрак").inOrder()
        assertThat(r.notes).startsWith("Источник: PDF, стр. 9.")
    }

    @Test fun `last recipe keeps its ingredient order`() {
        val r = BackupJson.decode(UserFixture.text()).recipes.last()
        assertThat(r.id).isEqualTo(140)
        assertThat(r.name).isEqualTo("Индейка, тушеная в сливках с брокколи")
        assertThat(r.ingredients.map { it.product })
            .containsExactly("филе грудки индейки", "лука", "брокколи", "масла гхи", "соль", "специи по вкусу", "сливок 10%").inOrder()
        assertThat(r.ingredients.map { it.amount }).containsExactly(120.0, 15.0, 150.0, 3.0, 0.0, 0.0, 120.0).inOrder()
        assertThat(r.ingredients.map { it.unit }).containsExactly("G", "G", "G", "G", "TO_TASTE", "TO_TASTE", "G").inOrder()
    }

    @Test fun `every to-taste ingredient of the fixture is read with amount 0`() {
        val all = BackupJson.decode(UserFixture.text()).recipes.flatMap { it.ingredients }
        val toTaste = all.filter { it.unit == "TO_TASTE" }
        assertThat(toTaste).hasSize(185)
        assertThat(toTaste.map { it.amount }.toSet()).containsExactly(0.0)
        assertThat(all.count { it.unit == "G" }).isEqualTo(537)
    }
}

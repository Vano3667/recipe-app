package com.example.homemenu.domain

import com.example.homemenu.F
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/** Task 5 / 23.10: shopping list text for WhatsApp / Telegram. */
class ShoppingShareTextTest {
    private val useCase = ShoppingListUseCase(ScalingUseCase())
    private val groups = useCase.build(listOf(PlannedDish(F.omelette, 1)))
    private val items = groups.flatMap { it.items }
    private fun key(name: String) = items.first { it.product.name == name }.key

    @Test fun `default text keeps old lines and adds bought counter`() {
        val text = useCase.shareText("Покупки", groups, setOf(key("Яйцо куриное")))
        assertThat(text).startsWith("🛒 Покупки")
        assertThat(text).contains("☑ Яйцо куриное — 3 шт")
        assertThat(text).contains("☐ Молоко — 50 мл")
        assertThat(text).endsWith("Куплено: 1 из ${items.size}")
    }

    @Test fun `nothing bought gives no counter`() {
        val text = useCase.shareText("Покупки", groups, emptySet())
        assertThat(text).doesNotContain("Куплено")
        assertThat(text).doesNotContain("☑")
    }

    @Test fun `only remaining drops bought items`() {
        val text = useCase.shareText("Покупки", groups, setOf(key("Яйцо куриное")), onlyRemaining = true)
        assertThat(text).doesNotContain("Яйцо куриное")
        assertThat(text).doesNotContain("☑")
        assertThat(text).contains("☐ Молоко — 50 мл")
        assertThat(text).doesNotContain("Куплено")
    }

    @Test fun `only remaining drops departments with nothing left`() {
        val first = groups.first()
        val text = useCase.shareText("Покупки", groups, first.items.map { it.key }.toSet(), onlyRemaining = true)
        assertThat(text).doesNotContain("\n${first.department.title}:")
    }

    @Test fun `only remaining with everything bought says so`() {
        val text = useCase.shareText("Покупки", groups, items.map { it.key }.toSet(), onlyRemaining = true)
        assertThat(text).contains("✅ Всё куплено")
        assertThat(text).doesNotContain("☐")
    }
}

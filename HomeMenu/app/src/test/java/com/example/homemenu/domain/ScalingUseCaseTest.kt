package com.example.homemenu.domain

import com.example.homemenu.F
import com.example.homemenu.domain.model.MeasureUnit
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test

class ScalingUseCaseTest {
    private val scaling = ScalingUseCase()

    @Test fun `whole result is exact`() {
        assertThat(scaling.scale(200.0, 2, 4)).isEqualTo(ScaledAmount(400, false))
    }

    @Test fun `fractional result is rounded up and marked approximate`() {
        // 1 egg for 4 servings -> 3 servings = 0.75
        assertThat(scaling.scale(1.0, 4, 3)).isEqualTo(ScaledAmount(1, true))
        // 3 pcs for 2 -> 3 = 4.5
        assertThat(scaling.scale(3.0, 2, 3)).isEqualTo(ScaledAmount(5, true))
    }

    @Test fun `format shows tilde only for approximate values`() {
        assertThat(ScaledAmount(5, true).format(MeasureUnit.PCS)).isEqualTo("~5 шт")
        assertThat(ScaledAmount(400, false).format(MeasureUnit.G)).isEqualTo("400 г")
    }

    @Test fun `floating point noise is not treated as fraction`() {
        assertThat(AmountRounding.roundUp(0.1 * 3 * 10)).isEqualTo(ScaledAmount(3, false))
        assertThat(AmountRounding.roundUp(2.0000000001)).isEqualTo(ScaledAmount(2, false))
    }

    @Test fun `zero or negative amount gives zero`() {
        assertThat(AmountRounding.roundUp(0.0)).isEqualTo(ScaledAmount(0, false))
        assertThat(AmountRounding.roundUp(-3.0)).isEqualTo(ScaledAmount(0, false))
    }

    @Test fun `non positive servings are rejected`() {
        assertThrows(IllegalArgumentException::class.java) { scaling.scaleRaw(100.0, 0, 2) }
        assertThrows(IllegalArgumentException::class.java) { scaling.scaleRaw(100.0, 2, 0) }
        assertThrows(IllegalArgumentException::class.java) { scaling.scaleRaw(100.0, 2, -1) }
    }

    @Test fun `scaleAll keeps order and units`() {
        val r = F.pancakes // 4 servings
        val scaled = scaling.scaleAll(r.ingredients, r.servings, 2)
        assertThat(scaled.map { it.text }).containsExactly("100 г", "250 мл", "1 шт", "~1 ст. л.").inOrder()
    }

    @Test fun `same servings returns original amounts`() {
        val r = F.chickenRice
        assertThat(scaling.scaleAll(r.ingredients, r.servings, r.servings).map { it.amount.value }).containsExactly(300L, 150L, 100L).inOrder()
    }
}

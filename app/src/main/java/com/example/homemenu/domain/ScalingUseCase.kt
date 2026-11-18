package com.example.homemenu.domain

import com.example.homemenu.domain.model.Ingredient
import com.example.homemenu.domain.model.MeasureUnit
import javax.inject.Inject
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.roundToLong

/** Rounded-up amount. [approximate] = the exact value was fractional, so it is shown as "~N". */
data class ScaledAmount(val value: Long, val approximate: Boolean) {
    fun format(unit: MeasureUnit): String =
        if (unit.isAmountless) unit.label else "${if (approximate) "~" else ""}$value ${unit.label}"
}

data class ScaledIngredient(val ingredient: Ingredient, val amount: ScaledAmount) {
    val text: String get() = amount.format(ingredient.unit)
}

object AmountRounding {
    private const val EPS = 1e-6

    /** Rounds up; whole values (within floating error) are exact, fractional ones get a tilde. */
    fun roundUp(raw: Double): ScaledAmount {
        if (raw <= 0.0) return ScaledAmount(0, false)
        val nearest = raw.roundToLong()
        return if (abs(raw - nearest) < EPS) ScaledAmount(nearest, false)
        else ScaledAmount(ceil(raw).toLong(), true)
    }
}

class ScalingUseCase @Inject constructor() {

    /** amount × (targetServings / baseServings), exact (not rounded). */
    fun scaleRaw(amount: Double, baseServings: Int, targetServings: Int): Double {
        require(baseServings > 0) { "baseServings must be > 0" }
        require(targetServings > 0) { "targetServings must be > 0" }
        return amount * targetServings.toDouble() / baseServings.toDouble()
    }

    fun scale(amount: Double, baseServings: Int, targetServings: Int): ScaledAmount =
        AmountRounding.roundUp(scaleRaw(amount, baseServings, targetServings))

    fun scaleAll(ingredients: List<Ingredient>, baseServings: Int, targetServings: Int): List<ScaledIngredient> =
        ingredients.map { ScaledIngredient(it, scale(it.amount, baseServings, targetServings)) }
}

package cognosco.gen

import kotlin.math.pow
import kotlin.random.Random

class DoubleRangeDevelopment(
    private val random: Random,
    private val average: Development<Double>,
    private val maxDerivation: Development<Double>,
    private val gravity: Development<Double>
) : Development<Double> {
    override fun at(time: Double): Double {
        val avg = average.at(time)
        val g = gravity.at(time)
        val maxDer = maxDerivation.at(time).pow(1 / g)
        val derivation = random.nextDouble(maxDer).pow(g)
        val sign = listOf(-1, +1).random(random)
        return avg + derivation * sign
    }

    override fun toString(): String {
        return "range (average=$average, maxDerivation=$maxDerivation, gravity=$gravity)"
    }
}
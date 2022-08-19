package cognosco.gen

import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.random.Random

class IntRangeDevelopment(
    private val random: Random,
    private val average: Development<Int>,
    private val maxDerivation: Development<Int>,
    private val gravity: Development<Double>
) : Development<Int> {
    override fun at(time: Double): Int {
        val avg = average.at(time)
        val g = gravity.at(time)
        val maxDer = maxDerivation.at(time).toDouble().pow(1 / g).roundToInt()
        val derivation = random.nextInt(maxDer).toDouble().pow(g).roundToInt()
        val sign = listOf(-1, +1).random(random)
        return avg + derivation * sign
    }

    override fun toString(): String {
        return "range (average=$average, maxDerivation=$maxDerivation, gravity=$gravity)"
    }

}
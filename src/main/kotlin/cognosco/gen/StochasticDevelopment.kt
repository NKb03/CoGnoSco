package cognosco.gen

import kotlin.random.Random

class StochasticDevelopment<T>(
    private val random: Random,
    private val options: List<Option<T>>
) : Development<T> {
    override fun at(time: Double): T {
        val probabilities = options.map { o ->
            o.probability.at(time).also { p ->
                require(p >= 0) { "invalid probability at $time given by $o: $p" }
            }
        }
        val total = probabilities.sum()
        val segments = probabilities.runningFold(0.0, Double::plus)
        val r = random.nextDouble(until = total + 0.001)
        var i = segments.binarySearch(r)
        if (i < 0) i = -(i + 2)
        return options[i.coerceAtMost(options.size - 1)].value.at(time)
    }

    override fun toString(): String = "stochastic development: $options"

    data class Option<out T>(val probability: Development<Double>, val value: Development<T>) {
        override fun toString(): String = "($value with probability $probability)"
    }
}
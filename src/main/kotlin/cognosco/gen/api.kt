package cognosco.gen

import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt
import kotlin.random.Random

fun <T> FixedDurationDevelopment<T>.at(time: Int): T {
    require(time in timeRange)
    return at(time / duration.toDouble())
}

val <T> FixedDurationDevelopment<T>.timeRange get() = 0 until duration

val <T> FixedDurationDevelopment<T>.values get() = timeRange.map { t -> at(t) }

fun <T> development(stringRepresentation: String, f: (t: Double) -> T): Development<T> =
    SimpleDevelopment(stringRepresentation, f)

fun <T> constant(value: T): Development<T> = development("constant $value") { value }

fun linearDevelopment(start: Double, end: Double): Development<Double> =
    development("from $start to $end") { t -> (1 - t) * start + t * end }

fun linearDevelopment(start: Int, end: Int): Development<Int> =
    linearDevelopment(start.toDouble(), end.toDouble()).transform("round", Double::roundToInt)

fun <T> id() = { x: T -> x }

fun <T, F> Development<T>.transform(transformationName: String, f: (T) -> F): Development<F> =
    TransformedDevelopment(this, id(), f, transformationName)

fun <T> Development<T>.transformTime(transformationName: String, f: (t: Double) -> Double): Development<T> =
    TransformedDevelopment(this, f, id(), transformationName)

private fun <T> FixedDurationDevelopment<T>.getSegments(): List<FixedDurationDevelopment<T>> =
    if (this is SequentialDevelopment) segments.flatMap { it.getSegments() }
    else listOf(this)

fun <T> sequential(segments: List<FixedDurationDevelopment<T>>): FixedDurationDevelopment<T> =
    SequentialDevelopment(segments.flatMap { s -> s.getSegments() })

fun <T> sequential(vararg segments: FixedDurationDevelopment<T>) = sequential(segments.asList())

infix fun <T> FixedDurationDevelopment<T>.then(next: FixedDurationDevelopment<T>) = sequential(this, next)

fun <T> Development<T>.reverse() = transformTime("reverse") { t -> 1 - t }

fun <T> Development<T>.smoothIn() = transformTime("smooth in") { t -> t.pow(2) }

fun <T> Development<T>.smoothOut() = transformTime("smooth out") { t -> sqrt(t) }

fun <T> Development<T>.smoothInAndOut(exponentIn: Double = 2.0, exponentOut: Double = exponentIn): Development<T> {
    val n = (0.25).pow(exponentIn)
    val m = (0.75).pow(1 / exponentOut) - n
    return transformTime("smooth with e1 = $exponentIn and e2 = $exponentOut") { t ->
        when (t) {
            in 0.0..0.25 -> t.pow(exponentIn)
            in 0.25..0.75 -> {
                val x = (t - 0.25) * 2
                m * x + n
            }

            else -> t.pow(1 / exponentOut)
        }
    }
}

infix fun <T> Development<T>.withDuration(duration: Int) = SimpleFixedDurationDevelopment(duration, this)

infix fun <T> Development<T>.withProbability(probability: Development<Double>) =
    StochasticDevelopment.Option(probability, this)

infix fun <T> T.withProbability(probability: Development<Double>) = constant(this) withProbability probability

fun <T> stochasticDevelopment(random: Random, options: List<StochasticDevelopment.Option<T>>): Development<T> =
    StochasticDevelopment(random, options)

fun <T> stochasticDevelopment(
    random: Random,
    vararg options: StochasticDevelopment.Option<T>
) = stochasticDevelopment(random, options.asList())

fun stochasticBooleanDevelopment(random: Random, probability: Development<Double>) =
    stochasticDevelopment(
        random,
        true withProbability probability,
        false withProbability probability.transform("inverse probability") { p -> 1 - p }
    )

@JvmName("doubleRangeDevelopment")
fun rangeDevelopment(
    random: Random,
    average: Development<Double>,
    maxDerivation: Development<Double>,
    gravity: Development<Double> = constant(1.0)
): Development<Double> = DoubleRangeDevelopment(random, average, maxDerivation, gravity)

@JvmName("intRangeDevelopment")
fun rangeDevelopment(
    random: Random,
    average: Development<Int>,
    maxDerivation: Development<Int>,
    gravity: Development<Double> = constant(1.0)
): Development<Int> = IntRangeDevelopment(random, average, maxDerivation, gravity)

fun <T, F> FixedDurationDevelopment<T>.keepDuration(
    transformation: FixedDurationDevelopment<T>.() -> Development<F>
): FixedDurationDevelopment<F> = SimpleFixedDurationDevelopment(duration, transformation(this))

fun Development<Double>.coerceAtMost(upperBound: Double) =
    transform("coerced <= $upperBound") { v -> v.coerceAtMost(upperBound) }

fun Development<Double>.coerceAtLeast(lowerBound: Double) =
    transform("coerced >= $lowerBound") { v -> v.coerceAtLeast(lowerBound) }

fun <T> FixedDurationDevelopment<T>.chooseUntil(t: Int, condition: (T) -> Boolean): T {
    while (true) {
        val v = at(t)
        if (condition(v)) return v
    }
}

fun main() {
    val values = linearDevelopment(0.0, 1.0).smoothInAndOut(1.5).withDuration(25).values
    println(values)
    for (value in values) {
        print(" ".repeat((value * 25).roundToInt()))
        println("x")
    }
}

/*
* f(x) = ax^3 + bx^2 + cx + d
* f'(x) = 3ax^2 + 2bx + c
* f''(x) = 6ax + 2b
* f(0) = 0 => d = 0
* f(1) = 1 => a + b + c = 1 => a = 4 => c = 3 und b = -6
* f'(1/2) = 0 => 3/4a + b + c = 0 => c = 3/4a
* f''(1/2) = 0 => 3a + 2b = 0 => 2b = -3a => b = -3/2a
*
* ==> f(x) = 4x^3 - 6x^2 + 3x
* ==> f'(x) = 12x^2 - 12x + 3 => f'(1/2) = 3 - 6 + 3 = 0
* ==> f''(x) = 24x - 12 => f''(1/2) = 0
* */
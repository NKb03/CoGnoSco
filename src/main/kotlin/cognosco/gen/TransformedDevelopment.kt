package cognosco.gen

class TransformedDevelopment<T, F>(
    private val source: Development<T>,
    private val transformTime: (Double) -> Double,
    private val transformValue: (T) -> F,
    private val transformationName: String
) : Development<F> {
    override fun at(time: Double): F = transformValue(source.at(transformTime(time)))

    override fun toString(): String = "($source transformed with $transformationName)"
}
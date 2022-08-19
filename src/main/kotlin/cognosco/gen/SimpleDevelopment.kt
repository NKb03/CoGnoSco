package cognosco.gen

class SimpleDevelopment<T>(
    private val stringRepresentation: String,
    private val valueAt: (t: Double) -> T
) : Development<T> {
    override fun at(time: Double): T = valueAt(time)

    override fun toString(): String = stringRepresentation
}
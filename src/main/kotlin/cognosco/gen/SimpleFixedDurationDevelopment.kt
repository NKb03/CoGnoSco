package cognosco.gen

class SimpleFixedDurationDevelopment<T>(override val duration: Int, private val development: Development<T>) :
    FixedDurationDevelopment<T> {
    override fun at(time: Double): T = development.at(time)
    override fun toString(): String {
        return "($development with duration $duration)"
    }
}
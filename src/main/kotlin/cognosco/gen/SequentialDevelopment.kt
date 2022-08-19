package cognosco.gen

class SequentialDevelopment<T>(val segments: List<FixedDurationDevelopment<T>>) : FixedDurationDevelopment<T> {
    override val duration: Int = segments.sumOf(FixedDurationDevelopment<T>::duration)

    private val starts = segments.runningFold(0.0) { t, s -> t + s.duration }.map { t -> t / duration.toDouble() }

    override fun at(time: Double): T {
        var i = starts.binarySearch(time)
        if (i < 0) i = -(i + 2)
        while (i + 1 < starts.size && starts[i] == starts[i + 1]) i++
        val s = segments[i]
        return s.at((time - starts[i]) * (duration / s.duration))
    }

    override fun toString(): String {
        return "sequential ${segments.joinToString("\n", "\n", "\n") { s -> "  $s" }}"
    }
}
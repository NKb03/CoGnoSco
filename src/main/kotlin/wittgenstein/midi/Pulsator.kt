package wittgenstein.midi

class Pulsator {
    private val listeners = mutableListOf<() -> Unit>()

    var pulse: Int = 0
        private set

    fun addListener(listener: () -> Unit) = also {
        listeners.add(listener)
    }

    fun reset() = also {
        pulse = 0
        listeners.forEach { it.invoke() }
    }

    fun nextPulse() = also {
        pulse += 1
        listeners.forEach { it.invoke() }
    }

    inline fun start(lastPulse: Int, action: (pulse: Int) -> Unit) {
        while (pulse <= lastPulse) {
            action(pulse)
            nextPulse()
        }
    }

    fun realtime(millisecondsPerPulse: Long) = addListener { Thread.sleep(millisecondsPerPulse) }


    override fun toString(): String = "Pulsator [ pulse = $pulse ]"
}
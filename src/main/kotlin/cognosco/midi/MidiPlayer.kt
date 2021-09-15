package cognosco.midi

class MidiPlayer(private val output: MidiOutput) {
    private val listeners = mutableListOf<(Int) -> Unit>()
    private var pulseMap: Map<Int, List<Event>> = emptyMap()

    @set: Synchronized
    var currentPulse = 0
        set(value) {
            field = value
            listeners.forEach { it.invoke(value) }
        }

    @set: Synchronized
    var isPlaying = false
        private set

    @Synchronized
    fun setEvents(events: List<Event>) {
        pulseMap = events.groupBy { it.pulse }
    }

    @Synchronized
    fun addListener(listener: (pulse: Int) -> Unit) = also {
        listeners.add(listener)
    }

    fun realtime(millisecondsPerPulse: Long) = addListener {
        Thread.sleep(millisecondsPerPulse)
    }

    @Synchronized
    fun play() {
        check(!isPlaying) { "already playing" }
        isPlaying = true
        output.resume()
        PlayerThread().start()
    }

    private inner class PlayerThread : Thread() {
        private val parent: Thread = currentThread()

        init {
            isDaemon = true
        }

        override fun run() {
            val lastPulse = pulseMap.keys.maxOrNull() ?: return
            while (isPlaying && currentPulse <= lastPulse) {
                val events = pulseMap[currentPulse].orEmpty()
                try {
                    synchronized(this) {
                        for (ev in events) ev.action()
                        output.receivePulse(currentPulse)
                        currentPulse += 1
                    }
                } catch (e: Throwable) {
                    parent.uncaughtExceptionHandler.uncaughtException(this, e)
                    this@MidiPlayer.stop()
                    break
                }
            }
            if (isPlaying) this@MidiPlayer.stop()
        }
    }

    @Synchronized
    fun pause() {
        check(isPlaying) { "not playing" }
        isPlaying = false
        output.pause()
    }

    @Synchronized
    fun stop() {
        if (isPlaying) pause()
        output.stopAll()
        currentPulse = 0
    }

    override fun toString(): String = "MidiPlayer [ playing = $isPlaying, currentPulse = $currentPulse ]"
}
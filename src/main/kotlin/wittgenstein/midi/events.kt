package wittgenstein.midi

data class Event(val pulse: Int, val action: () -> Unit)

const val PULSES_PER_BEAT = 32

class EventListBuilder(private val handle: MidiOutput.NoteHandle) {
    private val result = mutableListOf<Event>()

    fun at(beat: Int, action: MidiOutput.NoteHandle.() -> Unit) {
        val pulse = beat * PULSES_PER_BEAT
        result.add(Event(pulse) { handle.action() })
    }

    fun build(): List<Event> = result
}

fun MidiOutput.eventList(block: EventListBuilder.() -> Unit): List<Event> =
    EventListBuilder(createNoteHandle()).apply(block).build()

fun MidiOutput.play(pulseMap: Map<Int, List<Event>>) {
    val lastPulse = pulseMap.keys.maxOrNull()!!
    while (pulsator.pulse <= lastPulse) {
        val events = pulseMap[pulsator.pulse].orEmpty()
        for (ev in events) {
            ev.action()
        }
        pulsator.nextPulse()
    }
}

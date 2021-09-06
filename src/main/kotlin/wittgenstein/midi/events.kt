package wittgenstein.midi

data class Event(val pulse: Int, val action: () -> Unit)

const val PULSES_PER_BEAT = 32

class EventListBuilder(private val handle: MidiOutput.NoteHandle) {
    private val result = mutableListOf<Event>()

    fun at(pulse: Int, action: MidiOutput.NoteHandle.() -> Unit) {
        result.add(Event(pulse) { handle.action() })
    }

    fun build(): List<Event> = result
}

fun MidiOutput.eventList(block: EventListBuilder.() -> Unit): List<Event> =
    EventListBuilder(createNoteHandle()).apply(block).build()

fun MidiOutput.play(pulseMap: Map<Int, List<Event>>) {
    val lastPulse = pulseMap.keys.maxOrNull()!!
    pulsator.start(lastPulse) {
        val events = pulseMap[pulsator.pulse].orEmpty()
        for (ev in events) {
            ev.action()
        }
    }
}

fun List<Event>.toPulseMap(): Map<Int, List<Event>> = groupBy { it.pulse }
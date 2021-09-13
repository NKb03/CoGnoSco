package cognosco.midi

import cognosco.*

data class Event(val pulse: Int, val action: () -> Unit)

class EventListBuilder(private val handle: MidiOutput.NoteHandle) {
    private val result = mutableListOf<Event>()

    fun at(pulse: Int, action: MidiOutput.NoteHandle.() -> Unit) {
        result.add(Event(pulse) { handle.action() })
    }

    fun build(): List<Event> = result
}

fun MidiOutput.eventList(block: EventListBuilder.() -> Unit): List<Event> =
    EventListBuilder(createNoteHandle()).apply(block).build()

const val PULSES_PER_BEAT = 32

fun GraphicalScore.createEvents(output: MidiOutput): List<Event> =
    elements.flatMap { el ->
        output.eventList {
            at(el.start.value * PULSES_PER_BEAT) {
                noteOn(el.instrument.value!!, el.pitch.value, el.startDynamic.value)
                when {
                    el is Trill -> trill(el.secondaryPitch.value)
                    el.type.value == SimplePitchedContinuousElement.Type.FastRepeat -> tremolo(4)
                    el.type.value == SimplePitchedContinuousElement.Type.Repeat -> tremolo(32)
                    el.type.value == DiscretePitchedElement.Type.Percussive
                            && el.instrument.value!!.family == InstrumentFamily.Strings ->
                        programChange(45)  //Pizzicato Strings
                }
            }
            if (el is ContinuousElement) {
                var start = el.start.value
                for (phase in el.phases.value) {
                    at(start * PULSES_PER_BEAT) {
                        gradualVolumeChange(phase.end.value, phase.targetDynamic.value)
                    }
                    start = phase.end.value
                }
            }
            at(el.end.value * PULSES_PER_BEAT) { noteOff() }
        }
    }

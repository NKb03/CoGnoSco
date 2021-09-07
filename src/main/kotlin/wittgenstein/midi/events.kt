package wittgenstein.midi

import wittgenstein.*

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
            at(el.start * PULSES_PER_BEAT) {
                noteOn(el.instrument!!, el.pitch, el.startDynamic!!)
                when {
                    el is Trill -> trill(el.secondaryPitch!!)
                    el.type == SimplePitchedContinuousElement.FastRepeat -> tremolo(4)
                    el.type == SimplePitchedContinuousElement.Repeat -> tremolo(32)
                    el.type == DiscretePitchedElement.Percussive && el.instrument!!.family == InstrumentFamily.Strings ->
                        programChange(45)  //Pizzicato Strings
                }
            }
            if (el is ContinuousElement) {
                at(el.start * PULSES_PER_BEAT) {
                    gradualVolumeChange(
                        el.climax * PULSES_PER_BEAT,
                        el.climaxDynamic!!
                    )
                }
                at(el.climax * PULSES_PER_BEAT) {
                    gradualVolumeChange(
                        el.end * PULSES_PER_BEAT,
                        el.endDynamic!!
                    )
                }
            }
            at(el.end * PULSES_PER_BEAT) { noteOff() }
        }
    }

package cognosco.midi

import cognosco.ContinuousElement
import cognosco.DiscretePitchedElement.Type.Percussive
import cognosco.GraphicalScore
import cognosco.InstrumentFamily.Strings
import cognosco.SimplePitchedContinuousElement.Type.FastRepeat
import cognosco.SimplePitchedContinuousElement.Type.Repeat
import cognosco.Trill
import java.io.File
import javax.sound.midi.MidiSystem

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

const val PULSES_PER_BEAT = 64

fun GraphicalScore.createEvents(output: MidiOutput): List<Event> =
    elements.flatMap { element ->
        output.eventList {
            at(element.start.value * PULSES_PER_BEAT) {
                setup()
                val instr = element.instrument.value!!
                setInstrument(instr)
                if (instr.family == Strings) {
                    when (element.type.value) {
                        Percussive -> programChange(46)
                        FastRepeat -> programChange(45)
                        else -> {
                        }
                    }
                }
                noteOn(element.pitch.value, element.startDynamic.value)
                when {
                    element is Trill -> trill(element.secondaryPitch.value)
                    element.type.value == Repeat -> tremolo(32)
                }
            }
            if (element is ContinuousElement) {
                var start = element.start.value
                for (phase in element.phases.value) {
                    at(start * PULSES_PER_BEAT) {
                        gradualVolumeChange(phase.end.value * PULSES_PER_BEAT, phase.targetDynamic.value)
                    }
                    start = phase.end.value
                }
            }
            at(element.end.value * PULSES_PER_BEAT) { noteOff() }
        }
    }

fun main() {
    val synth = MidiSystem.getSynthesizer()
    synth.open()
    val trian = MidiSystem.getSoundbank(File("/home/nikolaus/Downloads/TrianGMGS.sf2"))
    synth.loadAllInstruments(trian)
    Thread.sleep(100)
    val ch = synth.channels[0]
    ch.programChange(1152, 48)
    ch.noteOn(60, 80)
    Thread.sleep(2000)
}

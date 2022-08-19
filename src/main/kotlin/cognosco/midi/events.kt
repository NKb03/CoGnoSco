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

const val PULSES_PER_BEAT = 44

fun GraphicalScore.createEvents(output: MidiOutput): List<Event> =
    elements.flatMap { element ->
        output.eventList {
            at(element.start.value * PULSES_PER_BEAT) {
                val instr = element.instrument.value!!
                setInstrument(instr)
                setPitch(element.pitch.value!!)
                setDynamic(element.startDynamic.value!!)
                when (element.type.value) {
                    Trill -> {
                        element as Trill
                        trill(element.secondaryPitch.value)
                    }
                    Percussive -> {
                        if (instr.family == Strings) programChange(46)
                    }
                    FastRepeat -> {
                        if (instr.family == Strings) programChange(45, 128)
                        else tremolo(4)
                    }
                    Repeat -> tremolo(32)
                    else -> {
                    }
                }
                noteOn()
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

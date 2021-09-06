package wittgenstein.midi

import wittgenstein.*
import wittgenstein.InstrumentFamily.Strings
import wittgenstein.SimplePitchedContinuousElement.FastRepeat
import wittgenstein.SimplePitchedContinuousElement.Repeat

private fun MidiOutput.createPulseMap(score: GraphicalScore): Map<Int, List<Event>> =
    score.elements.flatMap { el ->
        eventList {
            at(el.start * PULSES_PER_BEAT) {
                noteOn(el.instrument!!, el.pitch, el.startDynamic!!)
                when {
                    el is Trill ->  trill(el.secondaryPitch!!)
                    el.type == FastRepeat -> tremolo(8)
                    el.type == Repeat -> tremolo(16)
                    el.type == DiscretePitchedElement.Percussive && el.instrument!!.family == Strings ->
                         programChange(45)  //Pizzicato Strings
                }
            }
            if (el is ContinuousElement) {
                at(el.start * PULSES_PER_BEAT) { gradualVolumeChange(el.climax * PULSES_PER_BEAT, el.climaxDynamic!!) }
                at(el.climax * PULSES_PER_BEAT){ gradualVolumeChange(el.end * PULSES_PER_BEAT, el.endDynamic!!) }
            }
            at(el.end * PULSES_PER_BEAT) { noteOff() }
        }
    }.toPulseMap()

fun MidiOutput.play(score: GraphicalScore) {
    val pulseMap = createPulseMap(score)
    play(pulseMap)
    pulsator.reset()
}
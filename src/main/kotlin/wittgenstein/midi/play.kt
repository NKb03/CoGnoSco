package wittgenstein.midi

import wittgenstein.GraphicalScore

private fun MidiOutput.createPulseMap(score: GraphicalScore): Map<Int, List<Event>> =
    score.elements.flatMap { el ->
        eventList {
            at(el.start) { noteOn(el.instrument!!, el.pitch, el.startDynamic!!) }
            at(el.end) { noteOff() }
        }
    }.groupBy { it.pulse }


fun MidiOutput.play(score: GraphicalScore) {
    val pulseMap = createPulseMap(score)
    play(pulseMap)
    pulsator.reset()
}
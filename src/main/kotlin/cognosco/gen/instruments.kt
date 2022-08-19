package cognosco.gen

import cognosco.ContinuousNoise.Type.Breath
import cognosco.ContinuousNoise.Type.DrumRoll
import cognosco.Element
import cognosco.Instrument
import cognosco.Instrument.*
import cognosco.SimplePitchedContinuousElement.Type.*
import cognosco.Trill

val Instrument.range: IntRange
    get() = when (this) {
        Flute -> 60..93
        Oboe -> 58..87
        Clarinet -> 50..89
        Saxophone -> 56..89
        Horn -> 41..69
        Trumpet -> 52..80
        Trombone -> 40..71
        Tuba -> 32..58
        Violins -> 55..88
        Violas -> 48..79
        Violoncelli -> 36..67
        Contrabasses -> 32..62
        else -> 0 until 1
    }

val Instrument.supportedElementTypes: List<Element.Type<*>>
    get() = when (this) {
        Timpani, SnareDrum, BassDrum, Cymbal -> listOf(DrumRoll, Breath)
        else -> listOf(Regular, FastRepeat, Repeat, Noisy, Trill, Breath)
    }
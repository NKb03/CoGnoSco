package wittgenstein.midi

import wittgenstein.Dynamic
import wittgenstein.Instrument
import wittgenstein.Pitch

interface MidiOutput {
    val pulsator: Pulsator

    fun createNoteHandle(): NoteHandle

    interface NoteHandle {
        val active: Boolean

        val output: MidiOutput

        fun noteOn(instrument: Instrument, pitch: Pitch?, initialDynamic: Dynamic)

        fun noteOff()

        fun trill(secondaryPitch: Pitch)

        fun tremolo(pulsesPerRepetition: Int)

        fun gradualVolumeChange(targetPulse: Int, targetDynamic: Dynamic)

        fun setInstrument(instrument: Instrument)

        fun programChange(program: Int)

        fun setVolume(volume: Int)
    }
}
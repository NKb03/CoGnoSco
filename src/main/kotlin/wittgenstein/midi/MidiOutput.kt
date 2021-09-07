package wittgenstein.midi

import wittgenstein.Dynamic
import wittgenstein.Instrument
import wittgenstein.Pitch

interface MidiOutput {
    fun createNoteHandle(): NoteHandle

    fun receivePulse(pulse: Int)

    fun pause()

    fun resume()

    interface NoteHandle {
        val active: Boolean

        val output: MidiOutput

        fun noteOn(instrument: Instrument, pitch: Pitch?, initialDynamic: Dynamic)

        fun noteOff()

        fun trill(pitch: Pitch)

        fun tremolo(pulsesPerRepetition: Int)

        fun gradualVolumeChange(targetPulse: Int, targetDynamic: Dynamic)

        fun setInstrument(instrument: Instrument)

        fun programChange(program: Int)

        fun setVolume(volume: Int)
    }
}
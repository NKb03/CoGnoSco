package cognosco.midi

import cognosco.Dynamic
import cognosco.Instrument
import cognosco.Pitch

interface MidiOutput {
    fun createNoteHandle(): NoteHandle

    fun receivePulse(pulse: Int)

    fun pause()

    fun resume()

    fun stopAll()

    interface NoteHandle {
        val active: Boolean

        val output: MidiOutput

        fun setup()

        fun setInstrument(instr: Instrument)

        fun programChange(program: Int, bank: Int = 0)

        fun noteOn(pitch: Pitch?, initialDynamic: Dynamic)

        fun noteOff()

        fun setVolume(volume: Int)

        fun gradualVolumeChange(targetPulse: Int, targetDynamic: Dynamic)

        fun trill(secondaryPitch: Pitch)

        fun tremolo(pulsesPerRepetition: Int)
    }
}
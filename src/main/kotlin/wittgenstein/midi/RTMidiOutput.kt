package wittgenstein.midi

import wittgenstein.*
import javax.sound.midi.MidiChannel
import javax.sound.midi.MidiSystem
import javax.sound.midi.Synthesizer

class RTMidiOutput(synthesizer: Synthesizer, override val pulsator: Pulsator) : MidiOutput {
    private val freeChannels = synthesizer.channels.toMutableList()
    private val handles = mutableListOf<RTNoteHandle>()

    init {
        if (!synthesizer.isOpen) synthesizer.open()
        pulsator.addListener {
            handles.forEach { it.receivePulse(pulsator.pulse) }
        }
    }

    override fun createNoteHandle(): MidiOutput.NoteHandle = RTNoteHandle()

    private fun acquireChannel(): MidiChannel {
        check(freeChannels.isNotEmpty()) { "no channel available" }
        return freeChannels.removeLast()
    }

    private fun releaseChannel(channel: MidiChannel) {
        freeChannels.add(channel)
    }

    private data class VolumeChange(val startPulse: Int, val startVolume: Int, val endPulse: Int, val endVolume: Int)

    private inner class RTNoteHandle : MidiOutput.NoteHandle {
        override val output: MidiOutput
            get() = this@RTMidiOutput

        override val active: Boolean get() = _channel != null

        private var _channel: MidiChannel? = null
        private val channel get() = _channel ?: error("handle not active")

        private var pitch: Pitch? = null
        private var initialDynamic: Int = 0
        private var primaryPitchBend: Int = 0
        private var secondaryPitchBend: Int = 0
        private var currentVolume = 0
        private var volumeChange: VolumeChange? = null
        private var tremoloPulsesPerRepetition: Int = 0

        private fun centToBend(cent: Int) = 16348 / 2 + (cent / 400.0 * 16348).toInt()

        override fun noteOn(instrument: Instrument, pitch: Pitch?, initialDynamic: Dynamic) {
            check(!active) { "handle already active" }
            println("noteOn $pitch")
            handles.add(this)
            _channel = acquireChannel()
            this.pitch = pitch
            val volume = initialDynamic.midiVolume
            this.initialDynamic = volume
            channel.programChange(instrument.program)
            val step = if (pitch != null) {
                primaryPitchBend = pitch.accidental.bend
                channel.pitchBend = centToBend(pitch.accidental.bend)
                pitch.chromaticStep + 12
            } else instrument.key ?: error("no pitch provided")
            setVolume(volume)
            channel.noteOn(step, 127)
        }

        override fun setVolume(volume: Int) {
            if (currentVolume != volume) {
                println("volume = $volume")
                currentVolume = volume
                channel.controlChange(7, volume)
            }
        }

        override fun setInstrument(instrument: Instrument) {
            programChange(instrument.program)
        }

        override fun programChange(program: Int) {
            channel.programChange(program)
        }

        override fun noteOff() {
            println("note off $pitch")
            check(active) { "handle not active" }
            channel.allSoundOff()
            releaseChannel(channel)
            handles.remove(this)
            _channel = null
        }

        override fun trill(secondaryPitch: Pitch) {
            println("trill $secondaryPitch")
            secondaryPitchBend = primaryPitchBend + (secondaryPitch.cent - pitch!!.cent)
        }

        override fun gradualVolumeChange(targetPulse: Int, targetDynamic: Dynamic) {
            volumeChange = VolumeChange(pulsator.pulse, currentVolume, targetPulse, targetDynamic.midiVolume)
        }

        override fun tremolo(pulsesPerRepetition: Int) {
            tremoloPulsesPerRepetition = pulsesPerRepetition
        }

        fun receivePulse(pulse: Int) {
            println("receive pulse $pulse")
            val vc = volumeChange
            if (vc != null) {
                val prop = (pulse - vc.startPulse).toDouble() / (vc.endPulse - vc.startPulse)
                val v = (1 - prop) * vc.startVolume + prop * vc.endVolume
                setVolume(v.toInt())
                if (pulse == vc.endPulse) {
                    volumeChange = null
                }
            }
            if (secondaryPitchBend != 0) {
                if (pulse % 10 == 0) channel.pitchBend = centToBend(primaryPitchBend)
                if (pulse % 10 == 5) channel.pitchBend = centToBend(secondaryPitchBend)
                println("pitch bend = ${channel.pitchBend}")
            }
            if (tremoloPulsesPerRepetition != 0) {
                if (pulse % tremoloPulsesPerRepetition == 0) {
                    TODO()
                } else if (pulse % tremoloPulsesPerRepetition == 1) {
                    TODO()
                }
            }
        }

        override fun toString(): String = "RTNoteHandle [ active = $active, pitch = $pitch ]"
    }
}
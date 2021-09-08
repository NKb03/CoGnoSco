package cognosco.midi

import cognosco.*
import javax.sound.midi.MidiChannel
import javax.sound.midi.Synthesizer

class RTMidiOutput(synthesizer: Synthesizer) : MidiOutput {
    private var currentPulse = 0
    private val freeChannels = synthesizer.channels.toMutableList()
    private val handles = mutableListOf<RTNoteHandle>()

    init {
        if (!synthesizer.isOpen) synthesizer.open()
    }

    override fun receivePulse(pulse: Int) {
        currentPulse = pulse
        for (handle in handles) {
            handle.receivePulse(pulse)
        }
    }

    override fun pause() {
        for (handle in handles.toList()) {
            handle.pause()
        }
    }

    override fun resume() {
        for (handle in handles) {
            handle.resume()
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

        private var instrument: Instrument? = null
        private var pitch: Pitch? = null
        private var initialDynamic: Int = 0
        private var secondaryPitch: Pitch? = null
        private var currentVolume = 0
        private var volumeChange: VolumeChange? = null
        private var tremoloPulsesPerRepetition: Int = 0

        private fun centToBend(cent: Int) = 16348 / 2 + (cent / 400.0 * 16348).toInt()

        override fun noteOn(instrument: Instrument, pitch: Pitch?, initialDynamic: Dynamic) {
            check(!active) { "handle already active" }
            handles.add(this)
            _channel = acquireChannel()
            this.instrument = instrument
            this.pitch = pitch
            val volume = initialDynamic.midiVolume
            this.initialDynamic = volume
            channel.programChange(instrument.program)
            setVolume(volume)
            channel.noteOn(pitch.midiPitch(), 127)
        }

        fun pause() {
            channel.allNotesOff()
        }

        fun resume() {
            channel.noteOn(pitch.midiPitch(), 127)
        }

        private fun Pitch?.midiPitch(): Int {
            if (instrument!!.percussionKey != null) return instrument!!.percussionKey!!
            checkNotNull(this) { "no pitch provided" }
            channel.pitchBend = centToBend(accidental.bend)
            return chromaticStep + 12
        }

        override fun setVolume(volume: Int) {
            if (currentVolume != volume) {
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
            check(active) { "handle not active" }
            channel.allSoundOff()
            releaseChannel(channel)
            handles.remove(this)
            _channel = null
        }

        override fun trill(pitch: Pitch) {
            secondaryPitch = pitch
        }

        override fun gradualVolumeChange(targetPulse: Int, targetDynamic: Dynamic) {
            volumeChange = VolumeChange(currentPulse, currentVolume, targetPulse, targetDynamic.midiVolume)
        }

        override fun tremolo(pulsesPerRepetition: Int) {
            tremoloPulsesPerRepetition = pulsesPerRepetition
        }

        fun receivePulse(pulse: Int) {
            val vc = volumeChange
            if (vc != null) {
                val prop = (pulse - vc.startPulse).toDouble() / (vc.endPulse - vc.startPulse)
                val v = (1 - prop) * vc.startVolume + prop * vc.endVolume
                setVolume(v.toInt())
                if (pulse == vc.endPulse) {
                    volumeChange = null
                }
            }
            if (secondaryPitch != null) {
                if (pulse % 12 == 0) {
                    channel.allSoundOff()
                    channel.noteOn(secondaryPitch.midiPitch(), 127)
                }
                if (pulse % 12 == 6) {
                    channel.allSoundOff()
                    channel.noteOn(pitch.midiPitch(), 127)
                }
            }
            if (tremoloPulsesPerRepetition != 0) {
                if (pulse % tremoloPulsesPerRepetition == 0) {
                    channel.noteOn(pitch.midiPitch(), 127)
                } else if (pulse % tremoloPulsesPerRepetition == tremoloPulsesPerRepetition / 4 * 3) {
                    channel.allNotesOff()
                }
            }
        }

        override fun toString(): String = "RTNoteHandle [ active = $active, pitch = $pitch ]"
    }
}
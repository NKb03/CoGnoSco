package cognosco.midi

import cognosco.Dynamic
import cognosco.Instrument
import cognosco.Pitch
import java.io.File
import javax.sound.midi.MidiChannel
import javax.sound.midi.MidiSystem
import javax.sound.midi.Synthesizer

class RTMidiOutput(synthesizer: Synthesizer) : MidiOutput {
    private var currentPulse = 0
    private val freeChannels = synthesizer.channels.toMutableList()
    private val percussionChannel: MidiChannel
    private val handles = mutableListOf<RTNoteHandle>()

    init {
        if (!synthesizer.isOpen) synthesizer.open()
        val soundfont = MidiSystem.getSoundbank(File("/home/nikolaus/Musik/soundfonts/TrianGMGS.sf2"))
        synthesizer.loadAllInstruments(soundfont)
        percussionChannel = freeChannels.removeAt(9)
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

    override fun stopAll() {
        for (handle in handles.toList()) {
            handle.noteOff()
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

        private lateinit var instrument: Instrument
        private var program: Int = 0
        private var bank: Int = 0
        private lateinit var pitch: Pitch
        private var midiPitch: Int = -1
            get() = field.takeIf { it != -1 } ?: error("no midi pitch provided")
        private var primaryPitchBend: Int = 0
        private var secondaryPitchBend: Int = Int.MIN_VALUE
        private var currentVolume = 0
        private var volumeChange: VolumeChange? = null
        private var tremoloPulsesPerRepetition: Int = 0

        private fun centToBend(cent: Int) = (cent / 400.0 * 16348).toInt()

        override fun setInstrument(instr: Instrument) {
            instrument = instr
            if (instr.percussionKey != -1) midiPitch = instr.percussionKey
            programChange(instr.program, instr.bank)
        }

        override fun programChange(program: Int, bank: Int) {
            this.program = program - 1
            this.bank = bank
            if (active) channel.programChange(bank, program)
        }

        override fun setPitch(pitch: Pitch) {
            this.pitch = pitch
            midiPitch = pitch.chromaticStep + 12
            primaryPitchBend = 16348 / 2 + centToBend(pitch.accidental.bend)
            if (active) {
                channel.allNotesOff()
                channel.pitchBend = primaryPitchBend
                channel.noteOn(midiPitch, 127)
            }
        }

        override fun setDynamic(dynamic: Dynamic) {
            setVolume(dynamic.midiVolume)
        }

        override fun setVolume(volume: Int) {
            if (currentVolume != volume) {
                currentVolume = volume
                if (active) channel.controlChange(7, volume)
            }
        }

        override fun trill(secondaryPitch: Pitch) {
            val diff = secondaryPitch.cent - pitch.cent
            secondaryPitchBend = primaryPitchBend + centToBend(diff)
        }

        override fun gradualVolumeChange(targetPulse: Int, targetDynamic: Dynamic) {
            volumeChange = VolumeChange(currentPulse, currentVolume, targetPulse, targetDynamic.midiVolume)
        }

        override fun tremolo(pulsesPerRepetition: Int) {
            tremoloPulsesPerRepetition = pulsesPerRepetition
        }

        override fun noteOn() {
            _channel = if (instrument.percussionKey != -1) percussionChannel else acquireChannel()
            handles.add(this)
            channel.programChange(bank, program)
            channel.pitchBend = primaryPitchBend
            channel.controlChange(7, currentVolume)
            channel.noteOn(midiPitch, 127)
        }

        fun pause() {
            if (!active) return
            channel.allNotesOff()
        }

        fun resume() {
            if (!active) return
            channel.noteOn(midiPitch, 127)
        }

        override fun noteOff() {
            if (!active) return
            channel.allSoundOff()
            releaseChannel(channel)
            handles.remove(this)
            _channel = null
        }

        fun receivePulse(pulse: Int) {
            if (!active) return
            handleVolumeChange(pulse)
            handleTrill(pulse)
            handleTremolo(pulse)
        }

        private fun handleVolumeChange(pulse: Int) {
            val vc = volumeChange
            if (vc != null) {
                val prop = (pulse - vc.startPulse).toDouble() / (vc.endPulse - vc.startPulse)
                val v = (1 - prop) * vc.startVolume + prop * vc.endVolume
                setVolume(v.toInt())
                if (pulse == vc.endPulse) {
                    volumeChange = null
                }
            }
        }

        private fun handleTremolo(pulse: Int) {
            if (tremoloPulsesPerRepetition != 0) {
                if (pulse % tremoloPulsesPerRepetition == 0) {
                    channel.noteOn(midiPitch, 127)
                } else if (pulse % tremoloPulsesPerRepetition == tremoloPulsesPerRepetition / 2) {
                    channel.allNotesOff()
                }
            }
        }

        private fun handleTrill(pulse: Int) {
            if (secondaryPitchBend != Int.MIN_VALUE) {
                if (pulse % 10 == 0) channel.pitchBend = primaryPitchBend
                if (pulse % 10 == 5) channel.pitchBend = secondaryPitchBend
            }
        }

        override fun toString(): String = "RTNoteHandle [ active = $active, pitch = $pitch ]"
    }
}
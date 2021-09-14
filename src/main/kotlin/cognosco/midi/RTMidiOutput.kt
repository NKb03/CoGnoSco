package cognosco.midi

import cognosco.Dynamic
import cognosco.Dynamic.PPP
import cognosco.Instrument
import cognosco.Pitch
import java.io.File
import javax.sound.midi.MidiChannel
import javax.sound.midi.MidiSystem
import javax.sound.midi.Synthesizer

class RTMidiOutput(synthesizer: Synthesizer) : MidiOutput {
    private var currentPulse = 0
    private val freeChannels = synthesizer.channels.toMutableList()
    private val handles = mutableListOf<RTNoteHandle>()

    init {
        if (!synthesizer.isOpen) synthesizer.open()
        val trian = MidiSystem.getSoundbank(File("/home/nikolaus/Downloads/TrianGMGS.sf2"))
        synthesizer.loadAllInstruments(trian)
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

        private var instrument: Instrument? = null
        private var pitch: Pitch? = null
        private var primaryPitchBend: Int = 0
        private var secondaryPitchBend: Int = Int.MIN_VALUE
        private var initialVolume: Int = 0
        private var currentVolume = 0
        private var volumeChange: VolumeChange? = null
        private var tremoloPulsesPerRepetition: Int = 0

        private fun centToBend(cent: Int) = (cent / 400.0 * 16348).toInt()

        override fun setup() {
            _channel = acquireChannel()
            handles.add(this)
        }

        override fun setInstrument(instr: Instrument) {
            if (!active) return
            instrument = instr
            programChange(instr.program, instr.bank)
        }

        override fun programChange(program: Int, bank: Int) {
            if (!active) return
            channel.programChange(bank, program - 1)
        }

        override fun noteOn(pitch: Pitch?, initialDynamic: Dynamic) {
            if (!active) return
            this.pitch = pitch
            initialVolume = initialDynamic.midiVolume
            setVolume(initialVolume)
            channel.noteOn(pitch.midiPitch(), 127)
            primaryPitchBend = channel.pitchBend
        }

        fun pause() {
            if (!active) return
            channel.allNotesOff()
        }

        fun resume() {
            if (!active) return
            channel.noteOn(pitch.midiPitch(), 127)
        }

        private fun Pitch?.midiPitch(): Int {
            if (instrument!!.percussionKey != null) return instrument!!.percussionKey!!
            checkNotNull(this) { "no pitch provided" }
            channel.pitchBend = 16348 / 2 + centToBend(accidental.bend)
            return chromaticStep + 12
        }

        override fun setVolume(volume: Int) {
            if (!active) return
            if (currentVolume != volume) {
                currentVolume = volume
                channel.controlChange(7, volume)
            }
        }

        override fun noteOff() {
            if (!active) return
            channel.allSoundOff()
            releaseChannel(channel)
            handles.remove(this)
            _channel = null
        }

        override fun trill(secondaryPitch: Pitch) {
            val diff = secondaryPitch.cent - pitch!!.cent
            secondaryPitchBend = primaryPitchBend + centToBend(diff)
        }

        override fun gradualVolumeChange(targetPulse: Int, targetDynamic: Dynamic) {
            val long = (targetPulse - currentPulse) > 10000
            val start = if (!long && currentVolume == PPP.midiVolume) 2 else currentVolume
            val end = if (!long && targetDynamic == PPP) 2 else targetDynamic.midiVolume
            volumeChange = VolumeChange(currentPulse, start, targetPulse, end)
        }

        override fun tremolo(pulsesPerRepetition: Int) {
            tremoloPulsesPerRepetition = pulsesPerRepetition
        }

        fun receivePulse(pulse: Int) {
            handleVolumeChange(pulse)
            handleTrill(pulse)
            handleTremolo(pulse)
            if (pulse % 100 == 0) channel.noteOn(pitch.midiPitch(), 127)
        }

        private fun handleTremolo(pulse: Int) {
            if (tremoloPulsesPerRepetition != 0) {
                if (pulse % tremoloPulsesPerRepetition == 0) {
                    channel.noteOn(pitch.midiPitch(), 127)
                } else if (pulse % tremoloPulsesPerRepetition == tremoloPulsesPerRepetition / 4 * 3) {
                    channel.allNotesOff()
                }
            }
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

        private fun handleTrill(pulse: Int) {
            if (secondaryPitchBend != Int.MIN_VALUE) {
                if (pulse % 10 == 0) channel.pitchBend = primaryPitchBend
                if (pulse % 10 == 5) channel.pitchBend = secondaryPitchBend
            }
        }

        override fun toString(): String = "RTNoteHandle [ active = $active, pitch = $pitch ]"
    }
}
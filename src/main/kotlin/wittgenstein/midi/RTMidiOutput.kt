package wittgenstein.midi

import wittgenstein.Dynamic
import wittgenstein.Instrument
import wittgenstein.Pitch
import javax.sound.midi.MidiChannel
import javax.sound.midi.Synthesizer

class RTMidiOutput(synthesizer: Synthesizer, override val pulsator: Pulsator) : MidiOutput {
    private val freeChannels = synthesizer.channels.toMutableList()
    private val handles = mutableListOf<RTNoteHandle>()

    init {
        if (!synthesizer.isOpen) synthesizer.open()
        pulsator.addListener { handles.forEach { it.receivePulse() } }
    }

    override fun createNoteHandle(): MidiOutput.NoteHandle = RTNoteHandle()

    private fun acquireChannel(): MidiChannel {
        check(freeChannels.isNotEmpty()) { "no channel available" }
        return freeChannels.removeLast()
    }

    private fun releaseChannel(channel: MidiChannel) {
        freeChannels.add(channel)
    }

    private inner class RTNoteHandle : MidiOutput.NoteHandle {
        override val output: MidiOutput
            get() = this@RTMidiOutput

        override val active: Boolean get() = _channel != null

        private var _channel: MidiChannel? = null
        private val channel get() = _channel ?: error("handle not active")

        private var pitch: Pitch? = null
        private var initialDynamic: Int = 0

        override fun noteOn(instrument: Instrument, pitch: Pitch?, initialDynamic: Dynamic) {
            check(!active) { "handle already active" }
            _channel = acquireChannel()
            this.pitch = pitch
            val velo = initialDynamic.velocity
            this.initialDynamic = velo
            channel.programChange(instrument.program)
            val step = pitch?.chromaticStep?.plus(12) ?: instrument.key ?: error("no pitch provided")
            channel.noteOn(step, velo)
        }

        override fun noteOff() {
            check(active) { "handle not active" }
            channel.allSoundOff()
            releaseChannel(channel)
            handles.remove(this)
            _channel = null
        }

        override fun trill(secondaryPitch: Pitch) {
            TODO("Not yet implemented")
        }

        override fun gradualVolumeChange(targetPulse: Int, targetDynamic: Dynamic) {
            TODO("Not yet implemented")
        }

        override fun tremolo(speed: Int) {
            TODO("Not yet implemented")
        }

        fun receivePulse() {

        }

        override fun toString(): String = "RTNoteHandle [ active = $active, pitch = $pitch ]"
    }
}
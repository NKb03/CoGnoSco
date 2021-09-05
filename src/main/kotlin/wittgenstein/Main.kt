package wittgenstein

import javafx.application.Application
import wittgenstein.gui.App
import wittgenstein.lily.typeset
import wittgenstein.midi.PULSES_PER_BEAT
import wittgenstein.midi.Pulsator
import wittgenstein.midi.RTMidiOutput
import wittgenstein.midi.play
import java.io.File
import javax.sound.midi.MidiSystem

object Main {
    @JvmStatic
    fun main(args: Array<String>) {
        try {
            val file = args.getOrNull(1)?.let(::File)
            if (file != null && !file.exists()) {
                throw WittgensteinException("ERROR: Referenced file $file does not exist.")
            }
            when (args.getOrElse(0) { "launch" }) {
                "launch" -> launch(file)
                "typeset" -> typeset(file ?: error("no file provided"))
                "play" -> play(file ?: error("no file provided"))
                "help" -> help()
            }
        } catch (e: WittgensteinException) {
            System.err.println(e.message)
        } catch (e: Throwable) {
            System.err.println("Unerwarteter Fehler!")
            e.printStackTrace()
        }
    }

    private fun launch(file: File?) {
        if (file != null) Application.launch(App::class.java, file.absolutePath)
        else Application.launch(App::class.java)
    }

    private fun typeset(file: File) {
        val score = GraphicalScore.load(file)
        val name = file.name.removeSuffix(".wtg.json")
        val ly = file.resolveSibling("$name.ly")
        typeset(score, ly)
        workingDirectory.run("lilypond", "$ly").join()
        workingDirectory.run("okular", "$name.pdf").join()
    }

    private fun play(file: File) {
        val score = GraphicalScore.load(file)
        val synth = MidiSystem.getSynthesizer()
        val pulsator = Pulsator().realtime(16)
        println()
        pulsator.addListener {
            print("\r")
            val time = pulsator.pulse / PULSES_PER_BEAT
            val bar = time / 2
            val beat = time % 2
            print("Takt $bar, Schlag $beat")
        }
        val output = RTMidiOutput(synth, pulsator)
        output.play(score)
    }

    private fun help() {
        println("Usage: ")
        println("wtg launch <score>          launch the user interface opening the specified score")
        println("wtg typeset <score>         typeset the specified score using LilyPond")
        println("wtg play <score>            play the specified score using MIDI")
        println("wtg help                    print this help page")
    }

}
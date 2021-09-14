package cognosco

import cognosco.gui.App
import cognosco.lily.typeset
import cognosco.midi.MidiPlayer
import cognosco.midi.PULSES_PER_BEAT
import cognosco.midi.RTMidiOutput
import cognosco.midi.createEvents
import javafx.application.Application
import java.io.File
import javax.sound.midi.MidiSystem

object Main {
    @JvmStatic
    fun main(args: Array<String>) {
        try {
            val file = args.getOrNull(1)?.let(::File)
            if (file != null && !file.exists()) {
                throw CognoscoException("ERROR: Referenced file $file does not exist.")
            }
            when (val command = args.getOrElse(0) { "launch" }) {
                "launch" -> launch(file)
                "typeset" -> typeset(file ?: throw CognoscoException("no file provided"))
                "play" -> play(file ?: throw CognoscoException("no file provided"))
                "help" -> help()
                else -> throw CognoscoException("unknown command $command, run cognosco help to see available commands")
            }
        } catch (e: CognoscoException) {
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
        val name = file.name.removeSuffix(".json")
        val ly = file.resolveSibling("$name.ly")
        typeset(score, ly)
        workingDirectory.run("lilypond", "$ly").join()
        workingDirectory.run("okular", "$name.pdf").join()
    }

    private fun play(file: File) {
        val score = GraphicalScore.load(file)
        val synth = MidiSystem.getSynthesizer()
        val output = RTMidiOutput(synth)
        val player = MidiPlayer(output).realtime(16).addListener { pulse ->
            print("\r")
            val time = pulse / PULSES_PER_BEAT
            val bar = time / 2
            val beat = time % 2
            print("Takt $bar, Schlag $beat")
        }
        println()
        player.setEvents(score.createEvents(output))
        player.play()
    }

    private fun help() {
        println("Usage: ")
        println("cognosco launch <score>          launch the user interface opening the specified score")
        println("cognosco typeset <score>         typeset the specified score using LilyPond")
        println("cognosco play <score>            play the specified score using MIDI")
        println("cognosco help                    print this help page")
    }

}
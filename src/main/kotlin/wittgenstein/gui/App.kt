package wittgenstein.gui

import javafx.application.Application
import javafx.scene.Node
import javafx.scene.Scene
import javafx.scene.control.Button
import javafx.scene.layout.HBox
import javafx.scene.layout.VBox
import javafx.stage.FileChooser
import javafx.stage.Stage
import wittgenstein.Dynamic
import wittgenstein.RegularAccidental
import wittgenstein.Score
import wittgenstein.gui.Shortcuts.ESCAPE
import wittgenstein.gui.Shortcuts.FLAT
import wittgenstein.gui.Shortcuts.LOUDER
import wittgenstein.gui.Shortcuts.NATURAL
import wittgenstein.gui.Shortcuts.OPEN
import wittgenstein.gui.Shortcuts.PLAY
import wittgenstein.gui.Shortcuts.QUIETER
import wittgenstein.gui.Shortcuts.SAVE
import wittgenstein.gui.Shortcuts.SELECT_BEND
import wittgenstein.gui.Shortcuts.SELECT_INSTRUMENT
import wittgenstein.gui.Shortcuts.SELECT_TYPE
import wittgenstein.gui.Shortcuts.SHARP
import wittgenstein.gui.Shortcuts.TYPESET
import java.io.File

class App : Application() {
    private lateinit var stage: Stage
    private lateinit var fileChooser: FileChooser

    override fun start(primaryStage: Stage) {
        stage = primaryStage
        setupFileChooser()
        val actionsBar = ActionsBar()
        val typeSelector = ElementTypeSelector()
        val accidentalSelector = AccidentalSelector()
        val instrumentSelector = InstrumentSelector()
        val dynamicSelector = DynamicSelector()
        val scoreView = ScoreView(typeSelector, accidentalSelector, instrumentSelector, dynamicSelector)
        val layout = VBox(
            30.0,
            VBox(
                HBox(10.0, containerButton(actionsBar), containerButton(typeSelector)),
                HBox(10.0, containerButton(accidentalSelector), containerButton(dynamicSelector)),
                containerButton(instrumentSelector)
            ),
            scoreView
        )
        Shortcuts.listen(layout) { shortcut ->
            when (shortcut) {
                ESCAPE -> typeSelector.select(ElementTypeSelector.POINTER)
                SELECT_TYPE -> typeSelector.receiveFocus()
                SELECT_INSTRUMENT -> instrumentSelector.receiveFocus()
                LOUDER -> {
                    val selected = dynamicSelector.selected.value
                    val new = Dynamic.values().getOrElse(selected.ordinal + 1) { selected }
                    dynamicSelector.select(new)
                }
                QUIETER -> {
                    val selected = dynamicSelector.selected.value
                    val new = Dynamic.values().getOrElse(selected.ordinal - 1) { selected }
                    dynamicSelector.select(new)
                }
                SHARP -> accidentalSelector.regularAccidentalSelector.select(RegularAccidental.Sharp)
                NATURAL -> accidentalSelector.regularAccidentalSelector.select(RegularAccidental.Natural)
                FLAT -> accidentalSelector.regularAccidentalSelector.select(RegularAccidental.Flat)
                SELECT_BEND -> accidentalSelector.pitchBendSelector.receiveFocus()
                OPEN -> {
                    val file = fileChooser.showOpenDialog(stage) ?: return@listen
                    val encoded = file.readText()
                    val score = Score.decodeFromString(encoded)
                    scoreView.openScore(score)
                }
                SAVE -> {
                    val file = fileChooser.showSaveDialog(stage) ?: return@listen
                    val score = scoreView.getScore()
                    val encoded = score.encodeToString()
                    file.writeText(encoded)
                }
                PLAY -> {}
                TYPESET -> {}
                else -> scoreView.handleShortcut(shortcut)
            }
        }
        stage.scene = Scene(layout)
        stage.scene.stylesheets.add("wittgenstein/gui/style.css")
        stage.show()
    }

    private fun setupFileChooser() {
        fileChooser = FileChooser()
        fileChooser.extensionFilters.add(FileChooser.ExtensionFilter("Wittgenstein", "*.wtg"))
        fileChooser.initialDirectory = File("examples/")
    }

    private fun containerButton(content: Node) = Button(null, content).apply {
        isFocusTraversable = false
        prefHeight = 50.0
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            launch(App::class.java)
        }
    }
}
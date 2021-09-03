package wittgenstein.gui

import javafx.application.Application
import javafx.scene.Node
import javafx.scene.Scene
import javafx.scene.control.Button
import javafx.scene.input.KeyCombination
import javafx.scene.layout.HBox
import javafx.scene.layout.VBox
import javafx.stage.FileChooser
import javafx.stage.Stage
import wittgenstein.Dynamic
import wittgenstein.RegularAccidental
import wittgenstein.Score
import wittgenstein.gui.ActionsBar.Action
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
    private lateinit var actionsBar: ActionsBar
    private lateinit var typeSelector: ElementTypeSelector
    private lateinit var accidentalSelector: AccidentalSelector
    private lateinit var instrumentSelector: InstrumentSelector
    private lateinit var dynamicSelector: DynamicSelector
    private lateinit var scoreView: ScoreView
    private var defaultFile: File? = null
        set(value) {
            field = value
            val name = value?.name ?: "Neue Datei"
            stage.title = "Wittgenstein - $name"
        }

    override fun start(primaryStage: Stage) {
        stage = primaryStage
        setupFileChooser()
        actionsBar = ActionsBar()
        typeSelector = ElementTypeSelector()
        accidentalSelector = AccidentalSelector()
        instrumentSelector = InstrumentSelector()
        dynamicSelector = DynamicSelector()
        scoreView = ScoreView(typeSelector, accidentalSelector, instrumentSelector, dynamicSelector)
        val layout = layout()
        Shortcuts.listen(layout, this::handleShortcut)
        handleActions()
        stage.title = "Wittgenstein - Neue Datei"
        addIcons()
        stage.scene = Scene(layout)
        stage.scene.stylesheets.add("wittgenstein/gui/style.css")
        stage.show()
    }

    private fun addIcons() {
        val sizes = listOf(16, 32, 64)
        for (size in sizes) {
            stage.icons.add(loadImage("rubin_vase_${size}x${size}.png"))
        }
    }

    private fun handleActions() {
        actionsBar.setOnAction(Action.Open, ::open)
        actionsBar.setOnAction(Action.Save, ::save)
        actionsBar.setOnAction(Action.Play, ::play)
        actionsBar.setOnAction(Action.Typeset, ::typeset)
    }

    private fun handleShortcut(shortcut: KeyCombination) = when (shortcut) {
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
        OPEN -> open()
        SAVE -> save()
        PLAY -> {
        }
        TYPESET -> {
        }
        else -> scoreView.handleShortcut(shortcut)
    }

    private fun open() {
        val file = fileChooser.showOpenDialog(stage) ?: return
        val encoded = file.readText()
        val score = Score.decodeFromString(encoded)
        scoreView.openScore(score)
        defaultFile = file
    }

    private fun save() {
        val file = defaultFile ?: fileChooser.showSaveDialog(stage) ?: return
        val score = scoreView.getScore()
        val encoded = score.encodeToString()
        file.writeText(encoded)
        defaultFile = file
    }

    private fun typeset() {
        TODO("Not yet implemented")
    }

    private fun play() {
        TODO("Not yet implemented")
    }

    private fun layout(): VBox = VBox(
        30.0,
        VBox(
            HBox(10.0, containerButton(actionsBar), containerButton(typeSelector)),
            HBox(10.0, containerButton(accidentalSelector), containerButton(dynamicSelector)),
            containerButton(instrumentSelector)
        ),
        scoreView
    )

    private fun setupFileChooser() {
        fileChooser = FileChooser()
        fileChooser.extensionFilters.add(FileChooser.ExtensionFilter("JSON Files", "*.json"))
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
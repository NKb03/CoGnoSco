package wittgenstein.gui

import javafx.application.Application
import javafx.scene.Node
import javafx.scene.Scene
import javafx.scene.control.Alert.AlertType.ERROR
import javafx.scene.control.Button
import javafx.scene.input.KeyCombination
import javafx.scene.layout.HBox
import javafx.scene.layout.VBox
import javafx.stage.FileChooser
import javafx.stage.Stage
import wittgenstein.Dynamic
import wittgenstein.GraphicalScore
import wittgenstein.RegularAccidental
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
import wittgenstein.gui.impl.loadImage
import wittgenstein.gui.impl.show
import wittgenstein.lily.TypesettingException
import wittgenstein.lily.typeset
import java.io.File
import java.security.PrivilegedActionException
import java.util.logging.ConsoleHandler
import java.util.logging.FileHandler
import java.util.logging.Level
import java.util.logging.Logger
import java.util.stream.Stream
import kotlin.concurrent.thread

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
    private val subProcesses = mutableListOf<Process>()

    override fun start(primaryStage: Stage) {
        setExceptionHandler()
        stage = primaryStage
        setupFileChooser()
        instantiateComponents()
        val layout = layout()
        Shortcuts.listen(layout, this::handleShortcut)
        handleActions()
        stage.title = "Wittgenstein - Neue Datei"
        addIcons()
        stage.scene = Scene(layout)
        stage.scene.stylesheets.add("wittgenstein/gui/style.css")
        stage.show()
    }

    private fun setExceptionHandler() {
        logger.addHandler(ConsoleHandler())
        logger.addHandler(FileHandler("log.txt"))
        Thread.currentThread().setUncaughtExceptionHandler { _, exc -> handleUncaughtException(exc) }
    }

    private fun instantiateComponents() {
        actionsBar = ActionsBar()
        typeSelector = ElementTypeSelector()
        accidentalSelector = AccidentalSelector()
        instrumentSelector = InstrumentSelector()
        dynamicSelector = DynamicSelector()
        scoreView = ScoreView(typeSelector, accidentalSelector, instrumentSelector, dynamicSelector)
    }

    private fun handleUncaughtException(exc: Throwable) {
        val msg = exc.localizedMessage ?: exc.message ?: "<null>"
        when (exc) {
            is PrivilegedActionException -> handleUncaughtException(exc.exception)
            is TypesettingException -> ERROR.show(msg)
            else -> ERROR.show("Unerwarteter Fehler: '${msg}', see log.")
        }
        logger.log(Level.SEVERE, msg, exc)
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

    private fun handleShortcut(shortcut: KeyCombination) {
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
            OPEN -> open()
            SAVE -> save()
            PLAY -> play()
            TYPESET -> typeset()
            else -> scoreView.handleShortcut(shortcut)
        }
    }

    private fun open() {
        val file = fileChooser.showOpenDialog(stage) ?: return
        val encoded = file.readText()
        val score = GraphicalScore.decodeFromString(encoded)
        scoreView.openScore(score)
        defaultFile = file
    }

    private fun save(): File? {
        var file = defaultFile ?: fileChooser.showSaveDialog(stage) ?: return null
        if (file.extension.isEmpty()) file = file.parentFile.resolve("${file.name}.json")
        val score = scoreView.getScore()
        val encoded = score.encodeToString()
        file.writeText(encoded)
        defaultFile = file
        return defaultFile
    }

    private fun run(vararg command: String, wait: Boolean) {
        val dir = defaultFile?.parentFile ?: File("")
        val process = ProcessBuilder(*command)
            .inheritIO()
            .directory(dir)
            .start()
        subProcesses.add(process)
        if (wait) {
            val exitCode = process.waitFor()
            if (exitCode != 0) {
                error("Command '${command.joinToString(" ")}' finished with non-zero exit code")
            }
        }
    }

    private fun typeset() {
        val file = save() ?: return
        val dir = file.parentFile
        val name = file.nameWithoutExtension
        val ly = dir.resolve("$name.ly")
        val score = scoreView.getScore()
        typeset(score, ly)
        thread(isDaemon = true) {
            run("lilypond", "$name.ly", wait = true)
            run("okular", "--unique", "$name.pdf", wait = false)
        }.setUncaughtExceptionHandler { _, exc -> handleUncaughtException(exc) }
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

    override fun stop() {
        kill(subProcesses.stream().map { it.toHandle() })
    }

    private fun kill(processes: Stream<ProcessHandle>) {
        for (proc in processes) {
            if (proc.isAlive) proc.destroy()
            kill(proc.descendants())
        }
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            launch(App::class.java)
        }

        val logger: Logger = Logger.getLogger(App::class.java.name)
    }
}
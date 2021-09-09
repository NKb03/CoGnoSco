package cognosco.gui

import cognosco.*
import cognosco.gui.Shortcut.*
import cognosco.gui.impl.findParentOfType
import cognosco.gui.impl.loadImage
import cognosco.gui.impl.show
import cognosco.lily.typeset
import cognosco.midi.MidiPlayer
import cognosco.midi.RTMidiOutput
import cognosco.midi.createEvents
import javafx.application.Application
import javafx.application.Platform
import javafx.scene.Node
import javafx.scene.Scene
import javafx.scene.control.Alert.AlertType.ERROR
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.control.ScrollPane
import javafx.scene.layout.HBox
import javafx.scene.layout.VBox
import javafx.stage.FileChooser
import javafx.stage.Screen
import javafx.stage.Stage
import java.io.File
import java.security.PrivilegedActionException
import java.util.stream.Stream
import javax.sound.midi.MidiSystem
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
    private val midiOutput = RTMidiOutput(MidiSystem.getSynthesizer())
    private val player = MidiPlayer(midiOutput).realtime(16)
    private var defaultFile: File? = null
        set(value) {
            field = value
            val name = value?.name ?: "Neue Datei"
            stage.title = "CoGnoSco - $name"
        }
    private val subProcesses = mutableListOf<Process>()

    override fun start(primaryStage: Stage) {
        setExceptionHandler()
        stage = primaryStage
        setupFileChooser()
        instantiateComponents()
        actionsBar.setOnAction { action -> handleShortcut(action.shortcut) }
        synchronizePulse()
        openArgumentFile()
        show()
        Shortcut.listen(stage.scene, this::handleShortcut)
        createLayout()
        stage.widthProperty().addListener { _ -> createLayout() }
    }

    private fun show() {
        stage.title = "CoGnoSco - Neue Datei"
        addIcons()
        stage.scene = Scene(Label())
        stage.width = Screen.getPrimary().bounds.width - 200
        stage.height = 1300.0
        stage.centerOnScreen()
        stage.scene.stylesheets.add("cognosco/gui/style.css")
        stage.show()
    }

    private fun synchronizePulse() {
        player.addListener { pulse ->
            Platform.runLater {
                scoreView.setCurrentPulse(pulse)
            }
        }
    }

    private fun openArgumentFile() {
        if (parameters.raw.isNotEmpty()) {
            val file = File(parameters.raw[0])
            open(file)
        }
    }

    private fun setExceptionHandler() {
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
            is CognoscoException -> ERROR.show(msg)
            else -> ERROR.show("Unerwarteter Fehler: '${msg}', see log.")
        }
        exc.printStackTrace()
    }

    private fun addIcons() {
        val sizes = listOf(16, 32, 64)
        for (size in sizes) {
            stage.icons.add(loadImage("rubin_vase_${size}x${size}.png"))
        }
    }

    private fun handleShortcut(shortcut: Shortcut) {
        when (shortcut) {
            Escape -> typeSelector.select(ElementTypeSelector.POINTER)
            SelectType -> typeSelector.receiveFocus()
            SelectInstrument -> instrumentSelector.receiveFocus()
            SelectBend -> accidentalSelector.pitchBendSelector.receiveFocus()
            SelectDynamic -> dynamicSelector.receiveFocus()
            Louder -> {
                val selected = dynamicSelector.selected.value
                val new = Dynamic.values().getOrElse(selected.ordinal + 1) { selected }
                dynamicSelector.select(new)
            }
            Quieter -> {
                val selected = dynamicSelector.selected.value
                val new = Dynamic.values().getOrElse(selected.ordinal - 1) { selected }
                dynamicSelector.select(new)
            }
            Sharp -> accidentalSelector.regularAccidentalSelector.select(RegularAccidental.Sharp)
            Natural -> accidentalSelector.regularAccidentalSelector.select(RegularAccidental.Natural)
            Flat -> accidentalSelector.regularAccidentalSelector.select(RegularAccidental.Flat)
            is Digit -> {
                val bar = stage.scene.focusOwner.findParentOfType<SelectorBar<*>>()
                bar?.selectIndex(shortcut.value)
            }
            Open -> open()
            Save -> save()
            New -> new()
            Play -> playOrStop()
            Typeset -> typeset()
            else -> scoreView.handleShortcut(shortcut)
        }
    }

    private fun new() {
        scoreView.clearScore()
        defaultFile = null
    }

    private fun open() {
        val file = fileChooser.showOpenDialog(stage) ?: return
        fileChooser.initialDirectory = file.parentFile
        open(file)
    }

    private fun open(file: File) {
        val score = GraphicalScore.load(file)
        scoreView.openScore(score)
        defaultFile = file
    }

    private fun save(): File? {
        var file = defaultFile ?: fileChooser.showSaveDialog(stage) ?: return null
        fileChooser.initialDirectory = file.parentFile
        if (file.extension.isEmpty()) file = file.parentFile.resolve("${file.name}.json")
        val score = scoreView.getScore()
        val encoded = score.encodeToString()
        file.writeText(encoded)
        defaultFile = file
        return defaultFile
    }

    private fun run(vararg command: String): Process {
        val process = defaultFile?.parentFile.run(*command)
        subProcesses.add(process)
        return process
    }

    private fun typeset() {
        val file = save() ?: return
        val name = file.name.removeSuffix(".json")
        val ly = file.resolveSibling("$name.ly")
        val score = scoreView.getScore()
        thread(isDaemon = true) {
            typeset(score, ly)
            run("lilypond", "$name.ly").join()
            try {
                run("okular", "--unique", "$name.pdf")
            } catch (e: CognoscoException) {
                run("$name.pdf")
            }
        }.setUncaughtExceptionHandler { _, exc -> handleUncaughtException(exc) }
    }

    private fun playOrStop() {
        if (!player.isPlaying) {
            player.setEvents(scoreView.getScore().createEvents(midiOutput))
            player.play()
        } else player.pause()
    }

    private fun createLayout() {
        val bar = if (stage.width < 3000) {
            VBox(
                HBox(10.0, containerButton(actionsBar), containerButton(typeSelector)),
                HBox(10.0, containerButton(accidentalSelector), containerButton(dynamicSelector)),
                containerButton(instrumentSelector)
            )
        } else {
            HBox(
                10.0,
                containerButton(actionsBar),
                containerButton(typeSelector),
                containerButton(accidentalSelector),
                containerButton(dynamicSelector),
                containerButton(instrumentSelector)
            )
        }
        val stave = HBox(ScoreView.Clefs(), ScrollPane(scoreView).apply {
            prefWidth = stage.width - 130.0
            prefHeight = scoreView.prefHeight + 20
        })
        val layout = VBox(bar, stave)
        stage.scene.root = layout
    }

    private fun setupFileChooser() {
        fileChooser = FileChooser()
        fileChooser.extensionFilters.add(FileChooser.ExtensionFilter("JSON files", "*.json"))
    }

    private fun containerButton(content: Node) = Button(null, content).apply {
        isFocusTraversable = false
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
}
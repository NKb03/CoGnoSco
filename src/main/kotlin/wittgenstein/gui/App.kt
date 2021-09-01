package wittgenstein.gui

import javafx.application.Application
import javafx.geometry.Insets
import javafx.scene.Node
import javafx.scene.Scene
import javafx.scene.control.Button
import javafx.scene.layout.HBox
import javafx.scene.layout.VBox
import javafx.stage.Stage

class App : Application() {
    private lateinit var stage: Stage

    override fun start(primaryStage: Stage) {
        stage = primaryStage
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
        stage.scene = Scene(layout)
        stage.scene.stylesheets.add("wittgenstein/gui/style.css")
        stage.show()
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
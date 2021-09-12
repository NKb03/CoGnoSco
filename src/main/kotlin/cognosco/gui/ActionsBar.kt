package cognosco.gui

import javafx.scene.control.Button
import javafx.scene.control.Tooltip
import javafx.scene.layout.HBox
import org.controlsfx.glyphfont.FontAwesome
import org.controlsfx.glyphfont.FontAwesome.Glyph

class ActionsBar : HBox(5.0) {
    private var onAction: (Action) -> Unit = {}

    fun setOnAction(block: (Action) -> Unit) {
        onAction = block
    }

    init {
        for (action in Action.values()) {
            val glyph = FontAwesome().create(action.glyph)
                .sizeFactor(2)
            val btn = Button(null, glyph)
            btn.styleClass.add("action-icon")
            btn.tooltip = Tooltip(action.description)
            btn.setOnAction { onAction(action) }
            children.add(btn)
        }
    }

    enum class Action(desc: String, val shortcut: Shortcut, val glyph: Glyph) {
        Open("Datei öffnen", Shortcut.Open, Glyph.FOLDER_OPEN),
        Save("Datei speichern", Shortcut.Save, Glyph.SAVE),
        New("Neue Datei erstellen", Shortcut.New, Glyph.PLUS),
        Play("Play/Pause", Shortcut.Play, Glyph.PLAY),
        Stop("Stop", Shortcut.Stop, Glyph.STOP),
        Typeset("Partitur setzen", Shortcut.Typeset, Glyph.PRINT),
        ZoomIn("Hereinzoomen", Shortcut.ZoomIn, Glyph.SEARCH_PLUS),
        ZoomOut("Herauszoomen", Shortcut.ZoomOut, Glyph.SEARCH_MINUS)
        ;

        val description = "$desc ($shortcut)"
    }
}
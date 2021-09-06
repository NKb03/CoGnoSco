package wittgenstein.gui

import javafx.scene.control.Button
import javafx.scene.control.Tooltip
import javafx.scene.layout.HBox
import org.controlsfx.glyphfont.FontAwesome

class ActionsBar : HBox(5.0) {
    private var onAction = mutableMapOf<Action, () -> Unit>()

    fun setOnAction(action: Action, block: () -> Unit) {
        onAction[action] = block
    }

    init {
        for (action in Action.values()) {
            val glyph = FontAwesome().create(action.glyph)
                .sizeFactor(2)
            val btn = Button(null, glyph)
            btn.styleClass.add("action-icon")
            btn.tooltip = Tooltip(action.description)
            btn.setOnAction {
                onAction[action]?.invoke()
            }
            children.add(btn)
        }
    }

    enum class Action(val description: String, val glyph: FontAwesome.Glyph) {
        Open("Datei öffnen (Ctrl+O)", FontAwesome.Glyph.FOLDER_OPEN),
        Save("Datei speichern (Ctrl+S)", FontAwesome.Glyph.SAVE),
        New("Neue Datei erstellen (Ctrl+N)", FontAwesome.Glyph.PLUS),
        Play("Abspielen (Ctrl+SPACE)", FontAwesome.Glyph.PLAY),
        Typeset("Partitur setzen (Ctrl+P)", FontAwesome.Glyph.PRINT);
    }
}
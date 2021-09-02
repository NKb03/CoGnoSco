package wittgenstein.gui

import javafx.beans.binding.Bindings
import javafx.scene.image.ImageView
import wittgenstein.Accidental

class AccidentalView(
    acc: Accidental,
    head: NoteHead
) : ImageView() {
    init {
        setAccidental(acc)
        fitHeight(25.0)
        xProperty().bind(Bindings.subtract(head.xProperty(), 14))
        yProperty().bind(Bindings.subtract(head.yProperty(), 5))
    }

    fun setAccidental(acc: Accidental) {
        image = loadImage(acc)
    }
}
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
        xProperty().bind(Bindings.subtract(head.xProperty(), Bindings.multiply(scaleYProperty(), 12)))
        yProperty().bind(Bindings.subtract(head.yProperty(), 5))
    }

    fun setAccidental(acc: Accidental) {
        image = loadImage(acc)
    }

    fun phantom() = also { opacity = 0.5 }

    fun regular() = also { opacity = 1.0 }
}
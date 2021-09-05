package wittgenstein.gui

import javafx.beans.binding.Bindings
import javafx.scene.image.ImageView
import wittgenstein.Accidental
import wittgenstein.RegularAccidental
import wittgenstein.gui.impl.fitHeight
import wittgenstein.gui.impl.loadImage

class AccidentalView(
    acc: Accidental,
    head: NoteHead
) : ImageView() {
    var accidental: Accidental = acc
        set(value) {
            field = value
            image = loadImage(value)
        }

    init {
        fitHeight(25.0)
        xProperty().bind(Bindings.subtract(head.xProperty(), Bindings.multiply(scaleYProperty(), 12)))
        yProperty().bind(Bindings.subtract(head.yProperty(), 5))
    }

    fun phantom() = also { opacity = 0.5 }

    fun regular() = also { opacity = 1.0 }

    override fun toString(): String = "AccidentalView [ accidental = $accidental ]"
}
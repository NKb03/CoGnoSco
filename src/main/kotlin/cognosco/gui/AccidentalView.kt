package cognosco.gui

import javafx.beans.binding.Bindings
import javafx.scene.image.ImageView
import cognosco.Accidental
import cognosco.gui.impl.fitHeight
import cognosco.gui.impl.loadImage

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
        image = loadImage(acc)
        fitHeight(25.0)
        xProperty().bind(Bindings.subtract(head.xProperty(), Bindings.multiply(scaleYProperty(), 12)))
        yProperty().bind(Bindings.subtract(head.yProperty(), 5))
    }

    fun phantom() = also { opacity = 0.5 }

    fun regular() = also { opacity = 1.0 }

    override fun toString(): String = "AccidentalView [ accidental = $accidental ]"
}
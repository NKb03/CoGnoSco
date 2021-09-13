package cognosco.gui

import cognosco.Accidental
import cognosco.gui.impl.fitHeight
import cognosco.gui.impl.loadImage
import cognosco.gui.impl.selectDouble
import javafx.beans.binding.Bindings
import javafx.scene.image.ImageView

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
        isSmooth = true
        val width = selectDouble("image", "width").divide(selectDouble("image", "height")).multiply(25)
            .multiply(scaleXProperty())
        xProperty().bind(head.xProperty().subtract(width))
        yProperty().bind(Bindings.subtract(head.yProperty(), 5))
    }

    fun phantom() = also { opacity = 0.5 }

    fun regular() = also { opacity = 1.0 }

    override fun toString(): String = "AccidentalView [ accidental = $accidental ]"
}
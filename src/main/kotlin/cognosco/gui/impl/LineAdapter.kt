package cognosco.gui.impl

import javafx.scene.shape.Line
import javafx.scene.shape.Shape

class LineAdapter : ILine, Line() {
    override val shape: Shape
        get() = this
}
package cognosco.gui.impl

import javafx.beans.property.DoubleProperty
import javafx.scene.shape.Shape

interface ILine {
    fun startXProperty(): DoubleProperty
    fun startYProperty(): DoubleProperty
    fun endXProperty(): DoubleProperty
    fun endYProperty(): DoubleProperty

    val shape: Shape
}
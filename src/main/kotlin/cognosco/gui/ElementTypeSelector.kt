package cognosco.gui

import javafx.scene.Node
import javafx.scene.control.ToggleButton
import javafx.scene.shape.Ellipse
import javafx.scene.shape.Rectangle
import javafx.scene.shape.Shape
import cognosco.ContinuousElement
import cognosco.Element
import cognosco.Trill
import cognosco.gui.impl.*

class ElementTypeSelector : SelectorBar<Element.Type<*>?>(listOf(null), continuousElementTypes, discreteElementTypes) {
    override fun extractGraphic(option: Element.Type<*>?): Node {
        if (option == POINTER) return loadImage("pointer.png").view().fitHeight(32.0)
        val head = NoteHead.createShape(option.noteHeadType)
        val shape = if (option is ContinuousElement.Type<*>) {
            val line = if (option == Trill) ZigZagLine(3.0) else LineAdapter()
            line.startXProperty().set(10.0)
            line.endYProperty().bind(line.startYProperty())
            line.endXProperty().set(50.0)
            line.strokeDashArray.addAll(option.strokeDashArray)
            line.strokeWidth = 3.0
            val y = when (head) {
                is Ellipse -> 0.0
                is Rectangle -> 8.0
                else -> 7.0
            }
            line.startYProperty().set(y)
            Shape.union(head, line)
        } else head
        return shape
    }

    override fun ToggleButton.extraConfig(option: Element.Type<*>?) {
        prefWidth = graphic.prefWidth(-1.0) + 35.0
        prefHeight = 40.0
    }

    override fun extractDescription(option: Element.Type<*>?): String? = option?.description

    companion object {
        private val continuousElementTypes = Element.ALL_TYPES.filterIsInstance<ContinuousElement.Type<*>>()
        private val discreteElementTypes = Element.ALL_TYPES.filterNot { it is ContinuousElement.Type<*> }
        val POINTER = null
    }
}
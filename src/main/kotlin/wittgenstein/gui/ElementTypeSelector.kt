package wittgenstein.gui

import javafx.geometry.Insets
import javafx.scene.Node
import javafx.scene.control.ToggleButton
import javafx.scene.layout.Pane
import javafx.scene.shape.Line
import javafx.scene.shape.Polyline
import wittgenstein.ContinuousElement
import wittgenstein.Element
import wittgenstein.Trill
import wittgenstein.gui.impl.*

class ElementTypeSelector : SelectorBar<Element.Type<*>?>(listOf(null), continuousElementTypes, discreteElementTypes) {
    override fun extractGraphic(option: Element.Type<*>?): Node {
        if (option == null) return loadImage("pointer.png").view().fitHeight(30.0)
        val layout = Pane()
        val head = NoteHead()
        head.setNoteHeadType(option.noteHeadType)
        layout.children.add(head)
        if (option is ContinuousElement.Type<*>) {
            val shape = if (option == Trill) ZigZagLine(3.0) else LineAdapter()
            shape.endXProperty().set(35.0)
            shape.strokeDashArray.addAll(option.strokeDashArray)
            shape.strokeWidth = 3.0
            shape.layoutX = head.root.prefWidth(-1.0)
            shape.layoutY = head.root.prefHeight(-1.0) / 2
            layout.children.add(shape)
        }
        return layout
    }

    override fun extractDescription(option: Element.Type<*>?): String? = option?.description

    override fun ToggleButton.extraConfig(option: Element.Type<*>?) {
        if (option == null) {
            padding = Insets(5.0, 10.0, 5.0, 10.0)
        }
    }

    companion object {
        private val continuousElementTypes = Element.ALL_TYPES.filterIsInstance<ContinuousElement.Type<*>>()
        private val discreteElementTypes = Element.ALL_TYPES.filterNot { it is ContinuousElement.Type<*> }
        val POINTER = null
    }
}
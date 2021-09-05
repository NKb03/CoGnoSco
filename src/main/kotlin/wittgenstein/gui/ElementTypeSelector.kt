package wittgenstein.gui

import javafx.geometry.Insets
import javafx.scene.Node
import javafx.scene.control.ToggleButton
import javafx.scene.layout.BorderPane
import javafx.scene.layout.Pane
import wittgenstein.ContinuousElement
import wittgenstein.Element
import wittgenstein.Trill
import wittgenstein.gui.impl.*

class ElementTypeSelector : SelectorBar<Element.Type<*>?>(listOf(null), continuousElementTypes, discreteElementTypes) {
    override fun extractGraphic(option: Element.Type<*>?): Node {
        if (option == POINTER) return loadImage("pointer.png").view().fitHeight(30.0)
        val pane = Pane()
        val head = NoteHead()
        head.setNoteHeadType(option.noteHeadType)
        pane.children.add(head)
        if (option is ContinuousElement.Type<*>) {
            val shape = if (option == Trill) ZigZagLine(3.0) else LineAdapter()
            shape.endXProperty().set(35.0)
            shape.strokeDashArray.addAll(option.strokeDashArray)
            shape.strokeWidth = 3.0
            shape.layoutX = head.root.prefWidth(-1.0)
            shape.layoutY = head.root.prefHeight(-1.0) / 2
            pane.children.add(shape)
        }
        val layout = BorderPane(pane)
        val h = 0.0
        val v = (27 - head.prefHeight(-1.0)) / 1.5
        layout.padding = Insets(v, h, v, h)
        return layout
    }

    override fun extractDescription(option: Element.Type<*>?): String? = option?.description

    override fun ToggleButton.extraConfig(option: Element.Type<*>?) {
        if (option == POINTER) {
            prefHeight = 40.0
            prefWidth = 45.0
        }
    }

    companion object {
        private val continuousElementTypes = Element.ALL_TYPES.filterIsInstance<ContinuousElement.Type<*>>()
        private val discreteElementTypes = Element.ALL_TYPES.filterNot { it is ContinuousElement.Type<*> }
        val POINTER = null
    }
}
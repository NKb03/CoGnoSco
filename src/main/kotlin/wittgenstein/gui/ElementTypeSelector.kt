package wittgenstein.gui

import javafx.scene.Node
import wittgenstein.*

class ElementTypeSelector : SelectorBar<Element.Type<*>?>(listOf(null), continuousElementTypes, discreteElementTypes) {
    override fun extractGraphic(option: Element.Type<*>?): Node? =
        if (option == null) loadImage("pointer.png").view().fitHeight(30.0) else null

    override fun extractText(option: Element.Type<*>?): String? = option?.abbreviation

    override fun extractDescription(option: Element.Type<*>?): String? = option?.description

    companion object {
        private val continuousElementTypes = Element.ALL_TYPES.filterIsInstance<ContinuousElement.Type<*>>()
        private val discreteElementTypes = Element.ALL_TYPES.filterNot { it is ContinuousElement.Type<*> }
        val POINTER = null
    }
}
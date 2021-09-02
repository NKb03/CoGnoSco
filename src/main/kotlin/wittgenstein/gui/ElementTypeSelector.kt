package wittgenstein.gui

import javafx.scene.Node
import wittgenstein.*

class ElementTypeSelector : SelectorBar<Element.Type?>(listOf(null), continuousElementTypes, discreteElementTypes) {
    override fun extractGraphic(option: Element.Type?): Node? =
        if (option == null) loadImage("pointer.png").view().fitHeight(30.0) else null

    override fun extractText(option: Element.Type?): String? = option?.abbreviation

    override fun extractDescription(option: Element.Type?): String? = option?.description

    companion object {
        private val continuousElementTypes = listOf(
            PitchedContinuousElement.Regular,
            Trill,
            PitchedContinuousElement.FlutterTongue,
            PitchedContinuousElement.Tremolo,
            PitchedContinuousElement.Repeat,
            PitchedContinuousElement.Noisy,
            PitchedContinuousElement.Noisy,
            ContinuousNoise.DrumRoll,
            ContinuousNoise.Breath
        )

        private val discreteElementTypes = listOf(
            DiscretePitchedElement.Staccato,
            DiscretePitchedElement.Pizzicato,
            DiscretePitchedElement.Slap,
            DiscretePitchedElement.Slap
        )

        val POINTER = null
    }
}
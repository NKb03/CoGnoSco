package cognosco.gui

import cognosco.Accidental
import cognosco.BendedAccidental
import cognosco.QuarterToneAccidental
import cognosco.RegularAccidental
import cognosco.gui.impl.binding
import cognosco.gui.impl.fitHeight
import cognosco.gui.impl.loadImage
import cognosco.gui.impl.view
import javafx.beans.value.ObservableValue
import javafx.scene.Node
import javafx.scene.control.ToggleButton
import javafx.scene.layout.HBox

class AccidentalSelector : HBox(5.0) {
    val regularAccidentalSelector = RegularAccidentalSelector()
    val microtonalSelector = MicrotonalAccidentalSelector(RegularAccidental.Natural)

    val selected: ObservableValue<Accidental> = binding(
        regularAccidentalSelector.selected,
        microtonalSelector.selected
    ) { acc: RegularAccidental?, bend: Int? ->
        makeMicroTonalAccidental(acc ?: RegularAccidental.Natural, bend ?: 0)
    }

    init {
        microtonalSelector.select(+-0)
        regularAccidentalSelector.selected.addListener { _, _, reference ->
            microtonalSelector.reference = reference
        }
        children.addAll(regularAccidentalSelector, microtonalSelector)
    }

    fun select(value: Accidental) {
        regularAccidentalSelector.select(value.reference)
        microtonalSelector.select(value.bend)
    }

    class RegularAccidentalSelector : SelectorBar<RegularAccidental>(RegularAccidental.values().asList()) {
        override fun extractGraphic(option: RegularAccidental): Node = loadImage(option).view().fitHeight(30.0)

        override fun extractDescription(option: RegularAccidental): String = option.toString()

        override fun ToggleButton.extraConfig(option: RegularAccidental) {
            prefWidth = 45.0
            prefHeight = 40.0
        }
    }

    class MicrotonalAccidentalSelector(ref: RegularAccidental) : SelectorBar<Int>(STANDARD_BENDS) {
        var reference: RegularAccidental = ref
            set(value) {
                field = value
                reload()
            }
            get() = field

        override fun extractGraphic(option: Int): Node {
            val acc = makeMicroTonalAccidental(reference, option)
            return loadImage(acc).view().fitHeight(30.0)
        }

        override fun extractDescription(option: Int): String = makeMicroTonalAccidental(reference, option).toString()

        override fun ToggleButton.extraConfig(option: Int) {
            prefWidth = 45.0
            prefHeight = 40.0
        }
    }

    companion object {
        private val STANDARD_BENDS = listOf(-50, -31, -13, 0, +13, +31, +50)

        private fun makeMicroTonalAccidental(reference: RegularAccidental, bend: Int) = when (bend) {
            0 -> reference
            -50 -> when (reference) {
                RegularAccidental.Natural -> QuarterToneAccidental.QuarterFlat
                RegularAccidental.Flat -> QuarterToneAccidental.TreeQuarterFlat
                RegularAccidental.Sharp -> QuarterToneAccidental.QuarterSharp
            }
            +50 -> when (reference) {
                RegularAccidental.Natural -> QuarterToneAccidental.QuarterSharp
                RegularAccidental.Flat -> QuarterToneAccidental.QuarterFlat
                RegularAccidental.Sharp -> QuarterToneAccidental.TreeQuarterSharp
            }
            else -> BendedAccidental(reference, bend)
        }
    }
}
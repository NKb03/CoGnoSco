package wittgenstein.gui

import javafx.beans.value.ObservableValue
import javafx.geometry.Insets
import javafx.scene.Node
import javafx.scene.control.Button
import javafx.scene.control.ToggleButton
import javafx.scene.image.ImageView
import javafx.scene.layout.HBox
import wittgenstein.Accidental
import wittgenstein.BendedAccidental
import wittgenstein.QuarterToneAccidental
import wittgenstein.RegularAccidental
import wittgenstein.gui.impl.*

class AccidentalSelector : HBox(5.0) {
    val regularAccidentalSelector = RegularAccidentalSelector()
    val pitchBendSelector = StandardBendSelector()
    private val resultView = ImageView()

    val selected: ObservableValue<Accidental> =
        binding(regularAccidentalSelector.selected, pitchBendSelector.selected) { acc: RegularAccidental?, bend: Int? ->
            makeMicroTonalAccidental(acc ?: RegularAccidental.Natural, bend ?: 0)
        }

    init {
        resultView.imageProperty().bind(selected.map(::loadImage))
        pitchBendSelector.select(+-0)
        children.addAll(regularAccidentalSelector, pitchBendSelector, setupResultView())
    }

    private fun setupResultView(): Button {
        resultView.isPreserveRatio = true
        resultView.fitHeight = 30.0
        resultView.isSmooth = true
        val container = Button(null, resultView)
        container.prefWidth = 45.0
        return container
    }

    fun select(value: Accidental) {
        regularAccidentalSelector.select(value.reference)
        pitchBendSelector.select(value.bend)
    }

    class RegularAccidentalSelector : SelectorBar<RegularAccidental>(RegularAccidental.values().asList()) {
        override fun extractGraphic(option: RegularAccidental): Node = loadImage(option).view().fitHeight(30.0)

        override fun ToggleButton.extraConfig(option: RegularAccidental) {
            prefWidth = 45.0
            prefHeight = 40.0
        }
    }

    class StandardBendSelector : SelectorBar<Int>(STANDARD_BENDS) {
        override fun extractText(option: Int): String = option.toStringSigned()
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

        private fun Int.toStringSigned() = when {
            this < 0 -> toString()
            this == 0 -> "\u00B10"
            else -> "+$this"
        }
    }
}
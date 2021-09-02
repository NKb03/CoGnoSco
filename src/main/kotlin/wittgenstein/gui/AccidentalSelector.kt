package wittgenstein.gui

import javafx.beans.value.ObservableValue
import javafx.scene.Node
import javafx.scene.control.Button
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.layout.HBox
import wittgenstein.Accidental
import wittgenstein.BendedAccidental
import wittgenstein.QuarterToneAccidental
import wittgenstein.RegularAccidental

class AccidentalSelector : HBox(5.0) {
    val regularAccidentalSelector = RegularAccidentalSelector()
    val pitchBendSelector = StandardBendSelector()
    private val resultView = ImageView()

    val selected: ObservableValue<Accidental> =
        binding(regularAccidentalSelector.selected, pitchBendSelector.selected) { acc, bend: Int? ->
            val adjust = centToAdjustment(bend ?: 0)
            makeMicroTonalAccidental(adjust, acc)
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
        container.prefHeight = 40.0
        container.prefWidth = 40.0
        return container
    }

    fun select(value: Accidental) {
        when (value) {
            QuarterToneAccidental.QuarterFlat -> {
                regularAccidentalSelector.select(RegularAccidental.Natural)
                selectBend(-50)
            }
            QuarterToneAccidental.QuarterSharp -> {
                regularAccidentalSelector.select(RegularAccidental.Natural)
                selectBend(+50)
            }
            QuarterToneAccidental.TreeQuarterFlat -> {
                regularAccidentalSelector.select(RegularAccidental.Flat)
                selectBend(-50)
            }
            QuarterToneAccidental.TreeQuarterSharp -> {
                regularAccidentalSelector.select(RegularAccidental.Sharp)
                selectBend(+50)
            }
            RegularAccidental.Natural -> {
                regularAccidentalSelector.select(RegularAccidental.Natural)
                selectBend(0)
            }
            RegularAccidental.Flat -> {
                regularAccidentalSelector.select(RegularAccidental.Flat)
                selectBend(0)
            }
            RegularAccidental.Sharp -> {
                regularAccidentalSelector.select(RegularAccidental.Sharp)
            }
            is BendedAccidental -> {
                regularAccidentalSelector.select(value.reference)
                when (value.adjust) {
                    -2 -> selectBend(-31)
                    -1 -> selectBend(-13)
                    0 -> selectBend(+-0)
                    +1 -> selectBend(+13)
                    +2 -> selectBend(+31)
                }
            }
        }
    }

    private fun selectBend(value: Int) {
        pitchBendSelector.select(value)
    }

    class RegularAccidentalSelector : SelectorBar<RegularAccidental>(RegularAccidental.values().asList()) {
        override fun extractGraphic(option: RegularAccidental): Node = loadImage(option).view().fitHeight(30.0)
    }

    class StandardBendSelector : SelectorBar<Int>(STANDARD_BENDS) {
        override fun extractText(option: Int): String = option.toStringSigned()
    }

    companion object {
        private val STANDARD_BENDS = listOf(-50, -31, -13, 0, +13, +31, +50)

        private fun centToAdjustment(bend: Int) = when (bend) {
            in -60..-40 -> -3
            in -39..-25 -> -2
            in -24..-6 -> -1
            in -5..+5 -> 0
            in 6..24 -> 1
            in 25..39 -> 2
            in 40..60 -> 3
            else -> error("pitch bend out of range: $bend")
        }

        private fun makeMicroTonalAccidental(adjust: Int, acc: RegularAccidental) = when (adjust) {
            0 -> acc
            -3 -> when (acc) {
                RegularAccidental.Natural -> QuarterToneAccidental.QuarterFlat
                RegularAccidental.Flat -> QuarterToneAccidental.TreeQuarterFlat
                RegularAccidental.Sharp -> QuarterToneAccidental.QuarterSharp
            }
            3 -> when (acc) {
                RegularAccidental.Natural -> QuarterToneAccidental.QuarterSharp
                RegularAccidental.Flat -> QuarterToneAccidental.QuarterFlat
                RegularAccidental.Sharp -> QuarterToneAccidental.TreeQuarterSharp
            }
            else -> BendedAccidental(acc, adjust)
        }

        private fun Int.toStringSigned() = when {
            this < 0 -> toString()
            this == 0 -> "\u00B10"
            else -> "+$this"
        }
    }
}
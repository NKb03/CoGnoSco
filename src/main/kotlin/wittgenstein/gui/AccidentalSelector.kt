package wittgenstein.gui

import javafx.beans.property.SimpleObjectProperty
import javafx.geometry.Insets
import javafx.scene.Node
import javafx.scene.control.Button
import javafx.scene.control.TextField
import javafx.scene.control.ToggleButton
import javafx.scene.image.ImageView
import javafx.scene.layout.HBox
import javafx.scene.text.Font
import org.controlsfx.control.SegmentedButton
import wittgenstein.Accidental
import wittgenstein.BendedAccidental
import wittgenstein.QuarterToneAccidental
import wittgenstein.RegularAccidental
import kotlin.properties.Delegates

class AccidentalSelector : HBox(5.0) {
    private val regularAccidentalSelector = RegularAccidentalSelector()
    private val bendButtons = STANDARD_BENDS.associateWith { txt -> toggleButton(txt.toStringSigned()) }
    private val pitchBendChoice = SegmentedButton()
    private val pitchBendField = TextField()
    private val resultAccidentalView = ImageView()
    private var bend: Int by Delegates.observable(0) { _, _, _ -> syncResult() }
    private val result = SimpleObjectProperty<Accidental>(RegularAccidental.Natural)
    val selected get() = result

    init {
        regularAccidentalSelector.selected.addListener { _ -> syncResult() }
        setupStandardBends()
        setupPitchBendField()
        syncPitchBend()
        syncResult()
        val cont = setupResult()
        children.addAll(regularAccidentalSelector, pitchBendChoice, pitchBendField, cont)
    }

    private fun setupResult(): Button {
        resultAccidentalView.isPreserveRatio = true
        resultAccidentalView.fitHeight = 30.0
        resultAccidentalView.isSmooth = true
        val cont = Button(null, resultAccidentalView)
        cont.isFocusTraversable = false
        cont.prefHeight = 40.0
        cont.prefWidth = 40.0
        return cont
    }

    private fun setupStandardBends() {
        for (btn in bendButtons.values) {
            btn.prefHeight = 30.0
            pitchBendChoice.buttons.add(btn)
        }
        bendButtons[0]!!.isSelected = true
        pitchBendChoice.styleClass.add("dark")
        pitchBendChoice.toggleGroup.dontDeselectAll()
        setMargin(pitchBendChoice, Insets(5.0, 0.0, 0.0, 0.0))
    }

    private fun setupPitchBendField() {
        pitchBendField.prefWidth = 50.0
        pitchBendField.font = Font.font(14.0)
        setMargin(pitchBendField, Insets(6.5, 0.0, 0.0, 0.0))
        pitchBendField.text = bend.toStringSigned()
    }

    private fun syncPitchBend() {
        pitchBendField.setOnAction {
            bend = validateBend(pitchBendField.text) ?: 0
            val btn = bendButtons[bend]
            if (btn != null) btn.isSelected = true
            else pitchBendChoice.toggleGroup.selectToggle(null)
        }
        pitchBendChoice.toggleGroup.selectedToggleProperty().addListener { _, _, t ->
            t as ToggleButton
            bend = t.text.toIntOrNull() ?: 0
            pitchBendField.text = bend.toStringSigned()
        }
    }

    private fun syncResult() {
        val adjust = when (bend) {
            in -60..-40 -> -3
            in -39..-25 -> -2
            in -24..-6 -> -1
            in -5..+5 -> 0
            in 6..24 -> 1
            in 25..39 -> 2
            in 40..60 -> 3
            else -> error("invalid bend: $bend")
        }
        val regularAccidental = regularAccidentalSelector.selected.value!!
        val acc = when (adjust) {
            0 -> regularAccidental
            -3 -> when (regularAccidental) {
                RegularAccidental.Natural -> QuarterToneAccidental.QuarterFlat
                RegularAccidental.Flat -> QuarterToneAccidental.TreeQuarterFlat
                RegularAccidental.Sharp -> QuarterToneAccidental.QuarterSharp
            }
            3 -> when (regularAccidental) {
                RegularAccidental.Natural -> QuarterToneAccidental.QuarterSharp
                RegularAccidental.Flat -> QuarterToneAccidental.QuarterFlat
                RegularAccidental.Sharp -> QuarterToneAccidental.TreeQuarterSharp
            }
            else -> BendedAccidental(regularAccidental, adjust)
        }
        result.set(acc)
        resultAccidentalView.image = loadImage(acc)
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
        bendButtons[value]!!.isSelected = true
    }

    private class RegularAccidentalSelector : SelectorBar<RegularAccidental>(RegularAccidental.values().asList()) {
        override fun extractGraphic(option: RegularAccidental): Node = loadImage(option).view().fitHeight(30.0)
    }

    companion object {
        private val STANDARD_BENDS = listOf(-50, -31, -13, 0, +13, +31, +50)

        private fun Int.toStringSigned() = when {
            this < 0 -> toString()
            this == 0 -> "\u00B10"
            else -> "+$this"
        }

        private fun validateBend(txt: String) = txt.toIntOrNull()?.takeIf { it in -60..60 }
    }
}
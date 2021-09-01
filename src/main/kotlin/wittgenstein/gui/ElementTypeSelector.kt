package wittgenstein.gui

import javafx.beans.binding.Binding
import javafx.beans.binding.Bindings
import javafx.geometry.Insets
import javafx.scene.control.ToggleButton
import javafx.scene.control.ToggleGroup
import javafx.scene.layout.HBox
import org.controlsfx.control.SegmentedButton
import org.controlsfx.control.SegmentedButton.STYLE_CLASS_DARK
import wittgenstein.*

class ElementTypeSelector : HBox(10.0) {
    private val pointerButton = toggleButton(loadImage("pointer.png").view().fitHeight(30.0))
    private val continuousChoice = SegmentedButton()
    private val discreteChoice = SegmentedButton()
    private val group = ToggleGroup()

    init {
        continuousChoice.toggleGroup = group
        discreteChoice.toggleGroup = group
        continuousChoice.buttons.addAll(continuousTypes.keys.map(::toggleButton))
        discreteChoice.buttons.addAll(discreteTypes.keys.map(::toggleButton))
        for (btn in continuousChoice.buttons + discreteChoice.buttons) {
            btn.prefHeight = 30.0
        }
        continuousChoice.styleClass.add(STYLE_CLASS_DARK)
        discreteChoice.styleClass.add(STYLE_CLASS_DARK)
        setMargin(continuousChoice, Insets(5.0, 0.0, 0.0, 0.0))
        setMargin(discreteChoice, Insets(5.0, 0.0, 0.0, 0.0))
        pointerButton.prefHeight = 40.0
        val seg = SegmentedButton(pointerButton)
        seg.toggleGroup = group
        seg.styleClass.add(STYLE_CLASS_DARK)
        pointerButton.isSelected = true
        group.dontDeselectAll()
        children.addAll(seg, continuousChoice, discreteChoice)
    }

    val selected: Binding<Element.Type?> = Bindings.createObjectBinding({
        val selectedBtn = group.selectedToggle as ToggleButton? ?: return@createObjectBinding null
        val txt = selectedBtn.text ?: return@createObjectBinding null
        continuousTypes[txt] ?: discreteTypes[txt] ?: error("unknown element type '$txt'")
    }, group.selectedToggleProperty())

    companion object {
        private val continuousTypes = mapOf(
            "reg" to PitchedContinuousElement.Regular,
            "tr" to Trill,
            "f.t." to PitchedContinuousElement.FlutterTongue,
            "trem." to PitchedContinuousElement.Tremolo,
            "rep." to PitchedContinuousElement.Repeat,
            "c.l.tr." to PitchedContinuousElement.Noisy,
            "Luft" to PitchedContinuousElement.Noisy,
            "Wirbel" to ContinuousNoise.DrumRoll,
            "Atem" to ContinuousNoise.Breath
        )

        private val discreteTypes = mapOf(
            "stacc." to DiscretePitchedElement.Staccato,
            "pizz." to DiscretePitchedElement.Pizzicato,
            "c.l.b." to DiscretePitchedElement.Noisy,
            "s.t." to DiscretePitchedElement.Noisy
        )
    }
}
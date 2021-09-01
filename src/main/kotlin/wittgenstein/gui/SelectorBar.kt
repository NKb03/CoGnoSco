package wittgenstein.gui

import javafx.beans.binding.Binding
import javafx.beans.binding.Bindings
import javafx.geometry.Insets
import javafx.scene.Node
import javafx.scene.control.ToggleButton
import javafx.scene.control.Tooltip
import org.controlsfx.control.SegmentedButton

abstract class SelectorBar<T : Any>(options: List<T>) : SegmentedButton() {
    protected open fun extractGraphic(option: T): Node? = null
    protected open fun extractText(option: T): String? = null
    protected open fun extractDescription(option: T): String? = null
    protected open fun ToggleButton.extraConfig(option: T) {}

    private val map = mutableMapOf<T, ToggleButton>()

    fun select(option: T) {
        map.getValue(option).isSelected = true
    }

    init {
        for (option in options) {
            val btn = ToggleButton(this.extractText(option), this.extractGraphic(option))
            map[option] = btn
            btn.isFocusTraversable = false
            btn.userData = option
            btn.tooltip = this.extractDescription(option)?.let(::Tooltip)
            btn.prefHeight = 30.0
            @Suppress("LeakingThis")
            btn.extraConfig(option)
            buttons.add(btn)
        }
        buttons[0].isSelected = true
        toggleGroup.dontDeselectAll()
        styleClass.add(STYLE_CLASS_DARK)
    }

    val selected: Binding<T> = Bindings.createObjectBinding({
        toggleGroup.selectedToggle?.userData as T? ?: options[0]
    }, toggleGroup.selectedToggleProperty())
}
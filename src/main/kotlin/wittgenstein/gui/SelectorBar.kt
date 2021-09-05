package wittgenstein.gui

import javafx.beans.value.ObservableValue
import javafx.geometry.Insets
import javafx.scene.Node
import javafx.scene.control.ToggleButton
import javafx.scene.control.ToggleGroup
import javafx.scene.control.Tooltip
import javafx.scene.layout.HBox
import org.controlsfx.control.SegmentedButton
import wittgenstein.gui.impl.dontDeselectAll
import wittgenstein.gui.impl.map

abstract class SelectorBar<T>(vararg options: List<T>) : HBox(10.0) {
    protected open fun extractGraphic(option: T): Node? = null
    protected open fun extractText(option: T): String? = null
    protected open fun extractDescription(option: T): String? = null
    protected open fun ToggleButton.extraConfig(option: T) {}

    private val map = mutableMapOf<T, ToggleButton>()
    private val toggleGroup = ToggleGroup()

    init {
        for (group in options) {
            val seg = createSegment(group)
            seg.toggleGroup = toggleGroup
            children.add(seg)
        }
        select(options[0][0])
        toggleGroup.dontDeselectAll()
    }

    val selected: ObservableValue<T> = toggleGroup.selectedToggleProperty().map {
        @Suppress("UNCHECKED_CAST")
        it?.userData as T
    }

    private fun createSegment(group: List<T>): SegmentedButton {
        val seg = SegmentedButton()
        for (option in group) {
            val btn = ToggleButton(this.extractText(option), this.extractGraphic(option))
            map[option] = btn
            btn.userData = option
            btn.tooltip = this.extractDescription(option)?.let(::Tooltip)
            if (btn.graphic == null) btn.padding = Insets(11.0)
            @Suppress("LeakingThis")
            btn.extraConfig(option)
            seg.buttons.add(btn)
        }
        seg.styleClass.add("dark")
        return seg
    }

    fun select(option: T) {
        val btn = map.getValue(option)
        btn.isSelected = true
        btn.requestFocus()
    }

    fun receiveFocus() {
        val btn = toggleGroup.selectedToggle as ToggleButton
        btn.requestFocus()
    }

    override fun toString(): String = "${this::class.simpleName} [ selected = ${selected.value} ]"
}
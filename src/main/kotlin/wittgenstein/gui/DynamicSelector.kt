package wittgenstein.gui

import javafx.scene.control.ToggleButton
import javafx.scene.text.Font
import javafx.scene.text.FontPosture
import javafx.scene.text.FontWeight
import wittgenstein.Dynamic

class DynamicSelector : SelectorBar<Dynamic>(Dynamic.values().asList()) {
    override fun extractText(option: Dynamic): String = option.name.lowercase()

    override fun ToggleButton.extraConfig(option: Dynamic) {
        font = Font.font("System", FontWeight.BOLD, FontPosture.ITALIC, 12.0)
    }
}
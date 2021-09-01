package wittgenstein.gui

import javafx.scene.paint.Color
import javafx.scene.text.Font
import javafx.scene.text.FontPosture
import javafx.scene.text.FontWeight
import javafx.scene.text.Text
import wittgenstein.Dynamic
import kotlin.reflect.KMutableProperty0

class DynamicViewElement(
    x: Double,
    y: Double,
    private val property: KMutableProperty0<Dynamic?>
) : ViewElement, Text() {
    init {
        font = Font.font("System", FontWeight.BOLD, FontPosture.ITALIC, 12.0)
        text = property.get().toString()
        this.x = x
        this.y = y
    }

    var value: Dynamic?
        get() = property.get()
        set(value) {
            property.set(value)
            text = value.toString()
        }

    override var isSelected: Boolean = false
        set(value) {
            field = value
            fill = if (isSelected) Color.BLUE else Color.BLACK
        }
}
package cognosco.gui

import cognosco.Dynamic
import cognosco.Time
import javafx.beans.binding.Bindings
import javafx.beans.value.ObservableNumberValue
import javafx.scene.paint.Color
import javafx.scene.text.Font
import javafx.scene.text.FontPosture
import javafx.scene.text.FontWeight
import javafx.scene.text.Text
import kotlin.reflect.KMutableProperty0

class DynamicView(
    referenceX: ObservableNumberValue,
    referenceY: ObservableNumberValue,
    private val dynamicProperty: KMutableProperty0<Dynamic>,
    timeProperty: KMutableProperty0<Time>,
) : SelectableElement, Text() {
    init {
        font = Font.font("System", FontWeight.BOLD, FontPosture.ITALIC, 12.0)
        text = dynamicProperty.get().toString()
        translateX = (3 - text.length) * 3.0
        xProperty().bind(referenceX)
        yProperty().bind(Bindings.add(referenceY, 25))
        isFocusTraversable = true
    }

    var dynamic: Dynamic
        get() = dynamicProperty.get()
        set(value) {
            dynamicProperty.set(value)
            text = value.toString()
            translateX = (3 - text.length) * 3.0
        }

    var time: Time by timeProperty

    override var isSelected: Boolean = false
        set(value) {
            field = value
            fill = if (isSelected) Color.BLUE else Color.BLACK
            if (isSelected) requestFocus()
        }

    override fun toString(): String = "DynamicView [ dynamic = $dynamic, time = $time, selected = $isSelected ]"
}
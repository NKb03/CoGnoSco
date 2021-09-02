package wittgenstein.gui

import javafx.beans.binding.Bindings
import javafx.beans.property.Property
import javafx.beans.value.ObservableNumberValue
import javafx.scene.paint.Color
import javafx.scene.text.Font
import javafx.scene.text.FontPosture
import javafx.scene.text.FontWeight
import javafx.scene.text.Text
import wittgenstein.Dynamic
import wittgenstein.Moment
import kotlin.reflect.KMutableProperty0

class DynamicViewElement(
    referenceX: Property<Number>,
    referenceY: ObservableNumberValue,
    private val dynamicProperty: KMutableProperty0<Dynamic?>,
    momentProperty: KMutableProperty0<Moment?>,
) : ViewElement, Text() {
    init {
        font = Font.font("System", FontWeight.BOLD, FontPosture.ITALIC, 12.0)
        text = dynamicProperty.get().toString()
        xProperty().bindBidirectional(referenceX)
        yProperty().bind(Bindings.subtract(referenceY, 5))
        isFocusTraversable = true
    }

    var dynamic: Dynamic?
        get() = dynamicProperty.get()
        set(value) {
            dynamicProperty.set(value)
            text = value.toString()
        }

    var moment: Moment? by momentProperty

    override var isSelected: Boolean = false
        set(value) {
            field = value
            fill = if (isSelected) Color.BLUE else Color.BLACK
            if (isSelected) requestFocus()
        }
}
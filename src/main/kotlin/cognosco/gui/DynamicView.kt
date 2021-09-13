package cognosco.gui

import cognosco.Dynamic
import cognosco.Time
import cognosco.gui.impl.map
import javafx.beans.binding.Bindings
import javafx.beans.property.Property
import javafx.beans.value.ObservableNumberValue
import javafx.scene.paint.Color
import javafx.scene.text.Font
import javafx.scene.text.FontPosture
import javafx.scene.text.FontWeight
import javafx.scene.text.Text

class DynamicView(
    referenceX: ObservableNumberValue,
    referenceY: ObservableNumberValue,
    val dynamic: Property<Dynamic>,
    val time: Property<Time>,
) : SelectableElement, Text() {
    init {
        font = Font.font("System", FontWeight.BOLD, FontPosture.ITALIC, 12.0)
        textProperty().bind(dynamic.map { d -> d.toString() })
        translateXProperty().bind(textProperty().map { t -> (3 - t.length) * 3.0 })
        xProperty().bind(referenceX)
        yProperty().bind(Bindings.add(referenceY, 25))
        isFocusTraversable = true
    }

    override var isSelected: Boolean = false
        set(value) {
            field = value
            fill = if (isSelected) Color.BLUE else Color.BLACK
            if (isSelected) requestFocus()
        }

    override fun toString(): String = "DynamicView [ dynamic = $dynamic, time = $time, selected = $isSelected ]"
}
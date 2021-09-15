package cognosco.gui

import cognosco.Element
import cognosco.gui.impl.map
import cognosco.gui.impl.mapDouble
import javafx.scene.text.Font
import javafx.scene.text.Text

class InstrumentNameView(head: NoteHead, element: Element) : Text() {
    init {
        font = Font(14.0)
        xProperty().bind(head.xProperty().subtract(textProperty().mapDouble { it.length * 5.0 }))
        yProperty().bind(head.yProperty().subtract(10))
        textProperty().bind(element.instrument.map { it?.shortName ?: "?" })
    }
}
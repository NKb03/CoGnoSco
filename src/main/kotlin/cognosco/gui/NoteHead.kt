package cognosco.gui

import cognosco.Element
import cognosco.NoteHeadType
import cognosco.gui.impl.NodeWrapper
import javafx.beans.property.DoubleProperty
import javafx.scene.paint.Color
import javafx.scene.shape.*

class NoteHead(val element: Element? = null) : NodeWrapper<Shape>(), SelectableElement {
    var noteHeadType = NoteHeadType.Regular
        set(value) {
            field = value
            val node = createShape(value)
            node.fill = this.fill
            setRoot(node)
        }

    var x by this::layoutX
    var y by this::layoutY

    fun xProperty(): DoubleProperty = layoutXProperty()
    fun yProperty(): DoubleProperty = layoutYProperty()

    private var fill: Color = Color.BLACK
        set(value) {
            field = value
            root.fill = value
        }

    init {
        isFocusTraversable = true
        noteHeadType = NoteHeadType.Regular
        regular()
    }

    private fun fill(color: Color) = also { fill = color }

    fun regular() = fill(Color.BLACK)

    fun select() = fill(Color.BLUE)

    fun inCreation() = fill(Color.GREEN)

    fun phantom() = fill(Color.gray(0.0, 0.5))

    fun lastCreated() = fill(Color.PURPLE)

    override var isSelected: Boolean = false
        set(value) {
            if (field != value) {
                field = value
                if (value) select() else regular()
            }
        }

    override fun toString(): String =
        "NoteHead [ type = $noteHeadType, fill = $fill, scale = $scaleX, selected = $isSelected ]"

    companion object {
        fun leftParentheses() = QuadCurve(7.0, 0.0, 0.0, 10.0, 7.0, 20.0).apply {
            stroke = Color.BLACK
            fill = Color.TRANSPARENT
        }


        fun rightParentheses() = QuadCurve(13.0, 0.0, 20.0, 10.0, 13.0, 20.0).apply {
            stroke = Color.BLACK
            fill = Color.TRANSPARENT
        }

        fun createShape(headType: NoteHeadType): Shape = when (headType) {
            NoteHeadType.Regular -> Ellipse(10.0, 7.0).apply {
                rotate = -16.0
            }
            NoteHeadType.Triangle -> Polygon(
                0.0, 14.0,
                10.0, 0.0,
                20.0, 14.0
            )
            NoteHeadType.Rectangle -> Rectangle(22.0, 16.3)
            NoteHeadType.Rhombus -> Polygon(
                0.0, 7.0,
                12.5, 14.0,
                25.0, 8.0,
                12.5, 0.0
            )
            NoteHeadType.Cross -> Shape.union(
                Line(
                    0.0, 0.0,
                    15.0, 12.0
                ).also { it.strokeWidth = 3.0 },
                Line(
                    15.0, 0.0,
                    0.0, 12.0
                ).also { it.strokeWidth = 3.0 }
            )
            NoteHeadType.Slashed -> Line(-10.0, 0.0, 20.0, 15.0)
        }
    }
}
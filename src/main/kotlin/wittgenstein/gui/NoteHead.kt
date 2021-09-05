package wittgenstein.gui

import javafx.beans.property.DoubleProperty
import javafx.scene.paint.Color
import javafx.scene.shape.*
import wittgenstein.Element
import wittgenstein.NoteHeadType
import wittgenstein.gui.impl.NodeWrapper

class NoteHead(val element: Element? = null) : NodeWrapper<Shape>(), SelectableElement {
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
        setNoteHeadType(NoteHeadType.Regular)
        regular()
    }

    private fun fill(color: Color) = also { fill = color }

    fun regular() = fill(Color.BLACK)

    fun select() = fill(Color.BLUE)

    fun inCreation() = fill(Color.GREEN)

    fun phantom() = fill(Color.gray(0.0, 0.5))

    override var isSelected: Boolean = false
        set(value) {
            if (field != value) {
                field = value
                if (value) select() else regular()
            }
        }

    fun setNoteHeadType(headType: NoteHeadType) {
        val node = createShape(headType)
        node.fill = this.fill
        setRoot(node)
    }

    private fun createShape(headType: NoteHeadType): Shape = when (headType) {
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

    companion object {
        fun leftParentheses() = QuadCurve(7.0, 0.0, 0.0, 10.0, 7.0, 20.0).apply {
            stroke = Color.BLACK
            fill = Color.TRANSPARENT
        }


        fun rightParentheses() = QuadCurve(13.0, 0.0, 20.0, 10.0, 13.0, 20.0).apply {
            stroke = Color.BLACK
            fill = Color.TRANSPARENT
        }
    }
}
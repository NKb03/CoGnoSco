package cognosco.gui.impl

import javafx.beans.property.DoubleProperty
import javafx.scene.shape.Polyline
import javafx.scene.shape.Shape

class ZigZagLine(private val verticalDiff: Double) : Polyline(), ILine {
    private val startX = doubleProperty("startX", 0.0) { repaint() }
    private val startY = doubleProperty("startY", 0.0) { repaint() }
    private val endX = doubleProperty("endX", 0.0) { repaint() }
    private val endY = doubleProperty("endY", 0.0) { repaint() }

    override fun startXProperty(): DoubleProperty = startX
    override fun startYProperty(): DoubleProperty = startY
    override fun endXProperty(): DoubleProperty = endX
    override fun endYProperty(): DoubleProperty = endY

    override val shape: Shape
        get() = this

    private fun repaint() {
        points.clear()
        val w = endX.value - startX.value
        for (x in 0..(endX.value - startX.value).toInt() step 3) {
            val factor = x / w
            var y = startY.value * (1 - factor) + endY.value * factor
            if (x % 6 == 0) y += verticalDiff else y -= verticalDiff
            points.addAll(x + startX.value, y)
        }
    }

    override fun toString(): String =
        "ZigZagLine [ startX = ${startX.value}, startY = ${startY.value}, endX = ${endX.value}, endY = ${endY.value} ]"
}
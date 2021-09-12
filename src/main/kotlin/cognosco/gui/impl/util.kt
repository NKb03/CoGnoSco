package cognosco.gui.impl

import cognosco.Accidental
import cognosco.gui.App
import cognosco.lily.lilypond
import javafx.application.Platform
import javafx.beans.binding.Bindings.selectDouble
import javafx.beans.binding.DoubleBinding
import javafx.event.EventTarget
import javafx.scene.Node
import javafx.scene.control.Alert
import javafx.scene.control.ToggleGroup
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import kotlin.concurrent.thread

fun loadImage(acc: Accidental): Image {
    val res = "accidentals/${acc.lilypond()}.png"
    return loadImage(res)
}

private val imageCache = mutableMapOf<String, Image>()

fun loadImage(res: String): Image = imageCache.getOrPut(res) {
    val url = App::class.java.getResource(res) ?: error("resource $res not found")
    return Image(url.toExternalForm())
}

fun Image.view() = ImageView(this)

fun ImageView.fitHeight(height: Double) = apply {
    isPreserveRatio = true
    fitHeight = height
}

fun <N : Node> N.scale(factor: Double): N = also {
    scaleX = factor
    scaleY = factor
}

fun ToggleGroup.dontDeselectAll() {
    selectedToggleProperty().addListener { _, old, new ->
        if (new == null) old.isSelected = true
    }
}

fun Alert.AlertType.show(message: String) = Platform.runLater {
    Alert(this, message).run {
        isResizable = true
        setOnShown {
            thread {
                Thread.sleep(100)
                Platform.runLater {
                    isResizable = false
                }
            }
        }
        show()
    }
}

inline fun <reified R : Any> Any.safeCast(block: R.() -> Unit): Unit? = (this as? R)?.block()

inline fun <reified T> EventTarget.findParentOfType(): T? {
    var cur = this
    while (cur is Node) {
        if (cur is T) return cur
        else cur = cur.parent ?: return null
    }
    return null
}

fun Any.selectDouble(vararg steps: String): DoubleBinding = selectDouble(this, *steps)
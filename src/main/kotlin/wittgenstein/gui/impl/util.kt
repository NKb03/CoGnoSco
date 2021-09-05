package wittgenstein.gui.impl

import javafx.application.Platform
import javafx.scene.Node
import javafx.scene.control.Alert
import javafx.scene.control.ToggleGroup
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import wittgenstein.Accidental
import wittgenstein.gui.App

fun loadImage(acc: Accidental): Image {
    val res = "accidentals/$acc.png"
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

fun Alert.AlertType.show(message: String) {
    Platform.runLater { Alert(this, message).show() }
}

inline fun <reified R: Any> Any.safeCast(block: R.() -> Unit): Unit? = (this as? R)?.block()
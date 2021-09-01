package wittgenstein.gui

import javafx.scene.Node
import javafx.scene.control.ToggleButton
import javafx.scene.control.ToggleGroup
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import wittgenstein.Accidental

fun toggleButton(text: String) = ToggleButton(text).apply { styleToggleButton() }

fun toggleButton(graphic: Node) = ToggleButton(null, graphic).apply { styleToggleButton() }

private fun ToggleButton.styleToggleButton() {
    isFocusTraversable = false
}

fun loadImage(acc: Accidental): Image {
    val res = "accidentals/${acc.resourceName}.png"
    return loadImage(res)
}

private val imageCache = mutableMapOf<String, Image>()

fun loadImage(res: String): Image = imageCache.getOrPut(res) {
    val url = App::class.java.getResource(res)!!.toExternalForm()
    return Image(url)
}

fun Image.view() = ImageView(this)

fun ImageView.scaleBy(factor: Double) = apply {
    isPreserveRatio = true
    isSmooth = true
    fitHeight = image.height * factor
}

fun ImageView.fitHeight(height: Double) = apply {
    isPreserveRatio = true
    fitHeight = height
}

fun ImageView.opacity(value: Double) = apply {
    opacity = value
}

fun ToggleGroup.dontDeselectAll() {
    selectedToggleProperty().addListener { _, old, new ->
        if (new == null) old.isSelected = true
    }
}
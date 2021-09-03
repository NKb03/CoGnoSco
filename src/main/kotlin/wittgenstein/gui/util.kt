package wittgenstein.gui

import javafx.beans.Observable
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.value.ObservableValue
import javafx.scene.Node
import javafx.scene.control.ToggleGroup
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import wittgenstein.Accidental

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

fun <T> binding(vararg observables: Observable, compute: () -> T): ObservableValue<T> {
    val p = SimpleObjectProperty<T>()
    for (obs in observables) {
        obs.addListener { p.set(compute()) }
    }
    return p
}

fun <T, F> ObservableValue<T>.map(f: (T) -> F): ObservableValue<F> = binding(this) { f(value) }

fun <A, B, F> binding(a: ObservableValue<A>, b: ObservableValue<B>, f: (A, B) -> F): ObservableValue<F> =
    binding<F>(a, b) { f(a.value, b.value) }
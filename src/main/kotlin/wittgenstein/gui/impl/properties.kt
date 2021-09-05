package wittgenstein.gui.impl

import javafx.beans.Observable
import javafx.beans.property.*
import javafx.beans.value.ObservableValue
import kotlin.reflect.KProperty

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

fun Any.doubleProperty(name: String, initialValue: Double, handler: (value: Double) -> Unit = {}): DoubleProperty =
    object : DoublePropertyBase() {
        init {
            value = initialValue
        }

        override fun invalidated() {
            handler(value)
        }

        override fun getBean(): Any = this@doubleProperty

        override fun getName(): String = name
    }


operator fun <T> ObservableValue<T>.getValue(thisRef: Any?, property: KProperty<*>): T = value

operator fun <T> Property<T>.setValue(thisRef: Any?, property: KProperty<*>, newValue: T) {
    value = newValue
}


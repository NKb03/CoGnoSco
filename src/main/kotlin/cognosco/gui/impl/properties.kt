package cognosco.gui.impl

import javafx.beans.Observable
import javafx.beans.binding.Bindings
import javafx.beans.binding.DoubleBinding
import javafx.beans.binding.ObjectBinding
import javafx.beans.property.*
import javafx.beans.value.ObservableDoubleValue
import javafx.beans.value.ObservableNumberValue
import javafx.beans.value.ObservableValue
import kotlin.reflect.KProperty

private fun <T, P : Property<T>> bindingImpl(property: P, observables: Array<out Observable>, compute: () -> T): P {
    property.value = compute()
    for (obs in observables) {
        obs.addListener { property.value = compute() }
    }
    return property
}

fun <T> binding(vararg observables: Observable, compute: () -> T): ObservableValue<T> =
    bindingImpl(SimpleObjectProperty(), observables, compute)

fun doubleBinding(vararg observables: Observable, compute: () -> Double): ObservableDoubleValue =
    bindingImpl(SimpleDoubleProperty(), observables, compute)

fun <T, F> ObservableValue<out T>.map(f: (T) -> F): ObservableValue<F> = binding(this) { f(value) }

fun <T> ObservableValue<out T>.mapDouble(f: (T) -> Double): ObservableDoubleValue = doubleBinding(this) { f(value) }

fun <T, F> ObservableValue<out T>.flatMap(f: (T) -> ObservableValue<F>): ObservableValue<F> {
    val prop = SimpleObjectProperty<F>()
    prop.bind(f(value))
    addListener { _, _, new ->
        prop.unbind()
        prop.bind(f(new))
    }
    return prop
}

fun <T : Any?> Any.select(vararg steps: String): ObjectBinding<T> = Bindings.select(this, *steps)

@Suppress("UNCHECKED_CAST")
fun ObservableValue<out Number>.asObservableNumberValue(): ObservableNumberValue =
    object : ObservableNumberValue, ObservableValue<Number> by this as ObservableValue<Number> {
        override fun intValue(): Int = value.toInt()
        override fun longValue(): Long = value.toLong()
        override fun floatValue(): Float = value.toFloat()
        override fun doubleValue(): Double = value.toDouble()
    }

fun ObservableValue<out Double>.asObservableDoubleValue(): ObservableDoubleValue =
    object : ObservableDoubleValue, ObservableNumberValue by asObservableDoubleValue() {
        override fun get(): Double = this@asObservableDoubleValue.value
    }

fun Any.selectDouble(vararg steps: String): DoubleBinding = Bindings.selectDouble(this, *steps)

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


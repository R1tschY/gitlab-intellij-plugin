package de.richardliebscher.intellij.gitlab.utils

import com.intellij.openapi.Disposable
import com.intellij.ui.layout.ComponentPredicate
import com.intellij.util.containers.DisposableWrapperList
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

interface IModel<T> {
    fun get(): T
    fun addListener(listener: (T) -> Unit)
    fun addListener(listener: (T) -> Unit, disposable: Disposable)
}

interface IMutableModel<T> : IModel<T> {
    fun set(value: T)
}

open class Model<T>(initial: T) : IMutableModel<T>, ReadWriteProperty<Any?, T> {
    private val listeners = DisposableWrapperList<(T) -> Unit>()
    private var value: T = initial

    override fun get(): T {
        return value
    }

    override fun set(value: T) {
        if (value != this.value) {
            this.value = value
            listeners.forEach { it(value) }
        }
    }

    override fun addListener(listener: (T) -> Unit) {
        listeners.add(listener)
    }

    override fun addListener(listener: (T) -> Unit, disposable: Disposable) {
        listeners.add(listener, disposable)
    }

    override fun getValue(thisRef: Any?, property: KProperty<*>): T = get()

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) = set(value)
}

fun Model<Boolean>.toPredicate(): ComponentPredicate {
    return object : ComponentPredicate() {
        override fun invoke(): Boolean {
            return get()
        }

        override fun addListener(listener: (Boolean) -> Unit) {
            addListener(listener)
        }
    }
}

fun <T> Model<T>.toPredicate(map: (T) -> Boolean): ComponentPredicate {
    return object : ComponentPredicate() {
        override fun invoke(): Boolean {
            return map(get())
        }

        override fun addListener(listener: (Boolean) -> Unit) {
            this@toPredicate.addListener { value: T -> listener(map(value)) }
        }
    }
}
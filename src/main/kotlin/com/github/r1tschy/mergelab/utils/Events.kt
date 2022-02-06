package com.github.r1tschy.mergelab.utils

import kotlin.properties.ObservableProperty
import kotlin.reflect.KProperty


class Observable<T>(initial: T, private val onChange: (newValue: T) -> Unit) : ObservableProperty<T>(initial) {
    override fun afterChange(property: KProperty<*>, oldValue: T, newValue: T) {
        onChange(newValue)
    }
}
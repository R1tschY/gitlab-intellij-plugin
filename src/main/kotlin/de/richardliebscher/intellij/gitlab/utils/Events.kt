// Copyright 2022 Richard Liebscher.
// Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package de.richardliebscher.intellij.gitlab.utils

import kotlin.properties.ObservableProperty
import kotlin.reflect.KProperty


class Observable<T>(initial: T, private val onChange: (newValue: T) -> Unit) : ObservableProperty<T>(initial) {
    override fun afterChange(property: KProperty<*>, oldValue: T, newValue: T) {
        onChange(newValue)
    }
}
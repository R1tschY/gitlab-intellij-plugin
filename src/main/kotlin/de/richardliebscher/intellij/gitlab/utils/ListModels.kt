package de.richardliebscher.intellij.gitlab.utils

import javax.swing.ListModel

internal fun <E> ListModel<E>.indexOf(item: E): Int {
    for (i in 0 until size) {
        if (getElementAt(i) == item) return i
    }
    return -1
}

internal fun <E> ListModel<E>.iterable(): Iterable<E> {
    return Iterable {
        object : Iterator<E> {
            private var idx = 0

            override fun hasNext(): Boolean = idx < size

            override fun next(): E {
                return getElementAt(idx++)
            }
        }
    }
}
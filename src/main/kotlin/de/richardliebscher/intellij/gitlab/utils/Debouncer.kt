package de.richardliebscher.intellij.gitlab.utils

import com.intellij.openapi.application.invokeLater

class Debouncer {
    @Volatile
    private var locked = false

    fun invoke(fn: () -> Unit) {
        if (locked) {
            return
        }

        locked = true
        invokeLater {
            locked = false
            fn()
        }
    }
}
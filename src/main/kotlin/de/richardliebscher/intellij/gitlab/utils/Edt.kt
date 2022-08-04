package de.richardliebscher.intellij.gitlab.utils

import com.intellij.openapi.application.ApplicationManager

inline fun <T> computeInEdt(crossinline fn: () -> T): T {
    var result: T? = null
    ApplicationManager.getApplication().invokeAndWait {
        result = fn()
    }
    @Suppress("UNCHECKED_CAST")
    return result as T
}
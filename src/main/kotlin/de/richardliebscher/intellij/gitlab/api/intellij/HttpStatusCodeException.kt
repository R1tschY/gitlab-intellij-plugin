package de.richardliebscher.intellij.gitlab.api.intellij

import java.io.IOException

class HttpStatusCodeException : IOException {
    constructor(message: String, statusCode: Int) : super(message) {
        this.statusCode = statusCode
    }

    constructor(message: String, statusCode: Int, cause: Throwable) : super(message, cause) {
        this.statusCode = statusCode
    }

    private val statusCode: Int
}
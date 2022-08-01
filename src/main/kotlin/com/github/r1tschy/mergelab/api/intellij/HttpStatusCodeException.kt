package com.github.r1tschy.mergelab.api.intellij

import java.io.IOException

class HttpStatusCodeException : IOException {
    constructor(message: String, statusCode: Int) : super(message) {
        this.statusCode = statusCode
    }

    constructor(message: String, statusCode: Int, cause: Throwable) : super(message, cause) {
        this.statusCode = statusCode
    }

    val statusCode: Int
}
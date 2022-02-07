package com.github.r1tschy.mergelab.exceptions

/**
 * Exception type of this plugin.
 */
open class GitLabException : Exception {
    constructor(message: String) : super(message)
    constructor(message: String, cause: Throwable) : super(message, cause)
}
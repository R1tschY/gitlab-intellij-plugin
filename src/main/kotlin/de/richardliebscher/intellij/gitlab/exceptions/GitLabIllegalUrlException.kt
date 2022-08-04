package de.richardliebscher.intellij.gitlab.exceptions

/**
 * Illegal server URL given.
 */
class GitLabIllegalUrlException : GitLabException {
    constructor(message: String) : super(message)
    constructor(message: String, cause: Throwable) : super(message, cause)
}
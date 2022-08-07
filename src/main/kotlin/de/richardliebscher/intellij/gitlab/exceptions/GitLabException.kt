package de.richardliebscher.intellij.gitlab.exceptions

/**
 * Exception type of this plugin.
 */
open class GitLabException : Exception {
    protected constructor() : super()
    constructor(message: String) : super(message)
    constructor(message: String, cause: Throwable) : super(message, cause)
}

class UnauthorizedAccessException : GitLabException("Unauthorized access to GitLab. Please check your access token.")

class MissingAccessTokenException : GitLabException("Missing access token")
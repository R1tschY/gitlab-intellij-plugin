// Copyright 2022 Richard Liebscher.
// Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.github.r1tschy.mergelab.model.gitlab

import com.github.r1tschy.mergelab.exceptions.GitLabException


/**
 * User on GitLab server.
 */
data class GitLabUserDetails(val displayName: String, val userName: String, val avatarUrl: String)


/**
 * Communication error with GitLab server.
 */
open class GitLabClientException: GitLabException {
    constructor(message: String) : super(message)
    constructor(message: String, cause: Throwable) : super(message, cause)
}


/**
 * Client for communication with GitLab server.
 */
interface GitLabClient {
    fun echo(text: String): String

    fun currentUser(): GitLabUserDetails
}
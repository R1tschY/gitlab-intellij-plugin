package com.github.r1tschy.mergelab.accounts

import com.intellij.collaboration.auth.AccountDetails
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.util.concurrency.annotations.RequiresBackgroundThread
import java.awt.Image

/**
 * Exception type for problems with access token.
 */
class AccessTokenException(message: String) : Exception(message)

data class AccessTokenDetails(val scopes: List<String>)

data class UserDetails(val username: String, override val name: String, val avatarUrl: String?): AccountDetails

interface GitLabUserApiService {
    @RequiresBackgroundThread
    fun getUserDetails(processIndicator: ProgressIndicator): UserDetails

    @RequiresBackgroundThread
    fun getAvatar(processIndicator: ProgressIndicator, url: String): Image?
}
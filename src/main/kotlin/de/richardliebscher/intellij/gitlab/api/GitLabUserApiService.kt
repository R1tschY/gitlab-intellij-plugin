package de.richardliebscher.intellij.gitlab.api

import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.util.concurrency.annotations.RequiresBackgroundThread
import de.richardliebscher.intellij.gitlab.accounts.AccountDetails
import java.awt.Image
import java.io.IOException

/**
 * Details about GitLab user.
 */
data class UserDetails(
    /**
     * Unique name within GitLab instance.
     */
    val username: String,
    /**
     * Display name.
     */
    override val name: String,
    /**
     * URL for avatar (can be absolute or relative).
     */
    override val avatarUrl: String?
) : AccountDetails


/**
 * Service to load user information.
 */
interface GitLabUserApiService {
    @Throws(IOException::class)
    @RequiresBackgroundThread
    fun getUserDetails(processIndicator: ProgressIndicator): UserDetails

    @Throws(IOException::class)
    @RequiresBackgroundThread
    fun getAvatar(processIndicator: ProgressIndicator, location: String): Image?
}
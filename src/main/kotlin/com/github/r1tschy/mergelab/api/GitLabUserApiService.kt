package com.github.r1tschy.mergelab.api

import com.intellij.collaboration.auth.AccountDetails
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.util.concurrency.annotations.RequiresBackgroundThread
import java.awt.Image


data class UserDetails(val username: String, override val name: String, val avatarUrl: String?): AccountDetails

interface GitLabUserApiService {
    @RequiresBackgroundThread
    fun getUserDetails(processIndicator: ProgressIndicator): UserDetails

    @RequiresBackgroundThread
    fun getAvatar(processIndicator: ProgressIndicator, url: String): Image?
}
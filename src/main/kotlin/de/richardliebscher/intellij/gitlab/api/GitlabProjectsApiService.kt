package de.richardliebscher.intellij.gitlab.api

import com.intellij.openapi.progress.ProgressIndicator
import java.io.IOException

data class GitLabRepositoryUrls(val id: String, val name: String, val sshUrl: String?, val httpsUrl: String?)

interface GitlabProjectsApiService {
    @Throws(IOException::class)
    fun getRepositoriesWithMembership(processIndicator: ProgressIndicator): List<GitLabRepositoryUrls>

    @Throws(IOException::class)
    fun search(query: String?, membership: Boolean, sort: String = "stars_desc", processIndicator: ProgressIndicator): List<GitLabRepositoryUrls>
}
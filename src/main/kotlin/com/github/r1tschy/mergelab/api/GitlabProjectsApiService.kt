package com.github.r1tschy.mergelab.api

import com.intellij.openapi.progress.ProgressIndicator

data class GitlabRepositoryUrls(val id: String, val name: String, val sshUrl: String?, val httpsUrl: String?)

interface GitlabProjectsApiService {
    fun getRepositoriesWithMembership(processIndicator: ProgressIndicator): List<GitlabRepositoryUrls>
}
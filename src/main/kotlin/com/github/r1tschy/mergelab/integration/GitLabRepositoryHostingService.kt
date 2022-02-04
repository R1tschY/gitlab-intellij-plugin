package com.github.r1tschy.mergelab.integration

import com.intellij.dvcs.hosting.RepositoryListLoader
import com.intellij.openapi.project.Project
import git4idea.remote.GitRepositoryHostingService
import git4idea.remote.InteractiveGitHttpAuthDataProvider

class GitLabRepositoryHostingService : GitRepositoryHostingService() {
    override fun getServiceDisplayName(): String {
        return "GitLab"
    }

    override fun getRepositoryListLoader(project: Project): RepositoryListLoader? {
        return null
    }

    override fun getInteractiveAuthDataProvider(project: Project, url: String): InteractiveGitHttpAuthDataProvider? {
        return null
    }

    override fun getInteractiveAuthDataProvider(
        project: Project,
        url: String,
        login: String
    ): InteractiveGitHttpAuthDataProvider? {
        return null
    }
}
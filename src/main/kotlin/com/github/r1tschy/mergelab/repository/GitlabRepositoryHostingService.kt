package com.github.r1tschy.mergelab.repository

import com.github.r1tschy.mergelab.accounts.GitLabAuthService
import com.github.r1tschy.mergelab.api.GitLabApiService
import com.github.r1tschy.mergelab.model.SERVICE_DISPLAY_NAME
import com.intellij.dvcs.hosting.RepositoryListLoader
import com.intellij.dvcs.hosting.RepositoryListLoadingException
import com.intellij.openapi.components.service
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import git4idea.remote.GitRepositoryHostingService
import git4idea.remote.InteractiveGitHttpAuthDataProvider
import java.awt.Component

class GitlabRepositoryHostingService : GitRepositoryHostingService() {
    override fun getServiceDisplayName(): String {
        return SERVICE_DISPLAY_NAME
    }

    override fun getRepositoryListLoader(project: Project): RepositoryListLoader {
        return GitlabRepositoryListLoader(project)
    }

    override fun getInteractiveAuthDataProvider(project: Project, url: String): InteractiveGitHttpAuthDataProvider? {
        // TODO
        return super.getInteractiveAuthDataProvider(project, url)
    }

    override fun getInteractiveAuthDataProvider(
        project: Project,
        url: String,
        login: String
    ): InteractiveGitHttpAuthDataProvider? {
        // TODO
        return super.getInteractiveAuthDataProvider(project, url, login)
    }
}

private class GitlabRepositoryListLoader(private val project: Project) : RepositoryListLoader {
    override fun isEnabled(): Boolean {
        val authService: GitLabAuthService = service()
        return authService.getAccounts().any { authService.getToken(it) != null }
    }

    override fun enable(parentComponent: Component?): Boolean {
        val authService: GitLabAuthService = service()
        val accounts = authService.getAccounts()

        if (accounts.isEmpty()) {
            // TODO: authService.requestNewAccount(project)
            return false
        } else {
            @Suppress("SimplifiableCallChain")
            return accounts.filter { account ->
                @Suppress("RedundantIf")
                if (authService.getToken(account) == null) {
                    // TODO: authService.requestNewToken(account, project)
                    false
                } else {
                    true
                }
            }.isNotEmpty()
        }
    }

    override fun getAvailableRepositoriesFromMultipleSources(progressIndicator: ProgressIndicator): RepositoryListLoader.Result {
        val authService: GitLabAuthService = service()
        val apiService: GitLabApiService = service()

        val accounts = authService.getAccounts()
        val urls = mutableListOf<String>()
        val exceptions = mutableListOf<RepositoryListLoadingException>()

        accounts.forEach { account ->
            try {
                apiService.apiFor(account)
                    ?.getRepositoriesWithMembership(progressIndicator)
                    ?.let { repositoriesWithMembership ->
                        for (repo in repositoriesWithMembership) {
                            if (repo.httpsUrl != null) {
                                urls.add(repo.httpsUrl)
                            } else if (repo.sshUrl != null) {
                                urls.add(repo.sshUrl)
                            }
                        }
                    }
            } catch (e: RepositoryListLoadingException) {
                exceptions.add(e)
            } catch (e: Exception) {
                exceptions.add(
                    RepositoryListLoadingException(
                        "Failed to fetch repositories from GitLab ${account.server}",
                        e
                    )
                )
            }
        }

        return RepositoryListLoader.Result(urls, exceptions)
    }
}
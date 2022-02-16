package com.github.r1tschy.mergelab.repository

import com.github.r1tschy.mergelab.accounts.GitLabAuthService
import com.github.r1tschy.mergelab.api.GitLabApiService
import com.github.r1tschy.mergelab.model.GitLabProjectCoord
import com.github.r1tschy.mergelab.model.GitLabService
import com.github.r1tschy.mergelab.services.GitLabRemotesManager
import com.github.r1tschy.mergelab.utils.computeInEdt
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.components.*
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import git4idea.config.GitProtectedBranchProvider
import git4idea.config.GitSharedSettings
import git4idea.fetch.GitFetchHandler
import git4idea.repo.GitRemote
import git4idea.repo.GitRepository

/**
 * Detect branches where force push is not allowed.
 */
class GitlabProtectedBranchProvider : GitProtectedBranchProvider {
    override fun doGetProtectedBranchPatterns(project: Project): List<String> {
        val remotesManager: GitLabRemotesManager = service()
        val cache: GitlabProtectedBranchCache = project.service()
        return remotesManager.gitLabProjects.flatMap { cache.getProtectedBranchPatternsFor(it) ?: emptyList() }
    }
}

@State(
    name = "com.github.r1tschy.mergelab.GitlabProtectedBranches",
    storages = [Storage(StoragePathMacros.CACHE_FILE)],
    reportStatistic = false
)
internal class GitlabProtectedBranchCache :
    PersistentStateComponentWithModificationTracker<GitlabProtectedBranchCache.State> {

    private var state: State = State()

    class State : BaseState() {
        var protectedBranchPatterns by map<String, List<String>>()
    }

    fun setProtectedBranchPatternsFor(gitlabProject: GitLabProjectCoord, patterns: List<String>) {
        state.protectedBranchPatterns[gitlabProject.toUrl()] = patterns
    }

    fun getProtectedBranchPatternsFor(gitlabProject: GitLabProjectCoord): List<String>? {
        return state.protectedBranchPatterns[gitlabProject.toUrl()]
    }

    override fun getState(): State = state

    override fun loadState(state: State) {
        this.state = state
    }

    override fun getStateModificationCount(): Long = state.modificationCount
}

internal class GitlabProtectedBranchPatternsFetcher : GitFetchHandler {
    override fun doAfterSuccessfulFetch(
        project: Project,
        fetches: Map<GitRepository, List<GitRemote>>,
        indicator: ProgressIndicator
    ) {
        LOG.info("Fetching protected branch patterns ...")
        val protectedBranchPatterns = fetchProtectedBranchPatterns(project, fetches, indicator)
        if (!protectedBranchPatterns.isEmpty()) {
            runInEdt {
                val gitlabProtectedBranchCache = project.service<GitlabProtectedBranchCache>()
                protectedBranchPatterns.forEach {
                    gitlabProtectedBranchCache.setProtectedBranchPatternsFor(it.first, it.second)
                }
            }
        }
    }

    private fun fetchProtectedBranchPatterns(
        project: Project,
        fetches: Map<GitRepository, List<GitRemote>>,
        indicator: ProgressIndicator
    ): List<Pair<GitLabProjectCoord, List<String>>> {
        if (!GitSharedSettings.getInstance(project).isSynchronizeBranchProtectionRules) {
            return listOf()
        }

        indicator.text = "Loading protected branches from GitLab"

        val accounts = computeInEdt { service<GitLabAuthService>().getAccounts() }

        val gitlabProjects = mutableSetOf<GitLabProjectCoord>()
        for (account in accounts) {
            for ((repo, remotes) in fetches) {
                indicator.checkCanceled()

                for (remote in remotes) {
                    GitLabService.getMatchingRemote(repo, remote, account.server)?.let {
                        gitlabProjects.add(it.projectCoord)
                    }
                }
            }
        }

        val apiService: GitLabApiService = service()
        val result = mutableListOf<Pair<GitLabProjectCoord, List<String>>>()
        for (gitlabProject in gitlabProjects) {
            val serverUrl = gitlabProject.server
            val account = accounts.find { it.server == serverUrl }!!
            apiService.apiFor(account)?.let {
                val protectedBranches = it.getProtectedBranches(gitlabProject.projectPath, indicator)
                LOG.info("Fetched protected branch patterns for $gitlabProject: $protectedBranches")
                result.add(Pair(gitlabProject, protectedBranches))
            } ?: LOG.warn("Ignored $gitlabProject: credentials are missing")
        }
        return result
    }

    companion object {
        val LOG = thisLogger()
    }
}
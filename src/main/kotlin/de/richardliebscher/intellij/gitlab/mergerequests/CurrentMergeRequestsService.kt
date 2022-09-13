package de.richardliebscher.intellij.gitlab.mergerequests

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import com.intellij.dvcs.repo.VcsRepositoryManager
import com.intellij.dvcs.repo.VcsRepositoryMappingListener
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.BranchChangeListener
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.concurrency.annotations.RequiresBackgroundThread
import com.intellij.util.messages.Topic
import de.richardliebscher.intellij.gitlab.api.GitLabApiService
import de.richardliebscher.intellij.gitlab.model.GitLabProjectCoord
import de.richardliebscher.intellij.gitlab.model.GitLabRemote
import de.richardliebscher.intellij.gitlab.services.GitLabRemotesManager
import de.richardliebscher.intellij.gitlab.services.GitlabRemoteChangesListener
import de.richardliebscher.intellij.gitlab.ui.GitLabNotifications
import de.richardliebscher.intellij.gitlab.ui.GitLabNotifications.FAILED_GETTING_MERGE_REQUESTS_FOR_BRANCH
import git4idea.branch.GitBranchIncomingOutgoingManager
import git4idea.repo.GitRepository
import git4idea.repo.GitRepositoryChangeListener
import org.jetbrains.annotations.CalledInAny
import java.io.IOException
import java.time.Duration
import java.util.stream.Collectors.toList

data class MergeRequestWorkingCopy(val repoRoot: VirtualFile, val mr: MergeRequest)

@Service
class CurrentMergeRequestsService(private val project: Project) : Disposable {

    @Volatile
    private var currentMergeRequests: List<MergeRequestWorkingCopy> = listOf()
    private val currentMergeRequestsWriteMutex = Object()

    private var cache: Cache<CacheKey, List<MergeRequest>> = Caffeine.newBuilder()
        .maximumSize(1024)
        .expireAfterAccess(Duration.ofMinutes(15))
        .build()

    init {
        val connection = project.messageBus.connect(this)
        connection.subscribe(GitRepository.GIT_REPO_CHANGE, GitRepositoryChangeListener {
            LOG.debug("repository changed")
            updateCurrentMergeRequests()
        })
        connection.subscribe(VcsRepositoryManager.VCS_REPOSITORY_MAPPING_UPDATED, VcsRepositoryMappingListener {
            LOG.debug("repository mappings changed")
            updateCurrentMergeRequests()
        })
        connection.subscribe(GitlabRemoteChangesListener.TOPIC, object : GitlabRemoteChangesListener {
            override fun onRemotesChanged(remotes: List<GitLabRemote>) {
                LOG.debug("GitLab remotes changed")
                updateCurrentMergeRequests()
            }
        })
        connection.subscribe(GitBranchIncomingOutgoingManager.GIT_INCOMING_OUTGOING_CHANGED,
            GitBranchIncomingOutgoingManager.GitIncomingOutgoingListener {
                LOG.debug("repository incoming outgoing changed")
                updateCurrentMergeRequests()
            })
    }

    @CalledInAny
    fun getCurrentMergeRequests(): List<MergeRequestWorkingCopy> {
        return currentMergeRequests
    }

    @CalledInAny
    fun getCurrentMergeRequests(repo: GitRepository): List<MergeRequest> {
        val repoRoot = repo.root
        return currentMergeRequests.stream().filter { it.repoRoot == repoRoot }.map { it.mr }.collect(toList())
    }

    @RequiresBackgroundThread
    private fun fetchCurrentMergeRequests(progressIndicator: ProgressIndicator): List<MergeRequestWorkingCopy> {
        LOG.info("Detecting merge requests for current branches")
        val remotesManager: GitLabRemotesManager = project.service()
        val result: MutableList<MergeRequestWorkingCopy> = mutableListOf()
        for (remote in remotesManager.remotes) {
            val currentBranch = remote.repo.currentBranch
            if (currentBranch != null) {
                val branchTrackInfo = remote.repo.getBranchTrackInfo(currentBranch.name)
                if (branchTrackInfo != null && branchTrackInfo.remote == remote.remote) {
                    val remoteBranchName = branchTrackInfo.remoteBranch.nameForRemoteOperations
                    val mergeRequests: List<MergeRequest>? =
                        getMergeRequests(remote, remoteBranchName, progressIndicator)

                    if (!mergeRequests.isNullOrEmpty()) {
                        LOG.info("Found merge requests for ${currentBranch.name} on ${remote.projectCoord.server}: $mergeRequests")
                        for (mr in mergeRequests) {
                            result.add(MergeRequestWorkingCopy(remote.repo.root, mr))
                        }
                    }
                }
            }
        }

        return result
    }

    private fun getMergeRequests(
        remote: GitLabRemote,
        remoteBranchName: String,
        progressIndicator: ProgressIndicator
    ): List<MergeRequest>? {
        val currentBranch = remote.repo.currentBranch!!
        val apiService: GitLabApiService = service()

        // TODO: notify when token is missing
        // TODO: check if something really changed
        LOG.info("Searching for merge requests for ${currentBranch.name} on ${remote.projectCoord.server} ...")
        try {
            return cache.get(CacheKey(remote.projectCoord, remoteBranchName)) { it ->
                apiService.apiFor(it.projectCoord.server)
                    ?.findMergeRequestsUsingSourceBranch(
                        it.projectCoord.projectPath,
                        it.remoteBranchName,
                        progressIndicator
                    )
            }
        } catch (e: IOException) {
            GitLabNotifications.showError(
                project,
                FAILED_GETTING_MERGE_REQUESTS_FOR_BRANCH,
                "Failed getting merge requests",
                "Failed getting merge requests for ${currentBranch.name} in ${remote.projectCoord}: $e"
            )
            LOG.warn("Failed getting merge requests for ${currentBranch.name} in ${remote.projectCoord}: $e", e)
            return null
        }
    }

    @CalledInAny
    fun refresh() {
        cache.invalidateAll()
        updateCurrentMergeRequests()
    }

    @CalledInAny
    fun updateCurrentMergeRequests() {
        synchronized(currentMergeRequestsWriteMutex) {
            object : Task.Backgroundable(project, "Detecting GitLab merge requests") {
                override fun run(indicator: ProgressIndicator) {
                    synchronized(currentMergeRequestsWriteMutex) {
                        val currentMergeRequests = fetchCurrentMergeRequests(indicator)

                        if (this@CurrentMergeRequestsService.currentMergeRequests != currentMergeRequests) {
                            this@CurrentMergeRequestsService.currentMergeRequests = currentMergeRequests
                            project.messageBus.syncPublisher(CurrentMergeRequestsChangesListener.TOPIC)
                                .onCurrentMergeRequestsChanged(currentMergeRequests)
                        }
                    }
                }
            }.queue()
        }
    }

    class VcsChangesListener(private val project: Project) : BranchChangeListener {
        override fun branchWillChange(branchName: String) {}

        override fun branchHasChanged(branchName: String) {
            if (!project.isDisposed) {
                project.service<CurrentMergeRequestsService>().updateCurrentMergeRequests()
            }
        }
    }

    fun subscribeChanges(disposable: Disposable, listener: CurrentMergeRequestsChangesListener) {
        project.messageBus.connect(disposable).subscribe(CurrentMergeRequestsChangesListener.TOPIC, listener)
    }

    override fun dispose() {}

    companion object {
        private val LOG = logger<GitLabRemotesManager>()
    }
}

private data class CacheKey(val projectCoord: GitLabProjectCoord, val remoteBranchName: String)

interface CurrentMergeRequestsChangesListener {
    fun onCurrentMergeRequestsChanged(remotes: List<MergeRequestWorkingCopy>)

    companion object {
        val TOPIC = Topic.create("Current Merge Requests Changes", CurrentMergeRequestsChangesListener::class.java)
    }
}
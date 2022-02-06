package com.github.r1tschy.mergelab.services

import com.github.r1tschy.mergelab.model.GitLabRemote
import com.github.r1tschy.mergelab.model.GitLabService
import com.github.r1tschy.mergelab.settings.GitLabServerSettings
import com.github.r1tschy.mergelab.utils.Observable
import com.intellij.dvcs.repo.VcsRepositoryMappingListener
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.progress.EmptyProgressIndicator
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.util.messages.Topic
import git4idea.repo.GitRepository
import git4idea.repo.GitRepositoryChangeListener

@Service
class GitLabRemotesManager(private val project: Project) {

    private val gitlabService = GitLabService()
    private val gitlabServer = GitLabServerSettings()

    private var remotes by Observable(emptyList<GitLabRemote>()) { newValue ->
        project.messageBus.syncPublisher(REMOTES_CHANGES_TOPIC).onRemotesChanged(newValue)
    }

    fun getRemotesFor(gitRepository: GitRepository): List<GitLabRemote> {
        return remotes.filter { it.repo == gitRepository }
    }

    fun updateRepositories() {
        ProgressManager.getInstance().runProcessWithProgressAsynchronously(
            object : Task.Backgroundable(project, "Detecting GitLab remotes") {
                override fun run(indicator: ProgressIndicator) {
                    remotes = gitlabService.getRemotes(gitlabServer.getInstance(), project)
                    LOG.info("Refreshed GitLab remotes: $remotes")
                }
            },
            EmptyProgressIndicator()
        )
    }

    fun subscribeRemotesChanges(disposable: Disposable, listener: RemotesChangesListener) {
        project.messageBus.connect(disposable).subscribe(REMOTES_CHANGES_TOPIC, listener)
    }

    class VcsChangesListener(private val project: Project) : VcsRepositoryMappingListener, GitRepositoryChangeListener {
        override fun mappingChanged() {
            updateRepositories(project)
        }

        override fun repositoryChanged(repository: GitRepository) {
            updateRepositories(project)
        }
    }

    interface RemotesChangesListener {
        fun onRemotesChanged(remotes: List<GitLabRemote>)
    }

    companion object {
        private val LOG = logger<GitLabRemotesManager>()

        val REMOTES_CHANGES_TOPIC =
            Topic.create("GitLab Repository Changes", RemotesChangesListener::class.java)

        fun updateRepositories(project: Project) {
            if (!project.isDisposed) {
                project.service<GitLabRemotesManager>().updateRepositories()
            }
        }
    }
}

package com.github.r1tschy.mergelab.services

import com.github.r1tschy.mergelab.accounts.GitLabAccount
import com.github.r1tschy.mergelab.accounts.GitLabAccountsManager
import com.github.r1tschy.mergelab.accounts.GitLabAuthService
import com.github.r1tschy.mergelab.model.GitLabRemote
import com.github.r1tschy.mergelab.model.GitLabService
import com.github.r1tschy.mergelab.utils.Observable
import com.intellij.collaboration.auth.AccountsListener
import com.intellij.dvcs.repo.VcsRepositoryMappingListener
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.util.messages.Topic
import git4idea.repo.GitRepository
import git4idea.repo.GitRepositoryChangeListener

@Service
class GitLabRemotesManager(private val project: Project) {

    init {
        service<GitLabAccountsManager>().addListener(object: AccountsListener<GitLabAccount> {
            override fun onAccountListChanged(old: Collection<GitLabAccount>, new: Collection<GitLabAccount>) {
                updateRepositories()
            }
        })
    }

    private val gitlabService = GitLabService()

    private var remotes by Observable(emptyList<GitLabRemote>()) { newValue ->
        project.messageBus.syncPublisher(REMOTES_CHANGES_TOPIC).onRemotesChanged(newValue)
    }

    fun getRemotesFor(gitRepository: GitRepository): List<GitLabRemote> {
        return remotes.filter { it.repo == gitRepository }
    }

    fun updateRepositories() {
        object: Task.Backgroundable(project, "Detecting GitLab remotes") {
            override fun run(indicator: ProgressIndicator) {
                LOG.info("Detecting GitLab remotes")
                remotes = service<GitLabAuthService>().getAccounts()
                    .flatMap { gitlabService.getRemotes(it.server, project) }
                LOG.info("Refreshed GitLab remotes: $remotes")
            }
        }.queue()
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

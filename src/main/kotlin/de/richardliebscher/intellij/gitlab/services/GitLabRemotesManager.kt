// Copyright 2022 Richard Liebscher.
// Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package de.richardliebscher.intellij.gitlab.services

import com.intellij.dvcs.repo.VcsRepositoryMappingListener
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.util.messages.Topic
import de.richardliebscher.intellij.gitlab.accounts.AccountsListener
import de.richardliebscher.intellij.gitlab.accounts.GitLabAccount
import de.richardliebscher.intellij.gitlab.accounts.GitLabAccountsManager
import de.richardliebscher.intellij.gitlab.accounts.GitLabAuthService
import de.richardliebscher.intellij.gitlab.model.GitLabRemote
import de.richardliebscher.intellij.gitlab.model.GitLabService
import de.richardliebscher.intellij.gitlab.utils.Debouncer
import de.richardliebscher.intellij.gitlab.utils.computeInEdt
import git4idea.repo.GitRemote
import git4idea.repo.GitRepository
import git4idea.repo.GitRepositoryChangeListener

@Service
class GitLabRemotesManager(private val project: Project) {

    @Volatile
    private var _remotes: List<GitLabRemote> = emptyList()

    private val updateDebouncer = Debouncer()

    init {
        service<GitLabAccountsManager>().addListener(object : AccountsListener<GitLabAccount> {
            override fun onAccountListChanged(old: Collection<GitLabAccount>, new: Collection<GitLabAccount>) {
                updateRepositories()
            }
        })
    }

    var remotes: List<GitLabRemote>
        get() = _remotes
        private set(value) {
            val oldValue = _remotes
            _remotes = value
            if (!project.isDisposed && oldValue != value) {
                project.messageBus.syncPublisher(GitlabRemoteChangesListener.TOPIC).onRemotesChanged(value)
            }
        }

    fun getRemotesFor(gitRepository: GitRepository): List<GitLabRemote> {
        return remotes.filter { it.repo == gitRepository }
    }

    fun getRemoteFor(remote: GitRemote): GitLabRemote? {
        return remotes.find { it.remoteName == remote.name }
    }

    private fun updateRepositories() {
        updateDebouncer.invoke {
            object : Task.Backgroundable(project, "Detecting GitLab remotes") {
                override fun run(indicator: ProgressIndicator) {
                    LOG.info("Detecting GitLab remotes")
                    indicator.checkCanceled()
                    val remotes = computeInEdt { service<GitLabAuthService>().getAccounts() }
                        .flatMap { GitLabService.getRemotes(it.server, project) }
                    this@GitLabRemotesManager.remotes = remotes
                    LOG.info("Refreshed GitLab remotes: $remotes")
                }
            }.queue()
        }
    }

    fun subscribeRemotesChanges(disposable: Disposable, listener: GitlabRemoteChangesListener) {
        project.messageBus.connect(disposable).subscribe(GitlabRemoteChangesListener.TOPIC, listener)
    }

    internal class VcsChangesListener(private val project: Project) : VcsRepositoryMappingListener, GitRepositoryChangeListener {
        override fun mappingChanged() {
            updateRepositories(project)
        }

        override fun repositoryChanged(repository: GitRepository) {
            updateRepositories(project)
        }
    }

    companion object {
        private val LOG = logger<GitLabRemotesManager>()

        private fun updateRepositories(project: Project) {
            if (!project.isDisposed) {
                project.service<GitLabRemotesManager>().updateRepositories()
            }
        }
    }
}

interface GitlabRemoteChangesListener {
    fun onRemotesChanged(remotes: List<GitLabRemote>)

    companion object {
        val TOPIC = Topic.create("GitLab Remote Changes", GitlabRemoteChangesListener::class.java)
    }
}

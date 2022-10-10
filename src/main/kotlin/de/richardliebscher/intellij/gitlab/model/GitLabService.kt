// Copyright 2022 Richard Liebscher.
// Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package de.richardliebscher.intellij.gitlab.model

import com.intellij.openapi.project.Project
import com.intellij.util.concurrency.annotations.RequiresBackgroundThread
import git4idea.GitUtil
import git4idea.repo.GitRemote
import git4idea.repo.GitRepository
import java.net.MalformedURLException
import java.net.URL
import java.util.regex.Pattern

val SSH_REMOTE_URL_REGEX: Pattern = Pattern.compile("git@([^:]+):(.*).git")


data class GitLabRemote(
    val repo: GitRepository, val remoteName: String, val remoteUrl: String, val projectCoord: GitLabProjectCoord
)


// TODO: rename!
object GitLabService {

    @RequiresBackgroundThread
    fun getRemotes(instance: GitLabServerUrl, project: Project): List<GitLabRemote> {
        val repositoryManager = GitUtil.getRepositoryManager(project)

        val remotes: MutableList<GitLabRemote> = ArrayList()
        for (repository in repositoryManager.repositories) {
            for (remote in repository.remotes) {
                getMatchingRemote(repository, remote, instance)?.let {
                    remotes.add(it)
                }
            }
        }

        return remotes
    }

    fun getMatchingRemote(repo: GitRepository, remote: GitRemote, instance: GitLabServerUrl): GitLabRemote? {
        for (url in remote.urls) {
            getProjectFromUrl(url, instance)?.let { return GitLabRemote(repo, remote.name, url, it) }
        }
        return null
    }

    fun getProjectFromUrl(remoteUrl: String, instance: GitLabServerUrl): GitLabProjectCoord? {
        val sshUrlMatcher = SSH_REMOTE_URL_REGEX.matcher(remoteUrl)
        if (sshUrlMatcher.matches()) {
            // SSH
            val path = sshUrlMatcher.group(2)
            if (instance.host == sshUrlMatcher.group(1)) {
                return GitLabProjectCoord(instance, GitLabProjectPath(path))
            }
        } else {
            // HTTPS
            val url: URL
            try {
                url = URL(remoteUrl)
            } catch (exp: MalformedURLException) {
                return null
            }
            if (instance.host == url.host
                && instance.port == url.port
                && ((instance.https && url.protocol == "https") || (!instance.https && url.protocol == "http"))
                && url.path.endsWith(".git")
            ) {
                return GitLabProjectCoord(instance, GitLabProjectPath(url.path.removeSuffix(".git").removePrefix("/")))
            }
        }

        return null
    }
}
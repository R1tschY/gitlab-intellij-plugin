package com.github.r1tschy.mergelab.model

import com.intellij.openapi.project.Project
import git4idea.GitUtil
import git4idea.repo.GitRemote
import java.net.MalformedURLException
import java.net.URL
import java.util.regex.Pattern

val SSH_REMOTE_URL_REGEX: Pattern = Pattern.compile("git@([^:]+):(.*)")


data class GitLabRemote(val remote: String, val projectId: GitLabProjectId)


class GitLabService {

    fun getRemotes(gitlab: GitLabServer, project: Project): List<GitLabRemote> {
        val repositoryManager = GitUtil.getRepositoryManager(project)

        val remotes: MutableList<GitLabRemote> = ArrayList()
        for (repository in repositoryManager.repositories) {
            for (remote in repository.remotes) {
                getMatchingRemote(gitlab, remote)?.let {
                    remotes.add(it)
                }
            }
        }

        return remotes
    }

    fun getMatchingRemote(gitlab: GitLabServer, remote: GitRemote): GitLabRemote? {
        val gitLabUrl = URL(gitlab.getServerUrl())

        for (url in remote.pushUrls) {
            getMatchingRemoteFromUrl(remote.name, url, gitLabUrl)?.let { return it }
        }

        return null
    }

    fun getMatchingRemoteFromUrl(
        remoteName: String,
        remoteUrl: String,
        gitLabUrl: URL
    ): GitLabRemote? {
        val sshUrlMatcher = SSH_REMOTE_URL_REGEX.matcher(remoteUrl)
        if (sshUrlMatcher.matches()) {
            // SSH
            val path = sshUrlMatcher.group(2)
            if (gitLabUrl.host == sshUrlMatcher.group(1) && path.endsWith(".git")) {
                return GitLabRemote(remoteName, GitLabProjectId(path.removeSuffix(".git")))
            }
        } else {
            // HTTPS
            val httpsUrl: URL
            try {
                httpsUrl = URL(remoteUrl)
            } catch (exp: MalformedURLException) {
                return null
            }
            if (listOf("https", "http").contains(httpsUrl.protocol)
                && gitLabUrl.host == httpsUrl.host
                && httpsUrl.path.endsWith(".git")
            ) {
                return GitLabRemote(
                    remoteName,
                    GitLabProjectId(httpsUrl.path.removeSuffix(".git").removePrefix("/"))
                )
            }
        }

        return null
    }

}
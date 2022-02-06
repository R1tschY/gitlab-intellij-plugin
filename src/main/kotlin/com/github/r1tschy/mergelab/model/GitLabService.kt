package com.github.r1tschy.mergelab.model

import com.intellij.openapi.project.Project
import git4idea.GitUtil
import git4idea.repo.GitRemote
import git4idea.repo.GitRepository
import java.net.MalformedURLException
import java.net.URL
import java.util.regex.Pattern

val SSH_REMOTE_URL_REGEX: Pattern = Pattern.compile("git@([^:]+):(.*).git")


data class GitLabRemote(
    val repo: GitRepository, val remote: GitRemote, val remoteUrl: String, val projectCoord: GitLabProjectCoord)


class GitLabService {

    fun getRemotes(instance: GitLabInstanceCoord, project: Project): List<GitLabRemote> {
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

    fun getMatchingRemote(repo: GitRepository, remote: GitRemote, instance: GitLabInstanceCoord): GitLabRemote? {
        for (url in remote.urls) {
            getMatchingRemoteFromUrl(repo, remote, url, instance)?.let { return it }
        }
        return null
    }

    fun getMatchingRemoteFromUrl(
        repo: GitRepository,
        remote: GitRemote,
        remoteUrl: String,
        instance: GitLabInstanceCoord
    ): GitLabRemote? {
        val sshUrlMatcher = SSH_REMOTE_URL_REGEX.matcher(remoteUrl)
        if (sshUrlMatcher.matches()) {
            // SSH
            val path = sshUrlMatcher.group(2)
            if (instance.host == sshUrlMatcher.group(1)) {
                return GitLabRemote(repo, remote, remoteUrl, GitLabProjectCoord(instance, GitLabProjectPath(path)))
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
                return GitLabRemote(
                    repo,
                    remote,
                    remoteUrl,
                    GitLabProjectCoord(instance, GitLabProjectPath(url.path.removeSuffix(".git").removePrefix("/")))
                )
            }
        }

        return null
    }
}
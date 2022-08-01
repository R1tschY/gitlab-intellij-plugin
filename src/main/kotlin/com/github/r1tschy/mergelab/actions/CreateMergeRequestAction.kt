package com.github.r1tschy.mergelab.actions

import com.github.r1tschy.mergelab.GitLabIcons
import com.github.r1tschy.mergelab.model.GitLabRemote
import com.github.r1tschy.mergelab.services.GitLabRemotesManager
import com.github.r1tschy.mergelab.utils.buildNewMergeRequestUrl
import com.intellij.ide.BrowserUtil
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.components.service
import git4idea.GitUtil

class CreateMergeRequestAction : AnAction(
    "Create Merge Request",
    "Open create Merge Request dialog in browser",
    GitLabIcons.GITLAB
) {

    override fun actionPerformed(e: AnActionEvent) {
        val ctx = getContext(e.dataContext) ?: return
        val gitLabRemote = ctx.remote

        val url = buildNewMergeRequestUrl(gitLabRemote.projectCoord, null, ctx.branch, null, null)
        BrowserUtil.browse(url)
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = getContext(e.dataContext) != null
    }

    private fun getContext(ctx: DataContext): Context? {
        val project = ctx.getData(CommonDataKeys.PROJECT) ?: return null
        if (project.isDisposed) return null

        val gitLabRemotesManager = project.service<GitLabRemotesManager>()
        val remotes = gitLabRemotesManager.remotes
        if (remotes.size == 1) {
            val remote = remotes.single()
            val repo = remote.repo
            val branch = repo.currentBranchName ?: return null
            val branchTrackInfo = repo.getBranchTrackInfo(branch)?.remoteBranch ?: return null
            return Context(remote, branchTrackInfo.nameForRemoteOperations)
        }

        val file = ctx.getData(CommonDataKeys.VIRTUAL_FILE) ?: return null
        val repo = GitUtil.getRepositoryManager(project).getRepositoryForFileQuick(file) ?: return null
        val branch = repo.currentBranchName ?: return null
        val branchTrackInfo = repo.getBranchTrackInfo(branch)?.remoteBranch ?: return null
        val remote = gitLabRemotesManager.getRemoteFor(branchTrackInfo.remote) ?: return null
        return Context(remote, branchTrackInfo.nameForRemoteOperations)
    }

    private class Context(val remote: GitLabRemote, val branch: String)
}
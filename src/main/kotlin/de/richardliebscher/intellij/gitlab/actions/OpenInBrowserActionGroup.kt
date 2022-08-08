// Copyright 2022 Richard Liebscher.
// Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package de.richardliebscher.intellij.gitlab.actions

import com.intellij.ide.BrowserUtil
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.VcsDataKeys
import com.intellij.openapi.vcs.changes.Change
import com.intellij.openapi.vcs.changes.ChangeListManager
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.containers.ContainerUtil
import com.intellij.vcs.log.VcsLogDataKeys
import de.richardliebscher.intellij.gitlab.GitLabIcons
import de.richardliebscher.intellij.gitlab.model.GitLabRemote
import de.richardliebscher.intellij.gitlab.services.GitLabRemotesManager
import de.richardliebscher.intellij.gitlab.ui.GitLabNotifications
import de.richardliebscher.intellij.gitlab.ui.GitLabNotifications.OPEN_IN_BROWSER_FILE_IS_NOT_IN_REPO
import de.richardliebscher.intellij.gitlab.ui.GitLabNotifications.OPEN_IN_BROWSER_REPO_HAS_NO_HEAD
import de.richardliebscher.intellij.gitlab.utils.buildCommitUrl
import de.richardliebscher.intellij.gitlab.utils.buildFileUrl
import git4idea.GitFileRevision
import git4idea.GitUtil
import org.jetbrains.annotations.Nls


class OpenInBrowserActionGroup : ActionGroup(
    "Open on GitLab",
    "Open item in browser",
    GitLabIcons.GITLAB
) {

    override fun actionPerformed(e: AnActionEvent) {
        getContext(e.dataContext)?.let { OpenInBrowserAction(it.first()).actionPerformed(e) }
    }

    override fun getChildren(e: AnActionEvent?): Array<AnAction> {
        e ?: return emptyArray()

        val ctx = getContext(e.dataContext) ?: return emptyArray()
        if (ctx.size <= 1) return emptyArray()

        return ctx.map { OpenInBrowserAction(it) }.toTypedArray()
    }

    override fun update(e: AnActionEvent) {
        val ctx = getContext(e.dataContext)
        e.presentation.isEnabledAndVisible = !ContainerUtil.isEmpty(ctx)
        e.presentation.isPerformGroup = ctx?.size == 1
        e.presentation.isPopupGroup = true
    }

    private fun getContext(ctx: DataContext): List<Context>? {
        val project = ctx.getData(CommonDataKeys.PROJECT) ?: return null

        return getHistoryContext(project, ctx)
            ?: getLogContext(project, ctx)
            ?: getFileContext(project, ctx)
    }

    private fun getHistoryContext(project: Project, ctx: DataContext): List<Context>? {
        val fileRevision = ctx.getData(VcsDataKeys.VCS_FILE_REVISION) ?: return null
        if (fileRevision !is GitFileRevision) return null

        // TODO: use only root
        val repo = GitUtil.getRepositoryManager(project).getRepositoryForFileQuick(fileRevision.path) ?: return null

        val remotes = project.service<GitLabRemotesManager>().getRemotesFor(repo)
        if (remotes.isEmpty()) return null

        return remotes.map { Context.Commit(project, it, fileRevision.revisionNumber.asString()) }
    }

    private fun getLogContext(project: Project, ctx: DataContext): List<Context>? {
        val log = ctx.getData(VcsLogDataKeys.VCS_LOG) ?: return null

        val selected = log.selectedCommits
        val commit = ContainerUtil.getOnlyItem(selected) ?: return null

        // TODO: use only root
        val repo = GitUtil.getRepositoryManager(project).getRepositoryForRootQuick(commit.root) ?: return null

        val remotes = project.service<GitLabRemotesManager>().getRemotesFor(repo)
        if (remotes.isEmpty()) return null

        return remotes.map { Context.Commit(project, it, commit.hash.asString()) }
    }

    private fun getFileContext(project: Project, ctx: DataContext): List<Context>? {
        val file = ctx.getData(CommonDataKeys.VIRTUAL_FILE) ?: return null

        val repo = GitUtil.getRepositoryManager(project).getRepositoryForFileQuick(file) ?: return null

        val remotes = project.service<GitLabRemotesManager>().getRemotesFor(repo)
        if (remotes.isEmpty()) return null

        val changeListManager = ChangeListManager.getInstance(project)
        if (changeListManager.isUnversioned(file)) return null

        val change = changeListManager.getChange(file)
        if (change != null && change.type == Change.Type.NEW) {
            return null
        }

        return remotes.map { Context.File(project, it, file) }
    }

    private sealed class Context(val project: Project, val remote: GitLabRemote) {
        @Nls
        fun getName(): String {
            return remote.projectCoord.toDisplayName()
        }

        abstract fun open(e: AnActionEvent)

        class File(
            project: Project,
            remote: GitLabRemote,
            val file: VirtualFile
        ) : Context(project, remote) {

            override fun open(e: AnActionEvent) {
                val relativePath = VfsUtilCore.getRelativePath(file, remote.repo.root)
                if (relativePath == null) {
                    GitLabNotifications.showError(
                        project,
                        OPEN_IN_BROWSER_FILE_IS_NOT_IN_REPO,
                        "Can't open in browser",
                        "File ist not under repository root"
                    )
                    return
                }

                val hash = remote.repo.currentRevision
                if (hash == null) {
                    GitLabNotifications.showError(
                        project,
                        OPEN_IN_BROWSER_REPO_HAS_NO_HEAD,
                        "Can't open in browser",
                        "Repository has no HEAD"
                    )
                    return
                }

                val editor = e.getData(CommonDataKeys.EDITOR)
                BrowserUtil.browse(buildFileUrl(editor, remote.projectCoord, hash, relativePath))
            }
        }

        class Commit(project: Project, remote: GitLabRemote, val hash: String) : Context(project, remote) {
            override fun open(e: AnActionEvent) {
                BrowserUtil.browse(buildCommitUrl(remote.projectCoord, hash))
            }
        }
    }

    private companion object {
        class OpenInBrowserAction(val ctx: Context) : AnAction({ ctx.getName() }) {
            override fun actionPerformed(e: AnActionEvent) {
                ctx.open(e)
            }
        }
    }
}
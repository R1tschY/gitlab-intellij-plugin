// Copyright 2000-2020 JetBrains s.r.o.
// Copyright 2022 Richard Liebscher.
// Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package de.richardliebscher.intellij.gitlab.actions

import com.intellij.ide.BrowserUtil
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.VcsDataKeys
import com.intellij.openapi.vcs.annotate.AnnotationGutterActionProvider
import com.intellij.openapi.vcs.annotate.FileAnnotation
import com.intellij.openapi.vcs.annotate.UpToDateLineNumberListener
import com.intellij.openapi.vcs.changes.Change
import com.intellij.openapi.vcs.changes.ChangeListManager
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.containers.ContainerUtil
import com.intellij.vcs.log.VcsLogDataKeys
import com.intellij.vcsUtil.VcsUtil
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
import git4idea.annotate.GitFileAnnotation
import org.jetbrains.annotations.Nls

internal sealed class Target(val project: Project, val remote: GitLabRemote) {
    @Nls
    fun getName(): String {
        return remote.projectCoord.toDisplayName()
    }

    abstract fun open(e: AnActionEvent)

    class File(
        project: Project,
        remote: GitLabRemote,
        val file: VirtualFile
    ) : Target(project, remote) {

        override fun open(e: AnActionEvent) {
            val relativePath = VfsUtilCore.getRelativePath(file, remote.repo.root)
            if (relativePath == null) {
                GitLabNotifications.showError(
                    project,
                    OPEN_IN_BROWSER_FILE_IS_NOT_IN_REPO,
                    "Can't open in browser",
                    "File is not under repository root"
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

    class Commit(project: Project, remote: GitLabRemote, val hash: String) : Target(project, remote) {
        override fun open(e: AnActionEvent) {
            BrowserUtil.browse(buildCommitUrl(remote.projectCoord, hash))
        }
    }
}

abstract class AbstractOpenInBrowserAction : ActionGroup(
    "Open on GitLab",
    "Open item in browser",
    GitLabIcons.GITLAB
) {
    override fun actionPerformed(e: AnActionEvent) {
        getTargets(e.dataContext)?.let { Action(it.first()).actionPerformed(e) }
    }

    override fun getChildren(e: AnActionEvent?): Array<AnAction> {
        e ?: return emptyArray()

        val targets = getTargets(e.dataContext) ?: return emptyArray()
        if (targets.size <= 1) return emptyArray()

        return targets.map { Action(it) }.toTypedArray()
    }

    override fun update(e: AnActionEvent) {
        val ctx = getTargets(e.dataContext)
        e.presentation.isEnabledAndVisible = !ContainerUtil.isEmpty(ctx)
        e.presentation.isPerformGroup = ctx?.size == 1
        e.presentation.isPopupGroup = true
    }

    internal abstract fun getTargets(ctx: DataContext): List<Target>?

    companion object {
        private class Action(val target: Target) : AnAction({ target.getName() }) {
            override fun actionPerformed(e: AnActionEvent) {
                target.open(e)
            }
        }
    }
}


class OpenInBrowserAction : AbstractOpenInBrowserAction() {
    override fun getTargets(ctx: DataContext): List<Target>? {
        val project = ctx.getData(CommonDataKeys.PROJECT) ?: return null

        return getHistoryContext(project, ctx)
            ?: getLogContext(project, ctx)
            ?: getFileContext(project, ctx)
    }

    private fun getHistoryContext(project: Project, ctx: DataContext): List<Target>? {
        val fileRevision = ctx.getData(VcsDataKeys.VCS_FILE_REVISION) ?: return null
        if (fileRevision !is GitFileRevision) return null

        // TODO: use only root
        val repo = GitUtil.getRepositoryManager(project).getRepositoryForFileQuick(fileRevision.path) ?: return null

        val remotes = project.service<GitLabRemotesManager>().getRemotesFor(repo)
        if (remotes.isEmpty()) return null

        return remotes.map { Target.Commit(project, it, fileRevision.revisionNumber.asString()) }
    }

    private fun getLogContext(project: Project, ctx: DataContext): List<Target>? {
        val log = ctx.getData(VcsLogDataKeys.VCS_LOG) ?: return null

        val selected = log.selectedCommits
        val commit = ContainerUtil.getOnlyItem(selected) ?: return null

        // TODO: use only root
        val repo = GitUtil.getRepositoryManager(project).getRepositoryForRootQuick(commit.root) ?: return null

        val remotes = project.service<GitLabRemotesManager>().getRemotesFor(repo)
        if (remotes.isEmpty()) return null

        return remotes.map { Target.Commit(project, it, commit.hash.asString()) }
    }

    private fun getFileContext(project: Project, ctx: DataContext): List<Target>? {
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

        return remotes.map { Target.File(project, it, file) }
    }
}

class GitLabAnnotationGutterActionProvider : AnnotationGutterActionProvider {
    override fun createAction(annotation: FileAnnotation): AnAction {
        return OpenInBrowserFromAnnotationAction(annotation)
    }
}

class OpenInBrowserFromAnnotationAction(private val annotation: FileAnnotation)
    : AbstractOpenInBrowserAction(), UpToDateLineNumberListener {

    private var myLineNumber = -1

    override fun consume(integer: Int) {
        myLineNumber = integer
    }

    override fun getTargets(ctx: DataContext): List<Target>? {
        if (myLineNumber < 0) return null

        val annotation = (annotation as? GitFileAnnotation) ?: return null
        val project = annotation.project
        val virtualFile = annotation.file

        val filePath = VcsUtil.getFilePath(virtualFile)
        val repository = GitUtil.getRepositoryManager(project).getRepositoryForFileQuick(filePath) ?: return null

        val remotes = project.service<GitLabRemotesManager>().getRemotesFor(repository)
        if (remotes.isEmpty()) return null

        val revisionHash = annotation.getLineRevisionNumber(myLineNumber)?.asString() ?: return null

        return remotes.map { Target.Commit(project, it, revisionHash) }
    }
}
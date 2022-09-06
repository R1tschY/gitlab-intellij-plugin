package de.richardliebscher.intellij.gitlab.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.components.service
import de.richardliebscher.intellij.gitlab.mergerequests.CurrentMergeRequestsService
import de.richardliebscher.intellij.gitlab.services.GitLabRemotesManager

class RefreshMergeRequestsAction : AnAction(
    "Refresh Merge Requests Status",
    "Refresh information about Merge Requests status",
    null
) {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.dataContext.getData(CommonDataKeys.PROJECT) ?: return

        project.service<CurrentMergeRequestsService>().refresh()
    }

    override fun update(e: AnActionEvent) {
        val project = e.dataContext.getData(CommonDataKeys.PROJECT) ?: return

        e.presentation.isEnabledAndVisible = project.service<GitLabRemotesManager>().remotes.isNotEmpty()
    }
}
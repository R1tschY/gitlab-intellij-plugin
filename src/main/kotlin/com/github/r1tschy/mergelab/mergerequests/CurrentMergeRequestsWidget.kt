package com.github.r1tschy.mergelab.mergerequests

import com.github.r1tschy.mergelab.utils.invokeLaterInEdt
import com.intellij.dvcs.DvcsUtil
import com.intellij.ide.BrowserUtil
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.wm.StatusBar
import com.intellij.openapi.wm.StatusBarWidget
import com.intellij.openapi.wm.StatusBarWidgetFactory
import com.intellij.openapi.wm.impl.status.EditorBasedWidget
import com.intellij.openapi.wm.impl.status.widget.StatusBarWidgetsManager
import com.intellij.util.Consumer
import com.intellij.util.concurrency.annotations.RequiresEdt
import git4idea.GitUtil
import git4idea.config.GitVcsSettings
import git4idea.repo.GitRepository
import org.jetbrains.annotations.Nls
import java.awt.Component
import java.awt.event.MouseEvent

class CurrentMergeRequestsWidget(project: Project) : EditorBasedWidget(project), StatusBarWidget.TextPresentation {

    private var mergeRequest: PullRequest? = null
    private val mrService = project.service<CurrentMergeRequestsService>()

    init {
        update()

        mrService.subscribeChanges(this, object : CurrentMergeRequestsChangesListener {
            override fun onCurrentMergeRequestsChanged(remotes: List<PullRequest>) {
                invokeLaterInEdt { update() }
            }
        })
    }

    override fun ID(): String = WIDGET_ID

    @RequiresEdt
    override fun getTooltipText(): String? {
        return mergeRequest?.let {
            return "Merge Request ${it.id}: ${it.title}"
        }
    }

    override fun getClickConsumer(): Consumer<MouseEvent> {
        return Consumer<MouseEvent> {
            mergeRequest?.webUrl?.let { BrowserUtil.browse(it) }
        }
    }

    override fun getText(): String {
        return mergeRequest?.let { "!${it.iid.asString()}" } ?: "!???"
    }

    override fun getAlignment(): Float {
        return Component.LEFT_ALIGNMENT
    }

    override fun getPresentation(): StatusBarWidget.WidgetPresentation {
        return this
    }

    private fun update() {
        // TODO: val guessRepo = guessCurrentRepository()

        val mergeRequests = mrService.getCurrentMergeRequests()

        mergeRequest = if (mergeRequests.isNotEmpty()) {
            mergeRequests[0] // TODO: make a better guess
        } else {
            null
        }
    }

    private fun guessCurrentRepository(): GitRepository? {
        return DvcsUtil.guessCurrentRepositoryQuick(
            project, GitUtil.getRepositoryManager(project),
            GitVcsSettings.getInstance(project).recentRootPath
        )
    }

    class Factory : StatusBarWidgetFactory {

        override fun getId(): String {
            return WIDGET_ID
        }

        override fun getDisplayName(): @Nls String {
            return "GitLab Merge Request"
        }

        override fun isAvailable(project: Project): Boolean {
            return project.service<CurrentMergeRequestsService>().getCurrentMergeRequests().isNotEmpty()
        }

        override fun createWidget(project: Project): StatusBarWidget {
            return CurrentMergeRequestsWidget(project)
        }

        override fun disposeWidget(widget: StatusBarWidget) {
            Disposer.dispose(widget)
        }

        override fun canBeEnabledOn(statusBar: StatusBar): Boolean {
            return true
        }
    }

    class ChangesListener(private val project: Project) : CurrentMergeRequestsChangesListener {
        override fun onCurrentMergeRequestsChanged(remotes: List<PullRequest>) {
            project.service<StatusBarWidgetsManager>().updateWidget(Factory::class.java)
        }
    }

    companion object {
        const val WIDGET_ID: String = "mergelab-mr"
    }
}
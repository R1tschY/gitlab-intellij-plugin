package de.richardliebscher.intellij.gitlab.mergerequests

import com.intellij.dvcs.DvcsUtil
import com.intellij.ide.BrowserUtil
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerEvent
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.StatusBar
import com.intellij.openapi.wm.StatusBarWidget
import com.intellij.openapi.wm.StatusBarWidgetFactory
import com.intellij.openapi.wm.impl.status.EditorBasedWidget
import com.intellij.openapi.wm.impl.status.widget.StatusBarWidgetsManager
import com.intellij.util.Consumer
import com.intellij.util.concurrency.annotations.RequiresEdt
import de.richardliebscher.intellij.gitlab.services.GitLabRemotesManager
import git4idea.GitUtil
import git4idea.config.GitVcsSettings
import git4idea.repo.GitRepository
import org.jetbrains.annotations.Nls
import java.awt.Component
import java.awt.event.MouseEvent

class CurrentMergeRequestsWidget(project: Project) : EditorBasedWidget(project), StatusBarWidget.TextPresentation {

    @Volatile
    private var mergeRequest: MergeRequest? = null
    private val mrService = project.service<CurrentMergeRequestsService>()
    private val gitVcsSettings = GitVcsSettings.getInstance(project)
    private val gitRepositoryManager = GitUtil.getRepositoryManager(project)

    init {
        mrService.subscribeChanges(this, object : CurrentMergeRequestsChangesListener {
            override fun onCurrentMergeRequestsChanged(remotes: List<MergeRequestWorkingCopy>) {
                updateLater()
            }
        })
    }

    override fun ID(): String = WIDGET_ID

    @RequiresEdt
    override fun getTooltipText(): String? {
        return mergeRequest?.let {
            return "Merge Request ${it.iid.asString()}: ${it.title}"
        }
    }

    override fun getClickConsumer(): Consumer<MouseEvent> {
        return Consumer<MouseEvent> {
            mergeRequest?.webUrl?.let { BrowserUtil.browse(it) }
        }
    }

    override fun getText(): String {
        return mergeRequest?.let { "!${it.iid.asString()}" } ?: ""
    }

    override fun getAlignment(): Float {
        return Component.LEFT_ALIGNMENT
    }

    override fun getPresentation(): StatusBarWidget.WidgetPresentation {
        return this
    }

    override fun selectionChanged(event: FileEditorManagerEvent) {
        LOG.debug("selection changed")
        update()
    }

    override fun fileOpened(source: FileEditorManager, file: VirtualFile) {
        LOG.debug("file opened")
        update()
    }

    override fun fileClosed(source: FileEditorManager, file: VirtualFile) {
        LOG.debug("file closed")
        update()
    }

    override fun install(statusBar: StatusBar) {
        super.install(statusBar)
        updateLater()
    }

    private fun updateLater() {
        if (isDisposed) {
            return
        }

        ApplicationManager.getApplication().invokeLater(::update, project.disposed)
    }

    private fun update() {
        if (isDisposed) {
            return
        }

        val guessedRepo = guessCurrentRepository()

        val allMergeRequests = mrService.getCurrentMergeRequests()
        val mergeRequests = guessedRepo
            ?.let { repo -> allMergeRequests.filter { repo.root == it.repoRoot } }
            ?: listOf()

        val openMergeRequests = mergeRequests.filter { it.mr.state == MergeRequestState.OPEN }

        val newMergeRequest = if (openMergeRequests.isNotEmpty()) {
            // TODO: use newest
            openMergeRequests[0].mr
        } else if (mergeRequests.isNotEmpty()) {
            // TODO: use newest
            mergeRequests[0].mr
        } else {
            null
        }

        if (mergeRequest != newMergeRequest) {
            mergeRequest = newMergeRequest
            if (myStatusBar != null) {
                myStatusBar.updateWidget(ID())
            }
        }
    }

    private fun guessCurrentRepository(): GitRepository? {
        return DvcsUtil.guessCurrentRepositoryQuick(project, gitRepositoryManager, gitVcsSettings.recentRootPath)
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
        @Volatile
        private var lastState: Boolean? = null

        override fun onCurrentMergeRequestsChanged(remotes: List<MergeRequestWorkingCopy>) {
            val newState = remotes.isNotEmpty()
            if (lastState != newState) {
                lastState = newState
                project.service<StatusBarWidgetsManager>().updateWidget(Factory::class.java)
            }
        }
    }

    companion object {
        const val WIDGET_ID: String = "gitlab4devs-mr"

        private val LOG = logger<GitLabRemotesManager>()
    }
}
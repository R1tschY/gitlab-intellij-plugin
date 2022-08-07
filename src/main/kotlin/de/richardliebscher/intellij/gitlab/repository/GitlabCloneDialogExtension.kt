@file:Suppress("UnstableApiUsage")
// Copyright 2022 Richard Liebscher
// Copyright 2000-2021 JetBrains s.r.o.
// Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package de.richardliebscher.intellij.gitlab.repository

import com.intellij.collaboration.auth.AccountsListener
import com.intellij.dvcs.repo.ClonePathProvider
import com.intellij.dvcs.ui.CloneDvcsValidationUtils
import com.intellij.dvcs.ui.FilePathDocumentChildPathHandle
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.observable.util.addListDataListener
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vcs.CheckoutProvider
import com.intellij.openapi.vcs.ui.cloneDialog.VcsCloneDialogExtension
import com.intellij.openapi.vcs.ui.cloneDialog.VcsCloneDialogExtensionComponent
import com.intellij.openapi.vcs.ui.cloneDialog.VcsCloneDialogExtensionStatusLine
import com.intellij.openapi.vcs.ui.cloneDialog.VcsCloneDialogExtensionStatusLine.Companion.greyText
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.wm.IdeFocusManager
import com.intellij.ui.*
import com.intellij.ui.SingleSelectionModel
import com.intellij.ui.components.JBList
import com.intellij.ui.components.panels.Wrapper
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.gridLayout.HorizontalAlign
import com.intellij.ui.dsl.gridLayout.VerticalAlign
import com.intellij.ui.layout.listCellRenderer
import com.intellij.ui.speedSearch.NameFilteringListModel
import com.intellij.ui.speedSearch.SpeedSearch
import com.intellij.util.ui.JBEmptyBorder
import com.intellij.util.ui.UIUtil
import de.richardliebscher.intellij.gitlab.GitLabIcons
import de.richardliebscher.intellij.gitlab.GitlabBundle
import de.richardliebscher.intellij.gitlab.accounts.GitLabAccount
import de.richardliebscher.intellij.gitlab.accounts.GitLabAccountsManager
import de.richardliebscher.intellij.gitlab.accounts.GitLabAuthService
import de.richardliebscher.intellij.gitlab.api.GitLabApiService
import de.richardliebscher.intellij.gitlab.api.GitLabRepositoryUrls
import de.richardliebscher.intellij.gitlab.exceptions.MissingAccessTokenException
import de.richardliebscher.intellij.gitlab.exceptions.UnauthorizedAccessException
import de.richardliebscher.intellij.gitlab.model.GitLabProjectCoord
import de.richardliebscher.intellij.gitlab.model.GitLabProjectPath
import de.richardliebscher.intellij.gitlab.model.GitProtocol
import de.richardliebscher.intellij.gitlab.model.SERVICE_DISPLAY_NAME
import de.richardliebscher.intellij.gitlab.ui.GitLabNotifications
import de.richardliebscher.intellij.gitlab.utils.Model
import de.richardliebscher.intellij.gitlab.utils.toPredicate
import git4idea.GitUtil
import git4idea.checkout.GitCheckoutProvider
import git4idea.commands.Git
import git4idea.remote.GitRememberedInputs
import java.nio.file.Paths
import javax.swing.*
import javax.swing.event.DocumentEvent
import javax.swing.event.ListDataEvent
import javax.swing.event.ListDataEvent.CONTENTS_CHANGED
import javax.swing.event.ListDataListener
import kotlin.properties.Delegates
import kotlin.reflect.KClass

class GitlabCloneDialogExtension : VcsCloneDialogExtension {
    @Deprecated("Use createMainComponent(Project, ModalityState)")
    override fun createMainComponent(project: Project): VcsCloneDialogExtensionComponent {
        throw UnsupportedOperationException("Shouldn't be called")
    }

    override fun createMainComponent(project: Project, modalityState: ModalityState): VcsCloneDialogExtensionComponent {
        return GitlabCloneDialogExtensionComponent(project)
    }

    override fun getAdditionalStatusLines(): List<VcsCloneDialogExtensionStatusLine> {
        val authService: GitLabAuthService = service()
        val accounts = authService.getAccounts()
        return if (accounts.isEmpty()) {
            listOf(greyText("No accounts"))
        } else {
            accounts.map { greyText(it.toString()) }
        }
    }

    override fun getIcon(): Icon = GitLabIcons.GITLAB
    override fun getName(): String = SERVICE_DISPLAY_NAME
}


internal class GitlabCloneDialogExtensionComponent(
    private val project: Project
) : VcsCloneDialogExtensionComponent() {

    private val projectsModel: SimpleListModel<GitLabRepositoryUrls> = SimpleListModel()
    private val accountsModel: SortedAccountsListModel =
        SortedAccountsListModel(service<GitLabAuthService>().getAccounts())

    private val searchTextField = SearchTextField(false)
    private val targetPathSelector: TextFieldWithBrowseButton
    private val targetPathHandle: FilePathDocumentChildPathHandle

    private var selectedCloneUrl by Delegates.observable<String?>(null) { _, _, _ -> onSelectedCloneUrlChanged() }

    private val selectedAccountModel = Model<GitLabAccount?>(null)
    internal var selectedAccount by selectedAccountModel

    private val wrapper = Wrapper()
    private val projectsPanel: DialogPanel
    private val projectsListView: JBList<GitLabRepositoryUrls> = JBList(projectsModel).apply {
        cellRenderer = listCellRenderer { value, _, _ -> text = value.name }
        selectionModel = SingleSelectionModel()

        addListSelectionListener {
            if (!it.valueIsAdjusting) {
                updateSelectedProject()
            }
        }
    }

    init {
        val accountSelected = selectedAccountModel.toPredicate { it != null }

        targetPathSelector = TextFieldWithBrowseButton()
        targetPathHandle = FilePathDocumentChildPathHandle.install(
            document = targetPathSelector.textField.document,
            defaultParentPath = ClonePathProvider.defaultParentDirectoryPath(
                project, GitRememberedInputs.getInstance()
            )
        )

        projectsPanel = panel {
            row("Account:") {
                comboBox(
                    KComboboxProxyModel(GitLabAccount::class, accountsModel, selectedAccountModel),
                    listCellRenderer { value, _, _ -> text = value.toString() })
                    .horizontalAlign(HorizontalAlign.FILL)

//                cell(JSeparator(JSeparator.VERTICAL))
//                    .verticalAlign(VerticalAlign.FILL)

                // TODO: avatar
            }

            row("Search:") {
                cell(searchTextField)
                    .enabledIf(accountSelected)
                    .resizableColumn()
                    .verticalAlign(VerticalAlign.FILL)
                    .horizontalAlign(HorizontalAlign.FILL)
                    .apply {
                        attachSearch(projectsListView, component) { it.id }
                    }
            }

            row {
                cell(ScrollPaneFactory.createScrollPane(projectsListView))
                    .enabledIf(accountSelected)
                    .resizableColumn()
                    .horizontalAlign(HorizontalAlign.FILL)
                    .verticalAlign(VerticalAlign.FILL)
            }.resizableRow()

            row("Directory:") {
                cell(targetPathSelector)
                    .enabledIf(accountSelected)
                    .resizableColumn()
                    .horizontalAlign(HorizontalAlign.FILL)
                    .validationOnApply {
                        CloneDvcsValidationUtils.checkDirectory(it.text, it.textField as JComponent)
                    }
                    .apply {
                        component.addBrowseFolderListener(
                            "Destination Directory",
                            "Select a parent directory for the clone",
                            project,
                            FileChooserDescriptorFactory.createSingleFolderDescriptor().apply {
                                isShowFileSystemRoots = true
                                isHideIgnored = false
                            })
                    }
            }
        }
        projectsPanel.border = JBEmptyBorder(UIUtil.getRegularPanelInsets())

        wrapper.setContent(projectsPanel)

        createSelectedAccountListeners()

        Disposer.dispose(accountsModel)
    }

    private fun createSelectedAccountListeners() {
        accountsModel.addListDataListener(this, object : ListDataListener {
            override fun intervalAdded(e: ListDataEvent) {
                dialogStateListener.onListItemChanged()
            }

            override fun intervalRemoved(e: ListDataEvent) {
                dialogStateListener.onListItemChanged()
            }

            override fun contentsChanged(e: ListDataEvent) {
                for (i in e.index0..e.index1) {
                    if (accountsModel.getElementAt(i) == selectedAccount) {
                        loadAllProjects()
                    }
                }
                dialogStateListener.onListItemChanged()
            }
        })

        selectedAccountModel.addListener({ loadAllProjects() }, this)
    }

    private fun loadAllProjects() {
        projectsListView.setPaintBusy(true)

        val query = searchTextField.text

        object : Task.Backgroundable(project, "Search GitLab projects") {
            @Volatile
            private var projects: List<GitLabRepositoryUrls> = listOf()

            override fun run(indicator: ProgressIndicator) {
                projects = service<GitLabApiService>()
                    .requireApiFor(selectedAccount!!)
                    .search(query = query, membership = true, processIndicator = indicator)
            }

            override fun onSuccess() {
                projectsModel.reset(projects)
                projectsListView.emptyText.text = GitlabBundle.message("clone.dialog.no.project.found")
            }

            override fun onThrowable(error: Throwable) {
                LOG.error("Failed to search personal projects of ${selectedAccount}: $error")
                projectsModel.clear()
                projectsListView.emptyText.text = when (error) {
                    is MissingAccessTokenException -> GitlabBundle.message("account.access.token.missing")
                    is UnauthorizedAccessException -> GitlabBundle.message("credentials.invalid.auth.data")
                    else -> GitlabBundle.message("clone.dialog.project.search.error")
                }
            }

            override fun onFinished() {
                projectsListView.setPaintBusy(false)
            }
        }.queue()
    }

    private fun updateSelectedProject() {
        val selectedValue = projectsListView.selectedValue
        selectedCloneUrl = selectedValue?.let {
            // TODO: eval clone strategy setting
            it.httpsUrl
                ?: GitLabProjectCoord(selectedAccount!!.server, GitLabProjectPath(it.id))
                    .guessCloneUrl(GitProtocol.HTTPS)
        }
    }

    private fun onSelectedCloneUrlChanged() {
        val selected = selectedCloneUrl != null
        dialogStateListener.onOkActionEnabled(selected)
        if (selected) {
            val path = ClonePathProvider.relativeDirectoryPathForVcsUrl(project, selectedCloneUrl!!)
                .removeSuffix(GitUtil.DOT_GIT)
            targetPathHandle.trySetChildPath(path)
        }
    }

    override fun doClone(checkoutListener: CheckoutProvider.Listener) {
        val targetPath = Paths.get(targetPathSelector.text)
        val parent = targetPath.toAbsolutePath().parent
        val destinationValidation = CloneDvcsValidationUtils.createDestination(parent.toString())
        if (destinationValidation != null) {
            LOG.error("Unable to create destination directory", destinationValidation.message)
            GitLabNotifications.showError(
                project,
                GitLabNotifications.CLONE_UNABLE_TO_CREATE_DESTINATION_DIR,
                GitlabBundle.message("clone.dialog.clone.failed"),
                GitlabBundle.message("clone.error.unable.to.create.dest.dir")
            )
            return
        }

        val fs = LocalFileSystem.getInstance()
        val destinationParent = fs.findFileByIoFile(parent.toFile())
            ?: fs.refreshAndFindFileByIoFile(parent.toFile())
        if (destinationParent == null) {
            LOG.error("Clone Failed. Destination doesn't exist")
            GitLabNotifications.showError(
                project,
                GitLabNotifications.CLONE_UNABLE_TO_FIND_DESTINATION,
                GitlabBundle.message("clone.dialog.clone.failed"),
                GitlabBundle.message("clone.error.unable.to.find.dest")
            )
            return
        }

        GitCheckoutProvider.clone(
            /* project = */ project,
            /* git = */ Git.getInstance(),
            /* listener = */ checkoutListener,
            /* destinationParent = */ destinationParent,
            /* sourceRepositoryURL = */ selectedCloneUrl!!,
            /* directoryName = */ targetPath.fileName.toString(),
            /* parentDirectory = */ parent.toAbsolutePath().toString()
        )
    }

    override fun doValidateAll(): List<ValidationInfo> {
        return projectsPanel.validateAll()
    }

    override fun getView(): JComponent {
        return wrapper
    }

    override fun onComponentSelected() {
        if (accountsModel.size == 1) {
            selectedAccount = accountsModel.getElementAt(0)
        }

        dialogStateListener.onOkActionNameChanged("Clone")

        updateSelectedProject()

        getPreferredFocusedComponent().let { IdeFocusManager.getInstance(project).requestFocus(it, true) }
    }

    override fun getPreferredFocusedComponent(): JComponent {
        return searchTextField
    }

    companion object {
        val LOG = thisLogger()
    }
}

internal class SimpleListModel<T> : AbstractListModel<T>() {

    private val data = mutableListOf<T>()

    override fun getSize(): Int = data.size

    override fun getElementAt(index: Int): T = data[index]

    fun add(element: T) {
        data.add(element)

        val index = data.size - 1
        fireIntervalAdded(this, index, index)
    }

    fun addAll(elements: Collection<T>) {
        if (!elements.isEmpty()) {
            val firstIndex = data.size

            data.addAll(elements)

            fireIntervalAdded(this, firstIndex, data.size - 1)
        }
    }

    fun reset(elements: Collection<T>) {
        clear()
        addAll(elements)
    }

    fun clear() {
        if (data.isNotEmpty()) {
            val oldSize = data.size

            data.clear()

            fireIntervalRemoved(this, 0, oldSize - 1)
        }
    }

    fun refresh() {
        if (data.isNotEmpty()) {
            fireContentsChanged(this, 0, data.size - 1)
        }
    }
}

fun <T> attachSearch(list: JList<T>, searchTextField: SearchTextField, searchBy: (T) -> String) {
    val speedSearch = SpeedSearch(false)
    val filteringListModel =
        NameFilteringListModel<T>(list.model, searchBy, speedSearch::shouldBeShowing, speedSearch.filter::orEmpty)
    list.model = filteringListModel

    searchTextField.addDocumentListener(object : DocumentAdapter() {
        override fun textChanged(e: DocumentEvent) = speedSearch.updatePattern(searchTextField.text)
    })

    speedSearch.addChangeListener {
        val prevSelection = list.selectedValue // save to restore the selection on filter drop
        filteringListModel.refilter()
        if (filteringListModel.size > 0) {
            val fullMatchIndex = if (speedSearch.isHoldingFilter) filteringListModel.closestMatchIndex
            else filteringListModel.getElementIndex(prevSelection)
            if (fullMatchIndex != -1) {
                list.selectedIndex = fullMatchIndex
            }

            if (filteringListModel.size <= list.selectedIndex || !filteringListModel.contains(list.selectedValue)) {
                list.selectedIndex = 0
            }
        }
    }

    ScrollingUtil.installActions(list)
    ScrollingUtil.installActions(list, searchTextField.textEditor)
}


internal class KComboboxProxyModel<T : Any?>(
    private val cls: KClass<*>,
    private val model: ListModel<T>,
    private var selected: Model<T>
) : ComboBoxModel<T>, AbstractListModel<T>() {

    override fun getSize(): Int {
        return model.size
    }

    override fun getElementAt(index: Int): T {
        return model.getElementAt(index)
    }

    override fun addListDataListener(l: ListDataListener?) {
        l!!
        model.addListDataListener(l)
        super.addListDataListener(l)
        l.contentsChanged(ListDataEvent(this, CONTENTS_CHANGED, -1, -1))
    }

    override fun removeListDataListener(l: ListDataListener?) {
        model.removeListDataListener(l)
        super.removeListDataListener(l)
    }

    override fun setSelectedItem(anItem: Any?) {
        val selected = this.selected.get()
        if (selected != anItem) {
            if (anItem != null && !cls.isInstance(anItem)) {
                throw IllegalArgumentException(
                    "Selected item is not instance of model class: ${anItem::class.qualifiedName}"
                )
            }

            @Suppress("UNCHECKED_CAST")
            this.selected.set(anItem as T)

            fireContentsChanged(this, -1, -1)
        }
    }

    override fun getSelectedItem(): Any? = selected.get()
}


internal class ComboboxProxyModel<T>(private val model: ListModel<T>, private var selected: Any? = null) :
    ComboBoxModel<T>, AbstractListModel<T>() {

    override fun getSize(): Int = model.size

    override fun getElementAt(index: Int): T = model.getElementAt(index)

    override fun addListDataListener(l: ListDataListener?) {
        model.addListDataListener(l)
        super.addListDataListener(l)
    }

    override fun removeListDataListener(l: ListDataListener?) {
        model.removeListDataListener(l)
        super.removeListDataListener(l)
    }

    override fun setSelectedItem(anItem: Any?) {
        if (selected != anItem) {
            selected = anItem
            fireContentsChanged(this, -1, -1)
        }
    }

    override fun getSelectedItem(): Any? = selected
}


internal class SortedAccountsListModel(accounts: Collection<GitLabAccount>) : AbstractListModel<GitLabAccount>(),
    Disposable {

    private val data = mutableListOf<GitLabAccount>()

    init {
        update(accounts)
        service<GitLabAccountsManager>().addListener(this, object : AccountsListener<GitLabAccount> {
            override fun onAccountListChanged(old: Collection<GitLabAccount>, new: Collection<GitLabAccount>) {
                update(new)
            }

            override fun onAccountCredentialsChanged(account: GitLabAccount) {
                update(account)
            }
        })
    }

    override fun getSize(): Int = data.size

    override fun getElementAt(index: Int): GitLabAccount = data[index]

    private fun update(new: Collection<GitLabAccount>) {
        if (data.isNotEmpty()) {
            data.clear()
            fireIntervalRemoved(this, 0, data.size - 1)
        }

        data.addAll(new)
        if (data.isNotEmpty()) {
            data.sortWith(accountComparator)
            fireIntervalAdded(this, 0, data.size - 1)
        }
    }

    private fun update(account: GitLabAccount) {
        val indexOf = data.indexOf(account)
        if (indexOf >= 0) {
            fireContentsChanged(this, indexOf, indexOf)
        }
    }

    companion object {
        @JvmStatic
        private val accountComparator: Comparator<GitLabAccount> = compareBy({ it.server.host }, { it.name })
    }

    override fun dispose() {}
}

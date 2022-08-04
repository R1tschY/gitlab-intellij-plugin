@file:Suppress("UnstableApiUsage")

package de.richardliebscher.intellij.gitlab.repository

import com.intellij.collaboration.auth.AccountsListener
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.openapi.vcs.CheckoutProvider
import com.intellij.openapi.vcs.ui.cloneDialog.VcsCloneDialogExtension
import com.intellij.openapi.vcs.ui.cloneDialog.VcsCloneDialogExtensionComponent
import com.intellij.openapi.vcs.ui.cloneDialog.VcsCloneDialogExtensionStatusLine
import com.intellij.openapi.vcs.ui.cloneDialog.VcsCloneDialogExtensionStatusLine.Companion.greyText
import com.intellij.openapi.wm.IdeFocusManager
import com.intellij.ui.ScrollPaneFactory
import com.intellij.ui.SearchTextField
import com.intellij.ui.SingleSelectionModel
import com.intellij.ui.components.JBList
import com.intellij.ui.components.panels.Wrapper
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.gridLayout.HorizontalAlign
import com.intellij.ui.dsl.gridLayout.VerticalAlign
import com.intellij.ui.layout.listCellRenderer
import de.richardliebscher.intellij.gitlab.GitLabIcons
import de.richardliebscher.intellij.gitlab.accounts.GitLabAccount
import de.richardliebscher.intellij.gitlab.accounts.GitLabAccountsManager
import de.richardliebscher.intellij.gitlab.accounts.GitLabAuthService
import de.richardliebscher.intellij.gitlab.api.GitLabApiService
import de.richardliebscher.intellij.gitlab.api.GitlabRepositoryUrls
import de.richardliebscher.intellij.gitlab.model.SERVICE_DISPLAY_NAME
import de.richardliebscher.intellij.gitlab.utils.Model
import de.richardliebscher.intellij.gitlab.utils.toPredicate
import javax.swing.*
import javax.swing.event.ListDataEvent
import javax.swing.event.ListDataEvent.CONTENTS_CHANGED
import javax.swing.event.ListDataListener
import kotlin.reflect.KClass

class GitlabCloneDialogExtension : VcsCloneDialogExtension {
    @Suppress("OverridingDeprecatedMember")
    override fun createMainComponent(project: Project): VcsCloneDialogExtensionComponent {
        throw UnsupportedOperationException("Shouldn't be called")
    }

    override fun createMainComponent(project: Project, modalityState: ModalityState): VcsCloneDialogExtensionComponent {
        return GitlabCloneDialogExtensionComponent(project, modalityState)
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
    private val project: Project,
    private val modalityState: ModalityState
) : VcsCloneDialogExtensionComponent() {

    private val projectsModel: SimpleListModel<GitlabRepositoryUrls> = SimpleListModel()
    private val accountsModel: SortedAccountsListModel =
        SortedAccountsListModel(service<GitLabAuthService>().getAccounts())

    private val searchTextField = SearchTextField(false)

    internal var search: String = ""

    private val selectedAccountProperty = Model<GitLabAccount?>(null)
    internal var selectedAccount by selectedAccountProperty

    private val wrapper = Wrapper()
    private val projectsPanel: DialogPanel
    private val projectsList: JBList<GitlabRepositoryUrls> = JBList(projectsModel).apply {
        cellRenderer = listCellRenderer { value, _, _ -> text = value.name }
        selectionModel = SingleSelectionModel()
    }

    init {
        selectedAccountProperty.addListener {

        }

        val accountSelected = selectedAccountProperty.toPredicate { it != null }

        projectsPanel = panel {
            row("GitLab Server") {
                comboBox(
                    KComboboxProxyModel(GitLabAccount::class, accountsModel, selectedAccountProperty),
                    listCellRenderer { value, _, _ -> text = value.toString() })
                    .horizontalAlign(HorizontalAlign.FILL)

                cell(JSeparator(JSeparator.VERTICAL))
                    .verticalAlign(VerticalAlign.FILL)

                // TODO: avatar
            }

            row("Search:") {
                cell(searchTextField)
                    .enabledIf(accountSelected)
                    .horizontalAlign(HorizontalAlign.FILL)

                button("Go") {
                    doSearch()
                }
            }

            row {
                cell(ScrollPaneFactory.createScrollPane(projectsList))
                    .enabledIf(accountSelected)
                    .horizontalAlign(HorizontalAlign.FILL)
                    .verticalAlign(VerticalAlign.FILL)
                    .resizableColumn()
            }

            row("Directory:") {
                textFieldWithBrowseButton()
                    .enabledIf(accountSelected)
                    .horizontalAlign(HorizontalAlign.FILL)
                    .component
                    .addBrowseFolderListener(
                        "Destination Directory",
                        "Select a parent directory for the clone",
                        project,
                        FileChooserDescriptorFactory.createSingleFolderDescriptor()
                            .apply {
                                isShowFileSystemRoots = true
                                isHideIgnored = false
                            })
            }
        }

        wrapper.setContent(projectsPanel)
    }

    private fun doSearch() {
        projectsList.setPaintBusy(true)

        val query = searchTextField.text

        object : Task.Backgroundable(project, "Search GitLab projects") {
            @Volatile
            private var projects: List<GitlabRepositoryUrls> = listOf()

            override fun run(indicator: ProgressIndicator) {
                try {
                    projects = service<GitLabApiService>()
                        .apiFor(selectedAccount!!)!!
                        .search(query = query, membership = true, processIndicator = indicator)
                } catch (e: Exception) {
                    projects = listOf()
                    LOG.error("Failed to search for projects in ${selectedAccount!!.server}: $e")
                }
            }

            override fun onSuccess() {
                projectsModel.reset(projects)
            }

            override fun onFinished() {
                projectsList.setPaintBusy(false)
            }
        }.queue()
    }

    override fun doClone(checkoutListener: CheckoutProvider.Listener) {
        TODO("Not yet implemented")
    }

    override fun doValidateAll(): List<ValidationInfo> {
        TODO("Not yet implemented")
    }

    override fun getView(): JComponent {
        return wrapper
    }

    override fun onComponentSelected() {
        // TODO("Not yet implemented")

        getPreferredFocusedComponent()?.let { IdeFocusManager.getInstance(project).requestFocus(it, true) }
    }

    override fun getPreferredFocusedComponent(): JComponent? {
        // TODO
        return null
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

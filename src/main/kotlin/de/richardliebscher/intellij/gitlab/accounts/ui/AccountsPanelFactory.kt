// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package de.richardliebscher.intellij.gitlab.accounts.ui

import com.intellij.icons.AllIcons
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonShortcuts
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.keymap.KeymapUtil
import com.intellij.ui.ClientProperty
import com.intellij.ui.LayeredIcon
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.ToolbarDecorator
import com.intellij.ui.awt.RelativePoint
import com.intellij.ui.components.JBList
import com.intellij.ui.dsl.builder.Cell
import com.intellij.ui.dsl.builder.Row
import com.intellij.util.ui.EmptyIcon
import com.intellij.util.ui.StatusText
import com.intellij.util.ui.UIUtil
import de.richardliebscher.intellij.gitlab.GitlabBundle
import de.richardliebscher.intellij.gitlab.accounts.Account
import de.richardliebscher.intellij.gitlab.accounts.AccountManager
import de.richardliebscher.intellij.gitlab.accounts.AccountsListener
import de.richardliebscher.intellij.gitlab.accounts.PersistentDefaultAccountHolder
import de.richardliebscher.intellij.gitlab.utils.CompletableFutureUtil.handleOnEdt
import de.richardliebscher.intellij.gitlab.utils.indexOf
import de.richardliebscher.intellij.gitlab.utils.iterable
import kotlinx.coroutines.future.asCompletableFuture
import java.awt.event.MouseEvent
import java.util.concurrent.CompletableFuture
import javax.swing.*
import javax.swing.event.ListDataEvent
import javax.swing.event.ListDataListener
import kotlin.properties.Delegates


class AccountsPanelFactory<A : Account, Cred>
private constructor(disposable: Disposable,
                    private val accountManager: AccountManager<A, Cred>,
                    private val defaultAccountHolder: PersistentDefaultAccountHolder<A>?,
                    private val accountsModel: AccountsListModel<A, Cred>,
                    private val detailsLoader: AccountsDetailsLoader<A, *>
) {

    constructor(accountManager: AccountManager<A, Cred>,
                defaultAccountHolder: PersistentDefaultAccountHolder<A>,
                accountsModel: AccountsListModel.WithDefault<A, Cred>,
                detailsLoader: AccountsDetailsLoader<A, *>,
                disposable: Disposable) : this(disposable, accountManager, defaultAccountHolder, accountsModel, detailsLoader)

    constructor(accountManager: AccountManager<A, Cred>,
                accountsModel: AccountsListModel<A, Cred>,
                detailsLoader: AccountsDetailsLoader<A, *>,
                disposable: Disposable) : this(disposable, accountManager, null, accountsModel, detailsLoader)

    init {
        accountManager.addListener(disposable, object : AccountsListener<A> {
            override fun onAccountCredentialsChanged(account: A) {
                if (!isModified()) reset()
            }
        })
    }

    fun accountsPanelCell(row: Row, needAddBtnWithDropdown: Boolean, defaultAvatarIcon: Icon = EmptyIcon.ICON_16): Cell<JComponent> {
        val detailsMap = mutableMapOf<A, CompletableFuture<AccountsDetailsLoader.Result<*>>>()
        val detailsProvider = LoadedAccountsDetailsProvider { account: A ->
            detailsMap[account]?.getNow(null)
        }
        val avatarIconsProvider = LoadingAvatarIconsProvider(detailsLoader, defaultAvatarIcon) { account: A ->
            val result = detailsMap[account]?.getNow(null) as? AccountsDetailsLoader.Result.Success
            result?.details?.avatarUrl
        }

        val accountsList = createList {
            SimpleAccountsListCellRenderer(accountsModel, detailsProvider, avatarIconsProvider)
        }
        loadAccountsDetails(accountsList, detailsLoader, detailsMap)

        val component = wrapWithToolbar(accountsList, needAddBtnWithDropdown)

        return row.cell(component)
            .onIsModified(::isModified)
            .onReset(::reset)
            .onApply(::apply)
    }

    private fun isModified(): Boolean {
        val defaultModified = if (defaultAccountHolder != null && accountsModel is AccountsListModel.WithDefault) {
            accountsModel.defaultAccount != defaultAccountHolder.account
        }
        else false

        return accountsModel.newCredentials.isNotEmpty()
                || accountsModel.accounts != accountManager.accounts
                || defaultModified
    }

    private fun reset() {
        accountsModel.accounts = accountManager.accounts
        if (defaultAccountHolder != null && accountsModel is AccountsListModel.WithDefault) {
            accountsModel.defaultAccount = defaultAccountHolder.account
        }
        accountsModel.clearNewCredentials()
    }

    private fun apply() {
        val newTokensMap = mutableMapOf<A, Cred?>()
        newTokensMap.putAll(accountsModel.newCredentials)
        for (account in accountsModel.accounts) {
            newTokensMap.putIfAbsent(account, null)
        }
        accountManager.updateAccounts(newTokensMap)
        accountsModel.clearNewCredentials()

        if (defaultAccountHolder != null && accountsModel is AccountsListModel.WithDefault) {
            val defaultAccount = accountsModel.defaultAccount
            defaultAccountHolder.account = defaultAccount
        }
    }

    private fun <R> createList(listCellRendererFactory: () -> R): JBList<A> where R : ListCellRenderer<A>, R : JComponent {

        val accountsList = JBList(accountsModel.accountsListModel).apply {
            val renderer = listCellRendererFactory()
            cellRenderer = renderer
            JListHoveredRowMaterialiser.install(this, listCellRendererFactory())
            ClientProperty.put(this, UIUtil.NOT_IN_HIERARCHY_COMPONENTS, listOf(renderer))

            selectionMode = ListSelectionModel.SINGLE_SELECTION
        }

        accountsList.addListSelectionListener { accountsModel.selectedAccount = accountsList.selectedValue }

        accountsList.emptyText.apply {
            appendText(GitlabBundle.message("accounts.none.added"))
            appendSecondaryText(GitlabBundle.message("accounts.add.link"), SimpleTextAttributes.LINK_PLAIN_ATTRIBUTES) {
                val event = it.source
                val relativePoint = if (event is MouseEvent) RelativePoint(event) else null
                accountsModel.addAccount(accountsList, relativePoint)
            }
            appendSecondaryText(" (${KeymapUtil.getFirstKeyboardShortcutText(CommonShortcuts.getNew())})", StatusText.DEFAULT_ATTRIBUTES, null)
        }
        return accountsList
    }

    private fun wrapWithToolbar(accountsList: JBList<A>, needAddBtnWithDropdown: Boolean): JPanel {
        val addIcon: Icon = if (needAddBtnWithDropdown) LayeredIcon.ADD_WITH_DROPDOWN else AllIcons.General.Add

        val toolbar = ToolbarDecorator.createDecorator(accountsList)
            .disableUpDownActions()
            .setAddAction { accountsModel.addAccount(accountsList, it.preferredPopupPoint) }
            .setAddIcon(addIcon)

        if (accountsModel is AccountsListModel.WithDefault) {
            toolbar.addExtraAction(object : ToolbarDecorator.ElementActionButton(
                GitlabBundle.message("accounts.set.default"),
                AllIcons.Actions.Checked) {
                override fun actionPerformed(e: AnActionEvent) {
                    val selected = accountsList.selectedValue
                    if (selected == accountsModel.defaultAccount) return
                    if (selected != null) accountsModel.defaultAccount = selected
                }

                override fun updateButton(e: AnActionEvent) {
                    isEnabled = isEnabled && accountsModel.defaultAccount != accountsList.selectedValue
                }
            })
        }

        return toolbar.createPanel()
    }

    private fun <A : Account> loadAccountsDetails(accountsList: JBList<A>,
                                                  detailsLoader: AccountsDetailsLoader<A, *>,
                                                  resultsMap: MutableMap<A, CompletableFuture<AccountsDetailsLoader.Result<*>>>) {

        val listModel = accountsList.model
        listModel.addListDataListener(object : ListDataListener {
            private var loadingCount by Delegates.observable(0) { _, _, newValue ->
                accountsList.setPaintBusy(newValue != 0)
            }

            override fun intervalAdded(e: ListDataEvent) = loadDetails(e.index0, e.index1)
            override fun contentsChanged(e: ListDataEvent) = loadDetails(e.index0, e.index1)

            override fun intervalRemoved(e: ListDataEvent) {
                val accounts = listModel.iterable().toSet()
                for (account in resultsMap.keys - accounts) {
                    resultsMap.remove(account)?.cancel(true)
                }
            }

            private fun loadDetails(startIdx: Int, endIdx: Int) {
                if (startIdx < 0 || endIdx < 0) return

                for (i in startIdx..endIdx) {
                    val account = listModel.getElementAt(i)
                    resultsMap[account]?.cancel(true)
                    loadingCount++
                    val detailsLoadingResult = detailsLoader.loadDetailsAsync(account).asCompletableFuture()
                    detailsLoadingResult.handleOnEdt(ModalityState.any()) { _, _ ->
                        loadingCount--
                        repaint(account)
                    }
                    resultsMap[account] = detailsLoadingResult
                }
            }

            private fun repaint(account: A): Boolean {
                val idx = listModel.indexOf(account).takeIf { it >= 0 } ?: return true
                val cellBounds = accountsList.getCellBounds(idx, idx)
                accountsList.repaint(cellBounds)
                return false
            }
        })
    }
}

//
//internal fun Row.gitlabAccountsPanel(
//    disposable: Disposable,
//    indicatorProvider: ProgressIndicatorsProvider
//): Cell<JComponent> {
//    val accountsModel = GitLabAccountsModel()
//    return gitlabAccountsPanel(
//        service(),
//        accountsModel,
//        GitLabAccountsDetailsProvider(indicatorProvider, accountsModel),
//        disposable
//    )
//}
//
//
//internal fun Row.gitlabAccountsPanel(
//    accountManager: GitLabAccountsManager,
//    accountsModel: GitLabAccountsModel,
//    detailsProvider: GitLabAccountsDetailsProvider,
//    disposable: Disposable,
//): Cell<JComponent> {
//    accountsModel.addCredentialsChangeListener(detailsProvider::reset)
//    detailsProvider.loadingStateModel.addListener {
//        accountsModel.busyStateModel.value = it
//    }
//
//    fun isModified() = accountsModel.newCredentials.isNotEmpty() || accountsModel.accounts != accountManager.accounts
//
//    fun reset() {
//        accountsModel.accounts = accountManager.accounts
//        accountsModel.clearNewCredentials()
//        detailsProvider.resetAll()
//    }
//
//    fun apply() {
//        val newTokensMap = mutableMapOf<GitLabAccount, GitlabAccessToken?>()
//        newTokensMap.putAll(accountsModel.newCredentials)
//        for (account in accountsModel.accounts) {
//            newTokensMap.putIfAbsent(account, null)
//        }
//        accountManager.updateAccounts(newTokensMap)
//        accountsModel.clearNewCredentials()
//    }
//
//    accountManager.addListener(disposable, object : AccountsListener<GitLabAccount> {
//        override fun onAccountCredentialsChanged(account: GitLabAccount) {
//            if (!isModified()) reset()
//        }
//    })
//
//    val component = AccountsPanelFactory.create(accountsModel) {
//        SimpleAccountsListCellRenderer(accountsModel, detailsProvider, EmptyIcon.ICON_16)
//    }
//
//    return cell(component).onIsModified(::isModified).onReset(::reset).onApply(::apply)
//}
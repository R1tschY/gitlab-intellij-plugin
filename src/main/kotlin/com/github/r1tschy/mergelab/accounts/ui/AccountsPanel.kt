// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.github.r1tschy.mergelab.accounts.ui

import com.github.r1tschy.mergelab.accounts.GitLabAccount
import com.github.r1tschy.mergelab.accounts.GitLabAccountsManager
import com.github.r1tschy.mergelab.accounts.GitlabAccessToken
import com.intellij.collaboration.auth.Account
import com.intellij.collaboration.auth.AccountsListener
import com.intellij.collaboration.auth.ui.AccountsListModel
import com.intellij.collaboration.auth.ui.SimpleAccountsListCellRenderer
import com.intellij.collaboration.messages.CollaborationToolsBundle
import com.intellij.collaboration.ui.util.JListHoveredRowMaterialiser
import com.intellij.collaboration.util.ProgressIndicatorsProvider
import com.intellij.icons.AllIcons
import com.intellij.ide.DataManager
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.CommonShortcuts
import com.intellij.openapi.components.service
import com.intellij.openapi.keymap.KeymapUtil
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.ToolbarDecorator
import com.intellij.ui.awt.RelativePoint
import com.intellij.ui.components.JBList
import com.intellij.ui.dsl.builder.Cell
import com.intellij.ui.dsl.builder.Row
import com.intellij.util.ui.EmptyIcon
import com.intellij.util.ui.StatusText
import com.intellij.util.ui.UIUtil
import java.awt.event.MouseEvent
import javax.swing.JComponent
import javax.swing.ListCellRenderer
import javax.swing.ListSelectionModel

internal object AccountsPanelFactory {
    fun <A : Account, Cred, R> create(model: AccountsListModel<A, Cred>,
                                      listCellRendererFactory: () -> R): JComponent
            where R : ListCellRenderer<A>, R : JComponent {

        val accountsListModel = model.accountsListModel
        val accountsList = JBList(accountsListModel).apply {
            val decoratorRenderer = listCellRendererFactory()
            cellRenderer = decoratorRenderer
            JListHoveredRowMaterialiser.install(this, listCellRendererFactory())
            putClientProperty(UIUtil.NOT_IN_HIERARCHY_COMPONENTS, listOf(decoratorRenderer))

            selectionMode = ListSelectionModel.SINGLE_SELECTION
        }
        model.busyStateModel.addListener {
            accountsList.setPaintBusy(it)
        }

        accountsList.emptyText.apply {
            appendText(CollaborationToolsBundle.message("accounts.none.added"))
            appendSecondaryText(CollaborationToolsBundle.message("accounts.add.link"), SimpleTextAttributes.LINK_PLAIN_ATTRIBUTES) {
                val event = it.source
                val relativePoint = if (event is MouseEvent) RelativePoint(event) else null
                model.addAccount(accountsList, relativePoint)
            }
            appendSecondaryText(" (${KeymapUtil.getFirstKeyboardShortcutText(CommonShortcuts.getNew())})", StatusText.DEFAULT_ATTRIBUTES, null)
        }

        model.busyStateModel.addListener {
            accountsList.setPaintBusy(it)
        }

        val toolbar = ToolbarDecorator.createDecorator(accountsList)
            .disableUpDownActions()
            .setAddAction { model.addAccount(accountsList, it.preferredPopupPoint) }
            .setAddIcon(AllIcons.General.Add)

        return toolbar.createPanel()
    }
}

internal fun Row.gitlabAccountsPanel(
    disposable: Disposable,
    indicatorProvider: ProgressIndicatorsProvider
): Cell<JComponent> =
    gitlabAccountsPanel(service(), GitLabAccountsModel(), GitLabAccountsDetailsProvider(indicatorProvider), disposable)


internal fun Row.gitlabAccountsPanel(
    accountManager: GitLabAccountsManager,
    accountsModel: GitLabAccountsModel,
    detailsProvider: GitLabAccountsDetailsProvider,
    disposable: Disposable,
): Cell<JComponent> {
    accountsModel.addCredentialsChangeListener(detailsProvider::reset)
    detailsProvider.loadingStateModel.addListener {
        accountsModel.busyStateModel.value = it
    }

    fun isModified() = accountsModel.newCredentials.isNotEmpty() || accountsModel.accounts != accountManager.accounts

    fun reset() {
        accountsModel.accounts = accountManager.accounts
        accountsModel.clearNewCredentials()
        detailsProvider.resetAll()
    }

    fun apply() {
        val newTokensMap = mutableMapOf<GitLabAccount, GitlabAccessToken?>()
        newTokensMap.putAll(accountsModel.newCredentials)
        for (account in accountsModel.accounts) {
            newTokensMap.putIfAbsent(account, null)
        }
        accountManager.updateAccounts(newTokensMap)
        accountsModel.clearNewCredentials()
    }

    accountManager.addListener(disposable, object : AccountsListener<GitLabAccount> {
        override fun onAccountCredentialsChanged(account: GitLabAccount) {
            if (!isModified()) reset()
        }
    })

    val component = AccountsPanelFactory.create(accountsModel) {
        SimpleAccountsListCellRenderer(accountsModel, detailsProvider, EmptyIcon.ICON_16)
    }

    DataManager.registerDataProvider(component) { key ->
        if (GitLabAccountsEditor.DATA_KEY.`is`(key)) {
            accountsModel
        } else {
            null
        }
    }

    return cell(component).onIsModified(::isModified).onReset(::reset).onApply(::apply)
}
// Copyright 2000-2020 JetBrains s.r.o.
// Copyright 2022 Richard Liebscher.
// Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.github.r1tschy.mergelab.settings

import com.github.r1tschy.mergelab.accounts.GitLabAccountsManager
import com.github.r1tschy.mergelab.accounts.ui.GitLabAccountsDetailsProvider
import com.github.r1tschy.mergelab.accounts.ui.GitLabAccountsEditor
import com.github.r1tschy.mergelab.accounts.ui.GitLabAccountsModel
import com.github.r1tschy.mergelab.model.SERVICE_DISPLAY_NAME
import com.intellij.collaboration.auth.Account
import com.intellij.collaboration.auth.AccountManager
import com.intellij.collaboration.auth.AccountsListener
import com.intellij.collaboration.auth.ui.AccountsDetailsProvider
import com.intellij.collaboration.auth.ui.AccountsListModel
import com.intellij.collaboration.auth.ui.AccountsPanelFactory
import com.intellij.collaboration.auth.ui.SimpleAccountsListCellRenderer
import com.intellij.collaboration.util.ProgressIndicatorsProvider
import com.intellij.ide.DataManager
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.service
import com.intellij.openapi.options.BoundConfigurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.util.Disposer
import com.intellij.ui.dsl.builder.Cell
import com.intellij.ui.dsl.builder.Row
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.gridLayout.HorizontalAlign
import com.intellij.ui.dsl.gridLayout.VerticalAlign
import com.intellij.util.ui.EmptyIcon
import javax.swing.Icon
import javax.swing.JComponent

internal class GitLabSettingsConfigurable internal constructor(private val project: Project) :
    BoundConfigurable(SERVICE_DISPLAY_NAME) {

    override fun createPanel(): DialogPanel {
        val accountsManager = service<GitLabAccountsManager>()
        val indicatorsProvider = ProgressIndicatorsProvider().also {
            Disposer.register(disposable!!, it)
        }
        val model = GitLabAccountsModel()
        val detailsProvider = GitLabAccountsDetailsProvider(indicatorsProvider, accountsManager, model)

        return panel {
            row {
                accountsPanel(
                    accountsManager,
                    model,
                    detailsProvider,
                    disposable!!
                ).horizontalAlign(HorizontalAlign.FILL).verticalAlign(VerticalAlign.FILL).also {
                        DataManager.registerDataProvider(it.component) { key ->
                            if (GitLabAccountsEditor.DATA_KEY.`is`(key)) {
                                model
                            } else {
                                null
                            }
                        }
                    }
            }.resizableRow()
        }
    }
}

fun <A : Account, Cred> Row.accountsPanel(
    accountManager: AccountManager<A, Cred>,
    accountsModel: AccountsListModel<A, Cred>,
    detailsProvider: AccountsDetailsProvider<A, *>,
    disposable: Disposable,
    defaultAvatarIcon: Icon = EmptyIcon.ICON_16
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
        val newTokensMap = mutableMapOf<A, Cred?>()
        newTokensMap.putAll(accountsModel.newCredentials)
        for (account in accountsModel.accounts) {
            newTokensMap.putIfAbsent(account, null)
        }
        accountManager.updateAccounts(newTokensMap)
        accountsModel.clearNewCredentials()
    }

    accountManager.addListener(disposable, object : AccountsListener<A> {
        override fun onAccountCredentialsChanged(account: A) {
            if (!isModified()) reset()
        }
    })

    val component = AccountsPanelFactory.create(accountsModel) {
        SimpleAccountsListCellRenderer(accountsModel, detailsProvider, defaultAvatarIcon)
    }
    return cell(component).onIsModified(::isModified).onReset(::reset).onApply(::apply)
}
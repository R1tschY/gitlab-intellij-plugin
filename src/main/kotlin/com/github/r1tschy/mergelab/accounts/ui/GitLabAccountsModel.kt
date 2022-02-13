// Copyright 2022 Richard Liebscher.
// Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.github.r1tschy.mergelab.accounts.ui

import com.github.r1tschy.mergelab.accounts.GitLabAccount
import com.github.r1tschy.mergelab.accounts.GitlabAccessToken
import com.github.r1tschy.mergelab.model.GitLabServerUrl
import com.intellij.collaboration.auth.ui.AccountsListModelBase
import com.intellij.ide.DataManager
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service
import com.intellij.ui.awt.RelativePoint
import javax.swing.JComponent

class GitLabAccountsModel : AccountsListModelBase<GitLabAccount, GitlabAccessToken>(), GitLabAccountsEditor {

    override fun addAccount(parentComponent: JComponent, point: RelativePoint?) {
        // reconstruct event, because caller discarded it
        val dataContext = DataManager.getInstance().getDataContext(parentComponent)
        val actionEvent = AnActionEvent.createFromDataContext(ActionPlaces.UNKNOWN, null, dataContext)

        service<ActionManager>().getAction(AddGitLabAccount::class.qualifiedName!!).actionPerformed(actionEvent)
    }

    override fun editAccount(parentComponent: JComponent, account: GitLabAccount) {
        TODO("Not yet implemented")
    }

    override fun addAccount(server: GitLabServerUrl, token: GitlabAccessToken) {
        val account = GitLabAccount(name = "", server = server)
        accountsListModel.add(account)
        newCredentials[account] = token
        notifyCredentialsChanged(account)
    }

    override fun hasAccount(server: GitLabServerUrl): Boolean {
        return accountsListModel.items.any { it.server == server }
    }
}
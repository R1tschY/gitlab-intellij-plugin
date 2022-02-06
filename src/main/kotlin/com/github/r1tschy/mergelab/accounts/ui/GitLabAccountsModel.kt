// Copyright 2022 Richard Liebscher.
// Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.github.r1tschy.mergelab.accounts.ui

import com.github.r1tschy.mergelab.accounts.GitLabAccessToken
import com.github.r1tschy.mergelab.accounts.GitLabAccount
import com.github.r1tschy.mergelab.model.GitLabInstanceCoord
import com.intellij.collaboration.auth.ui.AccountsListModelBase
import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.ui.JBPopupMenu
import com.intellij.ui.awt.RelativePoint
import javax.swing.JComponent

class GitLabAccountsModel : AccountsListModelBase<GitLabAccount, GitLabAccessToken>(), GitLabAccountsEditor {

    private val actionManager = ActionManager.getInstance()

    override fun addAccount(parentComponent: JComponent, point: RelativePoint?) {
        val group = actionManager.getAction("com.github.r1tschy.mergelab.accounts.AddAccount") as ActionGroup
        val popup = actionManager.createActionPopupMenu(ActionPlaces.TOOLBAR, group)

        val actionPoint = point ?: RelativePoint.getCenterOf(parentComponent)
        popup.setTargetComponent(parentComponent)
        JBPopupMenu.showAt(actionPoint, popup.component)
    }

    override fun editAccount(parentComponent: JComponent, account: GitLabAccount) {
        TODO("Not yet implemented")
    }

    override fun addAccount(server: GitLabInstanceCoord, token: GitLabAccessToken) {
        val account = GitLabAccount(name = "", server = server)
        accountsListModel.add(account)
        newCredentials[account] = token
        notifyCredentialsChanged(account)
    }

    override fun hasAccount(server: GitLabInstanceCoord): Boolean {
        return accountsListModel.items.any { it.server == server }
    }
}
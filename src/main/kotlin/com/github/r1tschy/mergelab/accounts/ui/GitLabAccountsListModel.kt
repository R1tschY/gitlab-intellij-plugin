// Copyright 2022 Richard Liebscher.
// Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.github.r1tschy.mergelab.accounts.ui

import com.github.r1tschy.mergelab.accounts.GitLabAccount
import com.github.r1tschy.mergelab.accounts.GitlabAccessToken
import com.intellij.ide.DataManager
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.project.Project
import com.intellij.ui.awt.RelativePoint
import javax.swing.JComponent

class GitLabAccountsListModel(private val project: Project) :
    AccountsListModelBase<GitLabAccount, GitlabAccessToken>() {

    override fun addAccount(parentComponent: JComponent, point: RelativePoint?) {
        val dataContext = DataManager.getInstance().getDataContext(parentComponent)
        val project = dataContext.getData(CommonDataKeys.PROJECT)

        showAddAccountDialog(project, parentComponent)?.let { loginData ->
            val account = GitLabAccount(name = loginData.userDetails.username, server = loginData.server)
            accountsListModel.add(account)
            newCredentials[account] = loginData.token
            notifyCredentialsChanged(account)
        }
    }

    override fun editAccount(parentComponent: JComponent, account: GitLabAccount) {
        val dataContext = DataManager.getInstance().getDataContext(parentComponent)
        val project = dataContext.getData(CommonDataKeys.PROJECT)

        showEditTokenDialog(account, project, parentComponent)?.let { loginData ->
            account.name = loginData.userDetails.username
            newCredentials[account] = loginData.token
            notifyCredentialsChanged(account)
        }
    }

    private fun notifyCredentialsChanged(account: GitLabAccount) {
        accountsListModel.contentsChanged(account)
    }
}
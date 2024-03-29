// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package de.richardliebscher.intellij.gitlab.accounts.ui

import com.intellij.ui.awt.RelativePoint
import de.richardliebscher.intellij.gitlab.accounts.Account
import javax.swing.JComponent
import javax.swing.ListModel

interface AccountsListModel<A: Account, Cred> {
    var accounts: Set<A>
    var selectedAccount: A?
    val newCredentials: Map<A, Cred>

    val accountsListModel: ListModel<A>

    fun addAccount(parentComponent: JComponent, point: RelativePoint? = null)
    fun editAccount(parentComponent: JComponent, account: A)
    fun clearNewCredentials()

    interface WithDefault<A: Account, Cred>: AccountsListModel<A, Cred> {
        var defaultAccount: A?
    }
}

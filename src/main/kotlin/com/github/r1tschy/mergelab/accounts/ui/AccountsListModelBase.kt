// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.github.r1tschy.mergelab.accounts.ui

import com.intellij.collaboration.auth.Account
import com.intellij.ui.CollectionListModel

abstract class AccountsListModelBase<A : Account, Cred> : AccountsListModel.WithDefault<A, Cred> {
    override var accounts: Set<A>
        get() = accountsListModel.items.toSet()
        set(value) {
            accountsListModel.removeAll()
            accountsListModel.add(value.toList())
        }
    override var selectedAccount: A? = null
    override var defaultAccount: A? = null
    override val newCredentials: MutableMap<A, Cred> = mutableMapOf()

    override val accountsListModel = CollectionListModel<A>()

    override fun clearNewCredentials() = newCredentials.clear()

    protected fun add(account: A, cred: Cred) {
        accountsListModel.add(account)
        newCredentials[account] = cred
        notifyCredentialsChanged(account)
    }

    protected fun update(account: A, cred: Cred) {
        newCredentials[account] = cred
        notifyCredentialsChanged(account)
    }

    private fun notifyCredentialsChanged(account: A) {
        accountsListModel.contentsChanged(account)
    }
}

// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.github.r1tschy.mergelab.accounts.ui

import com.github.r1tschy.mergelab.accounts.ui.AccountsDetailsLoader.Result.Error
import com.github.r1tschy.mergelab.accounts.ui.AccountsDetailsLoader.Result.Success
import com.intellij.collaboration.auth.Account
import com.intellij.collaboration.auth.AccountDetails

internal class LoadedAccountsDetailsProvider<in A : Account, out D : AccountDetails>(
    private val resultsSupplier: (A) -> AccountsDetailsLoader.Result<D>?
) : AccountsDetailsProvider<A, D> {

    override fun getDetails(account: A): D? =
        (resultsSupplier(account) as? Success)?.details

    override fun getErrorText(account: A): String? =
        (resultsSupplier(account) as? Error)?.error

    override fun checkErrorRequiresReLogin(account: A): Boolean =
        (resultsSupplier(account) as? Error)?.needReLogin ?: false
}
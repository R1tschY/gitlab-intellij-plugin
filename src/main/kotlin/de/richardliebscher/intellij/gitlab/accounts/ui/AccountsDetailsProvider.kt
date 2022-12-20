// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package de.richardliebscher.intellij.gitlab.accounts.ui

import com.intellij.util.concurrency.annotations.RequiresEdt
import de.richardliebscher.intellij.gitlab.accounts.Account
import de.richardliebscher.intellij.gitlab.accounts.AccountDetails
import org.jetbrains.annotations.Nls

internal interface AccountsDetailsProvider<in A : Account, out D : AccountDetails> {
    @RequiresEdt
    fun getDetails(account: A): D?

    @RequiresEdt
    @Nls
    fun getErrorText(account: A): String?

    @RequiresEdt
    fun checkErrorRequiresReLogin(account: A): Boolean
}
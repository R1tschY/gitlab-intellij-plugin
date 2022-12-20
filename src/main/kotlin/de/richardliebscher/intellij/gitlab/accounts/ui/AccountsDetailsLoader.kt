// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package de.richardliebscher.intellij.gitlab.accounts.ui

import de.richardliebscher.intellij.gitlab.accounts.Account
import de.richardliebscher.intellij.gitlab.accounts.AccountDetails
import kotlinx.coroutines.Deferred
import org.jetbrains.annotations.Nls
import java.awt.Image

interface AccountsDetailsLoader<in A : Account, out D : AccountDetails> {

    fun loadDetailsAsync(account: A): Deferred<Result<D>>

    fun loadAvatarAsync(account: A, url: String): Deferred<Image?>

    sealed class Result<out D : AccountDetails> {
        class Success<out D : AccountDetails>(val details: D) : Result<D>()
        class Error<out D : AccountDetails>(val error: @Nls String?, val needReLogin: Boolean) : Result<D>()
    }
}
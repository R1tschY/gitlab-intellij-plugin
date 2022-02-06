// Copyright 2022 Richard Liebscher.
// Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.github.r1tschy.mergelab.accounts

import com.intellij.openapi.components.service
import org.jetbrains.annotations.CalledInAny

class GitLabAuthService {
    private val accountsManager: GitLabAccountsManager get() = service()

    @CalledInAny
    fun hasAccounts(): Boolean = accountsManager.accounts.isNotEmpty()

    @CalledInAny
    fun getAccounts(): Set<GitLabAccount> = accountsManager.accounts
}
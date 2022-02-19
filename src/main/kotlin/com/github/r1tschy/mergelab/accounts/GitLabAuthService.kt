// Copyright 2022 Richard Liebscher.
// Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.github.r1tschy.mergelab.accounts

import com.github.r1tschy.mergelab.model.GitLabServerUrl
import com.github.r1tschy.mergelab.settings.GitLabSettings
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.util.concurrency.annotations.RequiresEdt
import org.jetbrains.annotations.CalledInAny

@Service
class GitLabAuthService {
    private val accountsManager: GitLabAccountsManager get() = service()

    @RequiresEdt
    fun hasAccounts(): Boolean = service<GitLabSettings>().hasAccounts()

    @RequiresEdt
    fun getAccounts(): Set<GitLabAccount> = accountsManager.accounts

    @RequiresEdt
    fun findAccountByRemoteUrl(remoteUrl: String): GitLabAccount? {
        return getAccounts().find { it.server.isReferencedBy(remoteUrl) }
    }

    @RequiresEdt
    fun findAccountByServerUrl(serverUrl: GitLabServerUrl): GitLabAccount? {
        return getAccounts().find { it.server == serverUrl }
    }

    @CalledInAny
    fun getToken(account: GitLabAccount): GitlabAccessToken? = accountsManager.findCredentials(account)
}
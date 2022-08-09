// Copyright 2022 Richard Liebscher.
// Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package de.richardliebscher.intellij.gitlab.accounts

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.util.concurrency.annotations.RequiresEdt
import de.richardliebscher.intellij.gitlab.model.GitLabServerUrl
import de.richardliebscher.intellij.gitlab.settings.GitLabAccounts
import org.jetbrains.annotations.CalledInAny

@Service
class GitLabAuthService {
    private val accountsManager: GitLabAccountsManager get() = service()

    @RequiresEdt
    fun hasAccounts(): Boolean = service<GitLabAccounts>().hasAccounts()

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
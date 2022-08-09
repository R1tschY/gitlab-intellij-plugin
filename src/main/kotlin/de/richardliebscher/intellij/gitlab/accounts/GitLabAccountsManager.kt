// Copyright 2022 Richard Liebscher.
// Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package de.richardliebscher.intellij.gitlab.accounts

import com.intellij.collaboration.auth.AccountManagerBase
import com.intellij.collaboration.auth.AccountsRepository
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import de.richardliebscher.intellij.gitlab.model.SERVICE_DISPLAY_NAME
import de.richardliebscher.intellij.gitlab.settings.GitLabAccounts

@Service
internal class GitLabAccountsManager : AccountManagerBase<GitLabAccount, GitlabAccessToken>(SERVICE_DISPLAY_NAME) {
    override fun accountsRepository(): AccountsRepository<GitLabAccount> = service<GitLabAccounts>()

    override fun deserializeCredentials(credentials: String): GitlabAccessToken {
        return GitlabAccessToken(credentials)
    }

    override fun serializeCredentials(credentials: GitlabAccessToken): String {
        return credentials.asString()
    }
}
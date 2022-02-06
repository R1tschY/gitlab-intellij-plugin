package com.github.r1tschy.mergelab.accounts

import com.github.r1tschy.mergelab.model.SERVICE_DISPLAY_NAME
import com.github.r1tschy.mergelab.settings.GitLabSettings
import com.intellij.collaboration.auth.AccountManagerBase
import com.intellij.collaboration.auth.AccountsRepository
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service

@Service
class GitLabAccountsManager : AccountManagerBase<GitLabAccount, GitLabAccessToken>(SERVICE_DISPLAY_NAME) {
    override fun accountsRepository(): AccountsRepository<GitLabAccount> = service<GitLabSettings>()

    override fun deserializeCredentials(credentials: String): GitLabAccessToken {
        return GitLabAccessToken(credentials)
    }

    override fun serializeCredentials(credentials: GitLabAccessToken): String {
        return credentials.asString()
    }
}
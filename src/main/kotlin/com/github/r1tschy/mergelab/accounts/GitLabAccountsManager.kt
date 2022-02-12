// Copyright 2022 Richard Liebscher.
// Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.github.r1tschy.mergelab.accounts

import com.github.r1tschy.mergelab.model.SERVICE_DISPLAY_NAME
import com.github.r1tschy.mergelab.settings.GitLabSettings
import com.intellij.collaboration.auth.AccountManagerBase
import com.intellij.collaboration.auth.AccountsRepository
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service

@Service
internal class GitLabAccountsManager : AccountManagerBase<GitLabAccount, GitlabAccessToken>(SERVICE_DISPLAY_NAME) {
    override fun accountsRepository(): AccountsRepository<GitLabAccount> = service<GitLabSettings>()

    override fun deserializeCredentials(credentials: String): GitlabAccessToken {
        return GitlabAccessToken(credentials)
    }

    override fun serializeCredentials(credentials: GitlabAccessToken): String {
        return credentials.asString()
    }
}
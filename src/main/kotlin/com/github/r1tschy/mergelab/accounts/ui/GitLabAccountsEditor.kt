// Copyright 2022 Richard Liebscher.
// Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.github.r1tschy.mergelab.accounts.ui

import com.github.r1tschy.mergelab.accounts.GitlabAccessToken
import com.github.r1tschy.mergelab.model.GitLabServerUrl
import com.intellij.openapi.actionSystem.DataKey

interface GitLabAccountsEditor {
    fun addAccount(server: GitLabServerUrl, token: GitlabAccessToken)
    fun hasAccount(server: GitLabServerUrl): Boolean

    companion object {
        val DATA_KEY: DataKey<GitLabAccountsEditor> =
            DataKey.create("com.github.r1tschy.mergelab.accounts.ui.GitLabAccountsEditor")
    }
}
package com.github.r1tschy.mergelab.accounts.ui

import com.github.r1tschy.mergelab.accounts.GitLabAccessToken
import com.github.r1tschy.mergelab.model.GitLabInstanceCoord
import com.intellij.openapi.actionSystem.DataKey

interface GitLabAccountsEditor {
    fun addAccount(server: GitLabInstanceCoord, token: GitLabAccessToken)
    fun hasAccount(server: GitLabInstanceCoord): Boolean

    companion object {
        val DATA_KEY: DataKey<GitLabAccountsEditor> =
            DataKey.create("com.github.r1tschy.mergelab.accounts.ui.GitLabAccountsEditor")
    }
}
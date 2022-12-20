// Copyright 2022 Richard Liebscher.
// Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package de.richardliebscher.intellij.gitlab.settings

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.SettingsCategory
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.xmlb.XmlSerializerUtil
import com.intellij.util.xmlb.annotations.Tag
import de.richardliebscher.intellij.gitlab.accounts.AccountsRepository
import de.richardliebscher.intellij.gitlab.accounts.GitLabAccount


@State(
    name = "de.richardliebscher.intellij.gitlab.GitLabAccounts",
    storages = [Storage("gitlab-repositories.xml")],
    category = SettingsCategory.TOOLS,
    reportStatistic = false
)
class GitLabAccounts : AccountsRepository<GitLabAccount>, PersistentStateComponent<GitLabAccountsState> {

    @Volatile
    private var state = GitLabAccountsState()

    override var accounts: Set<GitLabAccount>
        get() = state.accounts.toSet()
        set(value) {
            state.accounts = value.toList()
        }

    fun hasAccounts(): Boolean = state.accounts.isNotEmpty()

    override fun getState(): GitLabAccountsState = state

    override fun loadState(state: GitLabAccountsState) {
        XmlSerializerUtil.copyBean(state, this.state)
    }
}

data class GitLabAccountsState(
    @Tag("account")
    @Volatile
    var accounts: List<GitLabAccount> = emptyList()
)
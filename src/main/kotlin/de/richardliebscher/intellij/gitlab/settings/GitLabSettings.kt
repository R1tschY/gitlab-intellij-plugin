// Copyright 2022 Richard Liebscher.
// Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package de.richardliebscher.intellij.gitlab.settings

import com.intellij.collaboration.auth.AccountsRepository
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.xmlb.XmlSerializerUtil
import com.intellij.util.xmlb.annotations.Tag
import de.richardliebscher.intellij.gitlab.accounts.GitLabAccount


@State(
    name = "de.richardliebscher.intellij.gitlab.GitLabSettings",
    storages = [Storage("gitlab-repositories.xml")]
)
class GitLabSettings : AccountsRepository<GitLabAccount>, PersistentStateComponent<GitLabSettingsState> {

    private var state = GitLabSettingsState()

    override var accounts: Set<GitLabAccount>
        get() = state.accounts.toSet()
        set(value) {
            state.accounts = value.toList()
        }

    fun hasAccounts(): Boolean = state.accounts.isNotEmpty()

    override fun getState(): GitLabSettingsState = state

    override fun loadState(state: GitLabSettingsState) {
        XmlSerializerUtil.copyBean(state, this.state)
    }
}

class GitLabSettingsState {
    @Tag("account")
    @Volatile
    var accounts = emptyList<GitLabAccount>()
}
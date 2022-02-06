// Copyright 2022 Richard Liebscher.
// Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.github.r1tschy.mergelab.settings

import com.github.r1tschy.mergelab.accounts.GitLabAccount
import com.intellij.collaboration.auth.AccountsRepository
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.xmlb.XmlSerializerUtil
import com.intellij.util.xmlb.annotations.Tag


@State(
    name = "com.github.r1tschy.mergelab.settings.GitLabSettings",
    storages = [Storage("mergelab.xml")]
)
class GitLabSettings : AccountsRepository<GitLabAccount>, PersistentStateComponent<GitLabSettingsState> {

    private var state = GitLabSettingsState()

    override var accounts: Set<GitLabAccount>
        get() = state.accounts.toSet()
        set(value) {
            state.accounts = value.toList()
        }

    override fun getState(): GitLabSettingsState = state

    override fun loadState(state: GitLabSettingsState) {
        XmlSerializerUtil.copyBean(state, this.state)
    }
}

class GitLabSettingsState {
    @Tag("account")
    var accounts = emptyList<GitLabAccount>()
}
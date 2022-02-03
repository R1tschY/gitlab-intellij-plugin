package com.github.r1tschy.mergelab.settings

import com.github.r1tschy.mergelab.model.GitProtocol
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.xmlb.XmlSerializerUtil
import java.util.*


@State(
    name = "org.intellij.sdk.settings.AppSettingsState",
    storages = [Storage("mergelab.xml")]
)
class GitLabSettingsState : PersistentStateComponent<GitLabSettingsState?> {

    var serverUrl: String = "https://gitlab.com"
    var userPrivateAccessToken: String? = null
    var preferredProtocol: GitProtocol = GitProtocol.HTTPS

    override fun getState(): GitLabSettingsState {
        return this
    }

    override fun loadState(state: GitLabSettingsState) {
        XmlSerializerUtil.copyBean(state, this)
    }

    override fun equals(other: Any?): Boolean {
        if (javaClass != other?.javaClass) return false

        other as GitLabSettingsState
        return (serverUrl == other.serverUrl)
                && (userPrivateAccessToken == other.userPrivateAccessToken)
    }

    override fun hashCode(): Int {
        return Objects.hash(serverUrl, userPrivateAccessToken)
    }
}
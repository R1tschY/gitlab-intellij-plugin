package de.richardliebscher.intellij.gitlab.settings

import com.intellij.openapi.components.*
import de.richardliebscher.intellij.gitlab.model.GitProtocol

@State(
    name = "de.richardliebscher.intellij.gitlab.GitLabSettings",
    storages = [Storage("gitlab-repositories.xml")],
    category = SettingsCategory.TOOLS,
    reportStatistic = false
)
class GitLabSettings : PersistentStateComponentWithModificationTracker<GitLabSettingsState> {

    private var state = GitLabSettingsState()

    override fun getState(): GitLabSettingsState = state

    override fun getStateModificationCount(): Long = state.modificationCount

    override fun loadState(state: GitLabSettingsState) {
        this.state = state
    }

    companion object {
        fun getState() = service<GitLabSettings>().state
    }
}

class GitLabSettingsState : BaseState() {
    var cloneUsingSsh by property(false)

    fun getPreferredGitProtocol(): GitProtocol {
        return if (cloneUsingSsh) {
            GitProtocol.SSH
        } else {
            GitProtocol.HTTPS
        }
    }
}
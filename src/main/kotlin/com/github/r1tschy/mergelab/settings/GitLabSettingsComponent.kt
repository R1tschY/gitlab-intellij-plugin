package com.github.r1tschy.mergelab.settings

import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.panel
import javax.swing.JComponent
import javax.swing.JPanel


class GitLabSettingsComponent(settings: GitLabServerSettings) {
    private val mainPanel: DialogPanel
    private var mainFocusComponent: JComponent? = null

    init {
        mainPanel = panel {
            row("GitLab server URL:") {
                mainFocusComponent = textField()
                    .bindText(
                        { settings.getServerUrl() },
                        { serverUrl -> settings.setServerUrl(serverUrl) })
                    .component
            }

            row("Private access token (PAT):") {
                textField()
                    .bindText(
                        { settings.getAccessToken() ?: "" },
                        { pat -> settings.setAccessToken(pat) })
            }
        }
    }

    fun getPanel(): JPanel {
        return mainPanel
    }

    fun getPreferredFocusedComponent(): JComponent? {
        return mainFocusComponent
    }

    fun apply() {
        mainPanel.apply()
    }

    fun reset() {
        mainPanel.reset()
    }

    fun isModified(): Boolean {
        return mainPanel.isModified()
    }
}
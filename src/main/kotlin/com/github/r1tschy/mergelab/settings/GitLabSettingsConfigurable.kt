package com.github.r1tschy.mergelab.settings

import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.ConfigurationException
import com.intellij.openapi.util.NlsContexts.ConfigurableName
import org.jetbrains.annotations.Nls
import javax.swing.JComponent

class GitLabSettingsConfigurable : Configurable {
    private var component: GitLabSettingsComponent? = null

    @Nls(capitalization = Nls.Capitalization.Title)
    override fun getDisplayName(): @ConfigurableName String {
        return "GitLab"
    }

    override fun createComponent(): JComponent {
        val component = GitLabSettingsComponent(GitLabServerSettings())
        this.component = component
        return component.getPanel()
    }

    override fun getPreferredFocusedComponent(): JComponent? {
        return component?.getPreferredFocusedComponent()
    }

    override fun isModified(): Boolean {
        return component?.isModified() ?: false
    }

    @Throws(ConfigurationException::class)
    override fun apply() {
        component?.apply()
    }

    override fun reset() {
        component?.reset()
    }

    override fun disposeUIResources() {
        component = null
    }
}
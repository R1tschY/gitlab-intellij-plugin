// Copyright 2022 Richard Liebscher.
// Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.github.r1tschy.mergelab.accounts.ui

import com.github.r1tschy.mergelab.accounts.GitLabAccessToken
import com.github.r1tschy.mergelab.accounts.buildNewTokenUrl
import com.github.r1tschy.mergelab.exceptions.GitLabIllegalUrlException
import com.github.r1tschy.mergelab.model.DEFAULT_URL
import com.github.r1tschy.mergelab.model.GitLabInstanceCoord
import com.intellij.ide.BrowserUtil
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformCoreDataKeys.CONTEXT_COMPONENT
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.DocumentAdapter
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.COLUMNS_SHORT
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.columns
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.layout.ComponentPredicate
import java.awt.Component
import javax.swing.JComponent
import javax.swing.event.DocumentEvent

class AddGitLabAccountFromToken : DumbAwareAction() {
    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = e.getData(GitLabAccountsEditor.DATA_KEY) != null
    }

    override fun actionPerformed(e: AnActionEvent) {
        val accountsEditor = e.getData(GitLabAccountsEditor.DATA_KEY)!!
        val dialog = AddGitLabAccountFromTokenDialog(e.project, e.getData(CONTEXT_COMPONENT), accountsEditor)

        if (dialog.showAndGet()) {
            accountsEditor.addAccount(
                GitLabInstanceCoord.parse(dialog.server), GitLabAccessToken(dialog.token))
        }
    }
}

class AddGitLabAccountFromTokenDialog(
    project: Project?, parent: Component?, accountsEditor: GitLabAccountsEditor
) : DialogWrapper(project, parent, false, IdeModalityType.PROJECT) {
    var server: String = DEFAULT_URL
    var token: String = ""

    private val serverTextField = JBTextField()
    private val tokenTextField = JBTextField()

    init {
        title = "Log In to GitLab"
        setOKButtonText("Log In")

        init()
    }

    override fun getPreferredFocusedComponent(): JComponent {
        return tokenTextField
    }

    override fun createCenterPanel(): JComponent {
        return panel {
            row("Server:") {
                cell(serverTextField)
                    .columns(COLUMNS_SHORT)
                    .bindText(::server)
                    .validationOnInput { if (it.text.isNullOrBlank()) { ValidationInfo("Token should not be empty", it) } else null }
                    .validationOnApply { if (!isValidServerUrl(serverTextField)) { ValidationInfo("Invalid URL", it) } else null }
            }

            row("Token:") {
                cell(tokenTextField)
                    .columns(COLUMNS_SHORT)
                    .bindText(::token)

                button("Generate\u2026") { browseNewToken() }
                    .enabledIf(ServerUrlValidPredicate(serverTextField))
            }
        }
    }

    private fun browseNewToken() {
        BrowserUtil.browse(buildNewTokenUrl(GitLabInstanceCoord.parse(serverTextField.text.trim())))
    }
}


private class ServerUrlValidPredicate(val serverTextField: JBTextField): ComponentPredicate() {
    override fun addListener(listener: (Boolean) -> Unit) {
        serverTextField.document.addDocumentListener(object : DocumentAdapter() {
            override fun textChanged(e: DocumentEvent) = listener(invoke())
        })
    }

    override fun invoke(): Boolean = isValidServerUrl(serverTextField)
}

private fun isValidServerUrl(serverTextField: JBTextField): Boolean {
    return try {
        GitLabInstanceCoord.parse(serverTextField.text.trim())
        true
    } catch (e: GitLabIllegalUrlException) {
        false
    }
}


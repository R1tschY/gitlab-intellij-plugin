// Copyright 2022 Richard Liebscher.
// Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.github.r1tschy.mergelab.accounts.ui

import com.github.r1tschy.mergelab.accounts.GitLabAccessToken
import com.github.r1tschy.mergelab.model.GitLabInstanceCoord
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformCoreDataKeys.CONTEXT_COMPONENT
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.panel
import java.awt.Component
import javax.swing.JComponent

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
    var server: String = ""
    var token: String = ""

    init {
        title = "Log In to GitLab"
        setOKButtonText("Log In")

        init()
    }

    override fun createCenterPanel(): JComponent {
        return panel {
            row("Server:") {
                textField().bindText(::server)
                    .validationOnInput { if (it.text.isNullOrBlank()) ValidationInfo("Token should not be empty", it) else null }
            }

            row("Token:") {
                textField().bindText(::token)
            }
        }
    }
}
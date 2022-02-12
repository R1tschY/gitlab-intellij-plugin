// Copyright 2022 Richard Liebscher.
// Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.github.r1tschy.mergelab.accounts.ui

import com.github.r1tschy.mergelab.accounts.GitlabAccessToken
import com.github.r1tschy.mergelab.accounts.UserDetails
import com.github.r1tschy.mergelab.accounts.buildNewTokenUrl
import com.github.r1tschy.mergelab.api.GitLabApiService
import com.github.r1tschy.mergelab.exceptions.GitLabIllegalUrlException
import com.github.r1tschy.mergelab.model.GitLabServerUrl
import com.intellij.collaboration.async.CompletableFutureUtil
import com.intellij.collaboration.async.CompletableFutureUtil.handleOnEdt
import com.intellij.collaboration.async.CompletableFutureUtil.submitIOTask
import com.intellij.ide.BrowserUtil
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformCoreDataKeys.CONTEXT_COMPONENT
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.components.service
import com.intellij.openapi.progress.EmptyProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.util.Disposer
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
        val dialog = AddGitLabAccountFromTokenDialog(e.project, e.getData(CONTEXT_COMPONENT))

        if (dialog.showAndGet()) {
            accountsEditor.addAccount(
                GitLabServerUrl.parse(dialog.server), GitlabAccessToken(dialog.token)
            )
        }
    }
}


private class AddGitLabAccountFromTokenDialog(
    project: Project?, parent: Component?
) : DialogWrapper(project, parent, false, IdeModalityType.PROJECT) {
    var token: String = ""
    var server: String = ""
    var userDetails: UserDetails? = null

    private val serverTextField = JBTextField()
    private val tokenTextField = JBTextField()

    private var tokenError: String? = null

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
                    .validationOnInput {
                        if (it.text.isNullOrBlank()) {
                            error("Token should not be empty")
                        } else null
                    }
                    .validationOnInput {
                        if (!isValidServerUrl(serverTextField)) {
                            error("Invalid URL")
                        } else null
                    }
            }

            row("Token:") {
                cell(tokenTextField)
                    .columns(COLUMNS_SHORT)
                    .bindText(::token)
                    .validationOnApply { tokenError?.let { error(it) } }

                button("Generate\u2026") { browseNewToken() }
                    .enabledIf(ServerUrlValidPredicate(serverTextField))
            }
        }
    }

    private fun browseNewToken() {
        BrowserUtil.browse(buildNewTokenUrl(getServerUrl()))
    }

    private fun getServerUrl() = GitLabServerUrl.parse(serverTextField.text.trim())

    private fun getAccessToken() = GitlabAccessToken(tokenTextField.text.trim())

    private fun setBusy(busy: Boolean) {
        serverTextField.isEnabled = !busy
        tokenTextField.isEnabled = !busy
    }

    private fun setError(throwable: Throwable) {
        this.tokenError = throwable.localizedMessage
    }

    override fun doOKAction() {
        this.tokenError = null

        val modalityState = ModalityState.stateForComponent(contentPanel)
        val emptyProgressIndicator = EmptyProgressIndicator(modalityState)
        Disposer.register(disposable) { emptyProgressIndicator.cancel() }

        val serverUrl = getServerUrl()
        val token = getAccessToken()

        setBusy(true)
        service<ProgressManager>()
            .submitIOTask(emptyProgressIndicator) {
                service<GitLabApiService>().apiFor(getServerUrl(), getAccessToken()).getUserDetails(it)
            }
            .handleOnEdt(modalityState) { userDetails: UserDetails?, error: Throwable? ->
                setBusy(false)

                if (error != null) {
                    val realError = CompletableFutureUtil.extractError(error)
                    if (!CompletableFutureUtil.isCancellation(realError)) {
                        setError(realError)
                        startTrackingValidation()
                    }
                } else {
                    userDetails!!
                    this.userDetails = userDetails
                    this.server = serverUrl.toUrl()
                    this.token = token.asString()

                    close(OK_EXIT_CODE, true)
                }
            }
    }
}


private class ServerUrlValidPredicate(val serverTextField: JBTextField) : ComponentPredicate() {
    override fun addListener(listener: (Boolean) -> Unit) {
        serverTextField.document.addDocumentListener(object : DocumentAdapter() {
            override fun textChanged(e: DocumentEvent) = listener(invoke())
        })
    }

    override fun invoke(): Boolean = isValidServerUrl(serverTextField)
}

private fun isValidServerUrl(serverTextField: JBTextField): Boolean {
    return try {
        GitLabServerUrl.parse(serverTextField.text.trim())
        true
    } catch (e: GitLabIllegalUrlException) {
        false
    }
}


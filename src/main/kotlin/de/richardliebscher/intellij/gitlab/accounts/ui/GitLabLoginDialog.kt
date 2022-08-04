// Copyright 2022 Richard Liebscher.
// Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package de.richardliebscher.intellij.gitlab.accounts.ui

import com.intellij.collaboration.async.CompletableFutureUtil
import com.intellij.collaboration.async.CompletableFutureUtil.handleOnEdt
import com.intellij.collaboration.async.CompletableFutureUtil.submitIOTask
import com.intellij.ide.BrowserUtil
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.components.service
import com.intellij.openapi.progress.EmptyProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.util.Disposer
import com.intellij.ui.DocumentAdapter
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.COLUMNS_SHORT
import com.intellij.ui.dsl.builder.columns
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.builder.text
import com.intellij.ui.dsl.gridLayout.HorizontalAlign
import com.intellij.ui.layout.ComponentPredicate
import de.richardliebscher.intellij.gitlab.accounts.GitLabAccount
import de.richardliebscher.intellij.gitlab.accounts.GitlabAccessToken
import de.richardliebscher.intellij.gitlab.accounts.REQUIRED_SCOPES
import de.richardliebscher.intellij.gitlab.accounts.buildNewTokenUrl
import de.richardliebscher.intellij.gitlab.api.GitLabApiService
import de.richardliebscher.intellij.gitlab.api.UserDetails
import de.richardliebscher.intellij.gitlab.exceptions.GitLabIllegalUrlException
import de.richardliebscher.intellij.gitlab.model.DEFAULT_SERVER_URL
import de.richardliebscher.intellij.gitlab.model.DEFAULT_URL
import de.richardliebscher.intellij.gitlab.model.GitLabServerUrl
import java.awt.Component
import javax.swing.JComponent
import javax.swing.event.DocumentEvent

/**
 * Information about successful login.
 */
data class GitlabLoginData(val server: GitLabServerUrl, val userDetails: UserDetails, val token: GitlabAccessToken)


/**
 * Show dialog to add new account.
 */
fun showAddAccountDialog(project: Project, parent: Component?): GitlabLoginData? {
    return LoginWithTokenDialog(GitlabLoginRequest(), project, parent).showAndGetLoginData()
}


/**
 * Show dialog to change access token.
 */
fun showEditTokenDialog(account: GitLabAccount, project: Project, parent: Component?): GitlabLoginData? {
    val request = GitlabLoginRequest(
        server = account.server,
        isServerEditable = false
    )
    return LoginWithTokenDialog(request, project, parent).showAndGetLoginData()
}


/**
 * Request data for LoginWithTokenDialog.
 */
internal data class GitlabLoginRequest(
    val title: String? = null,
    val server: GitLabServerUrl? = DEFAULT_SERVER_URL,
    val isServerEditable: Boolean = true,
    val requiredLoginName: String? = null,
)


/**
 * Request login information for login with PAT (Personal Access Token).
 */
internal class LoginWithTokenDialog(
    private val request: GitlabLoginRequest, val project: Project, parent: Component?
) : DialogWrapper(project, parent, false, IdeModalityType.PROJECT) {
    private var result: GitlabLoginData? = null
    private var tokenError: String? = null

    private val serverTextField = JBTextField()
    private val tokenTextField = JBTextField()

    init {
        title = request.title ?: "Log In To GitLab Using Personal Access Token"
        setOKButtonText("Log In")

        init()
    }

    override fun getPreferredFocusedComponent(): JComponent {
        return tokenTextField
    }

    override fun createCenterPanel(): JComponent {
        return panel {
            row("Server:") {
                resizableRow()
                cell(serverTextField)
                    .horizontalAlign(HorizontalAlign.FILL)
                    .enabled(request.isServerEditable)
                    .text(request.server?.toUrl() ?: DEFAULT_URL)
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
                    .comment("Following scopes are required: ${REQUIRED_SCOPES.joinToString(", ")}")
                    .columns(COLUMNS_SHORT)
                    .validationOnApply { tokenError?.let { error(it) } }

                button("Create New\u2026") { browseNewToken() }
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
                service<GitLabApiService>().apiFor(serverUrl, getAccessToken()).getUserDetails(it)
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
                    this.result = GitlabLoginData(
                        server = serverUrl,
                        userDetails = userDetails!!,
                        token = token
                    )

                    close(OK_EXIT_CODE, true)
                }
            }
    }

    fun showAndGetLoginData(): GitlabLoginData? {
        return if (showAndGet()) {
            result!!
        } else {
            null
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


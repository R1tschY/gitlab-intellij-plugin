package com.github.r1tschy.mergelab.settings

import com.github.r1tschy.mergelab.model.GitLabInstanceCoord
import com.github.r1tschy.mergelab.model.GitLabServer
import com.github.r1tschy.mergelab.model.GitProtocol
import com.intellij.credentialStore.CredentialAttributes
import com.intellij.credentialStore.Credentials
import com.intellij.credentialStore.generateServiceName
import com.intellij.ide.passwordSafe.PasswordSafe
import com.intellij.openapi.application.ApplicationManager


private const val CRED_SERVICE_NAME = "com.github.r1tschy.mergelab.GitLab"

class GitLabServerSettings : GitLabServer {
    private fun getApplicationState(): GitLabSettingsState {
        return ApplicationManager.getApplication().getService(GitLabSettingsState::class.java)
    }

    override fun getServerUrl(): String {
        return getApplicationState().serverUrl
    }

    fun getInstance(): GitLabInstanceCoord {
        return GitLabInstanceCoord.parse(getApplicationState().serverUrl)
    }

    private fun createAccessTokenCredentialAttributes(): CredentialAttributes {
        return CredentialAttributes(
            generateServiceName(CRED_SERVICE_NAME, getServerUrl())
        )
    }

    override fun getAccessToken(): String? {
        return PasswordSafe.instance.getPassword(createAccessTokenCredentialAttributes())
    }

    override fun getPreferredProtocol(): GitProtocol {
        return getApplicationState().preferredProtocol
    }

    fun setServerUrl(serverUrl: String) {
        getApplicationState().serverUrl = serverUrl
    }

    fun setAccessToken(accessToken: String) {
        val credentialAttributes = createAccessTokenCredentialAttributes()
        val credentials = Credentials(null, accessToken)
        PasswordSafe.instance.set(credentialAttributes, credentials)
    }

    fun setPreferredProtocol(protocol: GitProtocol) {
        getApplicationState().preferredProtocol = protocol
    }
}
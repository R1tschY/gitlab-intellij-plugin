// Copyright 2022 Richard Liebscher.
// Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package de.richardliebscher.intellij.gitlab.accounts.ui

import com.intellij.openapi.components.service
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import de.richardliebscher.intellij.gitlab.accounts.GitLabAccount
import de.richardliebscher.intellij.gitlab.accounts.GitLabAuthService
import de.richardliebscher.intellij.gitlab.accounts.GitLabAvatarService
import de.richardliebscher.intellij.gitlab.accounts.ui.AccountsDetailsLoader.Result
import de.richardliebscher.intellij.gitlab.api.GitLabApi
import de.richardliebscher.intellij.gitlab.api.GitLabApiService
import de.richardliebscher.intellij.gitlab.api.UserDetails
import de.richardliebscher.intellij.gitlab.exceptions.UnauthorizedAccessException
import de.richardliebscher.intellij.gitlab.utils.CompletableFutureUtil.submitIOTask
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.future.asDeferred
import java.awt.Image

internal class GitLabAccountsDetailsLoader(
    private val indicatorsProvider: ProgressIndicatorsProvider,
    private val accountsModel: GitLabAccountsListModel,
) : AccountsDetailsLoader<GitLabAccount, UserDetails> {

    override fun loadDetailsAsync(account: GitLabAccount): Deferred<Result<UserDetails>> {
        val api = getApiClient(account) ?: return CompletableDeferred<Result<UserDetails>>(
            Result.Error("Access token is not valid", true)
        )

        return ProgressManager.getInstance().submitIOTask(indicatorsProvider, true) {
            doLoadDetails(api, it)
        }.asDeferred()
    }

    private fun doLoadDetails(api: GitLabApi, indicator: ProgressIndicator)
            : Result<UserDetails> {
        // TODO: check scopes
        return try {
            Result.Success(api.getUserDetails(indicator))
        } catch (e: UnauthorizedAccessException) {
            Result.Error("Access token is not valid", true)
        } catch (e: Throwable) {
            Result.Error(e.localizedMessage ?: e.message, false)
        }
    }

    override fun loadAvatarAsync(account: GitLabAccount, url: String): Deferred<Image?> {
        val api = getApiClient(account) ?: return CompletableDeferred<Image?>(null).apply { complete(null) }

        return ProgressManager.getInstance().submitIOTask(indicatorsProvider) { indicator ->
            service<GitLabAvatarService>().loadAvatarSync(api, url, indicator)
        }.asDeferred()
    }

    private fun getApiClient(account: GitLabAccount): GitLabApi? {
        return (accountsModel.newCredentials[account] ?: service<GitLabAuthService>().getToken(account))
            ?.let { service<GitLabApiService>().apiFor(account.server, it) }
    }
}
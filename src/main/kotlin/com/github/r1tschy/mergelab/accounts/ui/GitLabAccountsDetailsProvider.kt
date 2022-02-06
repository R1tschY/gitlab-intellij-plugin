// Copyright 2022 Richard Liebscher.
// Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.github.r1tschy.mergelab.accounts.ui

import com.github.r1tschy.mergelab.accounts.GitLabAccount
import com.github.r1tschy.mergelab.accounts.GitLabAccountsManager
import com.github.r1tschy.mergelab.accounts.GitLabUser
import com.intellij.collaboration.auth.ui.LoadingAccountsDetailsProvider
import com.intellij.collaboration.util.ProgressIndicatorsProvider
import com.intellij.openapi.progress.ProgressIndicator
import java.awt.Image
import java.util.concurrent.CompletableFuture

class GitLabAccountsDetailsProvider(
    progressIndicatorsProvider: ProgressIndicatorsProvider,
    private val accountManager: GitLabAccountsManager,
    private val accountsModel: GitLabAccountsModel
): LoadingAccountsDetailsProvider<GitLabAccount, GitLabUser>(progressIndicatorsProvider) {
    override fun scheduleLoad(
        account: GitLabAccount,
        indicator: ProgressIndicator
    ): CompletableFuture<DetailsLoadingResult<GitLabUser>> {
        val accessToken = accountsModel.newCredentials.getOrElse(account) {
            accountManager.findCredentials(account)
        } ?: return CompletableFuture.completedFuture(error("Missing access token"))

        // TODO: load user details
        return CompletableFuture.completedFuture(success(GitLabUser("<unknown>"), null))
    }

    private fun error(message: String) = DetailsLoadingResult<GitLabUser>(null, null, message, true)
    private fun success(user: GitLabUser, image: Image?) = DetailsLoadingResult(user, image, null, false)
}
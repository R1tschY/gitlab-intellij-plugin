// Copyright 2022 Richard Liebscher.
// Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.github.r1tschy.mergelab.accounts

import com.github.r1tschy.mergelab.GitlabBundle
import com.github.r1tschy.mergelab.model.SERVICE_DISPLAY_NAME
import com.github.r1tschy.mergelab.ui.Notifications.MISSING_DEFAULT_ACCOUNT
import com.intellij.collaboration.auth.PersistentDefaultAccountHolder
import com.intellij.notification.NotificationAction
import com.intellij.notification.NotificationType
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.StoragePathMacros
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.VcsNotifier

@Suppress("UNCHECKED_CAST")
@State(
    name = "com.github.r1tschy.mergelab.accounts.GitLabProjectDefaultAccountHolder",
    storages = [Storage(StoragePathMacros.WORKSPACE_FILE)],
    reportStatistic = false
)
internal class GitLabProjectDefaultAccountState(project: Project) :
    PersistentDefaultAccountHolder<GitLabAccount>(project) {

    override fun accountManager() = service<GitLabAccountsManager>()

    override fun notifyDefaultAccountMissing() = runInEdt {
        val title = GitlabBundle.message("accounts.default.missing")
        LOG.info("${title}: ${""}")
        VcsNotifier.IMPORTANT_ERROR_NOTIFICATION.createNotification(title, NotificationType.WARNING)
            .setDisplayId(MISSING_DEFAULT_ACCOUNT)
            .addAction(createConfigureAction(project))
            .notify(project)
    }

    private fun createConfigureAction(project: Project): NotificationAction {
        return NotificationAction.createSimple("Configure\u2026") {
            ShowSettingsUtil.getInstance().showSettingsDialog(project, SERVICE_DISPLAY_NAME)
        }
    }

    companion object {
        val LOG = thisLogger()
    }
}
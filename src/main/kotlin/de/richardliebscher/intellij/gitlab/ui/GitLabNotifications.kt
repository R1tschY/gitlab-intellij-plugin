// Copyright 2022 Richard Liebscher.
// Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package de.richardliebscher.intellij.gitlab.ui

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NlsContexts.NotificationContent
import com.intellij.openapi.util.NlsContexts.NotificationTitle
import com.intellij.openapi.vcs.VcsNotifier

object GitLabNotifications {
    private val LOG: Logger = Logger.getInstance(GitLabNotifications::class.java)

    fun showError(
        project: Project,
        displayId: String?,
        @NotificationTitle title: String,
        @NotificationContent message: String
    ) {
        LOG.warn("$title: $message")
        VcsNotifier.getInstance(project).notifyError(displayId, title, message)
    }

    const val OPEN_IN_BROWSER_FILE_IS_NOT_IN_REPO = "de.richardliebscher.intellij.gitlab.open_in_browser.file_is_not_in_repo"
    const val OPEN_IN_BROWSER_REPO_HAS_NO_HEAD = "de.richardliebscher.intellij.gitlab.open_in_browser.repo_has_no_head"
    const val FAILED_GETTING_MERGE_REQUESTS_FOR_BRANCH = "de.richardliebscher.intellij.gitlab.mr.failed_getting_mr_for_branch"
    const val LOGIN_FAILED = "de.richardliebscher.intellij.gitlab.accounts.login_failed"
    const val MISSING_DEFAULT_ACCOUNT = "de.richardliebscher.intellij.gitlab.missing_default_account"
    const val CLONE_UNABLE_TO_CREATE_DESTINATION_DIR = "de.richardliebscher.intellij.gitlab.clone.unable_to_create_destination_dir"
    const val CLONE_UNABLE_TO_FIND_DESTINATION = "de.richardliebscher.intellij.gitlab.clone.unable_to_find_destination"
}
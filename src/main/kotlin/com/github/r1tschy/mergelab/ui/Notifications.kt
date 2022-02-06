// Copyright 2022 Richard Liebscher.
// Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.github.r1tschy.mergelab.ui

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NlsContexts.NotificationContent
import com.intellij.openapi.util.NlsContexts.NotificationTitle
import com.intellij.openapi.vcs.VcsNotifier

object Notifications {
    private val LOG: Logger = Logger.getInstance(Notifications::class.java)

    fun showError(
        project: Project,
        displayId: String?,
        @NotificationTitle title: String,
        @NotificationContent message: String
    ) {
        LOG.warn("$title: $message")
        VcsNotifier.getInstance(project).notifyError(displayId, title, message)
    }

    const val OPEN_IN_BROWSER_FILE_IS_NOT_IN_REPO = "com.github.r1tschy.mergelab.open_in_browser.file_is_not_in_repo"
    const val OPEN_IN_BROWSER_REPO_HAS_NO_HEAD = "com.github.r1tschy.mergelab.open_in_browser.repo_has_no_head"
    const val MISSING_DEFAULT_ACCOUNT = "com.github.r1tschy.mergelab.missing_default_account"
}
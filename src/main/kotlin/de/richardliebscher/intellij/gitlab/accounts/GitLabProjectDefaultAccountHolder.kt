// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package de.richardliebscher.intellij.gitlab.accounts

import com.intellij.collaboration.auth.PersistentDefaultAccountHolder
import com.intellij.openapi.components.*
import com.intellij.openapi.project.Project

@Service
@State(name = "de.richardliebscher.intellij.gitlab.GitLabDefaultAccount", storages = [Storage(StoragePathMacros.WORKSPACE_FILE)], reportStatistic = false)
internal class GitLabProjectDefaultAccountHolder(project: Project) : PersistentDefaultAccountHolder<GitLabAccount>(project) {
    override fun accountManager() = service<GitLabAccountsManager>()
    override fun notifyDefaultAccountMissing() {

    }
}
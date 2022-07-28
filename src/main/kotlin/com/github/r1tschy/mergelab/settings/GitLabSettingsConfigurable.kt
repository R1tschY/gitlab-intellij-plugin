// Copyright 2000-2022 JetBrains s.r.o. and contributors.
// Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.github.r1tschy.mergelab.settings

import com.github.r1tschy.mergelab.accounts.GitLabAccountsManager
import com.github.r1tschy.mergelab.accounts.GitLabProjectDefaultAccountHolder
import com.github.r1tschy.mergelab.accounts.ui.AccountsPanelFactory
import com.github.r1tschy.mergelab.accounts.ui.GitLabAccountsDetailsLoader
import com.github.r1tschy.mergelab.accounts.ui.GitLabAccountsListModel
import com.github.r1tschy.mergelab.model.SERVICE_DISPLAY_NAME
import com.intellij.collaboration.util.ProgressIndicatorsProvider
import com.intellij.openapi.components.service
import com.intellij.openapi.options.BoundConfigurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.util.Disposer
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.gridLayout.HorizontalAlign
import com.intellij.ui.dsl.gridLayout.VerticalAlign

internal class GitLabSettingsConfigurable(private val project: Project) : BoundConfigurable(SERVICE_DISPLAY_NAME) {
    override fun createPanel(): DialogPanel {
        val accountManager = service<GitLabAccountsManager>()
        val defaultAccountHolder = project.service<GitLabProjectDefaultAccountHolder>()

        val accountsModel = GitLabAccountsListModel(project)
        val indicatorsProvider = ProgressIndicatorsProvider().also {
            Disposer.register(disposable!!, it)
        }
        //val scope = CoroutineScope(SupervisorJob()).also { Disposer.register(disposable!!) { it.cancel() } }
        val detailsLoader = GitLabAccountsDetailsLoader(indicatorsProvider, accountsModel)
        val accountsPanelFactory =
            AccountsPanelFactory(accountManager, defaultAccountHolder, accountsModel, detailsLoader, disposable!!)

        return panel {
            row {
                accountsPanelFactory.accountsPanelCell(this, true)
                    .horizontalAlign(HorizontalAlign.FILL)
                    .verticalAlign(VerticalAlign.FILL)
            }.resizableRow()
        }
    }
}


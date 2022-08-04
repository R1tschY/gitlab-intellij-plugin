// Copyright 2000-2022 JetBrains s.r.o. and contributors.
// Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package de.richardliebscher.intellij.gitlab.settings

import com.intellij.collaboration.util.ProgressIndicatorsProvider
import com.intellij.openapi.components.service
import com.intellij.openapi.options.BoundConfigurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.util.Disposer
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.gridLayout.HorizontalAlign
import com.intellij.ui.dsl.gridLayout.VerticalAlign
import de.richardliebscher.intellij.gitlab.accounts.GitLabAccountsManager
import de.richardliebscher.intellij.gitlab.accounts.GitLabProjectDefaultAccountHolder
import de.richardliebscher.intellij.gitlab.accounts.ui.AccountsPanelFactory
import de.richardliebscher.intellij.gitlab.accounts.ui.GitLabAccountsDetailsLoader
import de.richardliebscher.intellij.gitlab.accounts.ui.GitLabAccountsListModel
import de.richardliebscher.intellij.gitlab.model.SERVICE_DISPLAY_NAME

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


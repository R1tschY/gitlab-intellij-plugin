// Copyright 2000-2020 JetBrains s.r.o.
// Copyright 2022 Richard Liebscher.
// Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.github.r1tschy.mergelab.settings

import com.github.r1tschy.mergelab.accounts.ui.gitlabAccountsPanel
import com.github.r1tschy.mergelab.model.SERVICE_DISPLAY_NAME
import com.intellij.collaboration.util.ProgressIndicatorsProvider
import com.intellij.openapi.options.BoundConfigurable
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.util.Disposer
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.gridLayout.HorizontalAlign
import com.intellij.ui.dsl.gridLayout.VerticalAlign

internal class GitLabSettingsConfigurable : BoundConfigurable(SERVICE_DISPLAY_NAME) {

    override fun createPanel(): DialogPanel {
        val indicatorsProvider = ProgressIndicatorsProvider().also {
            Disposer.register(disposable!!, it)
        }

        return panel {
            row {
                gitlabAccountsPanel(disposable!!, indicatorsProvider)
                    .horizontalAlign(HorizontalAlign.FILL)
                    .verticalAlign(VerticalAlign.FILL)
            }.resizableRow()
        }
    }
}


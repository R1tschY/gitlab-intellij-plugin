// Copyright 2022 Richard Liebscher.
// Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.github.r1tschy.mergelab.ui

import com.intellij.ui.components.JBList

class MergeRequestsView {
    val model = MergeRequestsListModel()
    private val list = JBList(model)

    init {
        list.emptyText.text = "There exists no merge requests"
        list.setPaintBusy(true)
    }
}
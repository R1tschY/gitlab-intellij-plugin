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
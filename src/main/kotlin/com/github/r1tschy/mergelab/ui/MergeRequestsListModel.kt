// Copyright 2022 Richard Liebscher.
// Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.github.r1tschy.mergelab.ui

import com.github.r1tschy.mergelab.model.query.MergeRequestPreview
import javax.swing.AbstractListModel
import javax.swing.ListModel
import javax.swing.event.ListDataListener

class MergeRequestsListModel : AbstractListModel<MergeRequestPreview>() {
    var mrs: List<MergeRequestPreview> = listOf()

    override fun getSize(): Int {
        return mrs.size
    }

    override fun getElementAt(index: Int): MergeRequestPreview {
        return mrs[index]
    }
}
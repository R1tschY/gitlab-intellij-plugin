// Copyright 2022 Richard Liebscher.
// Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package de.richardliebscher.intellij.gitlab.ui

import de.richardliebscher.intellij.gitlab.model.query.MergeRequestPreview
import javax.swing.AbstractListModel

class MergeRequestsListModel : AbstractListModel<MergeRequestPreview>() {
    var mrs: List<MergeRequestPreview> = listOf()

    override fun getSize(): Int {
        return mrs.size
    }

    override fun getElementAt(index: Int): MergeRequestPreview {
        return mrs[index]
    }
}
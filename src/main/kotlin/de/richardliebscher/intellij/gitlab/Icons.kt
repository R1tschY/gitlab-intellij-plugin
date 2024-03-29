// Copyright 2022 Richard Liebscher.
// Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package de.richardliebscher.intellij.gitlab

import com.intellij.openapi.util.IconLoader
import javax.swing.Icon

private fun icon(name: String): Icon {
    return IconLoader.getIcon("/icons/$name.svg", GitLabIcons::class.java)
}

object GitLabIcons {
    val GITLAB = icon("gitlab_logo")
    val MERGE_REQUEST = icon("merge-request")
    val MERGE_REQUEST_CLOSE = icon("merge-request-close")
}
// Copyright 2022 Richard Liebscher.
// Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package de.richardliebscher.intellij.gitlab.utils

import com.intellij.openapi.editor.Editor
import de.richardliebscher.intellij.gitlab.model.GitLabProjectCoord
import org.apache.commons.httpclient.util.URIUtil
import org.apache.http.client.utils.URIBuilder


fun buildCommitUrl(projectCoord: GitLabProjectCoord, hash: String): String {
    return "${projectCoord.toUrl()}/-/commit/${URIUtil.encodePath(hash)}$"
}


fun buildFileUrl(editor: Editor?, projectCoord: GitLabProjectCoord, ref: String, file: String?): String {
    if (file == null) {
        return "${projectCoord.toUrl()}/-/tree/${URIUtil.encodePath(ref)}"
    }

    val result = StringBuilder()
    result.append(projectCoord.toUrl()).append("/-/blob/").append(URIUtil.encodePath(ref)).append('/')
        .append(URIUtil.encodePath(file))
    if (editor != null && editor.document.lineCount >= 1) {
        val caret = editor.caretModel.currentCaret
        val begin = caret.selectionStartPosition.line + 1
        val end = caret.selectionEndPosition.line + 1
        result.append("#L").append(begin)
        if (begin != end) {
            result.append("-L").append(end)
        }
    }
    return result.toString()
}

fun buildNewMergeRequestUrl(
    projectCoord: GitLabProjectCoord,
    sourceProjectId: Int?,
    sourceBranch: String?,
    targetProjectId: Int?,
    targetBranch: String?
): String {
    val uriBuilder = URIBuilder("${projectCoord.toUrl()}/-/merge_requests/new")
    if (sourceProjectId != null) {
        uriBuilder.addParameter("merge_request[source_project_id]", sourceProjectId.toString())
    }
    if (sourceBranch != null) {
        uriBuilder.addParameter("merge_request[source_branch]", sourceBranch)
    }
    if (targetProjectId != null) {
        uriBuilder.addParameter("merge_request[target_project_id]", targetProjectId.toString())
    }
    if (targetBranch != null) {
        uriBuilder.addParameter("merge_request[target_branch]", targetBranch)
    }
    return uriBuilder.toString()
}
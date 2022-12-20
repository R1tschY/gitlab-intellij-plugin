// Copyright 2022 Richard Liebscher.
// Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package de.richardliebscher.intellij.gitlab.utils

import com.intellij.openapi.editor.Editor
import com.intellij.util.io.URLUtil
import de.richardliebscher.intellij.gitlab.model.GitLabProjectCoord
import java.net.URLEncoder
import java.nio.charset.StandardCharsets


fun buildCommitUrl(projectCoord: GitLabProjectCoord, hash: String): String {
    return "${projectCoord.toUrl()}/-/commit/${URLUtil.encodePath(hash)}"
}


fun buildFileUrl(editor: Editor?, projectCoord: GitLabProjectCoord, ref: String, file: String?): String {
    if (file == null) {
        return "${projectCoord.toUrl()}/-/tree/${URLUtil.encodePath(ref)}"
    }

    val result = StringBuilder()
    result.append(projectCoord.toUrl()).append("/-/blob/").append(URLUtil.encodePath(ref)).append('/')
        .append(URLUtil.encodePath(file))
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
    val url = "${projectCoord.toUrl()}/-/merge_requests/new"

    val params = HashMap<String, String>()
    if (sourceProjectId != null) {
        params["merge_request[source_project_id]"] = sourceProjectId.toString()
    }
    if (sourceBranch != null) {
        params["merge_request[source_branch]"] = sourceBranch
    }
    if (targetProjectId != null) {
        params["merge_request[target_project_id]"] = targetProjectId.toString()
    }
    if (targetBranch != null) {
        params["merge_request[target_branch]"] = targetBranch
    }

    if (params.isNotEmpty()) {
        val stringBuilder = StringBuilder(url)
        stringBuilder.append('?')
        var first = true
        for (param in params) {
            if (first) {
                first = false
            } else {
                stringBuilder.append('&')
            }
            stringBuilder.append(param.key).append("=").append(URLEncoder.encode(param.value, StandardCharsets.UTF_8))
        }
        return stringBuilder.toString()
    } else {
        return url
    }
}
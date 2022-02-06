package com.github.r1tschy.mergelab.utils

import com.github.r1tschy.mergelab.model.GitLabProjectCoord
import com.intellij.openapi.editor.Editor
import org.apache.commons.httpclient.util.URIUtil
import java.lang.StringBuilder


fun buildCommitUrl(projectCoord: GitLabProjectCoord, hash: String): String {
    return "${projectCoord.toUrl()}/-/commit/$hash"
}


fun buildFileUrl(editor: Editor?, projectCoord: GitLabProjectCoord, ref: String, file: String?): String {
    if (file == null) {
        return "${projectCoord.toUrl()}/-/tree/$ref"
    }

    val result = StringBuilder()
    result.append(projectCoord.toUrl()).append("/-/blob/").append(ref).append('/').append(URIUtil.encodePath(file))
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
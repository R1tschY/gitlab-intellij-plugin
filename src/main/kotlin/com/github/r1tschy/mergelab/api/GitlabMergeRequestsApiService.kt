package com.github.r1tschy.mergelab.api

import com.github.r1tschy.mergelab.mergerequests.MergeRequest
import com.github.r1tschy.mergelab.model.GitLabProjectPath
import com.intellij.openapi.progress.ProgressIndicator
import java.io.IOException


interface GitlabMergeRequestsApiService {
    @Throws(IOException::class)
    fun findMergeRequestsUsingSourceBranch(
        project: GitLabProjectPath,
        sourceBranch: String,
        processIndicator: ProgressIndicator
    ): List<MergeRequest>
}
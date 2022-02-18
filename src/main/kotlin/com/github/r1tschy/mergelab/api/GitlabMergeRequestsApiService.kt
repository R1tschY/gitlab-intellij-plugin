package com.github.r1tschy.mergelab.api

import com.github.r1tschy.mergelab.mergerequests.PullRequest
import com.github.r1tschy.mergelab.model.GitLabProjectPath
import com.intellij.openapi.progress.ProgressIndicator


interface GitlabMergeRequestsApiService {
    fun findMergeRequestsUsingSourceBranch(
        project: GitLabProjectPath,
        sourceBranch: String,
        processIndicator: ProgressIndicator
    ): List<PullRequest>
}
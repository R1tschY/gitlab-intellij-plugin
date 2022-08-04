package de.richardliebscher.intellij.gitlab.api

import com.intellij.openapi.progress.ProgressIndicator
import de.richardliebscher.intellij.gitlab.mergerequests.MergeRequest
import de.richardliebscher.intellij.gitlab.model.GitLabProjectPath
import java.io.IOException


interface GitlabMergeRequestsApiService {
    @Throws(IOException::class)
    fun findMergeRequestsUsingSourceBranch(
        project: GitLabProjectPath,
        sourceBranch: String,
        processIndicator: ProgressIndicator
    ): List<MergeRequest>
}
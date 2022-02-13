package com.github.r1tschy.mergelab.api

import com.github.r1tschy.mergelab.model.GitLabProjectPath
import com.intellij.openapi.progress.ProgressIndicator

interface GitlabProtectedBranchesApiService {
    fun getProtectedBranches(project: GitLabProjectPath, processIndicator: ProgressIndicator): List<String>
}
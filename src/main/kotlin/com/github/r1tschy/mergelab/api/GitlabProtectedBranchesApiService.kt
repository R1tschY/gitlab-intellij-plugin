package com.github.r1tschy.mergelab.api

import com.github.r1tschy.mergelab.model.GitLabProjectPath
import com.intellij.openapi.progress.ProgressIndicator
import java.io.IOException

interface GitlabProtectedBranchesApiService {
    @Throws(IOException::class)
    fun getProtectedBranches(project: GitLabProjectPath, processIndicator: ProgressIndicator): List<String>
}